package main

import (
	"database/sql"
	"fmt"
	"log"
	"reflect"
	"strconv"
	"strings"
	"time"

	// _ "github.com/mattn/go-sqlite3" // SQLite3
	_ "github.com/denisenkom/go-mssqldb" // SQL Server
	// _ "github.com/godror/godror"			// Oracle
	// only strings for MySQL (so far). see _prepareFetch below and related comments.
	// _ "github.com/go-sql-driver/mysql"	// MySQL
	// _ "github.com/ziutek/mymysql/godrv" // MySQL
	// _ "github.com/lib/pq" // PostgeSQL
)

/*
   SQL DAL Maker Web-Site: http://sqldalmaker.sourceforge.net
   This is an example of how to implement DataStore in GoLang using "database/sql".
   Copy-paste this code to your project and change it for your needs.
   Improvements are welcome: sdm@gmail.com
*/

type DataStore struct {
	paramPrefix string
	handle      *sql.DB
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

func (ds *DataStore) open() {
	var err error
	// === PostgeSQL ===========================
	// ds.paramPrefix = "$"
	// ds.handle, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=disable")
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
		log.Fatal(err)
	}
}

func (ds *DataStore) close() {
	err := ds.handle.Close()
	if err != nil {
		log.Fatal(err)
	}
}

func (ds *DataStore) begin() {
	// TODO
	// (*Tx, error) = ds.handle.Begin()
}

func (ds *DataStore) commit() {
	// TODO
}

func (ds *DataStore) rollback() {
	// TODO
}

func _execInsertPg(db *sql.DB, sql, aiNames string, args ...interface{}) interface{} {
	sql += " RETURNING " + aiNames
	rows, err := db.Query(sql, args...)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	for rows.Next() {
		var data interface{}
		err = rows.Scan(&data)
		if err != nil {
			log.Fatal(err)
		}
		return data
	}
	println("rows.Next() FAILED:" + sql)
	return nil
}

// 'insert ... returning ... into ...' not working with Query
// because of rows.Next() returns false

func _execInsertOracle(db *sql.DB, sql, aiNames string, args ...interface{}) interface{} {
	// TODO Problem of Oracle + 'insert ... returning ... into ...' and "github.com/godror/godror"
	// 1) sql += " returning " + aiNames ORA-00925: missing INTO keyword
	// 2) solution from https://github.com/rana/ora
	//    not working with "github.com/godror/godror":
	//    sql += " RETURNING " + aiNames + " /*LastInsertId*/" // " into :2"// + aiNames
	// 3) fetching of multiple AI values are not implemented so far
	sql += " returning " + aiNames + " into :" + aiNames
	//// DPI-1037: column at array position 0 fetched with error 1406:
	//// 	- var ai interface{}
	//// 	- var ai string
	var ai uint64 // int64, uint64, float64 are OK, but they remain 0
	args = append(args, &ai)
	rows, err := db.Query(sql, args...)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	for rows.Next() {
		var data interface{}
		err = rows.Scan(&data)
		if err != nil {
			log.Fatal(err)
		}
		return data
	}
	println("rows.Next() FAILED: " + sql)
	return nil
}

// 'insert ... returning ... into ...' not working with Prepare -> Exec too
// because of 'LastInsertId is not supported by this driver'

func _execInsertOracle2(db *sql.DB, sql, aiNames string, args ...interface{}) interface{} {
	// 1) sql += " returning " + aiNames ORA-00925: missing INTO keyword
	// 2) solution from https://github.com/rana/ora
	//    not working with "github.com/godror/godror":
	//    sql += " RETURNING " + aiNames + " /*LastInsertId*/" // " into :2"// + aiNames
	// 3) fetching of multiple AI values are not implemented so far
	sql += " returning " + aiNames + " into :" + aiNames
	//// DPI-1037: column at array position 0 fetched with error 1406:
	//// 	- var ai interface{}
	//// 	- var ai string
	var ai uint64 // int64, uint64, float64 are OK, but they remain 0
	args = append(args, &ai)
	stmt, err := db.Prepare(sql)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		err = stmt.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	res, err := stmt.Exec(args...)
	if err != nil {
		// log.Fatal(err)
		println(err.Error())
		println("Exec() FAILED: " + sql)
		return nil
	}
	// return ai; // remains 0
	// LastInsertId is not supported by this driver
	res64, err := res.LastInsertId()
	if err != nil {
		// log.Fatal(err)
		println(err.Error())
		println("res.LastInsertId() FAILED: " + sql)
		return nil
	}
	return res64
}

func _execInsertSqlServer(db *sql.DB, sql, aiNames string, args ...interface{}) interface{} {
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// LastInsertId should not be used with this driver (or SQL Server) due to
	// how the TDS protocol works. Please use the OUTPUT Clause or add a select
	// ID = convert(bigint, SCOPE_IDENTITY()); to the end of your query (ref SCOPE_IDENTITY).
	//  This will ensure you are getting the correct ID and will prevent a network round trip.
	sql += ";SELECT @@IDENTITY;"
	rows, err := db.Query(sql, args...)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	for rows.Next() {
		var data interface{}
		err = rows.Scan(&data) // returns []uint8
		if err != nil {
			log.Fatal(err)
		}
		return data
	}
	println("rows.Next() FAILED: " + sql)
	return nil
}

func _execInsertBuiltin(db *sql.DB, sql string, args ...interface{}) interface{} {
	// === Prepare -> Exec to access LastInsertId
	stmt, err := db.Prepare(sql)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		err = stmt.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	res, err := stmt.Exec(args...)
	if err != nil {
		log.Fatal(err)
	}
	id, err := res.LastInsertId()
	if err == nil {
		return id
	}
	// The Go builtin functions print and println print to stderr
	// https://stackoverflow.com/questions/29721449/how-can-i-print-to-stderr-in-go-without-using-log
	println(err.Error())
	println("res.LastInsertId() FAILED: " + sql)
	return nil
}

func (ds *DataStore) insert(sql, aiNames string, args ...interface{}) interface{} {
	// len(nil) == 0
	if len(aiNames) == 0 {
		log.Fatal("DataStore.insert is not applicable for aiNames = " + aiNames)
	}
	// Builtin LastInsertId works only with MySQL and SQLite3
	sql = ds._formatSQL(sql)
	if ds.isPostgreSQL() {
		return _execInsertPg(ds.handle, sql, aiNames, args...)
	} else if ds.isSqlServer() {
		return _execInsertSqlServer(ds.handle, sql, aiNames, args...)
	} else if ds.isOracle() {
		// Oracle: specify AI values explicitly:
		// <crud-auto dto="ProjectInfo" table="PROJECTS" generated="P_ID"/>
		return _execInsertOracle(ds.handle, sql, aiNames, args...)
	}
	return _execInsertBuiltin(ds.handle, sql, args...)
}

func (ds *DataStore) execDML(sql string, args ...interface{}) int64 {
	// === Prepare -> Exec to access RowsAffected
	sql = ds._formatSQL(sql)
	stmt, err := ds.handle.Prepare(sql)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		err = stmt.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	res, err := stmt.Exec(args...)
	if err != nil {
		log.Fatal(err)
	}
	ra, err := res.RowsAffected()
	if err != nil {
		println(err.Error())
		return -1
	}
	return ra
}

func (ds *DataStore) query(sql string, args ...interface{}) interface{} {
	var arr []interface{}
	manyRows := false
	onRow := func(date interface{}) {
		if arr == nil {
			arr = append(arr, date)
		} else {
			manyRows = true
		}
	}
	ds.queryAll(sql, onRow, args...)
	if arr == nil {
		return nil
	}
	if manyRows {
		// return nil
	}
	return arr[0]
}

func (ds *DataStore) queryAll(sql string, onRow func(interface{}), args ...interface{}) {
	sql = ds._formatSQL(sql)
	rows, err := ds.handle.Query(sql, args...)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	// all columns! if less, it returns nil-s
	_, _, values, valuePointers := ds._prepareFetch(rows)
	for rows.Next() {
		err = rows.Scan(valuePointers...)
		if err != nil {
			log.Fatal(err)
		}
		data := values[0]
		onRow(data)
	}
}

func (ds *DataStore) queryRow(sql string, args ...interface{}) map[string]interface{} {
	var arr []map[string]interface{}
	onRow := func(rowData map[string]interface{}) {
		arr = append(arr, rowData)
	}
	ds.queryAllRows(sql, onRow, args...)
	if arr == nil {
		return nil
	}
	if len(arr) > 1 {
		return nil
	}
	return arr[0]
}

func (ds *DataStore) queryAllRows(sql string, onRow func(map[string]interface{}), args ...interface{}) {
	// many thanks to:
	// https://stackoverflow.com/questions/51731423/how-to-read-a-row-from-a-table-to-a-map-without-knowing-columns
	sql = ds._formatSQL(sql)
	rows, err := ds.handle.Query(sql, args...)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		err = rows.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	colNames, data, values, valuePointers := ds._prepareFetch(rows)
	for rows.Next() {
		err = rows.Scan(valuePointers...)
		if err != nil {
			log.Fatal(err)
		}
		for i, colName := range colNames {
			data[colName] = values[i]
		}
		onRow(data)
	}
}

// func (ds *DataStore) _prepareFetch(rows *sql.Rows) ([]string, map[string]interface{}, []string, []interface{}) {   // MySQL
func (ds *DataStore) _prepareFetch(rows *sql.Rows) ([]string, map[string]interface{}, []interface{}, []interface{}) {
	colNames, _ := rows.Columns()
	data := make(map[string]interface{})
	// values := make([]string, len(colNames)) // fetch strings for MySQL. see comment about 'type-map' above
	values := make([]interface{}, len(colNames)) // interface{} is ok for SQLite3, Oracle, and SQL Server
	valuePointers := make([]interface{}, len(colNames))
	for i := range values {
		valuePointers[i] = &values[i]
	}
	return colNames, data, values, valuePointers
}

func (ds *DataStore) _formatSQL(sql string) string {
	if len(ds.paramPrefix) == 0 {
		return sql
	}
	i := 1
	for {
		pos := strings.Index(sql, "?")
		if pos == -1 {
			break
		}
		str1 := sql[0:pos]
		str2 := sql[pos+1:]
		sql = str1 + ds.paramPrefix + strconv.Itoa(i) + str2
		i += 1
	}
	return sql
}

/*
	TODO: Improve DataStore.assign(...) to convert strings (or byte-arrays) to more specific types
*/

func (ds *DataStore) assign(fieldAddr interface{}, value interface{}) {
	if value == nil {
		switch d := fieldAddr.(type) {
		case *interface{}:
			*d = nil
			return
		}
		return // leave as-is
	}
	switch d := fieldAddr.(type) {
	case *string:
		switch value.(type) {
		case []byte:
			*d = string(value.([]byte))
			return
		case int64:
			i64 := value.(int64) // MySQL
			*d = strconv.FormatInt(i64, 10)
			return
		case string:
			*d = value.(string)
			return
		}
	case *int32:
		switch value.(type) {
		case int32:
			*d = value.(int32)
			return
		case int64:
			*d = int32(value.(int64))
			return
		case []byte:
			str := string(value.([]byte))
			d64, err := strconv.ParseInt(str, 10, 32)
			if err == nil {
				*d = int32(d64)
			}
			return
		}
	case *int64:
		switch value.(type) {
		case int64:
			*d = value.(int64)
			return
		case []byte:
			str := string(value.([]byte))
			*d, _ = strconv.ParseInt(str, 10, 64)
			return
		}
	case *float64:
		switch value.(type) {
		case []byte:
			str := string(value.([]byte)) // PostgeSQL
			*d, _ = strconv.ParseFloat(str, 64)
			return
		default:
			*d = value.(float64)
		}
		return
	case *float32:
		switch value.(type) {
		case []byte:
			str := string(value.([]byte)) // PostgeSQL
			d64, _ := strconv.ParseFloat(str, 32)
			*d = float32(d64)
		default:
			*d = value.(float32)
		}
		return
	case *time.Time:
		*d = value.(time.Time)
		return
	case *bool:
		*d = value.(bool)
		return
	case *[]byte: // the same as uint8
		*d = value.([]byte)
		return
	case *interface{}:
		*d = value
		return
	}
	log.Fatal(fmt.Sprintf("Unexpected combination of param types in DataStore.assign(%v, %v)",
		reflect.TypeOf(fieldAddr), reflect.TypeOf(value)))
}
