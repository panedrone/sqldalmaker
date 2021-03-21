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

func initDb() (*sql.DB, error) {
	var err error
	var handle *sql.DB
	// === PostgeSQL ===========================
	// ds.paramPrefix = "$"
	// ds.handle, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=disable")
	// ds.handle, err = sql.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=verify-full")
	// === SQLite3 =============================
	// ds.handle, err = sql.Open("sqlite3", "./log.sqlite")
	// ds.handle, err = sql.Open("sqlite3", "./northwindEF.sqlite")
	// === MySQL ===============================
	handle, err = sql.Open("mysql", "root:root@/sakila")
	//ds.handle, err = sql.Open("mymysql", "sakila/root/root")
	// === SQL Server ==========================
	// https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	// ensure sqlserver:// in beginning. this one is not valid:
	// ------ ds.handle, err = sql.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// this one is ok:
	//ds.paramPrefix = "@p"
	//ds.handle, err = sql.Open("sqlserver", "sqlserver://sa:root@localhost:1433?database=AdventureWorks2014")
	// === Oracle =============================
	// "github.com/godror/godror"
	//ds.paramPrefix = ":"
	//ds.handle, err = sql.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
	return handle, err
}
