#!/usr/bin/env sh
set -e

printf "Initialize Vault for developer use..."

export VAULT_ADDR=http://127.0.0.1:8200
export VAULT_API_ADDR=http://127.0.0.1:8200

# Start the Vault server in the background
vault server -config /vault/config/local.json &
sleep 3
printf "\n"

if [ -f /vault/env/key ]; then
  printf "Existing key found in .vault/env. Using key to initialize Vault...\n\n"
  vault operator unseal "$(cat /vault/env/key)"
else
  printf "No key found in .vault/env. Creating a new key set...\n\n"
  
  # Generate a key for the vault
  vault operator init -key-shares=1 -key-threshold=1 > /tmp/init.log
  printf "\n$(cat /tmp/init.log)"

  # Capture the vault keys
  UNSEAL_KEY=`sed '1!d' /tmp/init.log | sed 's/Unseal Key 1: //g'`
  ROOT_TOKEN=`sed '3!d' /tmp/init.log | sed 's/Initial Root Token: //g'`
  echo "$UNSEAL_KEY" > /vault/env/key
  echo "VAULT_ROOT_TOKEN=\"$ROOT_TOKEN\"" > /vault/env/.env.local
fi

printf "\n"

# Bring Vault back to the foreground
wait $!