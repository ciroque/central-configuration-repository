#!/usr/bin/env bash

## MongoDB stuffs
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
echo "deb http://repo.mongodb.org/apt/ubuntu trusty/mongodb-org/3.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-3.0.list

sudo adduser ccrsvc

sudo apt-get update

sudo apt-get upgrade -y

sudo apt-get install -y jsvc mongodb-org redis-server

