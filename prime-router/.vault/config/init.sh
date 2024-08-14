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
else
  printf "No key found in .vault/env. Creating a new key set...\n\n"
  
  # Generate a key for the vault
  vault operator init -key-shares=1 -key-threshold=1 > /tmp/init.log
  printf "\n$(cat /tmp/init.log)"

  # Capture the vault keys
  UNSEAL_KEY=`sed '1!d' /tmp/init.log | sed 's/Unseal Key 1: //g'`
  ROOT_TOKEN=`sed '3!d' /tmp/init.log | sed 's/Initial Root Token: //g'`
  echo "$UNSEAL_KEY" > /vault/env/key
  echo "VAULT_TOKEN=\"$ROOT_TOKEN\"" > /vault/env/.env.local
  echo "CREDENTIAL_STORAGE_METHOD=HASHICORP_VAULT" >> /vault/env/.env.local
fi

# Create the TokenSigningSecret env var in the local env file, if its not there.
# Bug: Since ReportStream has already read that file before this runs,
# the first time you do this (only), 
# you'll have to restart the container to grab this new value.
if [ $(grep -c TokenSigningSecret /vault/env/.env.local) -eq 0 ] ; then
  printf "No TokenSigningSecret found in .env.local. Creating a new TokenSigningSecret...\n"
  TOKEN_SIGNING_SECRET=`cat /dev/urandom | head -c 64 | base64 | head -c 64`
  echo "TokenSigningSecret=$TOKEN_SIGNING_SECRET" >> /vault/env/.env.local
  printf "Done adding TokenSigningSecret to .env.local\n"
else 
  printf "TokenSigningSecret OK: found in .env.local\n"
fi

printf "\n\n"

# Init the auth token and unseal the vault
export $(cat /vault/env/.env.local | xargs)
vault operator unseal "$(cat /vault/env/key)"

printf "\n\n"

# Enable kv secrets
if vault secrets list | grep -q "kv"; then
  printf "Key/Value store is already initialized."
else
  printf "Initializing Key/Value store..."
  vault secrets enable -path=secret -version=1 kv
fi

printf "\n"

# Bring Vault back to the foreground
wait $!