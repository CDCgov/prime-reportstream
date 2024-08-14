#!/usr/bin/env bash

docker ps > /dev/null 2>&1

DOCKER_RC=$?

if [[ $DOCKER_RC -eq 0 ]]; then
  exit 0
else
  echo "Docker is not running. Docker is required to run ReportStream's Git hook. Please start Docker and retry."
  exit 1
fi
