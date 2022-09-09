#!/bin/bash

# Requires jq: sudo apt-get install jq -y
# Exit if any of the intermediate steps fail
set -e

# Parse the input
eval "$(jq -r '@sh "environment=\(.environment)"')"

upper_env=${environment^^}

# Query user SFTP SSH keys
SSH=$(az sshkey list --query "[?resourceGroup=='PRIME-DATA-HUB-$upper_env']" | jq -c '[.[] | select( .name | test("sftp.*-") ).name | (. / "-" | {instance: .[-2], user: .[-1]})]')
SSHNAMES=$(az sshkey list --query "[?resourceGroup=='PRIME-DATA-HUB-$upper_env']" | jq -c '[.[] | select( .name | test("sftp.*-") ).name] | sort')
INSTANCES=$(echo $SSH | jq -c '[.[] | .instance] | unique')
SSHGROUPS=$(echo $SSH | jq -c '[group_by(.instance)[] | {(.[0].instance): [.[] | .user]}]')
#SHARES=$(az storage share list --account-name=pdh${environment}sftp --only-show-errors | jq -c '[.[] | select( .name | test("sftp.*-share-.*")).name]')

# Safely produce a JSON object containing the result value.
# jq will ensure that the value is properly quoted
# and escaped to produce a valid JSON string.
jq -n --arg ssh "$SSH" --arg instances "$INSTANCES" --arg sshgroups "$SSHGROUPS" --arg sshnames "$SSHNAMES" '{"ssh":$ssh, "instances":$instances, "sshgroups":$sshgroups, "sshnames":$sshnames}'
