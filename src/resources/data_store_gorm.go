package dbal

import (
	"context"
	"database/sql"
	"database/sql/driver"
	"errors"
	"fmt"
	//	"github.com/godror/godror"
	"gorm.io/gorm"
	"io"
	"reflect"
	"strconv"
	"strings"
	"time"
)

/*
	This file is a part of SQL DAL Maker project: https://sqldalmaker.sourceforge.net
	It demonstrates how to implement interface DataStore in Go + Gorm.
	More about DataStore: https://sqldalmaker.sourceforge.net/data_store.html
	Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_gorm.go

	Successfully tested with:

		- "gorm.io/driver/sqlite"
		- "gorm.io/driver/mysql"
		- "github.com/cengsin/oracle"
		- "gorm.io/driver/postgres"

	Copy-paste this code to your project and change it for your needs.
	Improvements are welcome: sqldalmaker@gmail.com
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

	Exec(ctx context.Context, sqlStr string, args ...interface{}) (res int64, err error)
	Query(ctx context.Context, sqlStr string, args ...interface{}) (res interface{}, err error)
	QueryAll(ctx context.Context, sqlStr string, onRow func(interface{}), args ...interface{}) error
	QueryRow(ctx context.Context, sqlStr string, args ...interface{}) (data map[string]interface{}, err error)
	QueryAllRows(ctx context.Context, sqlStr string, onRow func(map[string]interface{}), args ...interface{}) error

	QueryByFA(ctx context.Context, sqlStr string, fa interface{}, args ...interface{}) error
	QueryAllByFA(ctx context.Context, sqlStr string, onRow func() (fa interface{}, onRowCompleted func()), args ...interface{}) error

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
	rootDb      *gorm.DB
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

// data_store_gorm_ex.go

package dbal

import (
	"context"
	"github.com/cengsin/oracle"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var ds = &_DS{}

func (ds *_DS) initDb() (err error) {
	dsn := "MY_TESTS/sa@127.0.0.1:1521/XEPDB1?charset=utf8mb4&parseTime=True&loc=Local"
	ds.rootDb, err = gorm.Open(oracle.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	return
}

// it can be used in a middleware to start separate sessions for incoming web-requests
//
// api := r.Group("", func(ctx *gin.Context) {
//		ctx.Set("db", dal.WithContext(ctx))
//	})

func WithContext(ctx context.Context) *gorm.DB {
	return ds.rootDb.WithContext(ctx)
}

func OpenDB() error {
	return ds.Open()
}

func CloseDB() error {
	return ds.Close()
}

func NewGroupsDao() *GroupsDao {
	return &GroupsDao{ds: ds}
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

func (ds *_DS) db(ctx context.Context) *gorm.DB {
	db, ok := ctx.Value("db").(*gorm.DB)
	if ok {
		return db
	}
	return ds.rootDb
}

func (ds *_DS) tx(ctx context.Context) *gorm.DB {
	tx, _ := ctx.Value("tx").(*gorm.DB)
	return tx
}

func (ds *_DS) Begin(ctx context.Context) (txCtx context.Context, err error) {
	tx := ds.tx(ctx)
	if tx != nil {
		return nil, errors.New("tx already started")
	}
	tx = ds.db(ctx).Begin()
	txCtx = context.WithValue(ctx, "tx", tx)
	return
}

func (ds *_DS) Commit(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return errors.New("no tx to commit")
	}
	tx := ds.tx(*txCtx)
	if tx == nil {
		return errors.New("ds.tx not started")
	}
	tx.Commit()
	*txCtx = nil // to prevent ds.tx.Rollback() in defer
	return
}

func (ds *_DS) Rollback(txCtx *context.Context) (err error) {
	if txCtx == nil {
		return nil // commit() was called, just do nothing
	}
	tx := ds.tx(*txCtx)
	if tx == nil {
		return errors.New("ds.tx not started")
	}
	tx.Rollback()
	*txCtx = nil
	return
}

// CRUD -----------------------------------

func (ds *_DS) Session(ctx context.Context) *gorm.DB {
	tx := ds.tx(ctx)
	if tx == nil {
		return ds.db(ctx)
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

func (ds *_DS) _query(ctx context.Context, sqlStr string, args ...interface{}) (*sql.Rows, error) {
	raw := ds.Session(ctx).Raw(sqlStr, args...)
	return raw.Rows()
}

func (ds *_DS) _exec(ctx context.Context, sqlStr string, args ...interface{}) (rowsAffected int64, err error) {
	res := ds.Session(ctx).Exec(sqlStr, args...)
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
		// 1) unlike MySQL, it must be done before _prepareFetch -> rows.Next()
		// 2) at the moment, it does not work with multiple Implicit RC
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
			err = ds._fetchRows(ctx, rows, onRow)
			if err != nil {
				return
			}
		default:
			return errors.New(fmt.Sprintf("Unexpected type: %v", onRow))
		}
		onRowIndex++
	}
	return
}

func (ds *_DS) _fetchRows(ctx context.Context, rows *sql.Rows, onRow func() (interface{}, func())) (err error) {
	for rows.Next() {
		fa, onRowCompleted := onRow()
		switch _fa := fa.(type) {
		case []interface{}:
			err = rows.Scan(_fa...)
		case interface{}:
			err = ds.Session(ctx).ScanRows(rows, fa)
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
			err = ds._fetchRows(ctx, rows, onRow)
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
	rowsAffected, err = ds._exec(ctx, sqlStr, args...)
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
	case func() ([]interface{}, func()):
		for {
			err = rows.Next(values)
			if err != nil {
				break
			}
			fa, onRowCompleted := onRow()
			for i, v := range values {
				err = SetRes(fa[i], v)
				if err != nil {
					return
				}
			}
			onRowCompleted()
		}
	default:
		return errors.New(fmt.Sprintf("Unexpected type: %v", reflect.TypeOf(onRow)))
	}
	return
}

func (ds *_DS) Exec(ctx context.Context, sqlStr string, args ...interface{}) (res int64, err error) {
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

func (ds *_DS) QueryRow(ctx context.Context, sqlStr string, args ...interface{}) (data map[string]interface{}, err error) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	colNames, data, values, valuePointers, pfErr := ds._prepareFetch(rows)
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
		data[colName] = values[i]
	}
	if rows.Next() {
		err = errors.New(fmt.Sprintf("More than 1 row found for %s", sqlStr))
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
		err = ds.Session(ctx).ScanRows(rows, fa)
	}
	if err != nil {
		return
	}
	if rows.Next() {
		err = errors.New(fmt.Sprintf("More than 1 row found for %s", sqlStr))
	}
	return
}

func (ds *_DS) QueryAllByFA(ctx context.Context, sqlStr string, onRow func() (fa interface{}, onRowCompleted func()), args ...interface{}) (err error) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(ctx, sqlStr, args...)
	if err != nil {
		return
	}
	defer _close(rows)
	for {
		for rows.Next() {
			fa, onRowCompleted := onRow()
			faArr, ok := fa.([]interface{})
			if ok {
				err = rows.Scan(faArr...)
			} else {
				err = ds.Session(ctx).ScanRows(rows, fa)
			}
			if err != nil {
				return
			}
			onRowCompleted()
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
	}
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

func SetBytes(d *[]byte, row map[string]interface{}, colName string, errMap map[string]int) error {
	value, err := _getValue(row, colName, errMap)
	if err == nil {
		return _setBytes(d, value)
	}
	return err
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

//func SetNumber(d *godror.Number, row map[string]interface{}, colName string, errMap map[string]int) error {
//	value, err := _getValue(row, colName, errMap)
//	if err == nil {
//		return _setNumber(d, value)
//	}
//	return err
//}
//
//func _setNumber(d *godror.Number, value interface{}) error {
//	err := d.Scan(value)
//	return err
//}

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
