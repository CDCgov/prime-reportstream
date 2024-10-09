#!/bin/bash

wsl --set-default $1

echo "Restarting docker"
powershell -c 'Stop-Process -Name "Docker Desktop"'
sleep 1
dpath=$(which docker | sed 's/\/resources\/bin\/docker/\/Docker Desktop.exe/g')
"$dpath"
sleep 1

echo "Waiting for docker..."
wsl -d $1 -e bash -c \
' \
until usermod -aG docker '"$3"'; do sleep 5; done \
' &>/dev/null

wsl -d $1 -u $3 -e bash -c \
' \
until docker info; do sleep 5; done \
' &>/dev/null

echo "Docker restart complete"

FILE=./modules/$2/docker_distro.sh
if [[ -f "$FILE" ]]; then
    echo "Updating module specific docker"
    $FILE $1 $2 $3 $4
    echo "Module specific docker update complete"
fi
