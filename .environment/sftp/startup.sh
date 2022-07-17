#!/bin/bash

# Copy SSH host to admin share if they do not exist
cp -n /etc/ssh/ssh_host_*_key* /etc/sftpadmin/

# Overwrite container SSH host keys from admin share
cp /etc/sftpadmin/ssh_host_*_key* /etc/ssh
