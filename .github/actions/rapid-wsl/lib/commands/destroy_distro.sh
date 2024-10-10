#!/bin/bash

./lib/commands/down_distro.sh $1 $2 $3 $4

wsl --unregister $1

if [[ ${?} == 0 ]]; then
    sleep 5
fi
