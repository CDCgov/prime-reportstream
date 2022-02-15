#!/usr/bin/env bash
# set the permissions correctly on the two keys for the server
echo Setting permissions for the keys
chmod 0600 /etc/ssh/ssh_host_ed25519_key
chmod 0600 /etc/ssh/ssh_host_rsa_key
