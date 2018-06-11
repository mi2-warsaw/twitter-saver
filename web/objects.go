package main

import (
	"database/sql"
	"errors"
	"fmt"
	"sort"
	"time"

	"github.com/jinzhu/gorm"
	"gitlab.com/kompu/twitter-saver/core"
)

type ObjectList struct {
	ID      uint   `json:"id"`
	Source  string `json:"source"`
	Type    string `json:"objectType"`
	Query   string `json:"query"`
	Deleted bool   `json:"deleted"`
}

func objectSource(source core.ObjectSource) string {
	switch source {
	case core.HistorySource:
		return "history"
	case core.StreamSource:
		return "stream"
	default:
		return "unknown"
	}
}

func toObjectSource(str string) (core.ObjectSource, error) {
	switch str {
	case "history":
		return core.HistorySource, nil
	case "stream":
		return core.StreamSource, nil
	default:
		return 0, errors.New("Unknown source " + str)
	}
}

func objectType(objectType core.ObjectType) string {
	switch objectType {
	case core.UserType:
		return "user"
	case core.KeywordType:
		return "keyword"
	default:
		return "unknown"
	}
}

func toObjectType(str string) (core.ObjectType, error) {
	switch str {
	case "user":
		return core.UserType, nil
	case "keyword":
		return core.KeywordType, nil
	default:
		return 0, errors.New("Unknown type " + str)
	}
}

type byCreatedAt []core.Object

func (s byCreatedAt) Len() int {
	return len(s)
}
func (s byCreatedAt) Swap(i, j int) {
	s[i], s[j] = s[j], s[i]
}
func (s byCreatedAt) Less(i, j int) bool {
	return s[i].CreatedAt.After(s[j].CreatedAt)
}

func listObjects(db *gorm.DB) []ObjectList {
	objects := core.FindAllObjects(db)
	sort.Sort(byCreatedAt(objects))

	res := make([]ObjectList, len(objects))
	for i, v := range objects {
		res[i] = ObjectList{
			ID:      v.ID,
			Source:  objectSource(v.Source),
			Type:    objectType(v.Type),
			Query:   v.Query,
			Deleted: v.DeletedAt != nil,
		}
	}
	return res
}

type ObjectAdd struct {
	Source  string `json:"source" binding:"required"`
	Type    string `json:"objectType" binding:"required"`
	Query   string `json:"query" binding:"required"`
	History string `json:"history"`
}

func addObject(db *gorm.DB, object *ObjectAdd) error {
	source, err := toObjectSource(object.Source)
	if err != nil {
		return err
	}

	typ, err := toObjectType(object.Type)
	if err != nil {
		return err
	}

	if source == core.StreamSource {
		count := core.CountStreamObjects(db)
		if count >= 2 {
			return errors.New("Two streams already exist.")
		}
	}

	if object.History != "" {
		history, err := time.Parse(time.RFC1123, object.History)
		if err != nil {
			return err
		}
		db.Create(&core.Object{
			Source:      source,
			Type:        typ,
			Query:       object.Query,
			HistoryFrom: &history,
		})
	} else {
		db.Create(&core.Object{
			Source: source,
			Type:   typ,
			Query:  object.Query,
		})
	}

	return nil
}

func deleteObject(db *gorm.DB, id uint, all bool) error {
	var object core.Object
	db.Unscoped().First(&object, id)
	if object.ID == id {
		if all {
			db.Unscoped().Delete(&object)
		} else {
			db.Delete(&object)
		}
		return nil
	} else {
		return errors.New(fmt.Sprintf("Object with id=%d doesn't exist.", id))
	}
}

type DayStats struct {
	Date  string `json:"date"`
	Count int    `json:"count"`
}

type ObjectStats struct {
	ID        int        `json:"id"`
	Query     string     `json:"query"`
	Deleted   bool       `json:"deleted"`
	AllTweets int        `json:"allTweets"`
	Days      []DayStats `json:"days"`
}

func objectStats(db *gorm.DB, id int, from string, to string) (*ObjectStats, error) {
	var obj core.Object
	db.Unscoped().First(&obj, id)

	var allCount int
	db.Model(&core.Tweet{}).Where("object_id = ?", id).Count(&allCount)

	var rows *sql.Rows
	var err error

	if from != "" && to != "" {
		fromTime, err := time.Parse(time.RFC1123, from)
		if err != nil {
			return nil, err
		}
		toTime, err := time.Parse(time.RFC1123, to)
		if err != nil {
			return nil, err
		}
		rows, err = db.Raw(`SELECT (published_at AT TIME ZONE 'Europe/Warsaw')::date, COUNT(*)
			FROM tweets
			WHERE object_id = ? AND published_at >= ? AND published_at <= ?
			GROUP BY 1 ORDER BY 1`, id, fromTime, toTime).Rows()
		if err != nil {
			return nil, err
		}
	} else if from != "" {
		fromTime, err := time.Parse(time.RFC1123, from)
		if err != nil {
			return nil, err
		}
		rows, err = db.Raw(
			`SELECT (published_at AT TIME ZONE 'Europe/Warsaw')::date, COUNT(*)
			FROM tweets
			WHERE object_id = ? AND published_at >= ?
			GROUP BY 1 ORDER BY 1`, id, fromTime).Rows()
		if err != nil {
			return nil, err
		}
	} else if to != "" {
		toTime, err := time.Parse(time.RFC1123, to)
		if err != nil {
			return nil, err
		}
		rows, err = db.Raw(
			`SELECT (published_at AT TIME ZONE 'Europe/Warsaw')::date, COUNT(*)
			FROM tweets
			WHERE object_id = ? AND published_at <= ?
			GROUP BY 1 ORDER BY 1`, id, toTime).Rows()
		if err != nil {
			return nil, err
		}
	} else {
		rows, err = db.Raw(
			`SELECT (published_at AT TIME ZONE 'Europe/Warsaw')::date, COUNT(*)
			FROM tweets
			WHERE object_id = ?
			GROUP BY 1 ORDER BY 1`, id).Rows()
		if err != nil {
			return nil, err
		}
	}

	days := make([]DayStats, 0)
	for rows.Next() {
		var createdAt time.Time
		var count int
		err := rows.Scan(&createdAt, &count)
		if err != nil {
			return nil, err
		}
		days = append(days, DayStats{
			Date:  createdAt.Format(time.RFC1123),
			Count: count,
		})
	}

	return &ObjectStats{
		ID:        id,
		Query:     obj.Query,
		Deleted:   obj.DeletedAt != nil,
		AllTweets: allCount,
		Days:      days,
	}, nil
}
