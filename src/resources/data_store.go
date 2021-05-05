package main

import (
	"database/sql"
	"database/sql/driver"
	"fmt"
	"reflect"
	"strconv"
	"strings"
	"time"
)

/*
   SQL DAL Maker Web-Site: http://sqldalmaker.sourceforge.net
   This is an example of how to implement DataStore in Go + "database/sql".
   Recent version: https://github.com/panedrone/sqldalmaker/blob/master/src/resources/data_store.go
   Copy-paste this code to your project and change it for your needs.
   Improvements are welcome: sdm@gmail.com
*/

type OutParam struct {
	/*

		var outRating float64 // <- no need to init it for OUT parameter
		cxDao.SpTestOutParams(47, OutParam{Dest: &inOut})
		// cxDao.SpTestOutParams(47, &outRating) // <- this one is also ok for OUT parameters
		fmt.Println(outRating)

	*/

	// Dest is a pointer to the value that will be set to the result of the
	// stored procedure's OUT parameter.
	Dest interface{}
}

type InOutParam struct {
	/*

		inOut := 123.0 // INOUT parameter must be initialised
		cxDao.SpTestInoutParams(InOutParam{Dest: &inOut})
		fmt.Println(inOut)

	*/

	// Dest is a pointer to the value that will be set to the result of the
	// stored procedure's OUT parameter.
	Dest interface{}
}

type DataStore struct {
	paramPrefix string
	handle      *sql.DB
	tx          *sql.Tx
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
	Locate function initDb(ds *DataStore) in an external file. This is an example:

// file data_store_db.go

package main

import (
	"database/sql"

   	// _ "github.com/mattn/go-sqlite3" // SQLite3
   	// _ "github.com/denisenkom/go-mssqldb" // SQL Server
   	// _ "github.com/godror/godror"			// Oracle
   	// only strings for MySQL (so far). see _prepareFetch below and related comments.
   	_ "github.com/go-sql-driver/mysql" // MySQL
   	// _ "github.com/ziutek/mymysql/godrv" // MySQL
   	// _ "github.com/lib/pq" // PostgeSQL
)

func initDb(ds *DataStore) {
	var err error
	// === PostgeSQL ===========================
	ds.paramPrefix = "$"
	ds.handle, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=disable")
	// ds.handle, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=verify-full")
	// === SQLite3 =============================
	// ds.handle, err = sql.Open("sqlite3", "./log.sqlite")
	// ds.handle, err = sql.Open("sqlite3", "./northwindEF.sqlite")
	// === MySQL ===============================
	// ds.handle, err = sql.Open("mysql", "root:root@/sakila")
	// ds.handle, err = sql.Open("mymysql", "sakila/root/root")
	// === SQL Server ==========================
	// https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	// ensure sqlserver:// in beginning. this one is not valid:
	// ------ ds.handle, err = sql.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// this one is ok:
	ds.paramPrefix = "@p"
	ds.handle, err = sql.Open("sqlserver", "sqlserver://sa:root@localhost:1433?database=AdventureWorks2014")
	// === Oracle =============================
	// "github.com/godror/godror"
	//ds.paramPrefix = ":"
	//ds.handle, err = sql.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
	if err != nil {
		panic(err)
	}
}

*/

func (ds *DataStore) Open() {
	initDb(ds)
}

func (ds *DataStore) Close() {
	err := ds.handle.Close()
	if err != nil {
		panic(err)
	}
}

func (ds *DataStore) Begin() {
	if ds.tx != nil {
		panic("Tx already started")
	}
	var err error
	ds.tx, err = ds.handle.Begin()
	if err != nil {
		panic(err)
	}
}

func (ds *DataStore) Commit() {
	if ds.tx == nil {
		panic("Tx not started")
		// return
	}
	err := ds.tx.Commit()
	if err != nil {
		panic(err)
	}
	// to prevent ds.tx.Rollback() in defer
	ds.tx = nil
}

func (ds *DataStore) Rollback() {
	if ds.tx == nil {
		// commit() was called, just do nothing:
		return
	}
	err := ds.tx.Rollback()
	if err != nil {
		panic(err)
	}
	ds.tx = nil
}

func (ds *DataStore) _query(sqlStr string, args ...interface{}) (*sql.Rows, error) {
	if ds.tx == nil {
		return ds.handle.Query(sqlStr, args...)
	}
	return ds.tx.Query(sqlStr, args...)
}

func (ds *DataStore) _exec(sqlStr string, args ...interface{}) (sql.Result, error) {
	if ds.tx == nil {
		return ds.handle.Exec(sqlStr, args...)
	}
	return ds.tx.Exec(sqlStr, args...)
}

func (ds *DataStore) PGFetch(cursor string) string {
	return fmt.Sprintf("fetch all from \"%s\"", cursor)
}

func (ds *DataStore) _pgInsert(sqlStr string, aiNames string, args ...interface{}) interface{} {
	// fetching of multiple AI values is not implemented so far:
	sqlStr += " RETURNING " + aiNames
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		panic(err)
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	for rows.Next() {
		var data interface{}
		err = rows.Scan(&data)
		if err != nil {
			panic(err)
		}
		return data
	}
	println("rows.Next() FAILED:" + sqlStr)
	return nil
}

func (ds *DataStore) _oracleInsert(sqlStr string, aiNames string, args ...interface{}) interface{} {
	// fetching of multiple AI values is not implemented so far:
	sqlStr = fmt.Sprintf("%s returning %s  into :%d", sqlStr, aiNames, len(args)+1)
	// https://ddcode.net/2019/05/11/how-does-go-call-oracles-stored-procedures-and-get-the-return-value-of-the-stored-procedures/
	var id64 float64
	// var id64 interface{} --- error
	// var id64 int64 --- it works, but...
	args = append(args, sql.Out{Dest: &id64, In: false})
	// ----------------
	_, err := ds._exec(sqlStr, args...)
	if err != nil {
		println(err.Error())
		println("Exec() FAILED: " + sqlStr)
		return nil
	}
	return id64
}

func (ds *DataStore) _mssqlInsert(sqlStr string, args ...interface{}) interface{} {
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// LastInsertId should not be used with this driver (or SQL Server) due to
	// how the TDS protocol works. Please use the OUTPUT Clause or add a select
	// ID = convert(bigint, SCOPE_IDENTITY()); to the end of your query (ref SCOPE_IDENTITY).
	//  This will ensure you are getting the correct ID and will prevent a network round trip.
	sqlStr += ";SELECT @@IDENTITY;"
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		panic(err)
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	for rows.Next() {
		var data interface{}
		err = rows.Scan(&data) // returns []uint8
		if err != nil {
			panic(err)
		}
		return data
	}
	println("rows.Next() FAILED: " + sqlStr)
	return nil
}

func (ds *DataStore) _execInsertBuiltin(sqlStr string, args ...interface{}) *int64 {
	res, err := ds._exec(sqlStr, args...)
	if err != nil {
		panic(err)
	}
	id, err := res.LastInsertId()
	if err == nil {
		return &id
	}
	// The Go builtin functions print and println print to stderr
	// https://stackoverflow.com/questions/29721449/how-can-i-print-to-stderr-in-go-without-using-log
	println(err.Error())
	println("res.LastInsertId() FAILED: " + sqlStr)
	return nil
}

func (ds *DataStore) Insert(sqlStr string, aiNames string, args ...interface{}) interface{} {
	// len(nil) == 0
	if len(aiNames) == 0 {
		panic("DataStore.insert is not applicable for aiNames = " + aiNames)
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
	return ds._execInsertBuiltin(sqlStr, args...)
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

func _validateDest(dest interface{}) {
	if dest == nil {
		panic("OutParam/InOutParam -> Dest is nil")
	}
	if !_isPtr(dest) {
		panic("OutParam/InOutParam -> Dest must be a Ptr")
	}
	if _pointsToNil(dest) {
		panic("OutParam/InOutParam -> Dest points to nil")
	}
}

func (ds *DataStore) _processExecParams(args []interface{}, onRowArr *[]func(map[string]interface{}),
	queryArgs *[]interface{}) (bool, bool) {
	implicitCursors := false
	outCursors := false
	for _, arg := range args {
		switch arg.(type) {
		case []func(map[string]interface{}):
			if outCursors {
				panic(fmt.Sprintf("Forbidden: %v", args))
			}
			implicitCursors = true
			funcArr := arg.([]func(map[string]interface{}))
			*onRowArr = append(*onRowArr, funcArr...)
		case func(map[string]interface{}):
			if implicitCursors {
				panic(fmt.Sprintf("Forbidden: %v", args))
			}
			outCursors = true
			onRow := arg.(func(map[string]interface{}))
			*onRowArr = append(*onRowArr, onRow)
			//var cursor interface{}
			var rows driver.Rows
			*queryArgs = append(*queryArgs, sql.Out{Dest: &rows, In: false})
		case *OutParam:
			p := arg.(*OutParam)
			_validateDest(p.Dest)
			*queryArgs = append(*queryArgs, sql.Out{Dest: p.Dest, In: false})
		case OutParam:
			p := arg.(OutParam)
			_validateDest(p.Dest)
			*queryArgs = append(*queryArgs, sql.Out{Dest: p.Dest, In: false})
		case *InOutParam:
			p := arg.(*InOutParam)
			_validateDest(p.Dest)
			*queryArgs = append(*queryArgs, sql.Out{Dest: p.Dest, In: true})
		case InOutParam:
			p := arg.(InOutParam)
			_validateDest(p.Dest)
			*queryArgs = append(*queryArgs, sql.Out{Dest: p.Dest, In: true})
		default:
			if _isPtr(arg) {
				if _pointsToNil(arg) {
					panic("arg points to nil")
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
	return implicitCursors, outCursors
}

func (ds *DataStore) _queryAllImplicitRcOracle(sqlStr string, onRowArr []func(map[string]interface{}), queryArgs ...interface{}) {
	rows, err := ds._query(sqlStr, queryArgs...)
	if err != nil {
		panic(err)
	}
	defer func() {
		err := rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	onRowIndex := 0
	for {
		// 1) unlike MySQL, it must be done before _prepareFetch -> rows.Next()
		// 2) at the moment, it does not work with multiple Implicit RC
		if !rows.NextResultSet() {
			break
		}
		// re-detect columns for each ResultSet
		colNames, data, values, valuePointers := ds._prepareFetch(rows)
		for rows.Next() {
			err := rows.Scan(valuePointers...)
			if err == nil {
				for i, colName := range colNames {
					data[colName] = values[i]
				}
				onRowArr[onRowIndex](data)
			} else {
				panic(err)
			}
		}
		onRowIndex++
	}
}

func (ds *DataStore) _queryAllImplicitRcMySQL(sqlStr string, onRowArr []func(map[string]interface{}), queryArgs ...interface{}) {
	rows, err := ds._query(sqlStr, queryArgs...)
	if err != nil {
		panic(err)
	}
	defer func() {
		err := rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	onRowIndex := 0
	for {
		// re-detect columns for each ResultSet
		colNames, data, values, valuePointers := ds._prepareFetch(rows)
		for rows.Next() {
			err := rows.Scan(valuePointers...)
			if err == nil {
				for i, colName := range colNames {
					data[colName] = values[i]
				}
				onRowArr[onRowIndex](data)
			} else {
				panic(err)
			}
		}
		if !rows.NextResultSet() {
			break
		}
		onRowIndex++
	}
}

func (ds *DataStore) _exec2(sqlStr string, onRowArr []func(map[string]interface{}), args ...interface{}) int64 {
	res, err := ds._exec(sqlStr, args...)
	if err != nil {
		panic(err)
	}
	onRowIndex := 0
	for _, arg := range args {
		switch arg.(type) {
		case sql.Out:
			out := arg.(sql.Out)
			if out.Dest != nil {
				switch out.Dest.(type) {
				case *driver.Rows:
					rows := out.Dest.(*driver.Rows)
					onRow := onRowArr[onRowIndex]
					_fetchAllFromCursor(*rows, onRow)
					onRowIndex++
				}
			}
		}
	}
	ra, err := res.RowsAffected()
	if err != nil {
		println(err.Error())
		return -1
	}
	return ra
}

func _fetchAllFromCursor(rows driver.Rows, onRow func(map[string]interface{})) {
	defer func() {
		err := rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	colNames := rows.Columns()
	data := make(map[string]interface{})
	values := make([]driver.Value, len(colNames))
	for {
		err := rows.Next(values)
		if err == nil {
			for i, colName := range colNames {
				data[colName] = values[i]
			}
			onRow(data)
		} else {
			break
		}
	}
}

func (ds *DataStore) Exec(sqlStr string, args ...interface{}) int64 {
	sqlStr = ds._formatSQL(sqlStr)
	var onRowArr []func(map[string]interface{})
	var queryArgs []interface{}
	// Syntax like [on_test1:Test, on_test2:Test] is used to call SP with IMPLICIT cursors
	implicitCursors, _ := ds._processExecParams(args, &onRowArr, &queryArgs)
	if implicitCursors {
		if ds.isOracle() {
			ds._queryAllImplicitRcOracle(sqlStr, onRowArr, queryArgs...)
		} else {
			ds._queryAllImplicitRcMySQL(sqlStr, onRowArr, queryArgs...) // it works with MySQL SP
		}
		return 0
	}
	return ds._exec2(sqlStr, onRowArr, queryArgs...)
}

func (ds *DataStore) _queryRowValues(sqlStr string, queryArgs ...interface{}) []interface{} {
	rows, err := ds._query(sqlStr, queryArgs...)
	if err != nil {
		panic(err)
	}
	defer func() {
		err := rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	outParamIndex := 0
	_, _, values, valuePointers := ds._prepareFetch(rows)
	if rows.Next() {
		err = rows.Scan(valuePointers...)
		if err == nil {
			for _, arg := range queryArgs {
				if _isPtr(arg) {
					ds.Assign(arg, values[outParamIndex])
				}
			}
		} else {
			panic(err)
		}
		outParamIndex++
	} else {
		panic(fmt.Sprintf("Rows found 0 for %s", sqlStr))
	}
	if rows.Next() {
		panic(fmt.Sprintf("More than 1 row found for %s", sqlStr))
	}
	return values
}

func (ds *DataStore) Query(sqlStr string, args ...interface{}) interface{} {
	sqlStr = ds._formatSQL(sqlStr)
	var onRowArr []func(map[string]interface{})
	var queryArgs []interface{}
	implicitCursors, outCursors := ds._processExecParams(args, &onRowArr, &queryArgs)
	if implicitCursors || outCursors {
		panic("Not supported in Query: implicitCursors || outCursors")
	}
	arr := ds._queryRowValues(sqlStr, queryArgs...)
	return arr // it returns []interface{} for cases like 'SELECT @value, @name;'
}

func (ds *DataStore) QueryAll(sqlStr string, onRow func(interface{}), args ...interface{}) {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		panic(err)
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	for {
		// re-detect columns for each ResultSet
		// fetch all columns! if to fetch less, Scan returns nil-s
		_, _, values, valuePointers := ds._prepareFetch(rows)
		for rows.Next() {
			err = rows.Scan(valuePointers...)
			if err == nil {
				// return whole row to enable multiple out params in mssql sp
				onRow(values)
			} else {
				panic(err)
			}
		}
		if !rows.NextResultSet() {
			break
		}
	}
}

func (ds *DataStore) QueryRow(sqlStr string, args ...interface{}) map[string]interface{} {
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		panic(err)
	}
	defer func() {
		err := rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	colNames, data, values, valuePointers := ds._prepareFetch(rows)
	if rows.Next() {
		err = rows.Scan(valuePointers...)
		if err == nil {
			for i, colName := range colNames {
				data[colName] = values[i]
			}
		} else {
			panic(err)
		}
	} else {
		panic(fmt.Sprintf("Rows found 0 for %s", sqlStr))
	}
	if rows.Next() {
		panic(fmt.Sprintf("More than 1 row found for %s", sqlStr))
	}
	return data
}

func (ds *DataStore) QueryAllRows(sqlStr string, onRow func(map[string]interface{}), args ...interface{}) {
	// many thanks to:
	// https://stackoverflow.com/questions/51731423/how-to-read-a-row-from-a-table-to-a-map-without-knowing-columns
	sqlStr = ds._formatSQL(sqlStr)
	rows, err := ds._query(sqlStr, args...)
	if err != nil {
		panic(err)
	}
	defer func() {
		err := rows.Close()
		if err != nil {
			panic(err)
		}
	}()
	for {
		// re-detect columns for each ResultSet
		colNames, data, values, valuePointers := ds._prepareFetch(rows)
		for rows.Next() {
			err := rows.Scan(valuePointers...)
			if err == nil {
				for i, colName := range colNames {
					data[colName] = values[i]
				}
				onRow(data)
			} else {
				panic(err)
			}
		}
		if !rows.NextResultSet() {
			break
		}
	}
}

/*
// MySQL: if string is ok for all types (no conversions needed), use this:
func (ds *DataStore) _prepareFetch(rows *sql.Rows) ([]string, map[string]interface{}, []string, []interface{}) {
	// ...
	values := make([]string, len(colNames))
*/
func (ds *DataStore) _prepareFetch(rows *sql.Rows) ([]string, map[string]interface{}, []interface{}, []interface{}) {
	colNames, _ := rows.Columns()
	data := make(map[string]interface{})
	// interface{} is ok for SQLite3, Oracle, and SQL Server.
	// MySQL and PostgreSQL may require some convertors from []uint8
	// https://github.com/ziutek/mymysql#type-mapping
	values := make([]interface{}, len(colNames))
	valuePointers := make([]interface{}, len(colNames))
	for i := range values {
		valuePointers[i] = &values[i]
	}
	return colNames, data, values, valuePointers
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
	switch value.(type) {
	case []byte:
		*d = string(value.([]byte))
	case int64:
		i64 := value.(int64) // MySQL
		*d = strconv.FormatInt(i64, 10)
	case int32:
		i64 := int64(value.(int32)) // MySQL
		*d = strconv.FormatInt(i64, 10)
	case string:
		*d = value.(string)
	case time.Time:
		t := value.(time.Time)
		*d = t.Format("2006-01-02 15:04:05")
	default:
		return false
	}
	return true
}

func _assignInt64(d *int64, value interface{}) bool {
	switch value.(type) {
	case int64:
		*d = value.(int64)
	case int32:
		*d = int64(value.(int32)) // MySQL
	case float64:
		*d = int64(value.(float64))
	case float32:
		*d = int64(value.(float32))
	case []byte:
		str := string(value.([]byte))
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
	switch value.(type) {
	case int32:
		*d = value.(int32)
	case int64:
		*d = int32(value.(int64))
	case float64:
		*d = value.(int32)
	case float32:
		*d = int32(value.(float32))
	case []byte:
		str := string(value.([]byte))
		d64, err := strconv.ParseInt(str, 10, 32)
		if err == nil {
			*d = int32(d64)
		}
	case string:
		str := value.(string)
		d64, err := strconv.ParseInt(str, 10, 32)
		if err == nil {
			*d = int32(d64)
		}
	default:
		return false
	}
	return true
}

func _assignFloat32(d *float32, value interface{}) bool {
	switch value.(type) {
	case float32:
		*d = value.(float32)
	case float64:
		*d = float32(value.(float64))
	case []byte:
		str := string(value.([]byte)) // PostgeSQL
		d64, _ := strconv.ParseFloat(str, 64)
		*d = float32(d64)
	case string:
		str := value.(string) // Oracle
		d64, _ := strconv.ParseFloat(str, 64)
		*d = float32(d64)
	default:
		return false
	}
	return true
}

func _assignFloat64(d *float64, value interface{}) bool {
	switch value.(type) {
	case float64:
		*d = value.(float64)
	case float32:
		*d = float64(value.(float32))
	case []byte:
		str := string(value.([]byte)) // PostgeSQL, MySQL
		*d, _ = strconv.ParseFloat(str, 64)
	case string:
		str := value.(string) // Oracle
		*d, _ = strconv.ParseFloat(str, 64)
	default:
		return false
	}
	return true
}

func _assignTime(d *time.Time, value interface{}) bool {
	switch value.(type) {
	case time.Time:
		*d = value.(time.Time)
	default:
		return false
	}
	return true
}

func _assignBoolean(d *bool, value interface{}) bool {
	switch value.(type) {
	case []byte:
		str := string(value.([]byte)) // MySQL
		db, _ := strconv.ParseBool(str)
		*d = db
	case bool:
		*d = value.(bool)
	default:
		return false
	}
	return true
}

func AssignValue(fieldAddr interface{}, value interface{}) {
	if value == nil {
		switch d := fieldAddr.(type) {
		case *interface{}:
			*d = nil
			return
		}
		return // leave as-is
	}
	assigned := false
	switch d := fieldAddr.(type) {
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
		switch value.(type) {
		case []byte:
			*d = value.([]byte)
			assigned = true
		}
	case *interface{}:
		*d = value
		assigned = true
	}
	if !assigned {
		panic(fmt.Sprintf("Unexpected params in AssignValue(%T, %T)", fieldAddr, value))
	}
}

// Extend/improve method Assign and related functions on demand:

func (ds *DataStore) Assign(fieldAddr interface{}, value interface{}) {
	switch value.(type) {
	case []interface{}:
		switch d := fieldAddr.(type) {
		case *[]interface{}:
			*d = value.([]interface{})
		default:
			arr := value.([]interface{})
			v0 := arr[0]
			// it includes processing of v0 == nil
			AssignValue(fieldAddr, v0)
		}
	default:
		// it includes processing of value == nil
		AssignValue(fieldAddr, value)
	}
}
