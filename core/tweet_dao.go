package core

import (
	"encoding/json"
	"errors"
	"strings"
	"time"

	"github.com/dghubble/go-twitter/twitter"
	"github.com/jinzhu/gorm"
)

func addField(jsonMap map[string]interface{}, result map[string]interface{}, field string) error {
	index := strings.Index(field, ".")
	if index > -1 {
		key := field[:index]
		rest := field[index+1:]
		value, prs := jsonMap[key]
		if !prs {
			return errors.New("Field " + key + " not present in json")
		}
		v, ok := value.(map[string]interface{})
		if !ok {
			return errors.New("Field " + key + " is not nested value in json")
		}

		r, prs := result[key]
		if !prs {
			r = make(map[string]interface{})
		}

		res := r.(map[string]interface{})
		err := addField(v, res, rest)
		if err != nil {
			return err
		}
		result[key] = res
		return nil
	} else {
		value, prs := jsonMap[field]
		if !prs {
			return errors.New("Field " + field + " not present in json")
		}
		result[field] = value
		return nil
	}
}

func jsonWithFields(j []byte, fields []string) ([]byte, error) {
	var jsonMap map[string]interface{}
	err := json.Unmarshal(j, &jsonMap)
	if err != nil {
		return nil, err
	}

	result := make(map[string]interface{})
	for _, field := range fields {
		err := addField(jsonMap, result, field)
		if err != nil {
			return nil, err
		}
	}

	resultBytes, err := json.Marshal(result)
	if err != nil {
		return nil, err
	}
	return resultBytes, nil
}

func InsertTweet(db *gorm.DB, tweet *twitter.Tweet, objectId uint, fields []string, allFields bool) error {
	publishedAt, err := time.Parse(time.RubyDate, tweet.CreatedAt)
	if err != nil {
		return err
	}

	entity := &Tweet{
		PublishedAt: publishedAt,
		TweetId:     tweet.ID,
		UserId:      tweet.User.ID,
		Text:        tweet.Text,
		ObjectId:    objectId,
	}

	if allFields || len(fields) > 0 {
		j, err := json.Marshal(tweet)
		if err != nil {
			db.Create(&entity)
			return err
		}

		if allFields {
			entity.ExtendedInfo = string(j)
		} else {
			res, err := jsonWithFields(j, fields)
			if err != nil {
				db.Create(&entity)
				return err
			}
			entity.ExtendedInfo = string(res)
		}
	}

	db.Create(&entity)
	return nil
}

func InsertTweetUnique(db *gorm.DB, tweet *twitter.Tweet, objectId uint, fields []string, allFields bool) error {
	count := 0
	db.Model(&Tweet{}).Where("tweet_id = ? AND object_id = ?", tweet.ID, objectId).Count(&count)
	if count == 0 {
		return InsertTweet(db, tweet, objectId, fields, allFields)
	} else {
		return nil
	}
}

func RemoveTweetsOlderThan(db *gorm.DB, date time.Time) error {
	_, err := db.Raw("DELETE FROM tweets WHERE published_at::date < ?", date).Rows()
	return err
}

func LatestTweetForObject(db *gorm.DB, objectId uint) (int64, error) {
	rows, err := db.Raw("SELECT MAX(tweet_id) FROM tweets WHERE object_id = ?", objectId).Rows()
	if err != nil {
		return 0, err
	}

	rows.Next()
	var tweetId int64
	err = rows.Scan(&tweetId)
	if err != nil {
		return 0, err
	}
	return tweetId, nil
}
