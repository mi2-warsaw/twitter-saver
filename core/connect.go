package core

import (
	"log"

	"github.com/jinzhu/gorm"
	_ "github.com/jinzhu/gorm/dialects/postgres"
)

func Connect(connectionString string) *gorm.DB {
	db, err := gorm.Open("postgres", connectionString)
	if err != nil {
		log.Fatal(err)
	}

	db.AutoMigrate(&Tweet{})
	db.AutoMigrate(&Object{})

	db.Model(&Tweet{}).AddForeignKey("object_id", "objects(id)", "CASCADE", "NO ACTION")

	return db
}
