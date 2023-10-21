package dbal

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"errors"
	"fmt"
	// "github.com/godror/godror"
	// "github.com/google/uuid"
	"gorm.io/gorm"
	"io"
	"reflect"
	"strconv"
	"time"
)

/*
	This file is a part of SQL DAL Maker project: https://sqldalmaker.sourceforge.net
	It demonstrates how to implement an interface DataStore in Go + Gorm.
	More about DataStore: https://sqldalmaker.sourceforge.net/preconfig.html#ds
	Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_gorm.go

	Successfully tested with:

		- "gorm.io/driver/sqlite"
		- "gorm.io/driver/mysql"
		- "github.com/cengsin/oracle"
		- "gorm.io/driver/postgres"

	Copy-paste this code to your project and change it for your needs.
	Improvements are welcome: sqldalmaker@gmail.com

	Demo project: https://github.com/panedrone/sdm_demo_todolist_golang
*/

type DataStore interface {
	Session(ctx context.Context) *gorm.DB

	Open() error
	Close() error

	Begin(ctx context.Context) (txCtx context.Context, err error)
	Commit(txCtx *context.Context) error
	Rollback(txCtx *context.Context) error

	// CRUD

	Create(ctx context.Context, table string, dataObjRef interface{}) error
	ReadAll(ctx context.Context, table string, sliceOfDataObjRef interface{}) error
	Read(ctx context.Context, table string, dataObjRef interface{}, pk ...interface{}) error
	Update(ctx context.Context, table string, dataObjRef interface{}) (rowsAffected int64, err error)
	Delete(ctx context.Context, table string, dataObjRef interface{}) (rowsAffected int64, err error)

	// raw-SQL

	Exec(ctx context.Context, sqlString string, args ...interface{}) (res int64, err error)
	Query(ctx context.Context, sqlString string, dest interface{}, args ...interface{}) error
	QueryAll(ctx context.Context, sqlString string, onRow func(interface{}), args ...interface{}) error
	QueryRow(ctx context.Context, sqlString string, args ...interface{}) (data map[string]interface{}, err error)
	QueryAllRows(ctx context.Context, sqlString string, onRow func(map[string]interface{}), args ...interface{}) error

	QueryByFA(ctx context.Context, sqlString string, fa interface{}, args ...interface{}) error
	QueryAllByFA(ctx context.Context, sqlString string, onRow func() (fa interface{}, onRowCompleted func()), args ...interface{}) error

	PGFetch(cursor string) string
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
	Dest interface{}
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
	Dest interface{}
}

type _DS struct {
	paramPrefix string
	rootDb      *gorm.DB
}

func _close(obj io.Closer) {
	_ = obj.Close()
}

func (ds *_DS) isPgSQL() bool {
	return ds.paramPrefix == "$"
}

func (ds *_DS) isOracle() bool {
	return ds.paramPrefix == ":"
}

func (ds *_DS) isMsSql() bool {
	return ds.paramPrefix == "@p"
}

/*
	Implement the method initDb() in an external file. This is an example:

// data_store_sqlx_ex.go

package dbal

import (
	"github.com/cengsin/oracle"
	// "github.com/go-sql-driver/mysql"
	// "gorm.io/driver/postgres"
	// "gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func (ds *_DS) initDb() (err error) {
	// === SQLite ==============================
	//ds.rootDb, err = gorm.Open(sqlite.Open("./todolist.sqlite"), &gorm.Config{
	//	Logger: logger.Default.LogMode(logger.Info),
	//})
	// === MySQL ===============================
	//dsn := "root:sa@tcp(127.0.0.1:3306)/my_tests?charset=utf8mb4&parseTime=True&loc=Local"
	//ds.rootDb, err = gorm.Open(mysql.Open(dsn), &gorm.Config{
	//	Logger: logger.Default.LogMode(logger.Info),
	//})
	// === PostgeSQL ===========================
	//dsn := "host=localhost user=postgres password=sa dbname=my_tests port=5432 sslmode=disable"
	//ds.rootDb, err = gorm.Open(postgres.Open(dsn), &gorm.Config{
	//	Logger: logger.Default.LogMode(logger.Info),
	//})
	// === Oracle ==============================
	dsn := "MY_TESTS/sa@127.0.0.1:1521/XE?charset=utf8mb4&parseTime=True&loc=Local"
	ds.rootDb, err = gorm.Open(oracle.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	return
}

var _ds = &_DS{}

func Ds() DataStore {
	return _ds
}

func NewTasksDao() *TasksDao {
	return &TasksDao{ds: ds}
}

*/

func (ds *_DS) Open() error {
	return ds.initDb()
}

func (ds *_DS) Close() error {
	// https://stackoverflow.com/questions/63816057/how-do-i-close-database-instance-in-gorm-1-20-0
	sqlDB, err := ds.rootDb.DB()
	if err != nil {
		return err
	}
	err = sqlDB.Close()
	if err != nil {
		return err
	}
	return nil
}

func (ds *_DS) getTx(ctx context.Context) *gorm.DB {
	tx, _ := ctx.Value("getTx").(*gorm.DB)
	return tx
}

func (ds *_DS) Begin(ctx context.Context) (txCtx context.Context, err error) {
	tx := ds.getTx(ctx)
	if tx != nil {
		return nil, errors.New("getTx already started")
	}
	tx = ds.rootDb.WithContext(ctx).Begin()
	txCtx = context.WithValue(ctx, "getTx", tx)
	return
}

func (ds *_DS) Commit(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return errors.New("no getTx to commit")
	}
	tx := ds.getTx(*txCtx)
	if tx == nil {
		return errors.New("ds.getTx not started")
	}
	tx.Commit()
	*txCtx = nil
	return
}

func (ds *_DS) Rollback(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return nil // commit() was called, just do nothing
	}
	tx := ds.getTx(*txCtx)
	if tx == nil {
		return errors.New("ds.getTx not started")
	}
	tx.Rollback()
	*txCtx = nil
	return
}

// CRUD -----------------------------------

func (ds *_DS) Session(ctx context.Context) *gorm.DB {
	tx := ds.getTx(ctx)
	if tx == nil {
		return ds.rootDb.WithContext(ctx)
	}
	return tx
}

func (ds *_DS) Create(ctx context.Context, table string, dataObjRef interface{}) error {
	return ds.Session(ctx).Table(table).Create(dataObjRef).Error
}

func (ds *_DS) ReadAll(ctx context.Context, table string, sliceOfDataObjRef interface{}) error {
	return ds.Session(ctx).Table(table).Find(sliceOfDataObjRef).Error
}

func (ds *_DS) Read(ctx context.Context, table string, dataObjRef interface{}, pk ...interface{}) error {
	return ds.Session(ctx).Table(table).Take(dataObjRef, pk...).Error
}

func (ds *_DS) Update(ctx context.Context, table string, dataObjRef interface{}) (rowsAffected int64, err error) {
	db := ds.Session(ctx).Table(table).Updates(dataObjRef)
	err = db.Error
	rowsAffected = db.RowsAffected
	return
}

func (ds *_DS) Delete(ctx context.Context, table string, dataObjRef interface{}) (rowsAffected int64, err error) {
	db := ds.Session(ctx).Table(table).Delete(dataObjRef)
	err = db.Error
	rowsAffected = db.RowsAffected
	return
}

// raw-SQL --------------------------------

func (ds *_DS) rawQuery(ctx context.Context, sqlString string, args ...interface{}) (*sql.Rows, error) {
	raw := ds.Session(ctx).Raw(sqlString, args...)
	return raw.Rows()
}

func (ds *_DS) rawExec(ctx context.Context, sqlString string, args ...interface{}) (rowsAffected int64, err error) {
	res := ds.Session(ctx).Exec(sqlString, args...)
	return res.RowsAffected, res.Error
}

func (ds *_DS) PGFetch(cursor string) string {
	return fmt.Sprintf(`fetch all from "%s"`, cursor)
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
		err = errors.New("Out/InOut -> Dest is nil")
	} else if !_isPtr(dest) {
		err = errors.New("Out/InOut -> Dest must be a Ptr")
	} else if _pointsToNil(dest) {
		err = errors.New("Out/InOut -> Dest points to nil")
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
		case func() (interface{}, func()), func() ([]interface{}, func()):
			if implicitCursors {
				err = errors.New(fmt.Sprintf("Forbidden: %v", args))
				return
			}
			outCursors = true
			*onRowArr = append(*onRowArr, param) // add single func
			var rows driver.Rows
			*queryArgs = append(*queryArgs, sql.Out{Dest: &rows, In: false})
		case *Out:
			err = _validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: false})
		case Out:
			err = _validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: false})
		case *InOut:
			err = _validateDest(param.Dest)
			if err != nil {
				return
			}
			*queryArgs = append(*queryArgs, sql.Out{Dest: param.Dest, In: true})
		case InOut:
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

func (ds *_DS) queryAllImplicitRcOracle(ctx context.Context, sqlString string, onRowArr []interface{}, queryArgs ...interface{}) (err error) {
	rows, err := ds.rawQuery(ctx, sqlString, queryArgs...)
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
			colNames, data, values, valuePointers, pfeErr := ds.prepareFetch(rows)
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
			err = ds.fetchRows(rows, onRow)
			if err != nil {
				return
			}
		default:
			return errUnexpectedType(onRow)
		}
		onRowIndex++
	}
	return
}

func (ds *_DS) fetchRows(rows *sql.Rows, onRow func() (interface{}, func())) (err error) {
	for rows.Next() {
		fa, onRowCompleted := onRow()
		switch _fa := fa.(type) {
		case []interface{}:
			err = rows.Scan(_fa...)
		default:
			err = errUnexpectedType(_fa)
		}
		if err != nil {
			return
		}
		onRowCompleted()
	}
	return
}

func (ds *_DS) queryAllImplicitRcMySQL(ctx context.Context, sqlString string, onRowArr []interface{}, queryArgs ...interface{}) (err error) {
	rows, err := ds.rawQuery(ctx, sqlString, queryArgs...)
	if err != nil {
		return
	}
	defer _close(rows)
	onRowIndex := 0
	for {
		switch onRow := onRowArr[onRowIndex].(type) {
		case func(map[string]interface{}):
			// re-detect columns for each ResultSet
			colNames, data, values, valuePointers, pfeErr := ds.prepareFetch(rows)
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
			err = ds.fetchRows(rows, onRow)
			if err != nil {
				return
			}
		default:
			return errUnexpectedType(onRow)
		}
		if !rows.NextResultSet() {
			break
		}
		onRowIndex++
	}
	return
}

func (ds *_DS) exec2(ctx context.Context, sqlString string, onRowArr []interface{}, args ...interface{}) (rowsAffected int64, err error) {
	rowsAffected, err = ds.rawExec(ctx, sqlString, args...)
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
	return
}

func fetchDriverRows(rows driver.Rows, onRowFunc interface{}) error {
	defer _close(rows)
	colNames := rows.Columns()
	values := make([]driver.Value, len(colNames))
	switch onRow := onRowFunc.(type) {
	case func(map[string]interface{}):
		data := make(map[string]interface{})
		for {
			err := rows.Next(values)
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
			err := rows.Next(values)
			if err != nil {
				if "EOF" == err.Error() {
					break
				}
				return err
			}
			fa, onRowCompleted := onRow()
			switch _fa := fa.(type) {
			case []interface{}:
				for i, v := range values {
					err = _setAny(_fa[i], v)
					if err != nil {
						return err
					}
				}
				onRowCompleted()
			default:
				return errUnexpectedType(fa)
			}
		}
	default:
		return errUnexpectedType(onRowFunc)
	}
	return nil
}

func (ds *_DS) Exec(ctx context.Context, sqlString string, args ...interface{}) (rowsAffected int64, err error) {
	sqlString = ds.formatSQL(sqlString)
	var onRowArr []interface{}
	var queryArgs []interface{}
	// Syntax like [on_test1:Test, on_test2:Test] is used to call SP with IMPLICIT cursors
	implicitCursors, _, err := ds._processExecParams(args, &onRowArr, &queryArgs)
	if err != nil {
		return
	}
	if implicitCursors {
		if ds.isOracle() {
			err = ds.queryAllImplicitRcOracle(ctx, sqlString, onRowArr, queryArgs...)
		} else {
			err = ds.queryAllImplicitRcMySQL(ctx, sqlString, onRowArr, queryArgs...) // it works with MySQL SP
		}
		return
	}
	return ds.exec2(ctx, sqlString, onRowArr, queryArgs...)
}

func (ds *_DS) Query(ctx context.Context, sqlString string, dest interface{}, args ...interface{}) error {
	sqlString = ds.formatSQL(sqlString)
	var onRowArr []interface{}
	var queryArgs []interface{}
	implicitCursors, outCursors, err := ds._processExecParams(args, &onRowArr, &queryArgs)
	if err != nil {
		return err
	}
	if implicitCursors || outCursors {
		err = errors.New("not supported in Query: implicitCursors || outCursors")
		return err
	}
	rows, err := ds.rawQuery(ctx, sqlString, queryArgs...)
	if err != nil {
		return err
	}
	defer _close(rows)
	_, _, values, valuePointers, err := ds.prepareFetch(rows)
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
	err = SetRes(dest, values)
	if err != nil {
		return err
	}
	if rows.Next() {
		return errMultipleRows(sqlString)
	}
	return nil
}

func (ds *_DS) QueryAll(ctx context.Context, sqlString string, onRow func(interface{}), args ...interface{}) (err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.rawQuery(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	for {
		// re-detect columns for each ResultSet
		// fetch all columns! if to fetch less, Scan returns nil-s
		_, _, values, valuePointers, pfErr := ds.prepareFetch(rows)
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

func (ds *_DS) QueryRow(ctx context.Context, sqlString string, args ...interface{}) (row map[string]interface{}, err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.rawQuery(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	colNames, row, values, valuePointers, pfErr := ds.prepareFetch(rows)
	if pfErr != nil {
		err = pfErr
		return
	}
	if !rows.Next() {
		err = errNoRows(sqlString)
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
		err = errMultipleRows(sqlString)
		return
	}
	return
}

func (ds *_DS) QueryAllRows(ctx context.Context, sqlString string, onRow func(map[string]interface{}), args ...interface{}) (err error) {
	// many thanks to:
	// https://stackoverflow.com/questions/51731423/how-to-read-a-row-from-a-table-to-a-map-without-knowing-columns
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.rawQuery(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	for {
		// re-detect columns for each ResultSet
		colNames, data, values, valuePointers, pfErr := ds.prepareFetch(rows)
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

func (ds *_DS) QueryByFA(ctx context.Context, sqlString string, dest interface{}, args ...interface{}) (err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.rawQuery(ctx, sqlString, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	if !rows.Next() {
		err = errNoRows(sqlString)
		return
	}
	faArr, ok := dest.([]interface{})
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

func errUnexpectedType(val interface{}) error {
	return errors.New(fmt.Sprintf("unexpected type: %v", reflect.TypeOf(val)))
}

func errNoRows(sqlString string) error {
	return errors.New(fmt.Sprintf("no rows found for %s", sqlString))
}

func errMultipleRows(sqlString string) error {
	return errors.New(fmt.Sprintf("more than 1 row found for %s", sqlString))
}

func (ds *_DS) QueryAllByFA(ctx context.Context, sqlString string, onRow func() (interface{}, func()), args ...interface{}) (err error) {
	sqlString = ds.formatSQL(sqlString)
	rows, err := ds.rawQuery(ctx, sqlString, args...)
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
				err = errUnexpectedType(fa)
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

	func (ds *_DS) prepareFetch(rows *sql.Rows) ([]string, map[string]interface{}, []string, []interface{}) {
		// ...
		values := make([]string, len(colNames))
*/
func (ds *_DS) prepareFetch(rows *sql.Rows) (colNames []string, data map[string]interface{}, values []interface{}, valuePointers []interface{}, err error) {
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

func (ds *_DS) formatSQL(sqlString string) string {

	// === seems that '?' is ok for GORM

	//if len(ds.paramPrefix) == 0 {
	//	return sqlString
	//}
	//i := 1
	//for {
	//	pos := strings.Index(sqlString, "?")
	//	if pos == -1 {
	//		break
	//	}
	//	str1 := sqlString[0:pos]
	//	str2 := sqlString[pos+1:]
	//	sqlString = str1 + ds.paramPrefix + strconv.Itoa(i) + str2
	//	i += 1
	//}
	return sqlString
}

func SetString(d *string, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		err = _setString(d, value)
		_updateErrMap(err, colName, errMap)
	}
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

func SetInt64(d *int64, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		err = _setInt64(d, value)
		_updateErrMap(err, colName, errMap)
	}
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

func SetInt32(d *int32, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		err = _setInt32(d, value)
		_updateErrMap(err, colName, errMap)
	}
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

func SetFloat32(d *float32, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		err = _setFloat32(d, value)
		_updateErrMap(err, colName, errMap)
	}
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

func SetFloat64(d *float64, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		err = _setFloat64(d, value)
		_updateErrMap(err, colName, errMap)
	}
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

func SetTime(d *time.Time, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		err = _setTime(d, value)
		_updateErrMap(err, colName, errMap)
	}
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

func SetBool(d *bool, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		err = _setBool(d, value)
		_updateErrMap(err, colName, errMap)
	}
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

func SetBytes(d *[]byte, row map[string]interface{}, colName string, errMap map[string]int) {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		err = _setBytes(d, value)
		_updateErrMap(err, colName, errMap)
	}
}

func _setBytes(d *[]byte, value interface{}) error {
	switch v := value.(type) {
	case []byte:
		*d = v
	default:
		return unknownTypeErr(d, value, "_setBytes")
	}
	return nil
}

//func SetNumber(d *godror.Number, row map[string]interface{}, colName string, errMap map[string]int) {
//	value, err := _getValue(row, colName, errMap)
//	if err == nil {
//		err = _setNumber(d, value)
//		_updateErrMap(err, colName, errMap)
//	}
//}
//
//func _setNumber(d *godror.Number, value interface{}) error {
//	err := d.Scan(value)
//	return err
//}

//func SetUUID(d *uuid.UUID, row map[string]interface{}, colName string, errMap map[string]int) {
//	value, err := _getValue(row, colName, errMap)
//	if err == nil {
//		err = _setUUID(d, value)
//		_updateErrMap(err, colName, errMap)
//	}
//}
//
//func _setUUID(d *uuid.UUID, value interface{}) error {
//	switch bv := value.(type) {
//	case []byte:
//		err := d.Scan(bv)
//		if err != nil {
//			return assignErr(d, value, "_setAny", err.Error())
//		}
//		return nil
//	default:
//		return unknownTypeErr(d, value, "_setAny")
//	}
//}

func assignErr(dstPtr interface{}, value interface{}, funcName string, errMsg string) error {
	return errors.New(fmt.Sprintf("%s %T <- %T %s", funcName, dstPtr, value, errMsg))
}

func unknownTypeErr(dstPtr interface{}, value interface{}, funcName string) error {
	return assignErr(dstPtr, value, funcName, "unknown type")
}

func _getValue(row map[string]interface{}, colName string, errMap map[string]int) (value interface{}, err error) {
	var ok bool
	value, ok = row[colName]
	if !ok {
		key := fmt.Sprintf("[%s] no such column", colName)
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

func _updateErrMap(err error, colName string, errMap map[string]int) {
	if err == nil {
		return
	}
	key := fmt.Sprintf("[%s] %s", colName, err.Error())
	count, ok := errMap[key]
	if ok {
		errMap[key] = count + 1
	} else {
		errMap[key] = 1
	}
}

func _setAny(dstPtr interface{}, value interface{}) error {
	if value == nil {
		switch d := dstPtr.(type) {
		case *interface{}:
			*d = nil
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
		err = _setBytes(d, value)
	//case *godror.Number:
	//	err = _setNumber(d, value)
	//case *uuid.UUID:
	//	err = _setUUID(d, value)
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
