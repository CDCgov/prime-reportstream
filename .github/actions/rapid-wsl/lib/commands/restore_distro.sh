#!/bin/bash

echo "Restoring backup: backups\\$1-$2-backup.tar"
wsl --import $1 'bin\'"$1"''-''"$2"'\' 'backups\'"$1"'-'"$2"'-backup.tar'

if [[ ${?} == 0 ]]; then
    ./lib/commands/up_distro.sh $1 $2 $3 $4
    echo "Restore complete"
fi
