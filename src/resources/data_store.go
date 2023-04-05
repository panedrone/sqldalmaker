package dbal

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"errors"
	"fmt"
	"io"
	//"github.com/godror/godror"
	"reflect"
	"strconv"
	"strings"
	"time"
)

/*
	This file is a part of SQL DAL Maker project: https://sqldalmaker.sourceforge.net
	It demonstrates how to implement interface DataStore in Go + "database/sql".
	More about DataStore: https://sqldalmaker.sourceforge.net/data_store.html
	Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store.go

	Copy-paste this code to your project and change it for your needs.
	Improvements are welcome: sqldalmaker@gmail.com
*/

type DataStore interface {
	Open() error
	Close() error

	Begin(ctx context.Context) (txCtx context.Context, err error)
	Commit(txCtx *context.Context) error
	Rollback(txCtx *context.Context) error

	// the methods called by generated code:

	Insert(ctx context.Context, sqlStr string, aiNames string, args ...interface{}) (id interface{}, err error)
	Exec(ctx context.Context, sqlStr string, args ...interface{}) (rowsAffected int64, err error)
	Query(ctx context.Context, sqlStr string, args ...interface{}) (res interface{}, err error)
	QueryAll(ctx context.Context, sqlStr string, onRow func(interface{}), args ...interface{}) error
	QueryRow(ctx context.Context, sqlStr string, args ...interface{}) (row map[string]interface{}, err error)
	QueryAllRows(ctx context.Context, sqlStr string, onRow func(map[string]interface{}), args ...interface{}) error

	QueryByFA(ctx context.Context, sqlStr string, fa interface{}, args ...interface{}) error
	QueryAllByFA(ctx context.Context, sqlStr string, onRow func() (interface{}, func()), args ...interface{}) error

	PGFetch(cursor string) string
}

type OutParam struct {
	/*
		var outParam float64 // no need to init
		cxDao.SpTestOutParams(47, OutParam{Dest: &outParam})
		// cxDao.SpTestOutParams(47, &outParam) // <- this one is also ok for OUT parameters
		fmt.Println(outParam)
	*/

	// Dest is a pointer to the value that will be set to the result of the
	// stored procedure's OUT parameter.
	Dest interface{}
}

type InOutParam struct {
	/*
		inOutParam := 123.0 // must be initialized for INOUT
		cxDao.SpTestInoutParams(InOutParam{Dest: &inOutParam})
		fmt.Println(inOutParam)
	*/

	// Dest is a pointer to the value that will be set to the result of the
	// stored procedure's OUT parameter.
	Dest interface{}
}

type _DS struct {
	paramPrefix string
	db          *sql.DB
}

func _close(obj io.Closer) {
	_ = obj.Close()
}

func (ds *_DS) isPostgreSQL() bool {
	return ds.paramPrefix == "$"
}

func (ds *_DS) isOracle() bool {
	return ds.paramPrefix == ":"
}

func (ds *_DS) isSqlServer() bool {
	return ds.paramPrefix == "@p"
}

/*
	Implement the method initDb() in an external file. This is an example:

// file data_store_db.go

package dbal

import (
	"database/sql"
   	// _ "github.com/mattn/go-sqlite3"      // SQLite3
   	// _ "github.com/denisenkom/go-mssqldb" // SQL Server
   	// _ "github.com/godror/godror"			// Oracle
   	_ "github.com/go-sql-driver/mysql"      // MySQL
   	// _ "github.com/ziutek/mymysql/godrv"  // MySQL
   	// _ "github.com/lib/pq"                // PostgeSQL
)

var ds = &_DS{}

func (ds *_DS) initDb() (err error) {
	// === PostgeSQL ===========================
	ds.paramPrefix = "$"
	ds.db, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=disable")
	// ds.db, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=verify-full")
	// === SQLite3 =============================
	// ds.db, err = sql.Open("sqlite3", "./log.sqlite")
	// ds.db, err = sql.Open("sqlite3", "./northwindEF.sqlite")
	// === MySQL ===============================
	// ds.db, err = sql.Open("mysql", "root:root@/sakila")
	// ds.db, err = sql.Open("mymysql", "sakila/root/root")
	// === SQL Server ==========================
	// https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	// ensure sqlserver:// in beginning. this one is not valid:
	// ------ ds.db, err = sql.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// this one is ok:
	ds.paramPrefix = "@p"
	ds.db, err = sql.Open("sqlserver", "sqlserver://sa:root@localhost:1433?database=AdventureWorks2014")
	// === Oracle =============================
	// "github.com/godror/godror"
	//ds.paramPrefix = ":"
	//ds.db, err = sql.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
	return
}

func OpenDB() error {
	return ds.Open()
}

func CloseDB() error {
	return ds.Close()
}

func NewTasksDao() *TasksDao {
	return &TasksDao{ds: ds}
}

*/

func (ds *_DS) Open() error {
	return ds.initDb()
}

func (ds *_DS) Close() error {
	return ds.db.Close()
}

func getTx(ctx context.Context) *sql.Tx {
	tx, _ := ctx.Value("tx").(*sql.Tx)
	return tx
}

func (ds *_DS) Begin(ctx context.Context) (txCtx context.Context, err error) {
	tx := getTx(ctx)
	if tx != nil {
		return nil, errors.New("tx already started")
	}
	tx, err = ds.db.Begin()
	txCtx = context.WithValue(ctx, "tx", tx)
	return
}

func (ds *_DS) Commit(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return errors.New("no tx to commit")
	}
	tx := getTx(*txCtx)
	if tx == nil {
		return errors.New("ds.tx not started")
	}
	err = tx.Commit()
	*txCtx = nil // to prevent ds.tx.Rollback() in defer
	return
}

func (ds *_DS) Rollback(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return errors.New("no tx to rollback")
	}
	tx := getTx(*txCtx)
	if tx == nil {
		return nil // commit() was called, just do nothing:
	}
	err = tx.Rollback()
	*txCtx = nil
	return
}

func (ds *_DS) _query(ctx context.Context, sqlStr string, args ...interface{}) (*sql.Rows, error) {
	tx := getTx(ctx)
	if tx == nil {
		return ds.db.QueryContext(ctx, sqlStr, args...)
	}
	return tx.QueryContext(ctx, sqlStr, args...)
}

func (ds *_DS) _exec(ctx context.Context, sqlStr string, args ...interface{}) (sql.Result, error) {
	tx := getTx(ctx)
	if tx == nil {
		return ds.db.ExecContext(ctx, sqlStr, args...)
	}
	return tx.ExecContext(ctx, sqlStr, args...)
}

func (ds *_DS) PGFetch(cursor string) string {
	return fmt.Sprintf(`fetch all from "%s"`, cursor)
}

func (ds *_DS) _pgInsert(ctx context.Context, sqlStr string, aiNames string, args ...interface{}) (id interface{}, err error) {
	// fetching of multiple AI values is not implemented so far:
	sqlStr += " RETURNING " + aiNames
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return nil, err
	}
	defer _close(rows)
	if rows.Next() {
		var data interface{}
		err = rows.Scan(&data)
		return
	}
	err = errors.New("rows.Next() FAILED:" + sqlStr)
	return
}

func (ds *_DS) _oracleInsert(ctx context.Context, sqlStr string, aiNames string, args ...interface{}) (id interface{}, err error) {
	// fetching of multiple AI values is not implemented so far:
	sqlStr = fmt.Sprintf("%s returning %s  into :%d", sqlStr, aiNames, len(args)+1)
	// https://ddcode.net/2019/05/11/how-does-go-call-oracles-stored-procedures-and-get-the-return-value-of-the-stored-procedures/
	var id64 float64
	// var id64 interface{} --- error
	// var id64 int64 --- it works, but...
	args = append(args, sql.Out{Dest: &id64, In: false})
	// ----------------
	_, err = ds._exec(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	return id64, nil
}

func (ds *_DS) _mssqlInsert(ctx context.Context, sqlStr string, args ...interface{}) (id interface{}, err error) {
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// LastInsertId should not be used with this driver (or SQL Server) due to
	// how the TDS protocol works. Please use the OUTPUT Clause or add a select
	// ID = convert(bigint, SCOPE_IDENTITY()); to the end of your query (ref SCOPE_IDENTITY).
	//  This will ensure you are getting the correct ID and will prevent a network round trip.
	sqlStr += ";SELECT @@IDENTITY;"
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	if rows.Next() {
		var data interface{}
		err = rows.Scan(&data) // returns []uint8
		return data, err
	}
	err = errors.New("SELECT @@IDENTITY FAILED: " + sqlStr)
	return
}

func (ds *_DS) _defaultInsert(ctx context.Context, sqlStr string, args ...interface{}) (id interface{}, err error) {
	res, err := ds._exec(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	id, err = res.LastInsertId()
	return
}

func (ds *_DS) Insert(ctx context.Context, sqlStr string, aiNames string, args ...interface{}) (id interface{}, err error) {
	if len(aiNames) == 0 { // len(nil) == 0
		err = errors.New("_DS.insert is not applicable for aiNames = " + aiNames)
		return
	}
	// Builtin LastInsertId works only with MySQL and SQLite3
	sqlStr = ds._formatSQL(sqlStr)
	if ds.isPostgreSQL() {
		return ds._pgInsert(ctx, sqlStr, aiNames, args...)
	} else if ds.isSqlServer() {
		return ds._mssqlInsert(ctx, sqlStr, args...)
	} else if ds.isOracle() {
		// Oracle: specify AI values explicitly:
		// <crud-auto dto="ProjectInfo" table="PROJECTS" generated="P_ID"/>
		return ds._oracleInsert(ctx, sqlStr, aiNames, args...)
	}
	return ds._defaultInsert(ctx, sqlStr, args...)
}

func _isPtr(p interface{}) bool {
	val := reflect.ValueOf(p)
	kindOfJ := val.Kind()
	return kindOfJ == reflect.Ptr
}

func _pointsToNil(p interface{}) bool {
	switch d := p.(type) {
	case *interface{}:
		pointsTo := *d
		return pointsTo == nil
	}
	return false
}

func _validateDest(dest interface{}) (err error) {
	if dest == nil {
		err = errors.New("OutParam/InOutParam -> Dest is nil")
	} else if !_isPtr(dest) {
		err = errors.New("OutParam/InOutParam -> Dest must be a Ptr")
	} else if _pointsToNil(dest) {
		err = errors.New("OutParam/InOutParam -> Dest points to nil")
	}
	return
}

func (ds *_DS) _processExecParams(args []interface{}, onRowArr *[]interface{},
	queryArgs *[]interface{}) (implicitCursors bool, outCursors bool, err error) {
	implicitCursors = false
	outCursors = false
	for _, arg := range args {
		switch param := arg.(type) {
		case []func(map[string]interface{}):
			if outCursors {
				err = errors.New(fmt.Sprintf("Forbidden: %v", args))
				return
			}
			implicitCursors = true
			for _, fn := range param {
				*onRowArr = append(*onRowArr, fn)
			}
		case func(map[string]interface{}):
			if implicitCursors {
				err = errors.New(fmt.Sprintf("Forbidden: %v", args))
				return
			}
			outCursors = true
			*onRowArr = append(*onRowArr, param) // add single func
			var rows driver.Rows
			*queryArgs = append(*queryArgs, sql.Out{Dest: &rows, In: false})
		case []interface{}:
			if outCursors {
				err = errors.New(fmt.Sprintf("Forbidden: %v", args))
				return
			}
			implicitCursors = true
			for _, fn := range param {
				_, ok1 := fn.(func() (interface{}, func()))
				_, ok2 := fn.(func(map[string]interface{}))
				if !ok1 && !ok2 {
					err = errors.New(fmt.Sprintf("Expected: 'func(map[string]interface{})' || "+
						"'func() (interface{}, func())'. Got: %v", reflect.TypeOf(fn)))
					return
				}
				*onRowArr = append(*onRowArr, fn)
			}
		case func() ([]interface{}, func()):
			if implicitCursors {
				err = errors.New(fmt.Sprintf("Forbidden: %v", args))
				return
			}
			outCursors = true
			*onRowArr = append(*onRowArr, param) // add single func
			var rows driver.Rows
			*queryArgs = append(*queryArgs, sql.Out{Dest: &rows, In: false})
		case *OutParam:
			err = _validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: false})
		case OutParam:
			err = _validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: false})
		case *InOutParam:
			err = _validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: true})
		case InOutParam:
			err = _validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: true})
		default:
			if _isPtr(arg) {
				if _pointsToNil(arg) {
					err = errors.New("arg points to nil")
					return
				}
				if ds.isOracle() {
					*queryArgs = append(*queryArgs, sql.Out{Dest: arg, In: false})
				} else {
					*queryArgs = append(*queryArgs, arg) // PostgreSQL
				}
			} else {
				*queryArgs = append(*queryArgs, arg)
			}
		}
	}
	// Syntax like [on_test1:Test, on_test2:Test] is used to call SP with IMPLICIT cursors
	return
}

func (ds *_DS) _queryAllImplicitRcOracle(ctx context.Context, sqlStr string, onRowArr []interface{}, queryArgs ...interface{}) (err error) {
	rows, err := ds._query(ctx, sqlStr, queryArgs...)
	if err != nil {
		return
	}
	defer _close(rows)
	onRowIndex := 0
	for {
		// 1) unlike MySQL, NextResultSet must be done before _prepareFetch -> rows.Next()
		// 2) it does not work with multiple Implicit RC + early versions of Driver
		if !rows.NextResultSet() {
			break
		}
		switch onRow := onRowArr[onRowIndex].(type) {
		case func(map[string]interface{}):
			// re-detect columns for each ResultSet
			colNames, data, values, valuePointers, pfeErr := ds._prepareFetch(rows)
			if pfeErr != nil {
				err = pfeErr
				return
			}
			for rows.Next() {
				err = rows.Scan(valuePointers...)
				if err != nil {
					return
				}
				for i, colName := range colNames {
					data[colName] = values[i]
				}
				onRow(data)
			}
		case func() (interface{}, func()):
			err = ds._fetchRows(rows, onRow)
			if err != nil {
				return
			}
		default:
			return errors.New(fmt.Sprintf("Unexpected type: %v", reflect.TypeOf(onRow)))
		}
		onRowIndex++
	}
	return
}

func (ds *_DS) _fetchRows(rows *sql.Rows, onRow func() (interface{}, func())) (err error) {
	for rows.Next() {
		fa, onRowCompleted := onRow()
		switch _fa := fa.(type) {
		case []interface{}:
			err = rows.Scan(_fa...)
		default:
			err = errors.New(fmt.Sprintf("Unexpected type: %v", reflect.TypeOf(_fa)))
		}
		if err != nil {
			return
		}
		onRowCompleted()
	}
	return
}

func (ds *_DS) _queryAllImplicitRcMySQL(ctx context.Context, sqlStr string, onRowArr []interface{}, queryArgs ...interface{}) (err error) {
	rows, err := ds._query(ctx, sqlStr, queryArgs...)
	if err != nil {
		return
	}
	defer _close(rows)
	onRowIndex := 0
	for {
		switch onRow := onRowArr[onRowIndex].(type) {
		case func(map[string]interface{}):
			// re-detect columns for each ResultSet
			colNames, data, values, valuePointers, pfeErr := ds._prepareFetch(rows)
			if pfeErr != nil {
				err = pfeErr
				return
			}
			for rows.Next() {
				err = rows.Scan(valuePointers...)
				if err != nil {
					return
				}
				for i, colName := range colNames {
					data[colName] = values[i]
				}
				onRow(data)
			}
		case func() (interface{}, func()):
			err = ds._fetchRows(rows, onRow)
			if err != nil {
				return
			}
		default:
			return errors.New(fmt.Sprintf("Unexpected type: %v", reflect.TypeOf(onRow)))
		}
		if !rows.NextResultSet() {
			break
		}
		onRowIndex++
	}
	return
}

func (ds *_DS) _exec2(ctx context.Context, sqlStr string, onRowArr []interface{}, args ...interface{}) (rowsAffected int64, err error) {
	res, err := ds._exec(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	onRowIndex := 0
	for _, arg := range args {
		switch param := arg.(type) {
		case sql.Out:
			if param.Dest != nil {
				switch param.Dest.(type) {
				case *driver.Rows:
					rows := param.Dest.(*driver.Rows)
					onRow := onRowArr[onRowIndex]
					err = _fetchAllFromCursor(*rows, onRow)
					if err != nil {
						return
					}
					onRowIndex++
				}
			}
		}
	}
	rowsAffected, err = res.RowsAffected()
	if err != nil {
		rowsAffected = -1
	}
	return
}

func _fetchAllFromCursor(rows driver.Rows, onRowFunc interface{}) (err error) {
	defer _close(rows)
	colNames := rows.Columns()
	values := make([]driver.Value, len(colNames))
	switch onRow := onRowFunc.(type) {
	case func(map[string]interface{}):
		data := make(map[string]interface{})
		for {
			err = rows.Next(values)
			if err != nil {
				break
			}
			for i, colName := range colNames {
				data[colName] = values[i]
			}
			onRow(data)
		}
	case func() (interface{}, func()):
		for {
			err = rows.Next(values)
			if err != nil {
				break
			}
			fa, onRowCompleted := onRow()
			switch _fa := fa.(type) {
			case []interface{}:
				for i, v := range values {
					err = _setAny(_fa[i], v)
					if err != nil {
						return
					}
				}
			default:
				err = errors.New(fmt.Sprintf("Unexpected type: %v", reflect.TypeOf(_fa)))
			}
			onRowCompleted()
		}
	default:
		return errors.New(fmt.Sprintf("Unexpected type: %v", reflect.TypeOf(onRow)))
	}
	return
}

func (ds *_DS) Exec(ctx context.Context, sqlStr string, args ...interface{}) (rowsAffected int64, err error) {
	sqlStr = ds._formatSQL(sqlStr)
	var onRowArr []interface{}
	var queryArgs []interface{}
	// Syntax like [on_test1:Test, on_test2:Test] is used to call SP with IMPLICIT cursors
	implicitCursors, _, err := ds._processExecParams(args, &onRowArr, &queryArgs)
	if err != nil {
		return
	}
	if implicitCursors {
		if ds.isOracle() {
			err = ds._queryAllImplicitRcOracle(ctx, sqlStr, onRowArr, queryArgs...)
		} else {
			err = ds._queryAllImplicitRcMySQL(ctx, sqlStr, onRowArr, queryArgs...) // it works with MySQL SP
		}
		return
	}
	return ds._exec2(ctx, sqlStr, onRowArr, queryArgs...)
}

func (ds *_DS) _queryRowValues(ctx context.Context, sqlStr string, queryArgs ...interface{}) (values []interface{}, err error) {
	rows, err := ds._query(ctx, sqlStr, queryArgs...)
	if err != nil {
		return
	}
	defer _close(rows)
	outParamIndex := 0
	_, _, values, valuePointers, err := ds._prepareFetch(rows)
	if err != nil {
		return
	}
	if !rows.Next() {
		err = sql.ErrNoRows
		return
	}
	err = rows.Scan(valuePointers...)
	if err != nil {
		return
	}
	for _, arg := range queryArgs {
		if _isPtr(arg) {
			err = SetRes(arg, values[outParamIndex])
			if err != nil {
				return
			}
		}
	}
	outParamIndex++
	if rows.Next() {
		err = errors.New(fmt.Sprintf("More than 1 row found for %s", sqlStr))
		return
	}
	return
}

func (ds *_DS) Query(ctx context.Context, sqlStr string, args ...interface{}) (arr interface{}, err error) {
	sqlStr = ds._formatSQL(sqlStr)
	var onRowArr []interface{}
	var queryArgs []interface{}
	implicitCursors, outCursors, err := ds._processExecParams(args, &onRowArr, &queryArgs)
	if err != nil {
		return
	}
	if implicitCursors || outCursors {
		err = errors.New("not supported in Query: implicitCursors || outCursors")
		return
	}
	arr, err = ds._queryRowValues(ctx, sqlStr, queryArgs...)
	return // it returns []interface{} for cases like 'SELECT @value, @name;'
}

func (ds *_DS) QueryAll(ctx context.Context, sqlStr string, onRow func(interface{}), args ...interface{}) (err error) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	for {
		// re-detect columns for each ResultSet
		// fetch all columns! if to fetch less, Scan returns nil-s
		_, _, values, valuePointers, pfErr := ds._prepareFetch(rows)
		if pfErr != nil {
			err = pfErr
			return
		}
		for rows.Next() {
			err = rows.Scan(valuePointers...)
			if err != nil {
				return
			}
			// return whole row to enable multiple out params in mssql sp
			onRow(values)
		}
		if !rows.NextResultSet() {
			break
		}
	}
	return
}

func (ds *_DS) QueryRow(ctx context.Context, sqlStr string, args ...interface{}) (row map[string]interface{}, err error) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	colNames, row, values, valuePointers, pfErr := ds._prepareFetch(rows)
	if pfErr != nil {
		err = pfErr
		return
	}
	if !rows.Next() {
		err = sql.ErrNoRows
		return
	}
	err = rows.Scan(valuePointers...)
	if err != nil {
		return
	}
	for i, colName := range colNames {
		row[colName] = values[i]
	}
	if rows.Next() {
		err = errors.New(fmt.Sprintf("more than 1 row found for %s", sqlStr))
	}
	return
}

func (ds *_DS) QueryAllRows(ctx context.Context, sqlStr string, onRow func(map[string]interface{}), args ...interface{}) (err error) {
	// many thanks to:
	// https://stackoverflow.com/questions/51731423/how-to-read-a-row-from-a-table-to-a-map-without-knowing-columns
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	for {
		// re-detect columns for each ResultSet
		colNames, data, values, valuePointers, pfErr := ds._prepareFetch(rows)
		if pfErr != nil {
			err = pfErr
			return
		}
		for rows.Next() {
			err = rows.Scan(valuePointers...)
			if err != nil {
				return
			}
			for i, colName := range colNames {
				data[colName] = values[i]
			}
			onRow(data)
		}
		if !rows.NextResultSet() {
			break
		}
	}
	return
}

func (ds *_DS) QueryByFA(ctx context.Context, sqlStr string, fa interface{}, args ...interface{}) (err error) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	if !rows.Next() {
		err = sql.ErrNoRows
		return
	}
	faArr, ok := fa.([]interface{})
	if ok {
		err = rows.Scan(faArr...)
	} else {
		err = errors.New("not implemented yet")
	}
	if err != nil {
		return
	}
	if rows.Next() {
		err = errors.New(fmt.Sprintf("More than 1 row found for %s", sqlStr))
	}
	return
}

func (ds *_DS) QueryAllByFA(ctx context.Context, sqlStr string, onRow func() (interface{}, func()), args ...interface{}) (err error) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	for {
		for rows.Next() {
			fa, onRowComplete := onRow()
			faArr, ok := fa.([]interface{})
			if ok {
				err = rows.Scan(faArr...)
			} else {
				err = errors.New("not implemented yet")
			}
			if err != nil {
				return
			}
			onRowComplete()
		}
		if !rows.NextResultSet() {
			break
		}
	}
	return
}

/*
// MySQL: if string is ok for all types (no conversions needed), use this:

	func (ds *_DS) _prepareFetch(rows *sql.Rows) ([]string, map[string]interface{}, []string, []interface{}) {
		// ...
		values := make([]string, len(colNames))
*/
func (ds *_DS) _prepareFetch(rows *sql.Rows) (colNames []string, data map[string]interface{}, values []interface{}, valuePointers []interface{}, err error) {
	colNames, err = rows.Columns()
	if err != nil {
		return
	}
	data = make(map[string]interface{})
	// interface{} is ok for SQLite3, Oracle, and SQL Server.
	// MySQL and PostgreSQL may require some convertors from []uint8
	// https://github.com/ziutek/mymysql#type-mapping
	values = make([]interface{}, len(colNames))
	valuePointers = make([]interface{}, len(colNames))
	for i := range values {
		valuePointers[i] = &values[i]
	}
	return
}

func (ds *_DS) _formatSQL(sqlStr string) string {
	if len(ds.paramPrefix) == 0 {
		return sqlStr
	}
	i := 1
	for {
		pos := strings.Index(sqlStr, "?")
		if pos == -1 {
			break
		}
		str1 := sqlStr[0:pos]
		str2 := sqlStr[pos+1:]
		sqlStr = str1 + ds.paramPrefix + strconv.Itoa(i) + str2
		i += 1
	}
	return sqlStr
}

func _getValue(row map[string]interface{}, colName string, errMap map[string]int) (value interface{}, err error) {
	var ok bool
	value, ok = row[colName]
	if !ok {
		key := fmt.Sprintf("%s: no such column", colName)
		count, ok := errMap[key]
		if ok {
			errMap[key] = count + 1
		} else {
			errMap[key] = 1
		}
		err = errors.New(key)
		return
	}
	return
}

func assignErr(dstPtr interface{}, value interface{}, funcName string, errMsg string) error {
	return errors.New(fmt.Sprintf("%s %T <- %T %s", funcName, dstPtr, value, errMsg))
}

func unknownTypeErr(dstPtr interface{}, value interface{}, funcName string) error {
	return assignErr(dstPtr, value, funcName, "unknown type")
}

func SetString(d *string, row map[string]interface{}, colName string, errMap map[string]int) error {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		return _setString(d, value)
	}
	return nil
}

func _setString(d *string, value interface{}) error {
	switch v := value.(type) {
	case []byte:
		*d = string(v)
	case int, int32, int64:
		*d = fmt.Sprintf("%v", v)
	case float64, float32:
		*d = fmt.Sprintf("%v", v) // %v prints just 0.12 instead of 0.120000
	case string:
		*d = v
	case time.Time:
		*d = v.Format("2006-01-02 15:04:05")
	default:
		return unknownTypeErr(d, value, "_setString")
	}
	return nil
}

func SetInt64(d *int64, row map[string]interface{}, colName string, errMap map[string]int) error {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		return _setInt64(d, value)
	}
	return err
}

func _setInt64(d *int64, value interface{}) error {
	switch v := value.(type) {
	case int64:
		*d = v
	case int32:
		*d = int64(v) // MySQL
	case float64:
		*d = int64(v)
	case float32:
		*d = int64(v)
	case []byte:
		str := string(v)
		i64, err := strconv.ParseInt(str, 10, 64)
		if err != nil {
			return assignErr(d, value, "_setInt64", err.Error())
		}
		*d = i64
	case string:
		str := value.(string)
		i64, err := strconv.ParseInt(str, 10, 64)
		if err != nil {
			return assignErr(d, value, "_setInt64", err.Error())
		}
		*d = i64
	default:
		return unknownTypeErr(d, value, "_setInt64")
	}
	return nil
}

func SetInt32(d *int32, row map[string]interface{}, colName string, errMap map[string]int) error {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		return _setInt32(d, value)
	}
	return err
}

func _setInt32(d *int32, value interface{}) error {
	switch v := value.(type) {
	case int32:
		*d = v
	case int64:
		*d = int32(v)
	case float64:
		*d = int32(v)
	case float32:
		*d = int32(v)
	case []byte:
		str := string(v)
		d64, err := strconv.ParseInt(str, 10, 32)
		if err != nil {
			return assignErr(d, value, "_setInt32", err.Error())
		}
		*d = int32(d64)
	case string:
		d64, err := strconv.ParseInt(v, 10, 32)
		if err != nil {
			return assignErr(d, value, "_setInt32", err.Error())
		}
		*d = int32(d64)
	default:
		return unknownTypeErr(d, value, "_setInt32")
	}
	return nil
}

func SetFloat32(d *float32, row map[string]interface{}, colName string, errMap map[string]int) error {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		return _setFloat32(d, value)
	}
	return err
}

func _setFloat32(d *float32, value interface{}) error {
	switch v := value.(type) {
	case float32:
		*d = v
	case float64:
		*d = float32(v)
	case []byte:
		str := string(v) // PostgeSQL
		d64, err := strconv.ParseFloat(str, 64)
		if err != nil {
			return assignErr(d, value, "_setFloat32", err.Error())
		}
		*d = float32(d64)
	case string:
		d64, err := strconv.ParseFloat(v, 64) // Oracle
		if err != nil {
			return assignErr(d, value, "_setFloat32", err.Error())
		}
		*d = float32(d64)
	default:
		return unknownTypeErr(d, value, "_setFloat32")
	}
	return nil
}

func SetFloat64(d *float64, row map[string]interface{}, colName string, errMap map[string]int) error {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		return _setFloat64(d, value)
	}
	return err
}

func _setFloat64(d *float64, value interface{}) error {
	switch v := value.(type) {
	case float64:
		*d = v
	case float32:
		*d = float64(v)
	case []byte:
		str := string(v) // PostgeSQL, MySQL
		var err error
		*d, err = strconv.ParseFloat(str, 64)
		if err != nil {
			return assignErr(d, value, "_setFloat64", err.Error())
		}
	case string:
		var err error
		*d, err = strconv.ParseFloat(v, 64) // Oracle
		if err != nil {
			return assignErr(d, value, "_setFloat64", err.Error())
		}
	default:
		return unknownTypeErr(d, value, "_setFloat64")
	}
	return nil
}

func SetTime(d *time.Time, row map[string]interface{}, colName string, errMap map[string]int) error {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		return _setTime(d, value)
	}
	return err
}

func _setTime(d *time.Time, value interface{}) error {
	switch v := value.(type) {
	case time.Time:
		*d = v
	default:
		return unknownTypeErr(d, value, "_setTime")
	}
	return nil
}

func SetBool(d *bool, row map[string]interface{}, colName string, errMap map[string]int) error {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		return _setBool(d, value)
	}
	return err
}

func _setBool(d *bool, value interface{}) error {
	switch v := value.(type) {
	case []byte:
		str := string(v) // MySQL
		db, err := strconv.ParseBool(str)
		if err != nil {
			return assignErr(d, value, "_setBool", err.Error())
		}
		*d = db
	case bool:
		*d = v
	default:
		return unknownTypeErr(d, value, "_setBool")
	}
	return nil
}

func _setAny(dstPtr interface{}, value interface{}) error {
	if value == nil {
		switch d := dstPtr.(type) {
		case *interface{}:
			*d = nil
			return nil
		}
		return nil // leave as-is
	}
	var err error
	switch d := dstPtr.(type) {
	case *string:
		err = _setString(d, value)
	case *int32:
		err = _setInt32(d, value)
	case *int64:
		err = _setInt64(d, value)
	case *float64:
		err = _setFloat64(d, value)
	case *float32:
		err = _setFloat32(d, value)
	case *time.Time:
		err = _setTime(d, value)
	case *bool:
		err = _setBool(d, value)
	case *[]byte: // the same as uint8
		switch bv := value.(type) {
		case []byte:
			*d = bv
			return nil
		default:
			return unknownTypeErr(d, value, "_setAny")
		}
	//case *godror.Number:
	//	switch v := value.(type) {
	//	case godror.Number:
	//		*d = v
	//		return nil
	//	default:
	//		return UnknownTypeErr(d, value, "_setAny")
	//	}
	//case *uuid.UUID:
	//	switch bv := value.(type) {
	//	case []byte:
	//		err := d.Scan(bv)
	//		if err != nil {
	//			return AssignErr(d, value, "_setAny", err.Error())
	//		}
	//		return nil
	//  default:
	//      return UnknownTypeErr(d, value, "_setAny")
	//	}
	//case *[]string:
	//	switch bv := value.(type) {
	//	case []byte:
	//		sa := pq.StringArray{}
	//		err := sa.Scan(bv)
	//		if err != nil {
	//			return AssignErr(d, value, "_setAny", err.Error())
	//		}
	//		*d = sa
	//		return nil
	//  default:
	//      return UnknownTypeErr(d, value, "_setAny")
	//	}
	//case *pq.StringArray:
	//	switch bv := value.(type) {
	//	case []byte:
	//		err := d.Scan(bv)
	//		if err != nil {
	//			return AssignErr(d, value, "_setAny", err.Error())
	//		}
	//		return nil
	//  default:
	//      return UnknownTypeErr(d, value, "_setAny")
	//	}
	case *interface{}:
		*d = value
		return nil
	}
	return err
}

func SetRes(dstPtr interface{}, value interface{}) error {
	var err error
	switch v := value.(type) {
	case []interface{}:
		switch d := dstPtr.(type) {
		case *[]interface{}:
			*d = v
		default:
			v0 := v[0]
			err = _setAny(dstPtr, v0)
		}
	default:
		err = _setAny(dstPtr, value)
	}
	return err
}

func SetScalarValue(dstPtr interface{}, value interface{}, errMap map[string]int) {
	err := SetRes(dstPtr, value)
	if err == nil {
		return
	}
	key := err.Error()
	count, ok := errMap[key]
	if ok {
		errMap[key] = count + 1
	} else {
		errMap[key] = 1
	}
}

func SetAny(dstPtr interface{}, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err != nil {
		return
	}
	SetScalarValue(dstPtr, value, errMap)
}

func ErrMapToErr(errMap map[string]int) (err error) {
	if len(errMap) > 0 {
		err = errors.New(fmt.Sprintf("%v", errMap))
	}
	return
}
