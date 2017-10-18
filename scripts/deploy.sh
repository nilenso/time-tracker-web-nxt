#!/usr/bin/env bash

# Deployment script for time-tracker web

# get last commit hash prepended with @ (i.e. @8a323d0)
function parse_git_hash() {
  git rev-parse --short HEAD 2> /dev/null | sed "s/\(.*\)/@\1/"
}

now=$(date)
USERNAME=enso
HOST=time.nilenso.com
LOGS=/home/enso/logs/time-tracker-web-nxt
DEPLOY_LOG=$LOGS/deploys.log
MSG="[$now] Deployed commit hash $(parse_git_hash)"

# Unicode symbol emojis
CHECK="✅"
ROCKON="🤘"

# Script to run on server
# SCRIPT="sudo rm -rf /var/www/*; sudo mv time-tracker-web-nxt/* /var/www/"
SCRIPT="
sudo rm -rf /var/www/*
sudo mv time-tracker-web-nxt/* /var/www/

mkdir -p $LOGS
touch $DEPLOY_LOG;
echo $MSG >> $DEPLOY_LOG"

echo "$CHECK  Cleaning previous build"
lein clean

echo "$CHECK  Compiling application"
lein cljsbuild once min

echo "$CHECK  Deploying artifacts to server"
scp -r resources/public/* ${USERNAME}@${HOST}:time-tracker-web-nxt/
ssh -l ${USERNAME} -t ${HOST} "${SCRIPT}"

echo "Deploy log written to $DEPLOY_LOG"

echo "$CHECK  Deployment Complete! $ROCKON"
