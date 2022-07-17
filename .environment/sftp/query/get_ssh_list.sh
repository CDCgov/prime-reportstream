#!/bin/bash

# Requires jq: sudo apt-get install jq -y
# Exit if any of the intermediate steps fail
set -e

# Parse the input
eval "$(jq -r '@sh "environment=\(.environment)"')"

# Query user SFTP SSH keys
SSH=$(az sshkey list --resource-group prime-data-hub-$environment | jq -c '[.[] | select( .name | test("sftp*-") ).name | (. / "-" | {instance: .[-2], user: .[-1]})]')
SSHINSTANCES=$(echo $SSH | jq '[.[] | .instance] | unique')

# Safely produce a JSON object containing the result value.
# jq will ensure that the value is properly quoted
# and escaped to produce a valid JSON string.
jq -n --arg ssh "$SSH" --arg instances "$INSTANCES" '{"ssh":$ssh, "instances":$instances}'
