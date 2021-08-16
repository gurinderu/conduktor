#! /usr/bin/env bash


docker-compose up -d
cd ./backend
sbtn run &
cd ../frontend
npm install
npm start &
