package main

import (
	"database/sql"
	"fmt"
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

func (ds *DataStore) open() {
	initDb(ds)
}

func (ds *DataStore) close() {
	err := ds.handle.Close()
	if err != nil {
		panic(err)
	}
}

func (ds *DataStore) begin() {
	if ds.tx != nil {
		panic("Tx already started")
	}
	var err error
	ds.tx, err = ds.handle.Begin()
	if err != nil {
		panic(err)
	}
}

func (ds *DataStore) commit() {
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

func (ds *DataStore) rollback() {
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

func (ds *DataStore) _query(sqlQuery string, args ...interface{}) (*sql.Rows, error) {
	if ds.tx == nil {
		return ds.handle.Query(sqlQuery, args...)
	}
	return ds.tx.Query(sqlQuery, args...)
}

func (ds *DataStore) _prepare(sqlQuery string) (*sql.Stmt, error) {
	if ds.tx == nil {
		return ds.handle.Prepare(sqlQuery)
	}
	return ds.tx.Prepare(sqlQuery)
}

func (ds *DataStore) pgFetch(cursor string) string {
	return fmt.Sprintf("fetch all from \"%s\"", cursor)
}

func (ds *DataStore) _pgInsert(sqlQuery string, aiNames string, args ...interface{}) interface{} {
	// fetching of multiple AI values is not implemented so far:
	sqlQuery += " RETURNING " + aiNames
	rows, err := ds._query(sqlQuery, args...)
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
	println("rows.Next() FAILED:" + sqlQuery)
	return nil
}

func (ds *DataStore) _oracleInsert(sqlQuery string, aiNames string, args ...interface{}) interface{} {
	// fetching of multiple AI values is not implemented so far:
	sqlQuery += " returning " + aiNames + " into :" + aiNames
	// https://ddcode.net/2019/05/11/how-does-go-call-oracles-stored-procedures-and-get-the-return-value-of-the-stored-procedures/
	// var id64 float64
	var id64 float64
	args = append(args, sql.Out{Dest: &id64, In: false})
	// ----------------
	stmt, err := ds._prepare(sqlQuery)
	if err != nil {
		panic(err)
	}
	defer func() {
		err = stmt.Close()
		if err != nil {
			panic(err)
		}
	}()
	_, err = stmt.Exec(args...)
	if err != nil {
		println(err.Error())
		println("Exec() FAILED: " + sqlQuery)
		return nil
	}
	return id64
}

func (ds *DataStore) _mssqlInsert(sqlQuery string, args ...interface{}) interface{} {
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// LastInsertId should not be used with this driver (or SQL Server) due to
	// how the TDS protocol works. Please use the OUTPUT Clause or add a select
	// ID = convert(bigint, SCOPE_IDENTITY()); to the end of your query (ref SCOPE_IDENTITY).
	//  This will ensure you are getting the correct ID and will prevent a network round trip.
	sqlQuery += ";SELECT @@IDENTITY;"
	rows, err := ds._query(sqlQuery, args...)
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
	println("rows.Next() FAILED: " + sqlQuery)
	return nil
}

func (ds *DataStore) _execInsertBuiltin(sqlQuery string, args ...interface{}) interface{} {
	// === Prepare -> Exec to access LastInsertId
	stmt, err := ds._prepare(sqlQuery)
	if err != nil {
		panic(err)
	}
	defer func() {
		err = stmt.Close()
		if err != nil {
			panic(err)
		}
	}()
	res, err := stmt.Exec(args...)
	if err != nil {
		panic(err)
	}
	id, err := res.LastInsertId()
	if err == nil {
		return id
	}
	// The Go builtin functions print and println print to stderr
	// https://stackoverflow.com/questions/29721449/how-can-i-print-to-stderr-in-go-without-using-log
	println(err.Error())
	println("res.LastInsertId() FAILED: " + sqlQuery)
	return nil
}

func (ds *DataStore) insert(sqlQuery string, aiNames string, args ...interface{}) interface{} {
	// len(nil) == 0
	if len(aiNames) == 0 {
		panic("DataStore.insert is not applicable for aiNames = " + aiNames)
	}
	// Builtin LastInsertId works only with MySQL and SQLite3
	sqlQuery = ds._formatSQL(sqlQuery)
	if ds.isPostgreSQL() {
		return ds._pgInsert(sqlQuery, aiNames, args...)
	} else if ds.isSqlServer() {
		return ds._mssqlInsert(sqlQuery, args...)
	} else if ds.isOracle() {
		// Oracle: specify AI values explicitly:
		// <crud-auto dto="ProjectInfo" table="PROJECTS" generated="P_ID"/>
		return ds._oracleInsert(sqlQuery, aiNames, args...)
	}
	return ds._execInsertBuiltin(sqlQuery, args...)
}

func _preprocessParams(args []interface{}, onRowArr *[]func(map[string]interface{}), queryArgs *[]interface{}) {
	for _, arg := range args {
		switch arg.(type) {
		case []func(map[string]interface{}):
			if len(*onRowArr) > 0 {
				panic("len(onRowArr) > 0")
			}
			funcArr := arg.([]func(map[string]interface{}))
			*onRowArr = append(*onRowArr, funcArr...)
		case func(map[string]interface{}):
			onRow := arg.(func(map[string]interface{}))
			*onRowArr = append(*onRowArr, onRow)
		default:
			*queryArgs = append(*queryArgs, arg)
		}
	}
}

func (ds *DataStore) execDML(sqlQuery string, args ...interface{}) int64 {
	sqlQuery = ds._formatSQL(sqlQuery)
	var onRowArr []func(map[string]interface{})
	var queryArgs []interface{}
	_preprocessParams(args, &onRowArr, &queryArgs)
	if len(onRowArr) > 0 {
		rows, err := ds._query(sqlQuery, queryArgs...)
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
		return 0
	}
	// === Prepare -> Exec to access RowsAffected
	stmt, err := ds._prepare(sqlQuery)
	if err != nil {
		panic(err)
	}
	defer func() {
		err = stmt.Close()
		if err != nil {
			panic(err)
		}
	}()
	res, err := stmt.Exec(args...)
	if err != nil {
		panic(err)
	}
	ra, err := res.RowsAffected()
	if err != nil {
		println(err.Error())
		return -1
	}
	return ra
}

func _validateQuery(found int) {
	if found != 1 {
		panic(fmt.Sprintf("1 row expected, but %d found. "+
			"If you need a 'find' mode, use 'query-list' or 'query-dto-list' instead", found))
	}
}

func (ds *DataStore) query(sqlQuery string, args ...interface{}) interface{} {
	var arr []interface{}
	onRow := func(date interface{}) {
		arr = append(arr, date)
	}
	ds.queryAll(sqlQuery, onRow, args...)
	_validateQuery(len(arr)) // A nil slice has a len of 0.
	return arr[0]
}

func (ds *DataStore) queryAll(sqlQuery string, onRow func(interface{}), args ...interface{}) {
	sqlQuery = ds._formatSQL(sqlQuery)
	rows, err := ds._query(sqlQuery, args...)
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

func (ds *DataStore) queryRow(sqlQuery string, args ...interface{}) map[string]interface{} {
	var arr []map[string]interface{}
	onRow := func(rowData map[string]interface{}) {
		arr = append(arr, rowData)
	}
	ds.queryAllRows(sqlQuery, onRow, args...)
	_validateQuery(len(arr)) // A nil slice has a len of 0.
	return arr[0]
}

func (ds *DataStore) queryAllRows(sqlQuery string, onRow func(map[string]interface{}), args ...interface{}) {
	// many thanks to:
	// https://stackoverflow.com/questions/51731423/how-to-read-a-row-from-a-table-to-a-map-without-knowing-columns
	sqlQuery = ds._formatSQL(sqlQuery)
	rows, err := ds._query(sqlQuery, args...)
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

func (ds *DataStore) _formatSQL(sqlQuery string) string {
	if len(ds.paramPrefix) == 0 {
		return sqlQuery
	}
	i := 1
	for {
		pos := strings.Index(sqlQuery, "?")
		if pos == -1 {
			break
		}
		str1 := sqlQuery[0:pos]
		str2 := sqlQuery[pos+1:]
		sqlQuery = str1 + ds.paramPrefix + strconv.Itoa(i) + str2
		i += 1
	}
	return sqlQuery
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

func (ds *DataStore) assignValue(fieldAddr interface{}, value interface{}) {
	if value == nil {
		switch d := fieldAddr.(type) {
		case *interface{}:
			*d = nil
			return
		}
		return // leave as-is
	}
	switch value.(type) {
	case []interface{}:
		panic("value of type []interface{} is not allowed here")
	}
	switch d := fieldAddr.(type) {
	case *string:
		if _assignString(d, value) {
			return
		}
	case *int32:
		if _assignInt32(d, value) {
			return
		}
	case *int64:
		if _assignInt64(d, value) {
			return
		}
	case *float64:
		if _assignFloat64(d, value) {
			return
		}
	case *float32:
		if _assignFloat32(d, value) {
			return
		}
	case *time.Time:
		if _assignTime(d, value) {
			return
		}
	case *bool:
		if _assignBoolean(d, value) {
			return
		}
	case *[]byte: // the same as uint8
		switch value.(type) {
		case []byte:
			*d = value.([]byte)
			return
		}
	case *interface{}:
		*d = value
		return
	}
	panic(fmt.Sprintf("Cannot process DataStore.assign(%T, %T)", fieldAddr, value))
}

// Extend/improve methods assign, assignValue and related functions on demand:

func (ds *DataStore) assign(fieldAddr interface{}, value interface{}) {
	switch value.(type) {
	case []interface{}:
		switch d := fieldAddr.(type) {
		case *[]interface{}:
			*d = value.([]interface{})
		default:
			arr := value.([]interface{})
			v0 := arr[0]
			// it includes processing of v0 == nil
			ds.assignValue(fieldAddr, v0)
		}
	default:
		// it includes processing of value == nil
		ds.assignValue(fieldAddr, value)
	}
}
