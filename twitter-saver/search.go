package main

import (
	"time"

	"github.com/dghubble/go-twitter/twitter"
)

func min(a, b int64) int64 {
	if a < b {
		return a
	}
	return b
}

func limitExceeded(apiError *twitter.APIError) bool {
	for _, err := range apiError.Errors {
		if err.Code == 88 {
			return true
		}
	}
	return false
}

func searchSince(client *twitter.Client, query string, date *time.Time, sinceId int64, maxId int64) ([]twitter.Tweet, int64, error) {
	results := make([]twitter.Tweet, 0)

	for {
		search, _, err := client.Search.Tweets(&twitter.SearchTweetParams{
			Query:   query,
			Count:   100,
			MaxID:   maxId,
			SinceID: sinceId,
		})
		if apiErr, ok := err.(twitter.APIError); ok && limitExceeded(&apiErr) {
			return results, maxId, nil
		}
		if err != nil {
			return results, 0, err
		}
		if len(search.Statuses) == 0 {
			return results, 0, nil
		}

		if maxId == 0 {
			maxId = search.Statuses[0].ID - 1
		}
		for _, tweet := range search.Statuses {
			maxId = min(maxId, tweet.ID-1)
			t, err := time.Parse(time.RubyDate, tweet.CreatedAt)
			if err != nil {
				return results, 0, err
			}
			if date != nil && t.Before(*date) {
				return results, 0, nil
			}
			results = append(results, tweet)
		}
	}
}
