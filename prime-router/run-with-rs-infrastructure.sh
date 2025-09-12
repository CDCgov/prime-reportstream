#!/bin/bash

# Wrapper script to run Prime Router with rs- infrastructure
# This allows us to use rs- prefixed services with standard Gradle tasks
#
# Usage: ./run-with-rs-infrastructure.sh [--status|--down|--spotless|--help]
#   --status    Show status of rs- infrastructure services
#   --down      Stop and remove all rs- infrastructure
#   --spotless  DESTRUCTIVE: Remove all rs- containers, volumes, and file mounts
#   --help      Show this help message

set -e

# Parse command line arguments
case "${1:-}" in
    --help|-h)
        echo "Usage: ./run-with-rs-infrastructure.sh [OPTIONS]"
        echo ""
        echo "Manages rs- prefixed hardened infrastructure for Prime Router development"
        echo ""
        echo "Options:"
        echo "  --status    Show status of all rs- infrastructure services"
        echo "  --down      Stop and remove all rs- infrastructure"
        echo "  --spotless  DESTRUCTIVE: Remove all rs- containers, volumes, and file mounts"
        echo "  --help      Show this help message"
        echo ""
        echo "Default behavior (no flags): Start complete rs- infrastructure"
        exit 0
        ;;
    --status)
        echo "RS- Infrastructure Status"
        echo "========================"
        echo ""
        echo "Docker Containers:"
        docker ps --filter "name=rs-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || echo "No rs- containers running"
        echo ""
        echo "Docker Compose Services:"
        docker-compose -f docker-compose.secure-multiarch.yml ps 2>/dev/null || echo "No compose services running"
        echo ""
        echo "Port Usage:"
        echo "PostgreSQL (5432): $(lsof -ti :5432 2>/dev/null && echo "OCCUPIED" || echo "AVAILABLE")"
        echo "Vault (8200): $(lsof -ti :8200 2>/dev/null && echo "OCCUPIED" || echo "AVAILABLE")"
        echo "Azurite (10000): $(lsof -ti :10000 2>/dev/null && echo "OCCUPIED" || echo "AVAILABLE")"
        echo "API (7071): $(lsof -ti :7071 2>/dev/null && echo "OCCUPIED" || echo "AVAILABLE")"
        exit 0
        ;;
    --down)
        echo "Stopping RS- Infrastructure"
        echo "==========================="
        echo ""
        echo "Stopping Docker Compose services..."
        docker-compose -f docker-compose.secure-multiarch.yml down --remove-orphans --volumes 2>/dev/null || true
        echo ""
        echo "Stopping manual rs- containers..."
        docker stop $(docker ps -q --filter "name=rs-") 2>/dev/null || echo "No rs- containers to stop"
        docker rm $(docker ps -aq --filter "name=rs-") 2>/dev/null || echo "No rs- containers to remove"
        echo ""
        echo "Cleaning up rs- networks..."
        docker network ls --filter "name=rs-" -q | xargs docker network rm 2>/dev/null || echo "No rs- networks to remove"
        echo ""
        echo "RS- infrastructure stopped and cleaned up"
        exit 0
        ;;
    --spotless)
        echo "WARNING: SPOTLESS CLEANUP - THIS WILL DELETE ALL FILE SHARES!"
        echo "========================================================="
        echo ""
        echo "This will PERMANENTLY DELETE:"
        echo "  • All rs- containers and images"
        echo "  • All rs- Docker volumes (including database data)"
        echo "  • All rs- networks"
        echo "  • All file mounts and shared data"
        echo ""
        read -p "Are you sure you want to continue? Type 'DELETE' to confirm: " confirmation
        if [ "$confirmation" != "DELETE" ]; then
            echo "Spotless cleanup cancelled"
            exit 0
        fi
        echo ""
        echo "Performing SPOTLESS cleanup..."
        echo "=============================="
        echo ""
        
        # Stop and remove Docker Compose services with volumes
        echo "Stopping Docker Compose services and removing volumes..."
        docker-compose -f docker-compose.secure-multiarch.yml down --remove-orphans --volumes --rmi all 2>/dev/null || true
        echo ""
        
        # Stop and remove all rs- containers
        echo "Stopping and removing all rs- containers..."
        docker stop $(docker ps -q --filter "name=rs-") 2>/dev/null || echo "No rs- containers to stop"
        docker rm $(docker ps -aq --filter "name=rs-") 2>/dev/null || echo "No rs- containers to remove"
        echo ""
        
        # Remove all rs- volumes (including file mounts)
        echo "Removing all rs- Docker volumes..."
        docker volume ls --filter "name=rs-" -q | xargs docker volume rm 2>/dev/null || echo "No rs- volumes to remove"
        echo ""
        
        # Remove all rs- networks
        echo "Removing all rs- networks..."
        docker network ls --filter "name=rs-" -q | xargs docker network rm 2>/dev/null || echo "No rs- networks to remove"
        echo ""
        
        # Remove rs- images  
        echo "Removing all rs- Docker images..."
        docker images --filter "reference=rs-*" -q | xargs docker rmi -f 2>/dev/null || echo "No rs- images to remove"
        echo ""
        
        # Clean up any remaining rs- related files
        echo "Cleaning up rs- related files..."
        rm -rf .vault/env/.env.local 2>/dev/null || true
        rm -rf logs/rs-* 2>/dev/null || true
        echo ""
        
        # Prune system to reclaim space
        echo "Pruning Docker system to reclaim space..."
        docker system prune -f --volumes 2>/dev/null || true
        echo ""
        
        echo "SPOTLESS cleanup completed successfully!"
        echo "All rs- infrastructure, volumes, and file shares have been removed."
        echo "You can now start fresh with ./run-with-rs-infrastructure.sh"
        exit 0
        ;;
    --*)
        echo "ERROR: Unknown option: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
esac

echo " Starting Prime Router with RS- Hardened Infrastructure"
echo "=============================================="

# Start rs- infrastructure first  
echo " Starting rs- prefixed hardened infrastructure..."

# Start vault first for credentials
docker-compose -f docker-compose.secure-multiarch.yml up -d rs-vault

# Start PostgreSQL using Docker Compose (standard approach)
echo " Starting PostgreSQL via Docker Compose..."
docker-compose -f docker-compose.secure-multiarch.yml up -d rs-postgresql

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
echo " Ready for Prime Router development with full local infrastructure!"