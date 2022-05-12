package dal

import (
	"database/sql"
	"database/sql/driver"
	"errors"
	"fmt"
	"gorm.io/gorm"
	"reflect"
	"strconv"
	"strings"
	"time"
)

/*
   SQL DAL Maker Web-Site: http://sqldalmaker.sourceforge.net
   This is an example of how to implement the class DataStore for Go + GORM.
   Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store_gorm.go
   Copy-paste this code to your project and change it for your needs.
   Improvements are welcome: sqldalmaker@gmail.com
*/

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

type DataStore struct {
	paramPrefix string
	db          *gorm.DB // public
	tx          *gorm.DB
}

func (ds *DataStore) isPostgreSQL() bool {
	return ds.paramPrefix == "$"
}

func (ds *DataStore) isOracle() bool {
	return ds.paramPrefix == ":"
}

func (ds *DataStore) isSqlServer() bool {
	return ds.paramPrefix == "@p"
}

/*
	Locate the method initDb() in an external file. This is an example:

// data_store_gorm_ex.go

package dal

import (
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

var ds = &DataStore{}

func (ds *DataStore) initDb() (err error) {
	ds.db, err = gorm.Open(sqlite.Open("./todolist.sqlite3"), &gorm.Config{})
	return
}

func Db() *gorm.DB {
	return ds.db
}
func OpenDB() error {
	return ds.Open()
}

func CloseDB() error {
	return ds.Close()
}

func NewTasksDao() *TasksDao {
	return &TasksDao{Ds: ds}
}

func NewGroupsDao() *GroupsDao {
	return &GroupsDao{Ds: ds}
}

*/

func (ds *DataStore) Open() error {
	return ds.initDb()
}

func (ds *DataStore) Close() (err error) {
	// ds.db.Close() // TODO
	return
}

func (ds *DataStore) Begin() (err error) {
	if ds.tx != nil {
		return errors.New("ds.tx already started")
	}
	ds.tx = ds.db.Begin()
	return
}

func (ds *DataStore) Commit() (err error) {
	if ds.tx == nil {
		return errors.New("ds.tx not started")
	}
	ds.tx.Commit()
	ds.tx = nil // to prevent ds.tx.Rollback() in defer
	return
}

func (ds *DataStore) Rollback() (err error) {
	if ds.tx == nil {
		return nil // commit() was called, just do nothing:
	}
	ds.tx.Rollback()
	ds.tx = nil
	return
}

func (ds *DataStore) _query(sqlStr string, args ...interface{}) (*sql.Rows, error) {
	var raw *gorm.DB
	if ds.tx == nil {
		raw = ds.db.Raw(sqlStr, args...)
	} else {
		raw = ds.tx.Raw(sqlStr, args...)
	}
	// func (db *DB) Rows() (*sql.Rows, error) {
	return raw.Rows()
}

func (ds *DataStore) _exec(sqlStr string, args ...interface{}) error {
	if ds.tx == nil { // TODO no sql.Result
		return ds.db.Exec(sqlStr, args...).Error
	} else {
		return ds.tx.Exec(sqlStr, args...).Error
	}
}

func (ds *DataStore) PGFetch(cursor string) string {
	return fmt.Sprintf(`fetch all from "%s"`, cursor)
}

func (ds *DataStore) _pgInsert(sqlStr string, aiNames string, args ...interface{}) (id interface{}, err error) {
	// fetching of multiple AI values is not implemented so far:
	sqlStr += " RETURNING " + aiNames
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		return nil, err
	}
	defer func() {
		err = rows.Close()
	}()
	if rows.Next() {
		var data interface{}
		err = rows.Scan(&data)
		return data, err
	}
	err = errors.New("rows.Next() FAILED:" + sqlStr)
	return
}

func (ds *DataStore) _oracleInsert(sqlStr string, aiNames string, args ...interface{}) (id interface{}, err error) {
	// fetching of multiple AI values is not implemented so far:
	sqlStr = fmt.Sprintf("%s returning %s  into :%d", sqlStr, aiNames, len(args)+1)
	// https://ddcode.net/2019/05/11/how-does-go-call-oracles-stored-procedures-and-get-the-return-value-of-the-stored-procedures/
	var id64 float64
	// var id64 interface{} --- error
	// var id64 int64 --- it works, but...
	args = append(args, sql.Out{Dest: &id64, In: false})
	// ----------------
	err = ds._exec(sqlStr, args...)
	if err != nil {
		return
	}
	return id64, nil
}

func (ds *DataStore) _mssqlInsert(sqlStr string, args ...interface{}) (id interface{}, err error) {
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// LastInsertId should not be used with this driver (or SQL Server) due to
	// how the TDS protocol works. Please use the OUTPUT Clause or add a select
	// ID = convert(bigint, SCOPE_IDENTITY()); to the end of your query (ref SCOPE_IDENTITY).
	//  This will ensure you are getting the correct ID and will prevent a network round trip.
	sqlStr += ";SELECT @@IDENTITY;"
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		return
	}
	defer func() {
		err = rows.Close()
	}()
	if rows.Next() {
		var data interface{}
		err = rows.Scan(&data) // returns []uint8
		return data, err
	}
	err = errors.New("SELECT @@IDENTITY FAILED: " + sqlStr)
	return
}

//func (ds *DataStore) _defaultInsert(sqlStr string, args ...interface{}) (id interface{}, err error) {
//	//res, err := ds._exec(sqlStr, args...)
//	//if err != nil {
//	//	return
//	//}
//	//id, err = res.LastInsertId()
//	//
//	//
//	return
//}

func (ds *DataStore) Insert(sqlStr string, aiNames string, args ...interface{}) (id interface{}, err error) {
	if len(aiNames) == 0 { // len(nil) == 0
		err = errors.New("DataStore.insert is not applicable for aiNames = " + aiNames)
		return
	}
	// Builtin LastInsertId works only with MySQL and SQLite3
	sqlStr = ds._formatSQL(sqlStr)
	if ds.isPostgreSQL() {
		return ds._pgInsert(sqlStr, aiNames, args...)
	} else if ds.isSqlServer() {
		return ds._mssqlInsert(sqlStr, args...)
	} else if ds.isOracle() {
		// Oracle: specify AI values explicitly:
		// <crud-auto dto="ProjectInfo" table="PROJECTS" generated="P_ID"/>
		return ds._oracleInsert(sqlStr, aiNames, args...)
	}
	return -1, errors.New("nothing like LastInsertId in GORM :(") // TODO
	// return ds._defaultInsert(sqlStr, args...)
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

func (ds *DataStore) _processExecParams(args []interface{}, onRowArr *[]func(map[string]interface{}),
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
			*onRowArr = append(*onRowArr, param...) // add an array of func
		case func(map[string]interface{}):
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

func (ds *DataStore) _queryAllImplicitRcOracle(sqlStr string, onRowArr []func(map[string]interface{}), queryArgs ...interface{}) (err error) {
	rows, err := ds._query(sqlStr, queryArgs...)
	if err != nil {
		return
	}
	defer func() {
		err = rows.Close()
	}()
	onRowIndex := 0
	for {
		// 1) unlike MySQL, it must be done before _prepareFetch -> rows.Next()
		// 2) at the moment, it does not work with multiple Implicit RC
		if !rows.NextResultSet() {
			break
		}
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
			onRowArr[onRowIndex](data)
		}
		onRowIndex++
	}
	return
}

func (ds *DataStore) _queryAllImplicitRcMySQL(sqlStr string, onRowArr []func(map[string]interface{}), queryArgs ...interface{}) (err error) {
	rows, err := ds._query(sqlStr, queryArgs...)
	if err != nil {
		return
	}
	defer func() {
		err = rows.Close()
	}()
	onRowIndex := 0
	for {
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
			onRowArr[onRowIndex](data)
		}
		if !rows.NextResultSet() {
			break
		}
		onRowIndex++
	}
	return
}

func (ds *DataStore) _exec2(sqlStr string, onRowArr []func(map[string]interface{}), args ...interface{}) (execRes int64, err error) {
	err = ds._exec(sqlStr, args...)
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
	// TODO no sql.Result
	//execRes, err = res.RowsAffected()
	//if err != nil {
	//	execRes = -1
	//}
	return
}

func _fetchAllFromCursor(rows driver.Rows, onRow func(map[string]interface{})) (err error) {
	defer func() {
		err = rows.Close()
	}()
	colNames := rows.Columns()
	data := make(map[string]interface{})
	values := make([]driver.Value, len(colNames))
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
	return
}

func (ds *DataStore) Exec(sqlStr string, args ...interface{}) (res int64, err error) {
	sqlStr = ds._formatSQL(sqlStr)
	var onRowArr []func(map[string]interface{})
	var queryArgs []interface{}
	// Syntax like [on_test1:Test, on_test2:Test] is used to call SP with IMPLICIT cursors
	implicitCursors, _, err := ds._processExecParams(args, &onRowArr, &queryArgs)
	if err != nil {
		return
	}
	if implicitCursors {
		if ds.isOracle() {
			err = ds._queryAllImplicitRcOracle(sqlStr, onRowArr, queryArgs...)
		} else {
			err = ds._queryAllImplicitRcMySQL(sqlStr, onRowArr, queryArgs...) // it works with MySQL SP
		}
		return
	}
	return ds._exec2(sqlStr, onRowArr, queryArgs...)
}

func (ds *DataStore) _queryRowValues(sqlStr string, queryArgs ...interface{}) (values []interface{}, err error) {
	rows, err := ds._query(sqlStr, queryArgs...)
	if err != nil {
		return
	}
	defer func() {
		err = rows.Close()
	}()
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
			err = assign(arg, values[outParamIndex])
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

func (ds *DataStore) Query(sqlStr string, args ...interface{}) (arr interface{}, err error) {
	sqlStr = ds._formatSQL(sqlStr)
	var onRowArr []func(map[string]interface{})
	var queryArgs []interface{}
	implicitCursors, outCursors, err := ds._processExecParams(args, &onRowArr, &queryArgs)
	if err != nil {
		return
	}
	if implicitCursors || outCursors {
		err = errors.New("not supported in Query: implicitCursors || outCursors")
		return
	}
	arr, err = ds._queryRowValues(sqlStr, queryArgs...)
	return // it returns []interface{} for cases like 'SELECT @value, @name;'
}

func (ds *DataStore) QueryAll(sqlStr string, onRow func(interface{}), args ...interface{}) (err error) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		return
	}
	defer func() {
		err1 := rows.Close()
		if err1 != nil {
			err = err1
		}
	}()
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

func (ds *DataStore) QueryRow(sqlStr string, args ...interface{}) (data map[string]interface{}, err error) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		return
	}
	defer func() {
		err = rows.Close()
	}()
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

func (ds *DataStore) QueryAllRows(sqlStr string, onRow func(map[string]interface{}), args ...interface{}) (err error) {
	// many thanks to:
	// https://stackoverflow.com/questions/51731423/how-to-read-a-row-from-a-table-to-a-map-without-knowing-columns
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		return
	}
	defer func() {
		err = rows.Close()
	}()
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

/*
// MySQL: if string is ok for all types (no conversions needed), use this:
func (ds *DataStore) _prepareFetch(rows *sql.Rows) ([]string, map[string]interface{}, []string, []interface{}) {
	// ...
	values := make([]string, len(colNames))
}
*/
func (ds *DataStore) _prepareFetch(rows *sql.Rows) (colNames []string, data map[string]interface{}, values []interface{}, valuePointers []interface{}, err error) {
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

func (ds *DataStore) _formatSQL(sqlStr string) string {
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

func _assignString(d *string, value interface{}) bool {
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
		return false
	}
	return true
}

func _assignInt64(d *int64, value interface{}) bool {
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
		*d, _ = strconv.ParseInt(str, 10, 64)
	case string:
		str := value.(string)
		*d, _ = strconv.ParseInt(str, 10, 64)
	default:
		return false
	}
	return true
}

func _assignInt32(d *int32, value interface{}) bool {
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
		if err == nil {
			*d = int32(d64)
		}
	case string:
		d64, err := strconv.ParseInt(v, 10, 32)
		if err == nil {
			*d = int32(d64)
		}
	default:
		return false
	}
	return true
}

func _assignFloat32(d *float32, value interface{}) bool {
	switch v := value.(type) {
	case float32:
		*d = v
	case float64:
		*d = float32(v)
	case []byte:
		str := string(v) // PostgeSQL
		d64, _ := strconv.ParseFloat(str, 64)
		*d = float32(d64)
	case string:
		d64, _ := strconv.ParseFloat(v, 64) // Oracle
		*d = float32(d64)
	default:
		return false
	}
	return true
}

func _assignFloat64(d *float64, value interface{}) bool {
	switch v := value.(type) {
	case float64:
		*d = v
	case float32:
		*d = float64(v)
	case []byte:
		str := string(v) // PostgeSQL, MySQL
		*d, _ = strconv.ParseFloat(str, 64)
	case string:
		*d, _ = strconv.ParseFloat(v, 64) // Oracle
	default:
		return false
	}
	return true
}

func _assignTime(d *time.Time, value interface{}) bool {
	switch v := value.(type) {
	case time.Time:
		*d = v
	default:
		return false
	}
	return true
}

func _assignBoolean(d *bool, value interface{}) bool {
	switch v := value.(type) {
	case []byte:
		str := string(v) // MySQL
		db, _ := strconv.ParseBool(str)
		*d = db
	case bool:
		*d = v
	default:
		return false
	}
	return true
}

func _assign(dstRef interface{}, value interface{}) error {
	if value == nil {
		switch d := dstRef.(type) {
		case *interface{}:
			*d = nil
			return nil
		}
		return nil // leave as-is
	}
	assigned := false
	switch d := dstRef.(type) {
	case *string:
		assigned = _assignString(d, value)
	case *int32:
		assigned = _assignInt32(d, value)
	case *int64:
		assigned = _assignInt64(d, value)
	case *float64:
		assigned = _assignFloat64(d, value)
	case *float32:
		assigned = _assignFloat32(d, value)
	case *time.Time:
		assigned = _assignTime(d, value)
	case *bool:
		assigned = _assignBoolean(d, value)
	case *[]byte: // the same as uint8
		switch bv := value.(type) {
		case []byte:
			*d = bv
			return nil
		}
	case *interface{}:
		*d = value
		return nil
	}
	if !assigned {
		return errors.New(fmt.Sprintf("%T <- %T", dstRef, value))
	}
	return nil
}

func fromVal(dstRef interface{}, value interface{}, errMap map[string]int) {
	err := assign(dstRef, value)
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

func assign(dstRef interface{}, value interface{}) error {
	var err error
	switch v := value.(type) {
	case []interface{}:
		switch d := dstRef.(type) {
		case *[]interface{}:
			*d = v
		default:
			// use "default:" instead of "case []interface{}":
			// because "dstRef" may be "*float64" and value may be like
			// []interface{}{[]uint16{49, 46, 50, 48}} // []uint16 in here is a string like 1.20
			// (e.g. PG + Query(`select get_test_rating(?)`, tId))
			v0 := v[0]
			err = _assign(dstRef, v0)
		}
	default:
		err = _assign(dstRef, value)
	}
	return err
}

func fromRow(dstRef interface{}, row map[string]interface{}, colName string, errMap map[string]int) {
	value, ok := row[colName]
	if !ok {
		key := fmt.Sprintf("%s: no such column", colName)
		count, ok := errMap[key]
		if ok {
			errMap[key] = count + 1
		} else {
			errMap[key] = 1
		}
		return
	}
	err := assign(dstRef, value)
	if err != nil {
		key := fmt.Sprintf("%s: %s", colName, err.Error())
		count, ok := errMap[key]
		if ok {
			errMap[key] = count + 1
		} else {
			errMap[key] = 1
		}
	}
}

func errMapToErr(errMap map[string]int) (err error) {
	if len(errMap) > 0 {
		err = errors.New(fmt.Sprintf("%v", errMap))
	}
	return
}
