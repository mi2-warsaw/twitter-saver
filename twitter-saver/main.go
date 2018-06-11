package main

import (
	"flag"
	"log"
	"os"
	"os/signal"
	"syscall"

	"github.com/robfig/cron"
	"gitlab.com/kompu/twitter-saver/core"
)

func main() {
	configPath := flag.String("config", "config.yaml", "Path to config file")
	flag.Parse()

	conf, err := readConfig(configPath)
	if err != nil {
		log.Fatalln(err)
	}

	conn, err := conf.Db.ConnectionString()
	if err != nil {
		log.Fatalln(err)
	}
	db := core.Connect(conn)
	defer db.Close()

	client, err := conf.Twitter.TwitterClient()
	if err != nil {
		log.Fatalln(err)
	}

	downloading := SafeFlag{v: false}

	scheduler := cron.New()
	scheduler.AddFunc("0 0 0 * * *", func() {
		startDownloadingHistory(conf, db, client, &downloading)
	})
	scheduler.Start()

	go startStreaming(conf, db, client)
	go startDownloadingHistory(conf, db, client, &downloading)

	exitSignal := make(chan os.Signal)
	signal.Notify(exitSignal, syscall.SIGINT, syscall.SIGTERM)
	<-exitSignal

	scheduler.Stop()
}
