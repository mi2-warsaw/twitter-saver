package main

import (
	"log"
	"sync"
	"time"

	"github.com/dghubble/go-twitter/twitter"
	"github.com/jinzhu/gorm"
	"gitlab.com/kompu/twitter-saver/core"
)

type SafeFlag struct {
	v   bool
	mux sync.Mutex
}

func (f *SafeFlag) SetFlag(v bool) {
	f.mux.Lock()
	f.v = v
	f.mux.Unlock()
}

func (f *SafeFlag) CheckFlag() bool {
	f.mux.Lock()
	defer f.mux.Unlock()
	return f.v
}

func startDownloadingHistory(config *Config, db *gorm.DB, client *twitter.Client, downloading *SafeFlag) {
	if downloading.CheckFlag() {
		log.Println("Already downloading history.")
		return
	}

	downloading.SetFlag(true)
	if config.AutoDeleteDays != nil {
		log.Println("Removing old tweets.")
		days := time.Duration(*config.AutoDeleteDays)
		date := time.Now().Add(days * -24 * time.Hour)
		err := core.RemoveTweetsOlderThan(db, date)
		if err != nil {
			log.Println(err)
		}
	}

	log.Println("Started downloading history objects.")

	objects := core.FindHistoryObjects(db)
	for _, object := range objects {
		var date *time.Time
		var sinceId int64
		if object.HistoryFrom != nil && !object.HistoryDone {
			date = object.HistoryFrom
		} else {
			var err error
			sinceId, err = core.LatestTweetForObject(db, object.ID)
			if err != nil {
				log.Println("Error getting from object", object.ID, "latest sinceId:", err)
			}
		}

		var query string
		if object.Type == core.UserType {
			query = "from:" + object.Query
		} else {
			query = object.Query
		}
		log.Printf("Downloading history for '%s'", query)

		lastMaxId := int64(0)
		for {
			results, maxId, err := searchSince(client, query, date, sinceId, lastMaxId)
			for _, tweet := range results {
				err := core.InsertTweet(db, &tweet, object.ID, config.Json.Fields, config.Json.All)
				if err != nil {
					log.Println("Json storing error for history object", query, ":", err)
				}
			}

			if err != nil {
				log.Println("Error downloading history for", query, ":", err)
			} else if object.HistoryFrom != nil && !object.HistoryDone {
				core.UpdateObjectHistory(db, &object, true)
			}

			if maxId != 0 {
				log.Println("Limit exceeded for history object", query)
				time.Sleep(15 * time.Minute)
				log.Println("Resuming downloading", query)
				lastMaxId = maxId
			} else {
				break
			}
		}
	}

	log.Println("Finished downloading history objects.")
	downloading.SetFlag(false)
}
