package core

import (
	"errors"
	"fmt"

	"github.com/dghubble/go-twitter/twitter"
	"github.com/dghubble/oauth1"
)

type DbConfig struct {
	Host     *string
	Port     *string
	User     *string
	Password *string
	DbName   *string `yaml:"dbName"`
	SslMode  *string `yaml:"sslMode"`
}

type TwitterConfig struct {
	ConsumerKey    *string `yaml:"consumerKey"`
	ConsumerSecret *string `yaml:"consumerSecret"`
	Token          *string
	TokenSecret    *string `yaml:"tokenSecret"`
}

type JsonConfig struct {
	All    bool
	Fields []string
}

type UserAccount struct {
	Username *string
	Password *string
}

type WebConfig struct {
	Port   *int
	Secret *string
	Users  []UserAccount
}

func (d *DbConfig) ConnectionString() (string, error) {
	if d.Host == nil {
		return "", errors.New("Database host not specified.")
	}
	if d.Port == nil {
		return "", errors.New("Database port not specified.")
	}
	if d.User == nil {
		return "", errors.New("Database user not specified.")
	}
	if d.Password == nil {
		return "", errors.New("Database password not specified.")
	}
	if d.DbName == nil {
		return "", errors.New("Database name not specified.")
	}
	if d.SslMode == nil {
		return "", errors.New("Database ssl mode not specified.")
	}
	return fmt.Sprintf("host=%s port=%s user=%s dbname=%s password=%s sslmode=%s",
		*d.Host, *d.Port, *d.User, *d.DbName, *d.Password, *d.SslMode), nil
}

func (t *TwitterConfig) TwitterClient() (*twitter.Client, error) {
	if t.ConsumerKey == nil {
		return nil, errors.New("Twitter consumer key not specified.")
	}
	if t.ConsumerSecret == nil {
		return nil, errors.New("Twitter consumer secret not specified.")
	}
	if t.Token == nil {
		return nil, errors.New("Twitter token not specified.")
	}
	if t.TokenSecret == nil {
		return nil, errors.New("Twitter token secret not specified.")
	}
	config := oauth1.NewConfig(*t.ConsumerKey, *t.ConsumerSecret)
	token := oauth1.NewToken(*t.Token, *t.TokenSecret)
	httpClient := config.Client(oauth1.NoContext, token)
	return twitter.NewClient(httpClient), nil
}

func (w *WebConfig) WebAccounts() (map[string]string, error) {
	result := make(map[string]string)
	for _, user := range w.Users {
		if user.Username == nil {
			return nil, errors.New("Username not specified for account.")
		}
		if user.Password == nil {
			return nil, errors.New("Password not specified for account.")
		}
		result[*user.Username] = *user.Password
	}
	return result, nil
}
