package core

import (
	"time"
)

type Tweet struct {
	ID        uint `gorm:"primary_key"`
	CreatedAt time.Time

	TweetId      int64     `gorm:"not null"`
	PublishedAt  time.Time `gorm:"not null"`
	UserId       int64     `gorm:"not null"`
	Text         string    `gorm:"not null" sql:"index"`
	ExtendedInfo string
	ObjectId     uint
}

type ObjectSource int32

const (
	HistorySource ObjectSource = iota + 1
	StreamSource
)

type ObjectType = int32

const (
	UserType ObjectType = iota + 1
	KeywordType
)

type Object struct {
	ID        uint `gorm:"primary_key"`
	CreatedAt time.Time
	DeletedAt *time.Time `sql:"index"`

	Source      ObjectSource `gorm:"not null"`
	Type        ObjectType   `gorm:"not null"`
	Query       string       `gorm:"not null"`
	HistoryFrom *time.Time
	HistoryDone bool
	Tweets      []Tweet
}
