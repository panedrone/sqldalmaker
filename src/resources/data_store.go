package main

import (
	"database/sql"
	"log"
	"strconv"
	"strings"

	// _ "github.com/mattn/go-sqlite3"		// SQLite3
	// _ "github.com/go-sql-driver/mysql"	// MySQL
	_ "github.com/denisenkom/go-mssqldb" // SQL Server
	// _ "github.com/godror/godror"			// Oracle
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
	ds.handle, err = sql.Open("sqlite3", "./todo-list.sqlite")

	// ds.handle, _ = sql.Open("sqlite3", "./sqlite-database.db")
	// ds.handle, _ = sql.Open("mysql", "root:root@/sakila")
	// -----------------
	// SQL Server https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	// ds.paramPrefix = "@p"
	// ds.handle, err = sql.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// -----------------
	//ds.paramPrefix = ":"
	//ds.handle, _ = sql.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
	if err != nil {
		log.Fatal(err)
	}
}

func (ds *DataStore) close() {
	ds.handle.Close()
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

func (ds *DataStore) insert(sql string, args ...interface{}) int64 {
	sql = ds.formatSQL(sql)
	stmt, err := ds.handle.Prepare(sql)
	if err != nil {
		log.Fatal(err)
		return -1
	}
	defer stmt.Close()
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
	sql = ds.formatSQL(sql)
	stmt, err := ds.handle.Prepare(sql)
	if err != nil {
		log.Fatal(err)
		return -1
	}
	defer stmt.Close()
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
	arr := ds.queryAll(sql, args)
	if arr == nil || len(arr) == 0 {
		return nil
	}
	if len(arr) > 1 {
		return nil
	}
	return arr[0]
}

func (ds *DataStore) queryAll(sql string, args ...interface{}) []interface{} {
	var arr []interface{}
	onRowHandler := func(rowData map[string]interface{}) {
		values := make([]string, len(rowData))
		arr = append(arr, values[0])
	}
	ds.queryAllRows(sql, onRowHandler, args...)
	return arr
}

func (ds *DataStore) queryRow(sql string, args ...interface{}) map[string]interface{} {
	var arr []map[string]interface{}
	onRowHandler := func(rowData map[string]interface{}) {
		arr = append(arr, rowData)
	}
	ds.queryAllRows(sql, onRowHandler, args...)
	if arr == nil || len(arr) == 0 {
		return nil
	}
	if len(arr) > 1 {
		return nil
	}
	return arr[0]
}

func (ds *DataStore) queryAllRows(sql string, onRow func(rowData map[string]interface{}), args ...interface{}) {
	// many thanks to:
	// https://stackoverflow.com/questions/51731423/how-to-read-a-row-from-a-table-to-a-map-without-knowing-columns
	sql = ds.formatSQL(sql)
	rows, err := ds.handle.Query(sql, args...)
	if err != nil {
		log.Fatal(err)
		return
	}
	defer rows.Close()
	colNames, _ := rows.Columns()
	data := make(map[string]interface{})
	values := make([]interface{}, len(colNames))
	valuePointers := make([]interface{}, len(colNames))
	for i := range values {
		valuePointers[i] = &values[i]
	}
	for rows.Next() {
		rows.Scan(valuePointers...)
		for i, colName := range colNames {
			data[colName] = values[i]
		}
		onRow(data)
	}
}

func (ds *DataStore) formatSQL(sql string) string {
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
