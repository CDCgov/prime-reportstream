#!/bin/bash

wsl -d $1 -u $3 -e bash -c \
' \
cd ~/repos/prime-reportstream/prime-router/; \
./devenv-infrastructure.sh down; \
docker-compose down --remove-orphans; \
'

docker stop $(docker ps -a --format "{{.ID}} {{.Names}}" | grep prime-router.*)
docker rm -f $(docker ps -a --format "{{.ID}} {{.Names}}" | grep prime-router.*)
