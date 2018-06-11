package main

import (
	"log"
	"time"

	"github.com/dghubble/go-twitter/twitter"
	"github.com/jinzhu/gorm"
	"gitlab.com/kompu/twitter-saver/core"
)

type StreamInfo struct {
	ID    uint
	Type  core.ObjectType
	Query string
}

type Streams map[StreamInfo]chan int

func streamsToRemove(running Streams, newObjects []core.Object) []StreamInfo {
	newInfo := make(map[StreamInfo]bool)
	for _, v := range newObjects {
		newInfo[StreamInfo{ID: v.ID, Type: v.Type, Query: v.Query}] = true
	}

	rem := make([]StreamInfo, 0)
	for k := range running {
		_, ok := newInfo[k]
		if !ok {
			rem = append(rem, k)
		}
	}
	return rem
}

func streamsToAdd(running Streams, newObjects []core.Object) []StreamInfo {
	add := make([]StreamInfo, 0)
	for _, v := range newObjects {
		s := StreamInfo{ID: v.ID, Type: v.Type, Query: v.Query}
		_, ok := running[s]
		if !ok {
			add = append(add, s)
		}
	}
	return add
}

func startStream(config *Config, db *gorm.DB, client *twitter.Client, info StreamInfo,
	quit <-chan int, errors chan<- StreamInfo) {
	log.Println("Starting stream", info)

	var query string
	if info.Type == core.UserType {
		query = "from:" + info.Query
	} else {
		query = info.Query
	}
	params := &twitter.StreamFilterParams{
		Track:         []string{query},
		StallWarnings: twitter.Bool(true),
	}

	stream, err := client.Streams.Filter(params)
	if err != nil {
		log.Println("Stream error:", err)
		errors <- info
		return
	}

	demux := twitter.NewSwitchDemux()
	demux.Tweet = func(tweet *twitter.Tweet) {
		err := core.InsertTweet(db, tweet, info.ID, config.Json.Fields, config.Json.All)
		if err != nil {
			log.Println("Json storing error for stream", info, ":", err)
		}
	}
	demux.Warning = func(warning *twitter.StallWarning) {
		log.Println("Stream", info, "is falling behind. Warning code:",
			warning.Code, ", message: ", warning.Message)
	}
	demux.StreamLimit = func(limit *twitter.StreamLimit) {
		log.Println("Stream", info, "reached limit,",
			limit.Track, "undelivered matches.")
		errors <- info
		stream.Stop()
		return
	}
	demux.StreamDisconnect = func(disconnect *twitter.StreamDisconnect) {
		log.Println("Stream", info, "disconnected. Disconection code:",
			disconnect.Code, ", reason:", disconnect.Reason)
		errors <- info
		stream.Stop()
		return
	}

	for {
		select {
		case message := <-stream.Messages:
			demux.Handle(message)
		case <-quit:
			log.Println("Stoping stream", info)
			stream.Stop()
			return
		}
	}
}

func startStreaming(config *Config, db *gorm.DB, client *twitter.Client) {
	running := make(Streams)
	errors := make(chan StreamInfo)
	ticker := time.NewTicker(60 * time.Second)

	work := func() {
		streams := core.FindStreamObjects(db)
		rem := streamsToRemove(running, streams)
		add := streamsToAdd(running, streams)

		if len(add) > 0 {
			log.Println("Streams to add:", add)
		}
		if len(rem) > 0 {
			log.Println("Streams to remove:", rem)
		}

		for _, v := range rem {
			quit := running[v]
			quit <- 0
			delete(running, v)
		}

		for _, v := range add {
			quit := make(chan int)
			running[v] = quit
			go startStream(config, db, client, v, quit, errors)
		}
	}
	work()

	for {
		select {
		case v := <-errors:
			delete(running, v)
		case <-ticker.C:
			work()
		}
	}
}
