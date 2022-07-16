package dao

import (
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var ds = &_DS{}

func (ds *_DS) initDb() (err error) {
	ds.db, err = gorm.Open(sqlite.Open("./todolist.sqlite3"), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
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

func NewGroupsDao() *GroupsDao {
	return &GroupsDao{ds: ds}
}
