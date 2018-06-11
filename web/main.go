package main

import (
	"flag"
	"fmt"
	"log"
	"net/http"
	"strconv"
	"time"

	jwt "github.com/appleboy/gin-jwt"
	"github.com/gin-gonic/gin"
	"gitlab.com/kompu/twitter-saver/core"
)

func main() {
	configPath := flag.String("config", "config.yaml", "Path to config file")
	flag.Parse()

	conf, err := readConfig(configPath)
	if err != nil {
		log.Fatalln(err)
	}
	if conf.Web.Port == nil {
		log.Fatalln("Web server port not specified.")
	}
	if conf.Web.Secret == nil {
		log.Fatalln("Web secret key not specified.")
	}

	accounts, err := conf.Web.WebAccounts()
	if err != nil {
		log.Fatalln(err)
	}

	conn, err := conf.Db.ConnectionString()
	if err != nil {
		log.Fatalln(err)
	}
	db := core.Connect(conn)
	defer db.Close()

	r := gin.Default()

	r.Static("/assets", "./public")
	r.StaticFile("/", "./public/index.html")

	authMiddleware := jwt.GinJWTMiddleware{
		Realm:      "twitter-saver",
		Key:        []byte(*conf.Web.Secret),
		Timeout:    24 * time.Hour,
		MaxRefresh: 7 * 24 * time.Hour,
		Authenticator: func(userId string, password string, c *gin.Context) (string, bool) {
			if pass, ok := accounts[userId]; ok {
				return userId, pass == password
			}
			return userId, false
		},
	}

	authorized := r.Group("/api")

	authorized.POST("/login", authMiddleware.LoginHandler)

	authorized.Use(authMiddleware.MiddlewareFunc())

	authorized.GET("/refresh_token", authMiddleware.RefreshHandler)

	authorized.GET("/objects", func(c *gin.Context) {
		objects := listObjects(db)
		c.JSON(http.StatusOK, objects)
	})
	authorized.POST("/objects", func(c *gin.Context) {
		var json ObjectAdd
		if err := c.ShouldBindJSON(&json); err == nil {
			err := addObject(db, &json)
			if err != nil {
				if err.Error() == "Two streams already exist." {
					c.JSON(http.StatusNotAcceptable, gin.H{"error": err.Error()})
				} else {
					c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
				}
			} else {
				c.JSON(http.StatusOK, gin.H{"message": "ok"})
			}
		} else {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		}
	})

	authorized.GET("/objects/:id", func(c *gin.Context) {
		idStr := c.Param("id")
		id, err := strconv.Atoi(idStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Wrong 'id' format"})
			return
		}
		from := c.DefaultQuery("from", "")
		to := c.DefaultQuery("to", "")
		stats, err := objectStats(db, id, from, to)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
			return
		}
		c.JSON(http.StatusOK, stats)
	})
	authorized.DELETE("/objects/:id", func(c *gin.Context) {
		idStr := c.Param("id")
		allStr := c.DefaultQuery("all", "false")
		id, err := strconv.ParseUint(idStr, 10, 64)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Wrong 'id' format"})
			return
		}
		all, err := strconv.ParseBool(allStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Wrong 'all' format"})
			return
		}
		err = deleteObject(db, uint(id), all)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		} else {
			c.JSON(http.StatusOK, gin.H{"message": "ok"})
		}
	})

	addr := fmt.Sprintf(":%d", *conf.Web.Port)
	r.Run(addr)
}
