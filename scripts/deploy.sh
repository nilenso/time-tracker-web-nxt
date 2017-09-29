#!/usr/bin/env bash

# Deployment script for time-tracker web

USERNAME=enso
HOST=time.nilenso.com
# Script to run on server
SCRIPT="sudo rm -rf /var/www/*; sudo mv time-tracker-web-nxt/* /var/www/"


echo "[1] Cleaning previous build"
lein clean

echo "[2] Compiling application"
lein cljsbuild once min

echo "[3] Deploying artifacts to server"
scp -r resources/public/* ${USERNAME}@${HOST}:time-tracker-web-nxt/
ssh -l ${USERNAME} -t ${HOST} "${SCRIPT}"

echo "[4] Deployment Complete. You're good to go!"
