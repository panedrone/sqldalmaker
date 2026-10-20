package dbal

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"errors"
	"fmt"
	"io"
	"reflect"
	"strconv"
	"strings"
	"time"
)

var DS func(context context.Context) DataStore

/*
	This file is a part of SQL DAL Maker Project: https://sqldalmaker.sourceforge.net
	It demonstrates how to implement an interface DataStore in Go using "database/sql" package directly (no-orm scenario).
	More about DataStore: https://sqldalmaker.sourceforge.net/preconfig.html#ds
	Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_no_orm.go

	Copy-paste this code to your project and change it for your needs.
	Improvements are welcome: sqldalmaker@gmail.com

	Demo project: https://github.com/panedrone/sdm_todolist_go_react_16_npm_sqlite3
*/

type DataStore interface {
	Db() *sql.DB

	Open() error
	Close() error

	Begin(ctx context.Context) (txCtx context.Context, err error)
	Commit(txCtx *context.Context) error
	Rollback(txCtx *context.Context) error

	// the methods called by generated code:

	Insert(ctx context.Context, sqlString string, aiNames string, args ...any) (id any, err error)
	Exec(ctx context.Context, sqlString string, args ...any) (rowsAffected int64, err error)

	Query(ctx context.Context, sqlString string, destPtr any, args ...any) error
	QueryAll(ctx context.Context, sqlString string, destSlicePtr any, args ...any) error
	QueryRow(ctx context.Context, sqlString string, args ...any) (row map[string]any, err error)
	QueryAllRows(ctx context.Context, sqlString string, onRow func(map[string]any), args ...any) error

	Select(ctx context.Context, sqlString string, dest any, args ...any) error
}

func FetchPg(cursor string) string {
	return fmt.Sprintf(`fetch all from "%s"`, cursor)
}

type Out struct {
	/*
		var outParam float64 // no need to init
		cxDao.SpTestOutParams(47, Out{Dest: &outParam})
		// cxDao.SpTestOutParams(47, &outParam) // <- this one is also ok for OUT parameters
		fmt.Println(outParam)

		// not working in MySQL, see https://sqldalmaker.sourceforge.net/sp-udf.html#mysql_out_params
	*/

	// Dest is a pointer to the value that will be set to the result of the
	// stored procedure's OUT parameter.
	Dest any
}

type InOut struct {
	/*
		inOutParam := 123.0 // must be initialized for INOUT
		cxDao.SpTestInoutParams(InOut{Dest: &inOutParam})
		fmt.Println(inOutParam)

		// not working in MySQL, see https://sqldalmaker.sourceforge.net/sp-udf.html#mysql_out_params
	*/

	// Dest is a pointer to the value that will be set to the result of the
	// stored procedure's OUT parameter.
	Dest any
}

type _DS struct {
	paramPrefix string
	db          *sql.DB
}

func (ds *_DS) Db() *sql.DB {
	return ds.db
}

/*

// 	Implement "func (ds *_DS) initDb()" in an external file:

package dbal

import (
	"database/sql"
   	// _ "github.com/mattn/go-sqlite3"      // SQLite3
   	// _ "github.com/denisenkom/go-mssqldb" // SQL Server
   	// _ "github.com/godror/godror"			// Oracle
   	// _ "github.com/go-sql-driver/mysql"   // MySQL
   	// _ "github.com/ziutek/mymysql/godrv"  // MySQL
   	_ "github.com/jackc/pgx/v5/stdlib"      // PostgreSQL
)

func (ds *_DS) initDb() (err error) {
	// === PostgreSQL ===========================
	ds.paramPrefix = "$"
	ds.db, err = sql.Open("pgx", "postgres://postgres:sa@localhost/my-tests?sslmode=disable")
	// ds.db, err = sql.Open("pgx", "postgres://postgres:sa@localhost/my-tests?sslmode=verify-full")
	// === SQLite3 =============================
	// ds.db, err = sql.Open("sqlite3", "./log.sqlite")
	// ds.db, err = sql.Open("sqlite3", "./northwindEF.sqlite")
	// === SQL Server ==========================
	// https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	// ensure sqlserver:// in beginning. this one is not valid:
	// ------ ds.db, err = sql.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// ds.paramPrefix = "@p"
	// ds.db, err = sql.Open("sqlserver", "sqlserver://sa:root@localhost:1433?database=AdventureWorks2014")
	// === MySQL ===============================
	// ds.db, err = sql.Open("mysql", "root:root@/sakila")
	// ds.db, err = sql.Open("mymysql", "sakila/root/root")
	// === Oracle ==============================
	// "github.com/godror/godror"
	// ds.paramPrefix = ":"
	// ds.db, err = sql.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
	return
}

var _ds = &_DS{}

func Ds() DataStore {
	return _ds
}

func init() {
	// generated DAO functions get DataStore via DS(ctx)
	DS = func(ctx context.Context) DataStore {
		return _ds
	}
}

*/

func (ds *_DS) Open() error {
	err := ds.initDb()
	if err != nil {
		return err
	}
	name := reflect.TypeOf(ds.db.Driver()).String()
	name = strings.ToLower(name)
	// "*stdlib.driver" is pgx, "*pq.driver" is lib/pq
	if strings.Contains(name, "stdlib") || strings.Contains(name, "pq") {
		ds.paramPrefix = "$"
	} else if strings.Contains(name, "or") {
		ds.paramPrefix = ":"
	}
	return err
}

func (ds *_DS) Close() error {
	return ds.db.Close()
}

func _close(obj io.Closer) {
	_ = obj.Close()
}

func (ds *_DS) isPgSql() bool {
	return ds.paramPrefix == "$"
}

func (ds *_DS) isOracle() bool {
	return ds.paramPrefix == ":"
}

func (ds *_DS) isMsSql() bool {
	return ds.paramPrefix == "@p"
}

const txKey = "tx"

func getTx(ctx context.Context) *sql.Tx {
	tx, _ := ctx.Value(txKey).(*sql.Tx)
	return tx
}

func (ds *_DS) Begin(ctx context.Context) (txCtx context.Context, err error) {
	tx := getTx(ctx)
	if tx != nil {
		return nil, errors.New("already in " + txKey)
	}
	tx, err = ds.db.Begin()
	txCtx = context.WithValue(ctx, txKey, tx)
	return
}

func (ds *_DS) Commit(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return errNilParam()
	}
	tx := getTx(*txCtx)
	if tx == nil {
		return errNotInTx()
	}
	err = tx.Commit()
	*txCtx = nil
	return
}

func (ds *_DS) Rollback(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return errNilParam()
	}
	tx := getTx(*txCtx)
	if tx == nil {
		return errNotInTx()
	}
	err = tx.Rollback()
	*txCtx = nil
	return
}

func (ds *_DS) query(ctx context.Context, sqlString string, args ...any) (*sql.Rows, error) {
	tx := getTx(ctx)
	if tx == nil {
		return ds.db.QueryContext(ctx, sqlString, args...)
	}
	return tx.QueryContext(ctx, sqlString, args...)
}

func (ds *_DS) exec(ctx context.Context, sqlString string, args ...any) (sql.Result, error) {
	tx := getTx(ctx)
	if tx == nil {
		return ds.db.ExecContext(ctx, sqlString, args...)
	}
	return tx.ExecContext(ctx, sqlString, args...)
}

func (ds *_DS) insertPgSql(ctx context.Context, sqlString string, aiNames string, args ...any) (id any, err error) {
	// fetching of multiple AI values is not implemented so far:
	sqlString += " RETURNING " + aiNames
	rows, err := ds.query(ctx, sqlString, args...)
	if err != nil {
		return nil, err
	}
	defer _close(rows)
	if rows.Next() {
		var data any
		err = rows.Scan(&data)
		return
	}
	err = errors.New("rows.Next() FAILED:" + sqlString)
	return
}

func (ds *_DS) insertOracle(ctx context.Context, sqlString string, aiNames string, args ...any) (id any, err error) {
	// fetching of multiple AI values is not implemented so far:
	sqlString = fmt.Sprintf("%s returning %s  into :%d", sqlString, aiNames, len(args)+1)
	// https://ddcode.net/2019/05/11/how-does-go-call-oracles-stored-procedures-and-get-the-return-value-of-the-stored-procedures/
	var id64 float64
	// var id64 any --- error
	// var id64 int64 --- it works, but...
	args = append(args, sql.Out{Dest: &id64, In: false})
	// ----------------
	_, err = ds.exec(ctx, sqlString, args...)
	if err != nil {
		return
	}
	return id64, nil
}

func (ds *_DS) insertMsSql(ctx context.Context, sqlString string, args ...any) (id any, err error) {
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// LastInsertId should not be used with this driver (or SQL Server) due to
	// how the TDS protocol works. Please use the OUTPUT Clause or add a select
	// ID = convert(bigint, SCOPE_IDENTITY()); to the end of your query (ref SCOPE_IDENTITY).
	//  This will ensure you are getting the correct ID and will prevent a network round trip.
	sqlString += ";SELECT @@IDENTITY;"
	rows, err := ds.query(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	if rows.Next() {
		var data any
		err = rows.Scan(&data) // returns []uint8
		return data, err
	}
	err = errors.New("SELECT @@IDENTITY FAILED: " + sqlString)
	return
}

func (ds *_DS) insertDefault(ctx context.Context, sqlString string, args ...any) (id any, err error) {
	res, err := ds.exec(ctx, sqlString, args...)
	if err != nil {
		return
	}
	id, err = res.LastInsertId()
	return
}

func (ds *_DS) Insert(ctx context.Context, sqlString string, aiNames string, args ...any) (id any, err error) {
	sqlString = ds.formatSQL(sqlString)
	if ds.isPgSql() {
		return ds.insertPgSql(ctx, sqlString, aiNames, args...)
	} else if ds.isMsSql() {
		return ds.insertMsSql(ctx, sqlString, args...)
	} else if ds.isOracle() {
		return ds.insertOracle(ctx, sqlString, aiNames, args...)
	}
	// Builtin LastInsertId works only with MySQL and SQLite3
	return ds.insertDefault(ctx, sqlString, args...)
}

func isPtr(p any) bool {
	val := reflect.ValueOf(p)
	kindOfJ := val.Kind()
	return kindOfJ == reflect.Ptr
}

func pointsToNil(p any) bool {
	switch d := p.(type) {
	case *any:
		pointsTo := *d
		return pointsTo == nil
	}
	return false
}

func validateDest(dest any) (err error) {
	if dest == nil {
		err = errors.New("Out/InOut -> Dest is nil")
	} else if !isPtr(dest) {
		err = errors.New("Out/InOut -> Dest must be a Ptr")
	} else if pointsToNil(dest) {
		err = errors.New("Out/InOut -> Dest points to nil")
	}
	return
}

func (ds *_DS) processExecParams(args []any, onRowArr *[]any, queryArgs *[]any) (hasParamsImplRc bool, hasParamsOutRc bool, err error) {
	for _, arg := range args {
		switch param := arg.(type) {
		case []func(map[string]any):
			if hasParamsOutRc {
				err = errUnexpectedType(args)
				return
			}
			hasParamsImplRc = true
			for _, fn := range param {
				*onRowArr = append(*onRowArr, fn)
			}
		case func(map[string]any):
			if hasParamsImplRc {
				err = errUnexpectedType(args)
				return
			}
			hasParamsOutRc = true
			*onRowArr = append(*onRowArr, param) // add single func
			var rows driver.Rows
			*queryArgs = append(*queryArgs, sql.Out{Dest: &rows, In: false})
		case []any:
			if hasParamsOutRc {
				err = errUnexpectedType(args)
				return
			}
			hasParamsImplRc = true
			for _, fn := range param {
				_, ok1 := fn.(func(func(any) error) error)
				_, ok2 := fn.(func(map[string]any))
				if !ok1 && !ok2 {
					err = fmt.Errorf("Expected: 'func(map[string]any)' || "+
						"'func(func(any) error) error'. Got: %v", reflect.TypeOf(fn))
					return
				}
				*onRowArr = append(*onRowArr, fn)
			}
		case func(func(any) error) error:
			if hasParamsImplRc {
				err = errUnexpectedType(args)
				return
			}
			hasParamsOutRc = true
			*onRowArr = append(*onRowArr, param) // add single func
			var rows driver.Rows
			*queryArgs = append(*queryArgs, sql.Out{Dest: &rows, In: false})
		case *Out:
			err = validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: false})
		case Out:
			err = validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: false})
		case *InOut:
			err = validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: true})
		case InOut:
			err = validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: true})
		default:
			if isPtr(arg) {
				if pointsToNil(arg) {
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

func (ds *_DS) queryImplRcOracle(ctx context.Context, sqlString string, onRowArr []any, queryArgs ...any) error {
	rows, err := ds.query(ctx, sqlString, queryArgs...)
	if err != nil {
		return err
	}
	defer _close(rows)
	onRowIndex := 0
	for {
		// 1) unlike MySQL, NextResultSet must be done before prepareFetch -> rows.Next()
		// 2) it does not work with multiple Implicit RC + early versions of Driver
		if !rows.NextResultSet() {
			break
		}
		switch onRow := onRowArr[onRowIndex].(type) {
		case func(map[string]any):
			// re-detect columns for each ResultSet
			colNames, values, valuePointers, err := ds.prepareFetch(rows)
			if err != nil {
				return err
			}
			data := make(map[string]any)
			for rows.Next() {
				err = rows.Scan(valuePointers...)
				if err != nil {
					return err
				}
				for i, col := range colNames {
					data[col] = values[i]
				}
				onRow(data)
			}
		case func(func(any) error) error:
			err = ds.fetchRows(rows, onRow)
			if err != nil {
				return err
			}
		default:
			return errUnexpectedType(onRow)
		}
		onRowIndex++
	}
	return nil
}

func (ds *_DS) fetchRows(rows *sql.Rows, onRow func(func(any) error) error) (err error) {
	scan := func(fa any) error {
		faArr, ok := fa.([]any)
		if !ok {
			return errUnexpectedType(fa)
		}
		return rows.Scan(faArr...)
	}
	for rows.Next() {
		err = onRow(scan)
		if err != nil {
			return
		}
	}
	return
}

func (ds *_DS) queryImplRc(ctx context.Context, sqlString string, onRowArr []any, queryArgs ...any) error {
	rows, err := ds.query(ctx, sqlString, queryArgs...)
	if err != nil {
		return err
	}
	defer _close(rows)
	onRowIndex := 0
	for {
		switch onRow := onRowArr[onRowIndex].(type) {
		case func(map[string]any):
			// re-detect columns for each ResultSet
			colNames, values, valuePointers, err := ds.prepareFetch(rows)
			if err != nil {
				return err
			}
			data := make(map[string]any)
			for rows.Next() {
				err = rows.Scan(valuePointers...)
				if err != nil {
					return err
				}
				for i, col := range colNames {
					data[col] = values[i]
				}
				onRow(data)
			}
		case func(func(any) error) error:
			err = ds.fetchRows(rows, onRow)
			if err != nil {
				return err
			}
		default:
			return errUnexpectedType(onRow)
		}
		if !rows.NextResultSet() {
			break
		}
		onRowIndex++
	}
	return nil
}

func (ds *_DS) exec2(ctx context.Context, sqlString string, onRowArr []any, args ...any) (rowsAffected int64, err error) {
	var res sql.Result
	res, err = ds.exec(ctx, sqlString, args...)
	if err != nil {
		return
	}
	onRowIndex := 0
	for _, arg := range args {
		switch param := arg.(type) {
		case sql.Out:
			if param.Dest != nil {
				switch rows := param.Dest.(type) {
				case *driver.Rows:
					onRow := onRowArr[onRowIndex]
					err = fetchDriverRows(*rows, onRow)
					if err != nil {
						return
					}
					onRowIndex++
				}
			}
		}
	}
	rowsAffected, _ = res.RowsAffected()
	return
}

func fetchDriverRows(rows driver.Rows, onRowFunc any) error {
	defer _close(rows)
	colNames := rows.Columns()
	values := make([]driver.Value, len(colNames))
	switch onRow := onRowFunc.(type) {
	case func(map[string]any):
		data := make(map[string]any)
		for {
			err := rows.Next(values)
			if err != nil {
				break
			}
			for i, col := range colNames {
				data[col] = values[i]
			}
			onRow(data)
		}
	case func(func(any) error) error:
		scan := func(fa any) error {
			_fa, ok := fa.([]any)
			if !ok {
				return errUnexpectedType(fa)
			}
			for i, v := range values {
				err := _setAny(_fa[i], v)
				if err != nil {
					return err
				}
			}
			return nil
		}
		for {
			err := rows.Next(values)
			if err != nil {
				if "EOF" == err.Error() {
					break
				}
				return err
			}
			err = onRow(scan)
			if err != nil {
				return err
			}
		}
	default:
		return errUnexpectedType(onRowFunc)
	}
	return nil
}

func assignPtrParams(values []any, queryArgs []any) error {
	for i, arg := range queryArgs {
		if isPtr(arg) {
			err := SetRes(arg, values[i])
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (ds *_DS) Exec(ctx context.Context, sqlString string, args ...any) (rowsAffected int64, err error) {
	sqlString = ds.formatSQL(sqlString)
	var onRowArr []any
	var queryArgs []any
	// Syntax like [on_test1:Test, on_test2:Test] is used to call SP with IMPLICIT cursors
	hasImplRcParams, _, err := ds.processExecParams(args, &onRowArr, &queryArgs)
	if err != nil {
		return
	}
	if hasImplRcParams {
		if ds.isOracle() {
			return 0, ds.queryImplRcOracle(ctx, sqlString, onRowArr, queryArgs...)
		}
		// it works with MySQL SP and PgSQL
		return 0, ds.queryImplRc(ctx, sqlString, onRowArr, queryArgs...)
	}
	return ds.exec2(ctx, sqlString, onRowArr, queryArgs...)
}

func (ds *_DS) Query(ctx context.Context, sqlString string, destPtr any, args ...any) error {
	sqlString = ds.formatSQL(sqlString)
	var onRowArr []any
	var queryArgs []any
	hasParamsImplRc, hasParamsRefRc, err := ds.processExecParams(args, &onRowArr, &queryArgs)
	if err != nil {
		return err
	}
	if hasParamsImplRc || hasParamsRefRc {
		return errUnexpectedInQuery()
	}
	rows, err := ds.query(ctx, sqlString, queryArgs...)
	if err != nil {
		return err
	}
	defer _close(rows)
	_, values, valuePointers, err := ds.prepareFetch(rows)
	if err != nil {
		return err
	}
	if !rows.Next() {
		return errNoRows(sqlString)
	}
	err = rows.Scan(valuePointers...)
	if err != nil {
		return err
	}
	err = SetRes(destPtr, values)
	if err != nil {
		return err
	}
	if rows.Next() {
		return errMultipleRows(sqlString)
	}
	err = assignPtrParams(values, queryArgs)
	if err != nil {
		return err
	}
	return nil
}

func (ds *_DS) QueryAll(ctx context.Context, sqlString string, destSlicePtr any, args ...any) (err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.query(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	err = ds.selectAllScalars(rows, destSlicePtr)
	return
}

func (ds *_DS) selectAllScalars(rows *sql.Rows, dest any) error {
	base, direct, elemIsPtr, err := prepareSelectScalars(dest)
	if err != nil {
		return err
	}
	errMap := make(map[string]int)
	for {
		// re-detect columns for each ResultSet
		// fetch all columns! if to fetch less, Scan returns nil-s
		_, values, valuePointers, pfErr := ds.prepareFetch(rows)
		if pfErr != nil {
			return pfErr
		}
		for rows.Next() {
			err = rows.Scan(valuePointers...)
			if err != nil {
				return err
			}
			vp := reflect.New(base)
			dataPtr := vp.Interface()
			SetScalarValue(dataPtr, values, errMap)
			// append
			if elemIsPtr {
				direct.Set(reflect.Append(direct, vp))
			} else {
				direct.Set(reflect.Append(direct, reflect.Indirect(vp)))
			}
		}
		if !rows.NextResultSet() {
			break
		}
	}
	err = rows.Err()
	if err != nil {
		return err
	}
	err = ErrMapToErr(errMap)
	return err
}

func prepareSelectScalars(dest any) (base reflect.Type, direct reflect.Value, elemIsPtr bool, err error) {
	value := reflect.ValueOf(dest)
	if value.Kind() != reflect.Ptr {
		err = errors.New("must pass a pointer, not a value")
		return
	}
	if value.IsNil() {
		err = errors.New("nil pointer passed")
		return
	}
	direct = reflect.Indirect(value)
	slice, errBase := baseType(value.Type(), reflect.Slice)
	if errBase != nil {
		err = errBase
		return
	}
	direct.SetLen(0)
	elemIsPtr = slice.Elem().Kind() == reflect.Ptr
	base = Deref(slice.Elem())
	return
}

func baseType(t reflect.Type, expected reflect.Kind) (reflect.Type, error) {
	t = Deref(t)
	if t.Kind() != expected {
		return nil, fmt.Errorf("expected %s but got %s", expected, t.Kind())
	}
	return t, nil
}

// Deref is Indirect for reflect.Types
func Deref(t reflect.Type) reflect.Type {
	if t.Kind() == reflect.Ptr {
		t = t.Elem()
	}
	return t
}

func (ds *_DS) QueryRow(ctx context.Context, sqlString string, args ...any) (row map[string]any, err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.query(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	colNames, values, valuePointers, err := ds.prepareFetch(rows)
	if err != nil {
		return
	}
	row = make(map[string]any)
	if !rows.Next() {
		err = errNoRows(sqlString)
		return
	}
	err = rows.Scan(valuePointers...)
	if err != nil {
		return
	}
	for i, col := range colNames {
		row[col] = values[i]
	}
	if rows.Next() {
		err = errMultipleRows(sqlString)
		return
	}
	return
}

func (ds *_DS) QueryAllRows(ctx context.Context, sqlString string, onRow func(map[string]any), args ...any) error {
	// many thanks to:
	// https://stackoverflow.com/questions/51731423/how-to-read-a-row-from-a-table-to-a-map-without-knowing-columns
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.query(ctx, sqlString, args...)
	if err != nil {
		return err
	}
	defer _close(rows)
	for {
		// re-detect columns for each ResultSet
		colNames, values, valuePointers, err := ds.prepareFetch(rows)
		if err != nil {
			return err
		}
		data := make(map[string]any)
		for rows.Next() {
			err = rows.Scan(valuePointers...)
			if err != nil {
				return err
			}
			for i, col := range colNames {
				data[col] = values[i]
			}
			onRow(data)
		}
		if !rows.NextResultSet() {
			break
		}
	}
	return nil
}

func (ds *_DS) Select(ctx context.Context, sqlString string, dest any, args ...any) (err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.query(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	onRow, ok := dest.(func(func(any) error) error)
	if ok {
		return ds.fetchAll(rows, onRow)
	}
	if !rows.Next() {
		err = errNoRows(sqlString)
		return
	}
	faArr, ok := dest.([]any)
	if ok {
		err = rows.Scan(faArr...)
	} else {
		err = errUnexpectedType(dest)
	}
	if err != nil {
		return
	}
	if rows.Next() {
		err = errMultipleRows(sqlString)
		return
	}
	return
}

func (ds *_DS) fetchAll(rows *sql.Rows, onRow func(func(any) error) error) (err error) {
	scan := func(fa any) error {
		faArr, ok := fa.([]any)
		if !ok {
			return errUnexpectedType(fa)
		}
		return rows.Scan(faArr...)
	}
	for {
		for rows.Next() {
			err = onRow(scan)
			if err != nil {
				return
			}
		}
		if !rows.NextResultSet() {
			return
		}
	}
}

func (ds *_DS) prepareFetch(rows *sql.Rows) (colNames []string, values []any, valuePointers []any, err error) {
	colNames, err = rows.Columns()
	if err != nil {
		return
	}
	// any is ok for SQLite3, Oracle, and SQL Server.
	// MySQL and PostgreSQL may require some convertors from []uint8
	// https://github.com/ziutek/mymysql#type-mapping
	values = make([]any, len(colNames))
	valuePointers = make([]any, len(colNames))
	for i := range values {
		valuePointers[i] = &values[i]
	}
	return
}

func (ds *_DS) formatSQL(sqlString string) string {
	if len(ds.paramPrefix) == 0 {
		return sqlString
	}
	i := 1
	for {
		pos := strings.Index(sqlString, "?")
		if pos == -1 {
			break
		}
		str1 := sqlString[0:pos]
		str2 := sqlString[pos+1:]
		sqlString = str1 + ds.paramPrefix + strconv.Itoa(i) + str2
		i += 1
	}
	return sqlString
}

/////////////////////////////////////////////////////////////

func SetString(d *string, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setString(d, value)
	})
}

func _setString(d *string, value any) error {
	if value == nil {
		*d = ""
		return nil
	}
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

func SetInt64(d *int64, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setInt64(d, value)
	})
}

func _setInt64(d *int64, value any) error {
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

func SetInt(d *int, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setInt(d, value)
	})
}

func _setInt(d *int, value any) error {
	switch v := value.(type) {
	case int:
		*d = v
	case int32:
		*d = int(v)
	case int64:
		*d = int(v)
	case float64:
		*d = int(v)
	case float32:
		*d = int(v)
	case []byte:
		str := string(v)
		d64, err := strconv.ParseInt(str, 10, 32)
		if err != nil {
			return assignErr(d, value, "_setInt", err.Error())
		}
		*d = int(d64)
	case string:
		d64, err := strconv.ParseInt(v, 10, 32)
		if err != nil {
			return assignErr(d, value, "_setInt", err.Error())
		}
		*d = int(d64)
	default:
		return unknownTypeErr(d, value, "_setInt")
	}
	return nil
}

func SetInt32(d *int32, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setInt32(d, value)
	})
}

func _setInt32(d *int32, value any) error {
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

func SetFloat32(d *float32, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setFloat32(d, value)
	})
}

func _setFloat32(d *float32, value any) error {
	switch v := value.(type) {
	case float32:
		*d = v
	case float64:
		*d = float32(v)
	case []byte:
		str := string(v) // PostgreSQL
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

func SetFloat64(d *float64, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setFloat64(d, value)
	})
}

func _setFloat64(d *float64, value any) error {
	switch v := value.(type) {
	case float64:
		*d = v
	case float32:
		*d = float64(v)
	case []byte:
		str := string(v) // PostgreSQL, MySQL
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

func SetTime(d *time.Time, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setTime(d, value)
	})
}

func _setTime(d *time.Time, value any) error {
	switch v := value.(type) {
	case time.Time:
		*d = v
	default:
		return unknownTypeErr(d, value, "_setTime")
	}
	return nil
}

func SetBool(d *bool, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setBool(d, value)
	})
}

func _setBool(d *bool, value any) error {
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

func SetBytes(d *[]byte, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _setBytes(d, value)
	})
}

func _setBytes(d *[]byte, value any) error {
	switch v := value.(type) {
	case []byte:
		*d = v
	default:
		return unknownTypeErr(d, value, "_setBytes")
	}
	return nil
}

func SetNum(d any, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		s, ok := d.(sql.Scanner)
		if ok {
			return _scan(s, value)
		}
		return _setAny(d, value)
	})
}

func Scan(d sql.Scanner, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		return _scan(d, value)
	})
}

func _scan(d sql.Scanner, value any) error {
	err := d.Scan(value)
	if err != nil {
		return assignErr(d, value, "_scan", err.Error())
	}
	return nil
}

func _copy(row map[string]any, col string, errMap map[string]int, fn func(value any) error) {
	var ok bool
	value, ok := row[col]
	if !ok {
		key := fmt.Sprintf("[%s] column not found", col)
		count, ok := errMap[key]
		if ok {
			errMap[key] = count + 1
		} else {
			errMap[key] = 1
		}
		return
	}
	err := fn(value)
	if err == nil {
		return
	}
	key := fmt.Sprintf("[%s] %s", col, err.Error())
	count, ok := errMap[key]
	if ok {
		errMap[key] = count + 1
	} else {
		errMap[key] = 1
	}
}

func _setAny(dstPtr any, value any) error {
	if value == nil {
		switch d := dstPtr.(type) {
		case *any:
			*d = nil
		}
		return nil // leave as-is
	}
	n, ok := value.(driver.Valuer)
	if ok {
		v, err := n.Value()
		if err == nil {
			err = _setAny(dstPtr, v)
		}
		return err
	}
	switch d := dstPtr.(type) {
	case *string:
		return _setString(d, value)
	case *int:
		return _setInt(d, value)
	case *int32:
		return _setInt32(d, value)
	case *int64:
		return _setInt64(d, value)
	case *float64:
		return _setFloat64(d, value)
	case *float32:
		return _setFloat32(d, value)
	case *time.Time:
		return _setTime(d, value)
	case *bool:
		return _setBool(d, value)
	case *[]byte: // the same as uint8
		return _setBytes(d, value)
	case sql.Scanner:
		return _scan(d, value)
	//case *[]string:
	//	switch bv := value.(type) {
	//	case []byte:
	//		sa := pq.StringArray{}
	//		err := sa.Scan(bv)
	//		if err != nil {
	//			return assignErr(d, value, "_setAny", err.Error())
	//		}
	//		*d = sa
	//		return nil
	//  default:
	//      return unknownTypeErr(d, value, "_setAny")
	//	}
	//case *pq.StringArray:
	//	switch bv := value.(type) {
	//	case []byte:
	//		err := d.Scan(bv)
	//		if err != nil {
	//			return assignErr(d, value, "_setAny", err.Error())
	//		}
	//		return nil
	//  default:
	//      return unknownTypeErr(d, value, "_setAny")
	//	}
	case *any:
		*d = value
		return nil
	}
	return errUnexpectedType(dstPtr)
}

func SetRes(dstPtr any, value any) error {
	var err error
	switch v := value.(type) {
	case []any:
		switch d := dstPtr.(type) {
		case *[]any:
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

func SetScalarValue(dstPtr any, value any, errMap map[string]int) {
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

func SetAny(dstPtr any, row map[string]any, col string, errMap map[string]int) {
	_copy(row, col, errMap, func(value any) error {
		SetScalarValue(dstPtr, value, errMap)
		return nil
	})
}

/////////////////////////////////////////////////////////////

func ErrMapToErr(errMap map[string]int) (err error) {
	if len(errMap) > 0 {
		err = fmt.Errorf("%v", errMap)
	}
	return
}

func assignErr(dstPtr any, value any, funcName string, errMsg string) error {
	return fmt.Errorf("%s %T <- %T %s", funcName, dstPtr, value, errMsg)
}

func unknownTypeErr(dstPtr any, value any, funcName string) error {
	return assignErr(dstPtr, value, funcName, "unknown type")
}

func errUnexpectedType(val any) error {
	return fmt.Errorf("unexpected type: %v", reflect.TypeOf(val))
}

func errNoRows(sqlString string) error {
	return fmt.Errorf("no rows found for %s", sqlString)
}

func errMultipleRows(sqlString string) error {
	return fmt.Errorf("more than 1 row found for %s", sqlString)
}

func errUnexpectedInQuery() error {
	return errors.New("not supported: 'query([onTest]) for implicit cursors', query(onTest) for out cursors")
}

func errNotInTx() error {
	return errors.New(txKey + " not started")
}

func errNilParam() error {
	return errors.New("nil parameter")
}
