package dbal

import (
	"github.com/jmoiron/sqlx"
	_ "github.com/mattn/go-sqlite3"
	// _ "github.com/denisenkom/go-mssqldb" // SQL Server
	// _ "github.com/godror/godror"			// Oracle
	// _ "github.com/go-sql-driver/mysql"   // MySQL
	// _ "github.com/ziutek/mymysql/godrv"  // MySQL
	// _ "github.com/lib/pq"                // PostgeSQL
)

var ds = &_DS{}

func (ds *_DS) initDb() (err error) {
	ds.db, err = sqlx.Open("sqlite3", "./todolist.sqlite")
	// === PostgeSQL ===========================
	//ds.paramPrefix = "$"
	//ds.db, err = sqlx.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=disable")
	// ds.db, err = sqlx.Open("postgres", "postgres://postgres:sa@localhost/my-tests?sslmode=verify-full")
	// === SQLite3 =============================
	// ds.db, err = sqlx.Open("sqlite3", "./log.sqlite")
	// ds.db, err = sqlx.Open("sqlite3", "./northwindEF.sqlite")
	// === MySQL ===============================
	// ds.db, err = sqlx.Open("mysql", "root:root@/sakila")
	// ds.db, err = sqlx.Open("mymysql", "sakila/root/root")
	// === SQL Server ==========================
	// https://github.com/denisenkom/go-mssqldb
	// The sqlserver driver uses normal MS SQL Server syntax and expects parameters in the
	// sql query to be in the form of either @Name or @p1 to @pN (ordinal position).
	// ensure sqlserver:// in beginning. this one is not valid:
	// ------ ds.db, err = sqlx.Open("sqlserver", "sa:root@/localhost:1433/SQLExpress?database=AdventureWorks2014")
	// this one is ok:
	//ds.paramPrefix = "@p"
	//ds.db, err = sqlx.Open("sqlserver", "sqlserver://sa:root@localhost:1433?database=AdventureWorks2014")
	// === Oracle =============================
	// "github.com/godror/godror"
	//ds.paramPrefix = ":"
	//ds.db, err = sqlx.Open("godror", `user="ORDERS" password="root" connectString="localhost:1521/orcl"`)
	return
}

func OpenDB() error {
	return ds.Open()
}

func CloseDB() error {
	return ds.Close()
}

func NewProjectsDao() *ProjectsDao {
	return &ProjectsDao{ds: ds}
}

func NewTasksDao() *TasksDao {
	return &TasksDao{ds: ds}
}
