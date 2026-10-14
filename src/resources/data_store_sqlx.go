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

	"github.com/jmoiron/sqlx"
	"github.com/jmoiron/sqlx/reflectx"
)

var DS func(context context.Context) DataStore

/*
	This file is a part of SQL DAL Maker Project: https://sqldalmaker.sourceforge.net
	It demonstrates how to implement an interface DataStore in Go + sqlx.
	More about DataStore: https://sqldalmaker.sourceforge.net/preconfig.html#ds
	Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_sqlx.go

	Copy-paste this code to your project and change it for your needs.
	Improvements are welcome: sqldalmaker@gmail.com

	Demo project: https://github.com/panedrone/sdm_todolist_go_react_16_npm_sqlite3
*/

type DataStore interface {
	Db() *sqlx.DB

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
	db          *sqlx.DB
}

func (ds *_DS) Db() *sqlx.DB {
	return ds.db
}

/*

// 	Implement "func (ds *_DS) initDb()" in an external file:

package dbal

import (
	"github.com/jmoiron/sqlx"
   	// _ "github.com/mattn/go-sqlite3"      // SQLite3
   	// _ "github.com/denisenkom/go-mssqldb" // SQL Server
   	// _ "github.com/godror/godror"			// Oracle
   	_ "github.com/go-sql-driver/mysql"      // MySQL
   	// _ "github.com/ziutek/mymysql/godrv"  // MySQL
   	// _ "github.com/jackc/pgx/v5/stdlib"   // PostgreSQL
)

var ds = &_DS{}

func (ds *_DS) initDb() (err error) {
	// === PostgreSQL ===========================
	ds.paramPrefix = "$"
	ds.db, err = sqlx.Open("pgx", "postgres://postgres:sa@localhost/my-tests?sslmode=disable")
	// ds.db, err = sqlx.Open("pgx", "postgres://postgres:sa@localhost/my-tests?sslmode=verify-full")
	// === SQLite3 =============================
	// ds.db, err = sqlx.Open("sqlite3", "./log.sqlite")
	// ds.db, err = sqlx.Open("sqlite3", "./northwindEF.sqlite")
	// === MySQL ===============================
	// ds.db, err = sqlx.Open("mysql", "root:root@/sakila")
	// ds.db, err = sqlx.Open("mymysql", "sakila/root/root")
	// === SQL Server ==========================
	// https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	// ensure sqlserver:// in beginning. this one is not valid:
	// ------ ds.db, err = sqlx.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// this one is ok:
	ds.paramPrefix = "@p"
	ds.db, err = sqlx.Open("sqlserver", "sqlserver://sa:root@localhost:1433?database=AdventureWorks2014")
	// === Oracle =============================
	// "github.com/godror/godror"
	//ds.paramPrefix = ":"
	//ds.db, err = sqlx.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
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

func getTxSqlx(ctx context.Context) *sqlx.Tx {
	tx, _ := ctx.Value(txKey).(*sqlx.Tx)
	return tx
}

func (ds *_DS) Begin(ctx context.Context) (txCtx context.Context, err error) {
	tx := getTxSqlx(ctx)
	if tx != nil {
		return nil, errors.New("already in " + txKey)
	}
	tx, err = ds.db.Beginx()
	txCtx = context.WithValue(ctx, txKey, tx)
	return
}

func (ds *_DS) Commit(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return errNilParam()
	}
	tx := getTxSqlx(*txCtx)
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
	tx := getTxSqlx(*txCtx)
	if tx == nil {
		return errNotInTx()
	}
	err = tx.Rollback()
	*txCtx = nil
	return
}

func (ds *_DS) selectSqlx(ctx context.Context, dest any, sqlString string, args ...any) error {
	tx := getTxSqlx(ctx)
	if tx == nil {
		return ds.db.SelectContext(ctx, dest, sqlString, args...)
	}
	return tx.SelectContext(ctx, dest, sqlString, args...)
}

func (ds *_DS) querySqlx(ctx context.Context, sqlString string, args ...any) (*sqlx.Rows, error) {
	tx := getTxSqlx(ctx)
	if tx == nil {
		return ds.db.QueryxContext(ctx, sqlString, args...)
	}
	return tx.QueryxContext(ctx, sqlString, args...)
}

func (ds *_DS) execSqlx(ctx context.Context, sqlString string, args ...any) (sql.Result, error) {
	tx := getTxSqlx(ctx)
	if tx == nil {
		return ds.db.ExecContext(ctx, sqlString, args...)
	}
	return tx.ExecContext(ctx, sqlString, args...)
}

func (ds *_DS) insertPgSql(ctx context.Context, sqlString string, aiNames string, args ...any) (id any, err error) {
	// fetching of multiple AI values is not implemented so far:
	sqlString += " RETURNING " + aiNames
	rows, err := ds.querySqlx(ctx, sqlString, args...)
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
	_, err = ds.execSqlx(ctx, sqlString, args...)
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
	rows, err := ds.querySqlx(ctx, sqlString, args...)
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
	res, err := ds.execSqlx(ctx, sqlString, args...)
	if err != nil {
		return
	}
	id, err = res.LastInsertId()
	return
}

func (ds *_DS) Insert(ctx context.Context, sqlString string, aiNames string, args ...any) (id any, err error) {
	if len(aiNames) == 0 { // len(nil) == 0
		err = errors.New("_DS.insert is not applicable for aiNames = " + aiNames)
		return
	}
	// Builtin LastInsertId works only with MySQL and SQLite3
	sqlString = ds.formatSQL(sqlString)
	if ds.isPgSql() {
		return ds.insertPgSql(ctx, sqlString, aiNames, args...)
	} else if ds.isMsSql() {
		return ds.insertMsSql(ctx, sqlString, args...)
	} else if ds.isOracle() {
		// Oracle: specify AI values explicitly:
		// <crud-auto model="ProjectInfo" table="PROJECTS" generated="P_ID"/>
		return ds.insertOracle(ctx, sqlString, aiNames, args...)
	}
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
				_, ok1 := fn.(func() (any, func()))
				_, ok2 := fn.(func(map[string]any))
				if !ok1 && !ok2 {
					err = errors.New(fmt.Sprintf("Expected: 'func(map[string]any)' || "+
						"'func() (any, func())'. Got: %v", reflect.TypeOf(fn)))
					return
				}
				*onRowArr = append(*onRowArr, fn)
			}
		case func() (any, func()), func() ([]any, func()):
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
	rows, err := ds.querySqlx(ctx, sqlString, queryArgs...)
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
			row := make(map[string]any)
			for rows.Next() {
				err = rows.MapScan(row)
				if err != nil {
					return err
				}
				onRow(row)
			}
		case func() (any, func()):
			err = ds.fetchSqlxRows(rows, onRow)
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
func (ds *_DS) queryImplRc(ctx context.Context, sqlString string, onRowArr []any, queryArgs ...any) error {
	rows, err := ds.querySqlx(ctx, sqlString, queryArgs...)
	if err != nil {
		return err
	}
	defer _close(rows)
	for _, fn := range onRowArr {
		switch onRow := fn.(type) {
		case func(map[string]any):
			// re-detect columns for each ResultSet
			row := make(map[string]any)
			for rows.Next() {
				err = rows.MapScan(row)
				if err != nil {
					return err
				}
				onRow(row)
			}
		case func() (any, func()):
			err = ds.fetchSqlxRows(rows, onRow)
			if err != nil {
				return err
			}
		default:
			return errUnexpectedType(onRow)
		}
		if !rows.NextResultSet() {
			break
		}
	}
	return nil
}

func (ds *_DS) fetchSqlxRows(rows *sqlx.Rows, onRow func() (any, func())) error {
	sc := scanner{m: ds.db.Mapper}
	for rows.Next() {
		fa, onRowCompleted := onRow()
		err := sc.Scan(rows, fa)
		if err != nil {
			return err
		}
		onRowCompleted()
	}
	return nil
}

type scanner struct {
	m      *reflectx.Mapper
	fields [][]int
}

func (s *scanner) Scan(rowsX *sqlx.Rows, fa any) error {
	// sqlx.Rows.StructScan is not working with NextResultSet and multiple struct types
	switch _fa := fa.(type) {
	case []any:
		return rowsX.Scan(_fa...)
	}
	if s.fields == nil {
		columns, err := rowsX.Columns()
		if err != nil {
			return err
		}
		elemType := reflect.TypeOf(fa)
		base := reflectx.Deref(elemType)
		s.fields = s.m.TraversalsByName(base, columns)
	}
	values, err := toValues(s.fields, fa)
	if err != nil {
		return err
	}
	return rowsX.Scan(values...)
}

func (ds *_DS) exec2(ctx context.Context, sqlString string, onRowArr []any, args ...any) (rowsAffected int64, err error) {
	var res sql.Result
	res, err = ds.execSqlx(ctx, sqlString, args...)
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
					err = ds.fetchDriverRows(*rows, onRow)
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

func (ds *_DS) fetchDriverRows(dr driver.Rows, onRowFunc any) error {
	defer _close(dr)
	rx := newRows(dr)
	switch onRow := onRowFunc.(type) {
	case func(map[string]any):
		data := make(map[string]any)
		for {
			if !rx.Next() {
				break // not an error
			}
			rx.toMap(data)
			onRow(data)
		}
	case func() (any, func()):
		for rx.Next() {
			fa, onRowCompleted := onRow()
			var err error
			switch _fa := fa.(type) {
			case []any:
				err = rx.toSlice(_fa)
			default:
				err = rx.toStruct(ds.db.Mapper, fa)
			}
			if err != nil {
				return err
			}
			onRowCompleted()
		}
	default:
		return errUnexpectedType(onRow)
	}
	return nil
}

type _Rows struct {
	rows     driver.Rows
	colNames []string
	values   []driver.Value

	fields [][]int
}

func newRows(rows driver.Rows) *_Rows {
	colNames := rows.Columns()
	return &_Rows{
		rows:     rows,
		colNames: rows.Columns(),
		values:   make([]driver.Value, len(colNames)),
	}
}

func (rx *_Rows) Close() error {
	// implement sqlx.rowsi
	return rx.rows.Close()
}

func (rx *_Rows) Columns() ([]string, error) {
	// implement sqlx.rowsi
	return rx.colNames, nil
}

func (rx *_Rows) Err() error {
	// implement sqlx.rowsi
	return nil
}

func (rx *_Rows) Next() bool {
	// implement sqlx.rowsi
	err := rx.rows.Next(rx.values)
	return err == nil
}

func (rx *_Rows) Scan(_fa ...any) error {
	// implement sqlx.rowsi --> to use in
	for i, v := range rx.values {
		err := _setAny(_fa[i], v)
		if err != nil {
			return err
		}
	}
	return nil
}

func (rx *_Rows) toMap(data map[string]any) {
	for i, col := range rx.colNames {
		data[col] = rx.values[i]
	}
}

func (rx *_Rows) toSlice(_fa []any) error {
	for i, v := range rx.values {
		err := _setAny(_fa[i], v)
		if err != nil {
			return err
		}
	}
	return nil
}

func (rx *_Rows) toStruct(m *reflectx.Mapper, fa any) error {
	if rx.fields == nil {
		elemType := reflect.TypeOf(fa)
		base := reflectx.Deref(elemType)
		rx.fields = m.TraversalsByName(base, rx.colNames)
	}
	values, err := toValues(rx.fields, fa)
	if err != nil {
		return err
	}
	return rx.Scan(values...)
}

func toValues(fields [][]int, fa any) ([]any, error) {
	values := make([]any, len(fields))
	vp := reflect.ValueOf(fa)
	v := reflect.Indirect(vp)
	err := fieldsByTraversal(v, fields, values, true)
	if err != nil {
		return nil, err
	}
	return values, nil
}

func fieldsByTraversal(v reflect.Value, traversals [][]int, values []any, ptrs bool) error {
	v = reflect.Indirect(v)
	if v.Kind() != reflect.Struct {
		return errors.New("argument not a struct")
	}
	for i, traversal := range traversals {
		if len(traversal) == 0 {
			values[i] = new(any)
			continue
		}
		f := reflectx.FieldByIndexes(v, traversal)
		if ptrs {
			values[i] = f.Addr().Interface()
		} else {
			values[i] = f.Interface()
		}
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
		// it works with MySQL SP and PgSql
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
	rows, err := ds.querySqlx(ctx, sqlString, queryArgs...)
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
	rows, err := ds.querySqlx(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	err = ds.selectAllScalars(rows, destSlicePtr)
	return
}

func (ds *_DS) selectAllScalars(rows *sqlx.Rows, dest any) error {
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
		err = fmt.Errorf("must pass a pointer, not a value: %T", dest)
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
	base = reflectx.Deref(slice.Elem())
	return
}

func baseType(t reflect.Type, expected reflect.Kind) (reflect.Type, error) {
	t = reflectx.Deref(t)
	if t.Kind() != expected {
		return nil, fmt.Errorf("expected %s but got %s", expected, t.Kind())
	}
	return t, nil
}

func (ds *_DS) QueryRow(ctx context.Context, sqlString string, args ...any) (row map[string]any, err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.querySqlx(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	if !rows.Next() {
		err = errNoRows(sqlString)
		return
	}
	row = make(map[string]any)
	err = rows.MapScan(row)
	if err != nil {
		return
	}
	if rows.Next() {
		err = errMultipleRows(sqlString)
		return
	}
	return
}

func (ds *_DS) QueryAllRows(ctx context.Context, sqlString string, onRow func(map[string]any), args ...any) (err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.querySqlx(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	for {
		// re-detect columns for each ResultSet
		row := make(map[string]any)
		for rows.Next() {
			err = rows.MapScan(row)
			if err != nil {
				return
			}
			onRow(row)
		}
		if !rows.NextResultSet() {
			break
		}
	}
	return
}

func isPtrSlice(i any) bool {
	// https://stackoverflow.com/questions/69675420/how-to-check-if-interface-is-a-a-pointer-to-a-slice
	if i == nil {
		return false
	}
	v := reflect.ValueOf(i)
	if v.Kind() != reflect.Ptr {
		return false
	}
	return v.Elem().Kind() == reflect.Slice
}

func (ds *_DS) Select(ctx context.Context, sqlString string, dest any, args ...any) error {
	sqlString = ds.formatSQL(sqlString)
	faArr, isUntypedSlice := dest.([]any)
	if !isUntypedSlice && isPtrSlice(dest) {
		return ds.selectSqlx(ctx, dest, sqlString, args...)
	}
	rows, err := ds.querySqlx(ctx, sqlString, args...)
	if err != nil {
		return err
	}
	defer _close(rows)
	onRow, ok := dest.(func() (any, func()))
	if ok {
		for {
			err := ds.fetchSqlxRows(rows, onRow)
			if err != nil {
				return err
			}
			if !rows.NextResultSet() {
				break
			}
		}
		return nil
	}
	if !rows.Next() {
		return errNoRows(sqlString)
	}
	if isUntypedSlice {
		err = rows.Scan(faArr...)
	} else {
		err = rows.StructScan(dest) // pointer to single DTO
	}
	if err != nil {
		return err
	}
	if rows.Next() {
		return errMultipleRows(sqlString)
	}
	return nil
}

func (ds *_DS) prepareFetch(rows *sqlx.Rows) (colNames []string, values []any, valuePointers []any, err error) {
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
	// https://jmoiron.github.io/sqlx/
	// bindvars
	return ds.db.Rebind(sqlString)
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
		err = errors.New(fmt.Sprintf("%v", errMap))
	}
	return
}

func assignErr(dstPtr any, value any, funcName string, errMsg string) error {
	return errors.New(fmt.Sprintf("%s %T <- %T %s", funcName, dstPtr, value, errMsg))
}

func unknownTypeErr(dstPtr any, value any, funcName string) error {
	return assignErr(dstPtr, value, funcName, "unknown type")
}

func errUnexpectedType(val any) error {
	return errors.New(fmt.Sprintf("unexpected type: %v", reflect.TypeOf(val)))
}

func errNoRows(sqlString string) error {
	return errors.New(fmt.Sprintf("no rows found for %s", sqlString))
}

func errMultipleRows(sqlString string) error {
	return errors.New(fmt.Sprintf("more than 1 row found for %s", sqlString))
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
