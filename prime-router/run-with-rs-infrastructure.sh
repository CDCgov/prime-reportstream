#!/bin/bash
# Shellcheck disables: SC2249 (case patterns), SC2312 (command masking with ||), 
# SC1091 (sourcing dynamic env file), SC2015 (A && B || C pattern for optional operations)
# shellcheck disable=SC2249,SC2312,SC1091,SC2015

# Wrapper script to run Prime Router with rs infrastructure
# This allows us to use rs prefixed services with standard Gradle tasks
#
# Usage: ./run-with-rs-infrastructure.sh [--status|--down|--spotless|--help]
#   --status    Show status of rs infrastructure services
#   --down      Stop and remove all rs infrastructure
#   --spotless  DESTRUCTIVE: Remove all rs containers, volumes, and file mounts
#   --help      Show this help message

set -e

# Parse command line arguments
case "${1:-}" in
    --help|-h)
        echo "Usage: ./run-with-rs-infrastructure.sh [OPTIONS]"
        echo ""
        echo "Manages rs prefixed hardened infrastructure for Prime Router development"
        echo ""
        echo "Options:"
        echo "  --status    Show status of all rs infrastructure services"
        echo "  --down      Stop and remove all rs infrastructure"
        echo "  --spotless  DESTRUCTIVE: Remove all rs containers, volumes, and file mounts"
        echo "  --help      Show this help message"
        echo ""
        echo "Default behavior (no flags): Start complete rs infrastructure"
        exit 0
        ;;
    --status)
        echo "rs Infrastructure Status"
        echo "========================"
        echo ""
        echo "Docker Containers:"
        echo "rs Infrastructure:"
        docker ps --filter "name=rs-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "No rs containers running"
        echo ""
        echo "Prime Router (Gradle-managed):"
        docker ps --filter "name=prime-router-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "No prime-router- containers running"
        echo ""
        echo "Docker Compose Services:"
        docker-compose -f docker-compose.matts-testing.yml ps 2>/dev/null || echo "No compose services running"
        echo ""
        echo "Port Usage:"
        echo "PostgreSQL (5432): $(lsof -ti :5432 2>/dev/null && echo "OCCUPIED" || echo "AVAILABLE")"
        echo "Vault (8200): $(lsof -ti :8200 2>/dev/null && echo "OCCUPIED" || echo "AVAILABLE")"
        echo "Azurite (10000): $(lsof -ti :10000 2>/dev/null && echo "OCCUPIED" || echo "AVAILABLE")"
        echo "API (7071): $(lsof -ti :7071 2>/dev/null && echo "OCCUPIED" || echo "AVAILABLE")"
        exit 0
        ;;
    --down)
        echo "Stopping rs Infrastructure"
        echo "==========================="
        echo ""
        echo "Stopping Docker Compose services..."
        docker-compose -f docker-compose.matts-testing.yml down --remove-orphans --volumes 2>/dev/null || true
        echo ""
        echo "Stopping manual rs and prime-router containers..."
        docker ps -q --filter "name=rs-" | xargs -r docker stop 2>/dev/null || echo "No rs containers to stop"
        docker ps -q --filter "name=prime-router-" | xargs -r docker stop 2>/dev/null || echo "No prime-router- containers to stop"
        docker ps -aq --filter "name=rs-" | xargs -r docker rm 2>/dev/null || echo "No rs containers to remove"
        docker ps -aq --filter "name=prime-router-" | xargs -r docker rm 2>/dev/null || echo "No prime-router- containers to remove"
        echo ""
        
        echo "Stopping Gradle and Azure Functions processes..."
        pkill -f 'gradlew.*run' 2>/dev/null || echo "No gradlew processes to stop"
        pkill -f 'java.*prime-router' 2>/dev/null || echo "No Prime Router Java processes to stop"
        pkill -f 'func host' 2>/dev/null || echo "No Azure Functions host processes to stop"
        pkill -f 'azure-functions' 2>/dev/null || echo "No Azure Functions processes to stop"
        echo ""
        echo "Cleaning up rs- networks..."
        docker network ls --filter "name=rs-" -q | xargs docker network rm 2>/dev/null || echo "No rs networks to remove"
        echo ""
        echo "rs infrastructure stopped and cleaned up"
        exit 0
        ;;
    --spotless)
        echo "WARNING: SPOTLESS CLEANUP - THIS WILL DELETE ALL FILE SHARES!"
        echo "========================================================="
        echo ""
        echo "This will PERMANENTLY DELETE:"
        echo "  • All rs and prime-router containers and images"
        echo "  • All rs and prime-router Docker volumes (including database data)"
        echo "  • All rs and prime-router networks"
        echo "  • All file mounts and shared data"
        echo ""
        read -r -p "Are you sure you want to continue? [y/N]: " confirmation
        if [[ "${confirmation}" != "y" && "${confirmation}" != "Y" ]]; then
            echo "Spotless cleanup cancelled"
            exit 0
        fi
        echo ""
        echo "Performing SPOTLESS cleanup..."
        echo "=============================="
        echo ""
        
        # Stop and remove Docker Compose services with volumes
        echo "Stopping Docker Compose services and removing volumes..."
        docker-compose -f docker-compose.matts-testing.yml down --remove-orphans --volumes --rmi all 2>/dev/null || true
        echo ""
        
        # Stop and remove all rs- and prime-router- containers
        echo "Stopping and removing all rs and prime-router containers..."
        docker ps -q --filter "name=rs-" | xargs -r docker stop 2>/dev/null || echo "No rs containers to stop"
        docker ps -q --filter "name=prime-router-" | xargs -r docker stop 2>/dev/null || echo "No prime-router- containers to stop"
        docker ps -aq --filter "name=rs-" | xargs -r docker rm 2>/dev/null || echo "No rs containers to remove"
        docker ps -aq --filter "name=prime-router-" | xargs -r docker rm 2>/dev/null || echo "No prime-router- containers to remove"
        echo ""
        
        echo "Stopping all Gradle and Azure Functions processes..."
        pkill -f 'gradlew.*run' 2>/dev/null || echo "No gradlew processes to stop"
        pkill -f 'java.*prime-router' 2>/dev/null || echo "No Prime Router Java processes to stop"
        pkill -f 'func host' 2>/dev/null || echo "No Azure Functions host processes to stop"
        pkill -f 'azure-functions' 2>/dev/null || echo "No Azure Functions processes to stop"
        echo ""
        
        # Remove all rs- and prime-router- volumes (including file mounts)
        echo "Removing all rs and prime-router Docker volumes..."
        docker volume ls --filter "name=rs-" -q | xargs docker volume rm 2>/dev/null || echo "No rs volumes to remove"
        docker volume ls --filter "name=prime-router" -q | xargs docker volume rm 2>/dev/null || echo "No prime-router- volumes to remove"
        echo ""
        
        # Remove all rs- and prime-router- networks
        echo "Removing all rs and prime-router networks..."
        docker network ls --filter "name=rs-" -q | xargs docker network rm 2>/dev/null || echo "No rs networks to remove"
        docker network ls --filter "name=prime-router" -q | xargs docker network rm 2>/dev/null || echo "No prime-router- networks to remove"
        echo ""
        
        # Remove rs- and prime-router- images  
        echo "Removing all rs and prime-router Docker images..."
        docker images --filter "reference=rs-*" -q | xargs docker rmi -f 2>/dev/null || echo "No rs images to remove"
        docker images --filter "reference=prime-router-*" -q | xargs docker rmi -f 2>/dev/null || echo "No prime-router- images to remove"
        echo ""
        
        # Clean up any remaining rs- related files
        echo "Cleaning up rs related files..."
        rm -rf .vault/env/.env.local 2>/dev/null || true
        rm -rf logs/rs-* 2>/dev/null || true
        echo ""
        
        # Prune system to reclaim space
        echo "Pruning Docker system to reclaim space..."
        docker system prune -f --volumes 2>/dev/null || true
        echo ""
        
        echo "SPOTLESS cleanup completed successfully!"
        echo "All rs infrastructure, volumes, and file shares have been removed."
        echo "You can now start fresh with ./run-with-rs-infrastructure.sh"
        exit 0
        ;;
    --*)
        echo "ERROR: Unknown option: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
esac

echo " Starting Prime Router with rs Hardened Infrastructure"
echo "=============================================="

# Start rs infrastructure first  
echo " Starting rs prefixed hardened infrastructure..."

# Check if PostgreSQL image exists, build if needed
if ! docker image inspect rs-postgresql:latest >/dev/null 2>&1; then
    echo " rs-postgresql:latest not found locally, building..."
    pushd ../operations/utils/postgres > /dev/null
    if docker build --pull -t rs-postgresql:latest -f Dockerfile.postgres .; then
        echo " rs-postgresql:latest built successfully"
    else
        echo " ERROR: Failed to build rs-postgresql:latest"
        exit 1
    fi
    popd > /dev/null
else
    echo " rs-postgresql:latest found locally"
fi

# Start vault first for credentials
docker-compose -f docker-compose.matts-testing.yml up -d rs-vault

# Start PostgreSQL using Docker Compose (standard approach)
echo " Starting PostgreSQL via Docker Compose..."
docker-compose -f docker-compose.matts-testing.yml up -d rs-postgresql

# Start remaining services
docker-compose -f docker-compose.matts-testing.yml up -d rs-azurite rs-sftp rs-soap-webservice rs-rest-webservice

# Wait for PostgreSQL to be ready
echo " Waiting for PostgreSQL to be ready..."
for i in {1..30}; do
    if docker exec rs-postgresql pg_isready -U prime -d prime_data_hub &>/dev/null; then
        echo " PostgreSQL is ready"
        break
    elif [[ "${i}" -eq 30 ]]; then
        echo " PostgreSQL failed to start"
        exit 1
    fi
    sleep 2
done

# Wait for vault initialization (vault always runs init scripts on startup)
echo " Waiting for vault initialization..."
echo " Note: Vault runs init scripts on every startup to ensure proper state"

# Wait for vault to become responsive and credentials to be generated/loaded
echo " Waiting for vault credentials and API readiness..."
for attempt in {1..90}; do
    # Check if credentials exist first
    if [ -s ".vault/env/.env.local" ]; then
        # Then check if vault API is responding and operational
        vault_status=$(curl -s http://localhost:8200/v1/sys/health 2>/dev/null)
        if [ -n "$vault_status" ]; then
            if echo "$vault_status" | jq -e '.initialized == true and .sealed == false' >/dev/null 2>&1; then
                echo " SUCCESS: Vault fully operational and credentials ready"
                break
            elif echo "$vault_status" | jq -e '.initialized == false' >/dev/null 2>&1; then
                # Vault responding but still initializing - this is normal
                if [ $((attempt % 15)) -eq 0 ]; then
                    echo " Vault API responding, initialization in progress... (attempt $attempt/90)"
                fi
            fi
        fi
    fi
    
    if [ $attempt -eq 90 ]; then
        echo " WARNING: Vault initialization taking longer than expected (3 minutes)"
        echo " Current status: $(curl -s http://localhost:8200/v1/sys/health 2>/dev/null | jq '{initialized, sealed}' 2>/dev/null)"
        echo " Credentials available: $([ -f ".vault/env/.env.local" ] && echo "Yes" || echo "No")"
        echo " Note: Infrastructure may still be usable for basic operations"
        echo " Debug: Check vault logs with: docker logs -f rs-vault"
        break
    fi
    
    sleep 2
done

# Final verification of required credentials
echo " Verifying vault credentials..."
if ! grep -q "TokenSigningSecret" .vault/env/.env.local 2>/dev/null; then
    echo " ERROR: TokenSigningSecret missing - database operations will fail"
    exit 1
fi

if ! grep -q "VAULT_TOKEN" .vault/env/.env.local 2>/dev/null; then
    echo " ERROR: VAULT_TOKEN missing - authentication will fail"
    exit 1
fi

echo " Vault credentials verified and ready"

echo ""
echo " rs Infrastructure is ready!"
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
echo ""
echo " To stop rs infrastructure:"
echo "   docker-compose -f docker-compose.matts-testing.yml down"
echo ""

# Load environment variables for this session (if available)
if [[ -f ".vault/env/.env.local" ]] && [[ -s ".vault/env/.env.local" ]]; then
    set -a && source .vault/env/.env.local 2>/dev/null && set +a || true
    echo " Environment variables loaded for this session"
else
    echo " No vault environment file found - variables will be set by individual scripts"
fi
