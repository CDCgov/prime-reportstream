#!/bin/bash

# Wrapper script to run Prime Router with rs- infrastructure
# This allows us to use rs- prefixed services with standard Gradle tasks

set -e

echo " Starting Prime Router with RS- Hardened Infrastructure"
echo "=============================================="

# Start rs- infrastructure first  
echo " Starting rs- prefixed hardened infrastructure..."

# Start vault first for credentials
docker-compose -f docker-compose.secure-multiarch.yml up -d rs-vault

# Start hardened PostgreSQL manually (image exists locally)
echo " Starting hardened PostgreSQL..."
docker run -d --name rs-postgresql \
    --network host \
    -e POSTGRES_USER=prime \
    -e POSTGRES_PASSWORD="changeIT!" \
    -e POSTGRES_DB=prime_data_hub \
    rs-postgresql:latest

# Start remaining services
docker-compose -f docker-compose.secure-multiarch.yml up -d rs-azurite rs-azurite-stage rs-sftp rs-soap-webservice rs-rest-webservice

# Wait for PostgreSQL to be ready
echo " Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec rs-postgresql pg_isready -U prime -d prime_data_hub &>/dev/null; then
        echo " PostgreSQL is ready"
        break
    elif [ $i -eq 30 ]; then
        echo " PostgreSQL failed to start"
        exit 1
    fi
    sleep 2
done

# Wait for vault credentials
echo " Waiting for vault credentials..."
while [[ $(wc -c < ".vault/env/.env.local") == 0 ]]; do 
    sleep 2
done
echo " Vault credentials ready"

echo ""
echo " RS Infrastructure is ready!"
echo " You can now run standard Gradle commands:"
echo "   ./gradlew run                    # Start API"
echo "   ./gradlew test                   # Run tests"  
echo "   ./gradlew testSmoke             # Run smoke tests"
echo "   ./gradlew testEnd2End           # Run E2E tests"
echo ""
echo " Services running on:"
echo "   PostgreSQL: 127.0.0.1:5432"
echo "   Vault: 127.0.0.1:8200" 
echo "   Azurite: 127.0.0.1:10000-10002"
echo "   SFTP: 127.0.0.1:2222"
echo "   SOAP: 127.0.0.1:8087"
echo "   REST: 127.0.0.1:3001"
echo "   Azurite Stage: 127.0.0.1:11000-11001"
echo ""
echo " To stop rs- infrastructure:"
echo "   docker-compose -f docker-compose.secure-multiarch.yml down"
echo ""

# Load environment variables for this session
export $(cat .vault/env/.env.local | xargs)
echo " Environment variables loaded for this session"
echo " Ready for Prime Router development with complete local infrastructure!"