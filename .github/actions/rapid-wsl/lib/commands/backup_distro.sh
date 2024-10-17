#!/bin/bash

echo "Creating backup: backups\\$1-$2-backup.tar"
wsl --export $1 'backups\'"$1"'-'"$2"'-backup.tar'
echo "Backup complete"
