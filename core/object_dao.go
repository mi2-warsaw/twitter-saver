package core

import (
	"github.com/jinzhu/gorm"
)

func FindStreamObjects(db *gorm.DB) []Object {
	var objects []Object
	db.Where(&Object{Source: StreamSource}).Find(&objects)
	return objects
}

func CountStreamObjects(db *gorm.DB) int {
	var count int
	db.Model(&Object{}).Where("source = ?", StreamSource).Count(&count)
	return count
}

func FindHistoryObjects(db *gorm.DB) []Object {
	var objects []Object
	db.Where(&Object{Source: HistorySource}).Find(&objects)
	return objects
}

func FindAllObjects(db *gorm.DB) []Object {
	var objects []Object
	db.Unscoped().Find(&objects)
	return objects
}

func UpdateObjectHistory(db *gorm.DB, object *Object, history bool) {
	db.Model(object).Update("history_done", history)
}
