#!/bin/sh

# replaces init.sh 
# Author: @devopsmatt 
# Update package list and install jq and curl 📦
apk update && apk add --no-cache jq curl

# Set Vault address for future CLI calls
export VAULT_ADDR='http://127.0.0.1:8200'

# Start the Vault server in the background
vault server -config=/vault/config/local.json &
VAULT_PID=$!
echo "Vault server process started with PID: $VAULT_PID"

# Wait for Vault to start up
echo "Waiting for Vault to start..."
sleep 5

# --- AUTOMATED INITIALIZATION AND UNSEALING ---

if [ -s "/vault/file/core/_keyring" ]; then
    echo "Vault is already initialized. Unsealing..."

    # Ensure .env.local exists for existing vault
    if [ ! -f "/vault/env/.env.local" ] && [ -f "/vault/file/keys.txt" ]; then
        echo "Creating missing .env.local from existing vault keys..."
        ROOT_TOKEN=$(grep 'Root Token:' /vault/file/keys.txt | awk '{print $3}')
        
        echo "VAULT_TOKEN=\"$ROOT_TOKEN\"" > /vault/env/.env.local
        echo "CREDENTIAL_STORAGE_METHOD=HASHICORP_VAULT" >> /vault/env/.env.local
        
        # Generate TokenSigningSecret if missing
        TOKEN_SIGNING_SECRET=$(cat /dev/urandom | head -c 64 | base64 | head -c 64)
        echo "TokenSigningSecret=$TOKEN_SIGNING_SECRET" >> /vault/env/.env.local
        
        echo "Prime Router credentials created in /vault/env/.env.local"
    fi
else
    echo "Vault is not initialized. Initializing and unsealing..."
    
    # Initialize Vault and capture the JSON output
    INIT_OUTPUT=$(vault operator init -key-shares=1 -key-threshold=1 -format=json)
    
    # Extract keys using the now-installed jq command
    UNSEAL_KEY=$(echo "$INIT_OUTPUT" | jq -r .unseal_keys_b64[0])
    ROOT_TOKEN=$(echo "$INIT_OUTPUT" | jq -r .root_token)
    
    # Persist the keys to the mounted volume
    echo "Unseal Key: $UNSEAL_KEY" > /vault/file/keys.txt
    echo "Root Token: $ROOT_TOKEN" >> /vault/file/keys.txt
    
    echo "Vault initialized. Keys stored in /vault/file/keys.txt"
    
    # Create Prime Router compatible .env.local file for host scripts
    echo "VAULT_TOKEN=\"$ROOT_TOKEN\"" > /vault/env/.env.local
    echo "CREDENTIAL_STORAGE_METHOD=HASHICORP_VAULT" >> /vault/env/.env.local
    
    # Generate TokenSigningSecret for Prime Router
    TOKEN_SIGNING_SECRET=$(cat /dev/urandom | head -c 64 | base64 | head -c 64)
    echo "TokenSigningSecret=$TOKEN_SIGNING_SECRET" >> /vault/env/.env.local
    
    echo "Prime Router credentials created in /vault/env/.env.local"
fi

# Unseal the Vault using the key from the file
UNSEAL_KEY_FROM_FILE=$(grep 'Unseal Key:' /vault/file/keys.txt | awk '{print $3}')
vault operator unseal "$UNSEAL_KEY_FROM_FILE"

# --- KEEP CONTAINER ALIVE ---
echo "Vault is running. Tailing logs..."
echo "Root token can be found in /vault/file/keys.txt"

wait $VAULT_PID
