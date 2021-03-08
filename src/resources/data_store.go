package main

import (
	"database/sql"
	"fmt"
	"log"
	"strconv"
	"strings"
	"time"

	// _ "github.com/mattn/go-sqlite3"		// SQLite3
	_ "github.com/denisenkom/go-mssqldb" // SQL Server
	// _ "github.com/godror/godror"			// Oracle

	// only strings for MySQL (so far). see _prepareFetch below and related comments.

	// _ "github.com/go-sql-driver/mysql"	// MySQL
	// _ "github.com/ziutek/mymysql/godrv" // MySQL
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

func (ds *DataStore) open() {
	var err error
	// ds.handle, err = sql.Open("sqlite3", "./todo-list.sqlite")
	// ds.handle, err = sql.Open("sqlite3", "./northwindEF.sqlite")
	// -----------------
	// ds.handle, err = sql.Open("mysql", "root:root@/sakila")
	// ds.handle, err = sql.Open("mymysql", "sakila/root/root")
	// -----------------
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	ds.paramPrefix = "@p"
	// ensure sqlserver:// in beginning. this one is not valid:
	// ------ ds.handle, err = sql.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// this one is ok:
	ds.handle, err = sql.Open("sqlserver", "sqlserver://sa:root@localhost:1433?database=AdventureWorks2014")
	//query := url.Values{}
	//query.Add("app name", "AdventureWorks2014")
	//u := &url.URL{
	//	Scheme:   "sqlserver",
	//	User:     url.UserPassword("sa", "root"),
	//	Host:     fmt.Sprintf("%s:%d", "localhost", 1433),
	//	// Path:  instance, // if connecting to an instance instead of a port
	//	RawQuery: query.Encode(),
	//}
	// ds.handle, err = sql.Open("sqlserver", u.String())
	// -----------------
	//ds.paramPrefix = ":"
	//ds.handle, _ = sql.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
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
}

func (ds *DataStore) commit() {
	// TODO
}

func (ds *DataStore) rollback() {
	// TODO
}

func (ds *DataStore) insert(sql string, args ...interface{}) interface{} {
	sql = ds._formatSQL(sql)
	stmt, err := ds.handle.Prepare(sql)
	if err != nil {
		log.Fatal(err)
		return -1
	}
	defer func() {
		err = stmt.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	res, err2 := stmt.Exec(args...)
	if err2 != nil {
		log.Fatal(err)
		return -1
	}
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// LastInsertId should not be used with this driver (or SQL Server) due to
	// how the TDS protocol works. Please use the OUTPUT Clause or add a select
	// ID = convert(bigint, SCOPE_IDENTITY()); to the end of your query (ref SCOPE_IDENTITY).
	//  This will ensure you are getting the correct ID and will prevent a network round trip.
	// ---------
	id, err3 := res.LastInsertId()
	if err3 == nil {
		return id
	}
	return -1
}

func (ds *DataStore) execDML(sql string, args ...interface{}) int64 {
	sql = ds._formatSQL(sql)
	stmt, err := ds.handle.Prepare(sql)
	if err != nil {
		log.Fatal(err)
		return -1
	}
	defer func() {
		err = stmt.Close()
		if err != nil {
			log.Fatal(err)
		}
	}()
	res, err2 := stmt.Exec(args...)
	if err2 != nil {
		log.Fatal(err)
		return -1
	}
	ra, err3 := res.RowsAffected()
	if err3 == nil {
		return ra
	}
	return -1
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
		return
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
		return
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
/*
Temporary workaround for MySQL:

<type-map default="">
	<type detected="java.lang.Short" target="string"/>
	<type detected="java.lang.Integer" target="string"/>
	<type detected="java.lang.String" target="string"/>
	<type detected="java.util.Date" target="string"/>
	<type detected="byte[]" target="string"/>
	<type detected="java.lang.Boolean" target="string"/>
	<type detected="java.math.BigDecimal" target="string"/>
</type-map>

Origin is described here:

https://github.com/ziutek/mymysql#type-mapping

After text query you always receive a text result.
Mysql text result corresponds to []byte type in mymysql.
It isn't string type due to avoidance of unnecessary type conversions.
You can always convert []byte to string yourself...

TODO: Enable something like type-convertors in 'type-map' and generated code.

<type detected="java.lang.String" target="ds.toStr(...)"/>

*/

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

// extend this method on demand:

func (ds *DataStore) assign(fieldAddr interface{}, value interface{}) {
	if value == nil {
		return // leave as-is
	}
	switch d := fieldAddr.(type) {
	case *string:
		*d = value.(string)
		return
	case *int64:
		*d = value.(int64)
		return
	case *float64:
		*d = value.(float64)
		return
	case *time.Time:
		*d = value.(time.Time)
		return
	case *bool:
		*d = value.(bool)
		return
	case *[]byte: // the same as []uint8
		*d = value.([]byte)
		return
	case *interface{}:
		*d = value
		return
	default:
		log.Fatal(fmt.Sprintf("Unknown type in DataStore.assign(...): %T", d))
	}
}
