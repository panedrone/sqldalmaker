package dal

import (
	"context"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var ds = &_DS{}

func (ds *_DS) initDb() (err error) {
	dsn := "root:sa@tcp(127.0.0.1:3306)/todolist?charset=utf8mb4&parseTime=True&loc=Local"
	ds.rootDb, err = gorm.Open(mysql.Open(dsn), &gorm.Config{
		Logger: logger.Default.LogMode(logger.Info),
	})
	return
}

// WithContext can be used in a middleware to start separate sessions for incoming web-requests
//
// api := r.Group("", func(ctx *gin.Context) {
//		ctx.Set("db", dal.WithContext(ctx))
//	})

func WithContext(ctx context.Context) *gorm.DB {
	return ds.rootDb.WithContext(ctx)
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
