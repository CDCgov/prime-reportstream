#!/usr/bin/env bash
# set the permissions correctly on the two keys for the server
# when I committed the files, the permissions for the two host keys were set to 0600
# but that apparently is not respected by Git. When Git copies them back in, it copies
# them in with 0644, and then atmoz will not load them. This script will make sure that
# the permissions for the keys are set properly before it spins up the daemon.
echo Setting permissions for the keys
chmod 0600 /etc/ssh/ssh_host_ed25519_key
chmod 0600 /etc/ssh/ssh_host_rsa_key
echo Done with SFTP init
