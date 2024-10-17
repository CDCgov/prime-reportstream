#!/bin/bash

set -e

DEFAULT_KEY_PATH="$HOME"

prompt() {
  local prompt=$1
  read -p "$prompt: " value
  echo "${value}"
}

prompt_with_default() {
  local prompt=$1
  local default_value=$2
  read -p "$prompt [$default_value]: " value
  echo "${value:-$default_value}"
}

remote_user=$(prompt "Enter remote username")
remote_hostname=$(prompt "Enter remote hostname")
remote_port=$(prompt "Enter remote port")

key_path=$(prompt_with_default "Enter the key path" "$DEFAULT_KEY_PATH")

echo "y" | ssh-keygen -t ed25519 -b 4096  -f $key_path/.ssh/id_ed25519 -N ""

USER_AT_HOST="${remote_user}@${remote_hostname}"
PUBKEYPATH="$key_path/.ssh/id_ed25519.pub"

pubKey=$(cat "$PUBKEYPATH")
ssh -p $remote_port "$USER_AT_HOST" "mkdir -p ~/.ssh && chmod 700 ~/.ssh && echo '$pubKey' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"
