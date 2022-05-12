package dal

import (
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
)

var ds = &DataStore{}

func (ds *DataStore) initDb() (err error) {
	ds.db, err = gorm.Open(sqlite.Open("./todolist.sqlite3"), &gorm.Config{})
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

func NewTasksDao() *TasksDao {
	return &TasksDao{Ds: ds}
}

func NewGroupsDao() *GroupsDao {
	return &GroupsDao{Ds: ds}
}
