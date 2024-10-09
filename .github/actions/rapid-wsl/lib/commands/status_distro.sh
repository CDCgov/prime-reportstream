#!/bin/bash

echo '
################################
#      installed distros       #
################################
'
wsl -l --all

echo '
################################
#            images            #
################################
'
docker images

echo '
################################
#          containers          #
################################
'
docker ps -q | xargs -i sh -c '\
echo "========================================================="; \
echo "========================================================="; \
docker ps --filter "id={}" --format "{{.Names}}"; \
docker logs --tail 8 {};'

echo '
################################
#          application         #
################################
'

FILE=./modules/$2/status_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Fetching module specific application status"
    $FILE $1 $2 $3 $4
    echo "Completed module specific application status"
fi
