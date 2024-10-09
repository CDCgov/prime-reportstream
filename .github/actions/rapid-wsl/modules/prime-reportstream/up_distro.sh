#!/bin/bash

wsl -d $1 -u $3 -e bash -c \
' \
cd ~/repos/prime-reportstream/prime-router; \
docker-compose --file "docker-compose.build.yml" up --detach; \
./devenv-infrastructure.sh up; \
cd ../; \
'
