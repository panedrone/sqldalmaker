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

func initDb(ds *DataStore) (err error) {
	// === PostgeSQL ===========================
	// ds.paramPrefix = "$"
	// ds.db, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=disable")
	// ds.db, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=verify-full")
	// === SQLite3 =============================
	// ds.db, err = sql.Open("sqlite3", "./log.sqlite")
	// ds.db, err = sql.Open("sqlite3", "./northwindEF.sqlite")
	// === MySQL ===============================
	ds.db, err = sql.Open("mysql", "root:root@/sakila")
	//ds.db, err = sql.Open("mymysql", "sakila/root/root")
	// === SQL Server ==========================
	// https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	// ensure sqlserver:// in beginning. this one is not valid:
	// ------ ds.db, err = sql.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// this one is ok:
	//ds.paramPrefix = "@p"
	//ds.db, err = sql.Open("sqlserver", "sqlserver://sa:root@localhost:1433?database=AdventureWorks2014")
	// === Oracle =============================
	// "github.com/godror/godror"
	//ds.paramPrefix = ":"
	//ds.db, err = sql.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
	return
}
