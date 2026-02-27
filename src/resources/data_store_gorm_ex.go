package dbal

import (
	"github.com/cengsin/oracle"
	// "github.com/go-sql-driver/mysql"
	// "gorm.io/driver/postgres"
	// "gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

func (ds *_DS) initDb() (err error) {
	// === SQLite ==============================
	//ds.rootDb, err = gorm.Open(sqlite.Open("./todolist.sqlite"), &gorm.Config{
	//	Logger: logger.Default.LogMode(logger.Info),
	//})
	// === MySQL ===============================
	//dsn := "root:sa@tcp(127.0.0.1:3306)/my_tests?charset=utf8mb4&parseTime=True&loc=Local"
	//ds.rootDb, err = gorm.Open(mysql.Open(dsn), &gorm.Config{
	//	Logger: logger.Default.LogMode(logger.Info),
	//})
	// === PostgreSQL ===========================
	//dsn := "host=localhost user=postgres password=sa dbname=my_tests port=5432 sslmode=disable"
	//ds.rootDb, err = gorm.Open(postgres.Open(dsn), &gorm.Config{
	//	Logger: logger.Default.LogMode(logger.Info),
	//})
	// === Oracle ==============================
	dsn := "MY_TESTS/sa@127.0.0.1:1521/XE?charset=utf8mb4&parseTime=True&loc=Local"
	ds.rootDb, err = gorm.Open(oracle.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	return
}

var _ds = &_DS{}

func Ds() DataStore {
	return _ds
}

func NewCxOracleTestDao() *CxOracleTestDao {
	return &CxOracleTestDao{ds: _ds}
}

func WithContext(ctx context.Context) *gorm.DB {
	return _ds.Session(ctx)
}

func RunTx(ctx context.Context, txFunc func(tx *gorm.DB) error) error {
	return WithContext(ctx).Transaction(func(tx *gorm.DB) error {
		return txFunc(tx)
	})
}
