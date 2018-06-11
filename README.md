# twitter-saver

## Description

### Info
The program was created for *Team project - development of data analysis system* course run by [@pbiecek](https://github.com/pbiecek) at Warsaw University of Technology.

### Program description

The aim of the created program is to download tweets from twitter by defined user or keyword. Downloading can be done in two modes:

* stream
* history

### Further information

Full specification and more detailed description of summarization features (in Polish) can be found in [this file](https://github.com/minorczyka/twitter-saver/blob/master/docs/Instrukcja%20obs%C5%82ugi.pdf).

## Installation

1. Download binary file from latest release
2. Prepare `config.yaml`
3. Run binaries `twitter-saver` and `web`

## Usage

### Running

The programs can be run from command line with following arguments:

* `--config` - path to config file

Config file is stored in YAML format. It contains following information:

- `db`:
  - `host`
  - `port`
  - `user`
  - `password`
  - `dbName` - database name in which data will be stored
  - `sslMode` - `enable` or `disable`
- `web` - web interface parameters:
  - `port` - port on which server will be working
  - `secret` - private key used to sign session identifiers. Should be random and renewed periodically. Keys shorter than 256 bits are not recommended.
- `users` - sequence of user accounts. Each account consists of:
  - `username`
  - `password`
- `twitter` - twitter API keys:
  - `consumerKey`
  - `consumerSecret`
  - `token`
  - `tokenSecret`
- `json` - defines additional fields from tweet saved in database
  - `all` - saves whole tweet content
  - `fields` - sequence of field names to be stored
- `autoDeleteDays` - number of days after which data will be automatically removed

### Screenshots and live examples

Screenshot of project UI:
![the screenshot](https://github.com/minorczyka/twitter-saver/blob/master/misc/screenshots/screen1.png)

## Authors

* Piotr Krzeszewski
* Łukasz Ławniczak
* Artur Minorczyk
