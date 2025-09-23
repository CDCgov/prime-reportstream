#!/bin/bash
#
# This script provides a complete validation suite for the Prime Router
# secure multi-architecture setup. It automates infrastructure management,
# application builds, and test execution. 
#

set -eo pipefail

# --- Configuration Parameters ---
# High Priority - Core Application References
PRIME_JAR_NAME="prime-router-0.2-SNAPSHOT-all.jar"
PRIME_JAR_PATH="build/libs/${PRIME_JAR_NAME}"
API_IMAGE_NAME="rs-prime-router-api:latest"
POSTGRESQL_IMAGE_NAME="rs-postgresql:latest"
SFTP_IMAGE_NAME="prime-router-rs-sftp:latest"
API_CONTAINER_NAME="rs-prime-router-api"
POSTGRESQL_CONTAINER_NAME="rs-postgresql"
VAULT_CONTAINER_NAME="rs-vault"
SFTP_CONTAINER_NAME="rs-sftp"

# API deployment mode configuration
# Valid options: "gradle" | "container"
API_MODE="container"

# --- Global Variables ---
API_PID=""

# Global test result tracking (using simple variables for compatibility)
TOTAL_TESTS_RUN=0
TOTAL_TESTS_PASSED=0
TOTAL_TESTS_FAILED=0
TOTAL_SUITES_PASSED=0
TOTAL_SUITES_FAILED=0

# Test suite tracking (simple approach)
TEST_SUITE_RESULTS=""

# Debug mode configuration
DEBUG_MODE=false

# --- Utility Functions ---
readonly COLOR_GREEN='\033[0;32m'
readonly COLOR_YELLOW='\033[0;33m'
readonly COLOR_RED='\033[0;31m'
readonly COLOR_BLUE='\033[0;34m'
readonly COLOR_RESET='\033[0m'

print_header() {
    echo -e "\n${COLOR_BLUE}# $1${COLOR_RESET}"
}

print_success() {
    echo -e "${COLOR_GREEN}SUCCESS: $1${COLOR_RESET}"
}

print_warning() {
    echo -e "${COLOR_YELLOW}WARN: $1${COLOR_RESET}"
}

print_error() {
    echo -e "${COLOR_RED}ERROR: $1${COLOR_RESET}" >&2
}

print_info() {
    echo -e "$1"
}

#
# Wait for a network service to become available.
# Usage: wait_for_service <host> <port> <service_name> [timeout_seconds]
wait_for_service() {
    local host="$1"
    local port="$2"
    local service_name="$3"
    local timeout=${4:-120} # Default timeout: 120 seconds
    local start_time
    start_time=$(date +%s)

    print_info "Waiting for $service_name to be ready at $host:$port..."

    until nc -z "$host" "$port"; do
        local current_time
        current_time=$(date +%s)
        local elapsed=$((current_time - start_time))

        if [ "$elapsed" -ge "$timeout" ]; then
            print_error "$service_name was not ready within $timeout seconds."
            exit 1
        fi
        sleep 2
    done

    print_info "$service_name is ready."
}

# Generic function to wait for a condition to be true
wait_for_condition() {
    local description="$1"
    local condition_cmd="$2"
    local timeout=${3:-30} # Default timeout: 30 seconds
    local warning_msg="${4:-Condition not met}"
    local start_time
    start_time=$(date +%s)

    print_info "Waiting for $description..."

    while ! eval "$condition_cmd" >/dev/null 2>&1; do
        local current_time
        current_time=$(date +%s)
        local elapsed=$((current_time - start_time))

        if [ "$elapsed" -ge "$timeout" ]; then
            print_warning "$warning_msg"
            return 1
        fi
        sleep 2
    done

    print_success "$description completed."
    return 0
}


# --- Infrastructure and Application Management ---
# Stop all background processes and Docker containers.
cleanup() {
    print_header "Cleaning up environment"
    
    if [ -n "$API_PID" ]; then
        if [ "$API_MODE" = "container" ]; then
            print_info "Stopping Prime Router API container (ID: $API_PID)..."
            docker stop "$API_CONTAINER_NAME" 2>/dev/null || print_warning "API container not found or already stopped."
        else
            print_info "Stopping Prime Router API (PID: $API_PID)..."
            kill "$API_PID" 2>/dev/null || print_warning "API process not found or already stopped."
        fi
    fi

    # Kill all func processes that might be holding ports
    pkill -f "func host" 2>/dev/null || true
    pkill func 2>/dev/null || true
    pkill -f "Microsoft.Azure.WebJobs" 2>/dev/null || true

    # Clean up API containers (both standalone and compose-managed)
    docker stop "$API_CONTAINER_NAME" 2>/dev/null || true
    docker rm "$API_CONTAINER_NAME" 2>/dev/null || true

    # Ensure we're in the correct directory for docker-compose file
    pushd prime-router > /dev/null 2>&1 || true  # May already be in prime-router
    docker-compose -f docker-compose.matts-testing.yml down --volumes --remove-orphans
    popd > /dev/null 2>&1 || true
    
    print_info "Cleanup complete."
}


# Start the Docker infrastructure and wait for critical services to be healthy
start_docker_infrastructure() {
    print_header "Starting Docker infrastructure"
    
    # Check if PostgreSQL image exists, build if needed
    if ! docker image inspect "$POSTGRESQL_IMAGE_NAME" >/dev/null 2>&1; then
        print_info "$POSTGRESQL_IMAGE_NAME not found locally, building..."
        pushd ../operations/utils/postgres > /dev/null
        if docker build --pull -t "$POSTGRESQL_IMAGE_NAME" -f Dockerfile.postgres .; then
            print_info "$POSTGRESQL_IMAGE_NAME built successfully"
        else
            print_error "Failed to build $POSTGRESQL_IMAGE_NAME"
            popd > /dev/null
            exit 1
        fi
        popd > /dev/null
    else
        print_info "$POSTGRESQL_IMAGE_NAME found locally"
    fi
    
    # Start all infrastructure services (API will be started separately after build)
    docker-compose -f docker-compose.matts-testing.yml up -d rs-postgresql rs-vault rs-azurite rs-sftp rs-soap-webservice rs-rest-webservice

    # Wait for core services
    wait_for_service "localhost" "5432" "PostgreSQL"
    wait_for_service "localhost" "8200" "Vault"

    # Initialize vault with fresh state (clear old credentials)
    print_info "Initializing vault with fresh state..."
    rm -f .vault/env/.env.local .vault/env/key 2>/dev/null || true
    docker-compose -f docker-compose.matts-testing.yml restart rs-vault >/dev/null 2>&1
    sleep 5
    
    # Wait for fresh vault initialization
    wait_for_condition "Vault credentials generation" "[ -s \".vault/env/.env.local\" ]" 60 "Vault credential file not generated"
    
    print_info "Restarting vault for TokenSigningSecret loading..."
    docker-compose -f docker-compose.matts-testing.yml restart rs-vault >/dev/null 2>&1
    sleep 5
    
    # Verify vault is fully operational with extended timeout
    for attempt in {1..30}; do
        # Check if vault container is still running
        if ! docker-compose -f docker-compose.matts-testing.yml ps rs-vault | grep -q "Up"; then
            print_warning "Vault container not running, restarting... (attempt $attempt/30)"
            docker-compose -f docker-compose.matts-testing.yml restart rs-vault >/dev/null 2>&1
            sleep 5
            continue
        fi
        
        vault_status=$(curl -s http://localhost:8200/v1/sys/health 2>/dev/null)
        if echo "$vault_status" | grep -q '"initialized":true' && echo "$vault_status" | grep -q '"sealed":false'; then
            print_info "Vault fully operational and ready"
            break
        elif [ $attempt -eq 30 ]; then
            print_error "Vault failed to become operational after 60 seconds"
            print_info "Vault container status:"
            docker-compose -f docker-compose.matts-testing.yml ps rs-vault
            print_info "Vault logs:"
            docker logs rs-vault --tail 10
            exit 1
        else
            if [ $((attempt % 5)) -eq 0 ]; then
                print_info "Waiting for vault to become operational... (attempt $attempt/30)"
            fi
            sleep 2
        fi
    done
    
    # Verify TokenSigningSecret was added after restart (required by init.sh)
    if ! grep -q "TokenSigningSecret" .vault/env/.env.local 2>/dev/null; then
        print_error "TokenSigningSecret missing after vault restart - database operations will fail"
        exit 1
    fi

    # Export environment variables for Gradle tasks (using localhost for host context)
    export POSTGRES_URL="jdbc:postgresql://localhost:5432/prime_data_hub"
    export POSTGRES_USER="prime"
    export POSTGRES_PASSWORD="changeIT!"
    export VAULT_API_ADDR="http://localhost:8200"
    export AzureWebJobsStorage="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://localhost:10000/devstoreaccount1;QueueEndpoint=http://localhost:10001/devstoreaccount1;TableEndpoint=http://localhost:10002/devstoreaccount1"
    # Read VAULT_TOKEN from the generated file and export it
    local vault_credential_file=".vault/env/.env.local"
    local vault_token
    vault_token=$(grep -o 'VAULT_TOKEN="[^"]*"' "$vault_credential_file" | cut -d'"' -f2)
    export VAULT_TOKEN="$vault_token"

    wait_for_service "localhost" "10000" "Azurite"
    wait_for_service "localhost" "2222" "SFTP"

    print_info "All infrastructure services ready and verified."
    
    # Set up debug logging after infrastructure is ready
    setup_debug_logging
}

# Builds the Prime Router application.
build_prime_router_app() {
    print_header "Building Prime Router application"
    # Use standard gradle but skip compose since we manage infrastructure
    pushd .. > /dev/null
    ./gradlew :prime-router:fatJar -x composeUp
    popd > /dev/null
    print_success "Prime Router application built successfully."
}

# Load application settings and lookup tables into the database.
verify_infrastructure_ready() {
    print_header "Final infrastructure verification"
    
    # Check fi db is ready
    if ! PGPASSWORD="changeIT!" psql -U prime -h localhost -p 5432 -d prime_data_hub -c "SELECT 1;" >/dev/null 2>&1; then
        print_error "Database connection lost"
        exit 1
    fi
    
    # Verify vault is still responding
    if ! curl -s http://localhost:8200/v1/sys/health >/dev/null 2>&1; then
        print_error "Vault connection lost"
        exit 1
    fi
    
    print_info "Infrastructure verified and ready for test execution."
}

# --- Vault Management Functions ---
# Ensure vault is healthy for test operations
ensure_vault_healthy() {
    local test_suite_name=${1:-"test suite"}
    
    # Silent vault health check (only show if issues arise)
    
    # Check if vault service is running using docker-compose
    if ! docker-compose -f docker-compose.matts-testing.yml ps rs-vault | grep -q "Up"; then
        print_warning "Vault service not running - restarting for $test_suite_name"
        restart_vault_for_testing
        return $?
    fi
    
    # Check if vault is responsive and ready
    vault_status=$(curl -s http://localhost:8200/v1/sys/health 2>/dev/null) || vault_status=""
    if [ -z "$vault_status" ] || ! echo "$vault_status" | grep -q '"initialized":true'; then
        print_warning "Vault not properly initialized - restarting for $test_suite_name"
        restart_vault_for_testing
        return $?
    fi
    
    if echo "$vault_status" | grep -q '"sealed":true'; then
        print_warning "Vault is sealed - restarting for $test_suite_name"
        restart_vault_for_testing
        return $?
    fi
    
    # Check if vault has proper configuration
    if [ ! -f ".vault/env/.env.local" ] || ! grep -q "VAULT_TOKEN" .vault/env/.env.local 2>/dev/null; then
        print_warning "Vault configuration missing - restarting for $test_suite_name"
        restart_vault_for_testing
        return $?
    fi

    return 0
}

# Restart vault with clean state for testing
restart_vault_for_testing() {
    print_info "Restarting vault with clean state..."
    
    # Stop and remove existing vault
    docker stop rs-vault 2>/dev/null || true
    docker rm rs-vault 2>/dev/null || true
    docker volume rm rs-vault-data 2>/dev/null || true
    
    # Clean vault state
    rm -f .vault/env/key .vault/env/.env.local 2>/dev/null || true
    
    # Restart vault using docker-compose
    docker-compose -f docker-compose.matts-testing.yml up -d rs-vault >/dev/null 2>&1
    
    # Wait for vault to initialize
    print_info "Waiting for vault to initialize..."
    for i in {1..30}; do
        if curl -sf http://localhost:8200/v1/sys/health >/dev/null 2>&1 && [ -f ".vault/env/.env.local" ]; then
            vault_status=$(curl -s http://localhost:8200/v1/sys/health 2>/dev/null)
            if echo "$vault_status" | grep -q '"initialized":true' && echo "$vault_status" | grep -q '"sealed":false'; then
                print_info "Vault restarted and initialized"
                print_info "Allowing vault to stabilize before operations..."
                sleep 5
                return 0
            fi
        elif [ "$i" -eq 30 ]; then
            print_error "Vault restart failed - cannot continue"
            return 1
        fi
        sleep 2
    done
}

# --- Test Result Tracking Functions ---
# Parse Gradle test results from output logs
parse_gradle_test_results() {
    local test_name=$1
    local log_file=$2
    local exit_code=$3
    
    local passed=0
    local failed=0
    # local skipped=0  # Reserved for future use
    local total=0
    
    # Parse different Gradle output patterns
    if [ -f "$log_file" ]; then
        # Pattern: "123 tests completed, 120 passed, 3 failed"
        if grep -q "tests completed" "$log_file"; then
            total=$(grep "tests completed" "$log_file" | grep -o "[0-9]\+ tests completed" | grep -o "[0-9]\+" | head -1)
            passed=$(grep "tests completed" "$log_file" | grep -o "[0-9]\+ passed" | grep -o "[0-9]\+" | head -1)
            failed=$(grep "tests completed" "$log_file" | grep -o "[0-9]\+ failed" | grep -o "[0-9]\+" | head -1)
        # Pattern for CLI smoke tests: "[color]TestName Test PASSED/FAILED[0m"
        elif grep -q "Test PASSED\|Test FAILED\|test passed\|test FAILED" "$log_file"; then
            passed=$(grep -c "Test passed\|test passed\|PII removal test passed" "$log_file")
            failed=$(grep -c "Test FAILED\|test FAILED\|\*\*\*.*Test FAILED\*\*\*\|\*\*\*.*FAILED\*\*\*" "$log_file")
            total=$((passed + failed))
        # Pattern: "BUILD SUCCESSFUL" or "BUILD FAILED"  
        elif grep -q "BUILD SUCCESSFUL" "$log_file"; then
            # For tasks like clearDB, reloadTables - no specific test count
            total=1
            passed=1
            failed=0
        elif grep -q "BUILD FAILED" "$log_file"; then
            total=1
            passed=0
            failed=1
        fi
    fi
    
    # Store results in simple format: "testName:STATUS:passed/total:duration"
    local status="PASS"
    
    # Determine suite status based on test results, not exit code (continue-on-failure)
    if [[ "$test_name" =~ ^(test|testSmoke|testIntegration|testEnd2End|testEnd2EndUP)$ ]]; then
        # For test suites: FAIL if any individual tests failed
        if [ "$failed" -gt 0 ]; then
            status="FAIL"
            TOTAL_SUITES_FAILED=$((TOTAL_SUITES_FAILED + 1))
        else
            TOTAL_SUITES_PASSED=$((TOTAL_SUITES_PASSED + 1))
        fi
    else
        # For non-test tasks: use exit code
        if [ "$exit_code" -ne 0 ]; then
            status="FAIL"
            TOTAL_SUITES_FAILED=$((TOTAL_SUITES_FAILED + 1))
        else
            TOTAL_SUITES_PASSED=$((TOTAL_SUITES_PASSED + 1))
        fi
    fi
    
    # Append to results string
    TEST_SUITE_RESULTS="$TEST_SUITE_RESULTS$test_name:$status:$passed/$total;"
    
    # Update totals
    TOTAL_TESTS_RUN=$((TOTAL_TESTS_RUN + total))
    TOTAL_TESTS_PASSED=$((TOTAL_TESTS_PASSED + passed))
    TOTAL_TESTS_FAILED=$((TOTAL_TESTS_FAILED + failed))
}

# Report individual test suite results
report_test_suite_result() {
    local test_name=$1
    local duration=$2
    
    # Extract status and counts from results string
    local result_entry
    result_entry=$(echo "$TEST_SUITE_RESULTS" | grep -o "$test_name:[^;]*")
    if [ -n "$result_entry" ]; then
        local status
        status=$(echo "$result_entry" | cut -d: -f2)
        local counts
        counts=$(echo "$result_entry" | cut -d: -f3)
        
        if [ "$status" = "PASS" ]; then
            print_success "$test_name: $counts tests passed (${duration}s)"
        else
            print_error "$test_name: FAILED ($counts tests, ${duration}s)"
        fi
    fi
}

# --- API and Data Management ---
# Start API based on deployment mode (gradle or container)
start_api() {
    print_header "Starting Prime Router API"
    
    case $API_MODE in
        "gradle")
            start_gradle_api
            ;;
        "container")
            start_containerized_api  
            ;;
        *)
            print_error "Invalid API mode: $API_MODE"
            exit 1
            ;;
    esac
}

# Start API via Gradle (current approach)
start_gradle_api() {
    print_info "Starting API via Azure Functions host..."
    
    # Record API startup timing
    API_START_TIME=$(date +%s)
    print_info "API startup initiated at: $(date)"
    
    # Step 1: Use quickPackage to avoid test dependencies
    pushd .. > /dev/null
    # Let gradle set test-AzureWebJobsStorage defaults for tests
    unset AzureWebJobsStorage  
    print_info "Packaging Azure Functions"
    if ! ./gradlew -Dorg.gradle.jvmargs="-Xmx6g" :prime-router:package -x fatjar 2>&1 | tee prime-router/package-debug.log; then
        print_error "Azure Functions package failed"
        print_info "Failure details for debugging:"
        
        # Show the actual gradle failure output
        if [ -f "prime-router/package-debug.log" ]; then
            print_info "Last 20 lines of gradle output:"
            tail -20 prime-router/package-debug.log | sed 's/^/  /'
            
            # Look for specific error patterns
            if grep -q "What went wrong" prime-router/package-debug.log; then
                print_info "Gradle error details:"
                grep -A 10 "What went wrong" prime-router/package-debug.log | sed 's/^/  /'
            fi
            
            # Show any exception stack traces
            if grep -q "Caused by" prime-router/package-debug.log; then
                print_info "Root cause:"
                grep -A 5 "Caused by" prime-router/package-debug.log | head -10 | sed 's/^/  /'
            fi
        fi
        
        print_info "Debug commands to run manually:"
        print_info "  cd .. && ./gradlew :prime-router:package -x fatjar --stacktrace"
        print_info "  cd .. && ./gradlew :prime-router:package -x fatjar --info"
        print_info "  Check log: cat prime-router/package-debug.log"
        
        popd > /dev/null
        exit 1
    fi
    popd > /dev/null
    
    # Step 2: Start func host directly (avoid interactive prompts in start_func.sh)
    print_info "Starting API using func host directly..."
    cd build/azure-functions/prime-data-hub-router
    
    # Set required environment variables for func host
    export POSTGRES_URL="jdbc:postgresql://127.0.0.1:5432/prime_data_hub"
    export POSTGRES_USER="prime"
    export POSTGRES_PASSWORD="changeIT!"
    export VAULT_API_ADDR="http://127.0.0.1:8200"
    export AzureWebJobsStorage="DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;QueueEndpoint=http://127.0.0.1:10001/devstoreaccount1;TableEndpoint=http://127.0.0.1:10002/devstoreaccount1"
    
    # Enhanced Java memory settings for Azure Functions stability
    export JAVA_OPTS="-Xmx6g -Xms6g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    
    # Start func host
    func host start \
        --cors http://localhost:10000,http://127.0.0.1:10000,http://localhost:8090,http://localhost:3000 \
        --port 7071 \
        --host 0.0.0.0 \
        --verbose \
        > ../../../gradle-output.log 2>&1 &
    API_PID=$!
    cd ../../..
    
    print_info "API started via func host directly (PID: $API_PID)"
}

# Start API via hardened container (future approach)
start_containerized_api() {
    print_info "Starting API via hardened container (multi-architecture)..."
    
    # Build the API container using Dockerfile.hardened
    print_info "Building hardened API container..."
    if ! docker build --platform linux/amd64 -t "$API_IMAGE_NAME" -f Dockerfile.hardened .; then
        print_error "Hardened API container build failed"
        exit 1
    fi
    
    # Security scan of the built API container
    print_info "Running security scan of hardened API container..."
    if trivy image "$API_IMAGE_NAME" 2>/dev/null; then
        print_info "Security scan completed"
    else
        print_warning "Security scan unavailable (trivy not installed or accessible)"
    fi
    
    # Clean up any existing API container before starting
    docker stop "$API_CONTAINER_NAME" 2>/dev/null || true
    docker rm "$API_CONTAINER_NAME" 2>/dev/null || true
    
    # Start the containerized API using docker-compose
    print_info "Starting containerized API via docker-compose..."
    pushd prime-router > /dev/null 2>&1 || true
    docker-compose -f docker-compose.matts-testing.yml up -d rs-prime-router-api
    popd > /dev/null 2>&1 || true
    
    # Get container ID for monitoring
    API_PID=$(docker ps -q --filter "name=$API_CONTAINER_NAME")
    print_info "Containerized API started via docker-compose (Container ID: $API_PID)"
}

# Wait for API to be ready (deployment-agnostic)
wait_for_api_ready() {
    print_info "Waiting for API to be ready..."
    
    # For containerized API, test API from host on port 7071
    if [ "$API_MODE" = "container" ]; then
        print_info "Waiting for API to respond on host port 7071 (up to 5 minutes)..."
        for attempt in {1..60}; do
            # Test API with HEAD request for HTTP 200 status
            if curl -I -s -f http://localhost:7071/api/lookuptables/list >/dev/null 2>&1; then
                print_info "API responding with HTTP 200 OK from host port 7071"
                return 0
            fi
            
            # Check if container is still running
            if ! docker-compose -f docker-compose.matts-testing.yml ps "$API_CONTAINER_NAME" | grep -q "Up"; then
                print_error "API container stopped unexpectedly"
                docker logs "$API_CONTAINER_NAME" --tail 10
                exit 1
            fi
            
            if [ $((attempt % 6)) -eq 0 ]; then
                # Show what HTTP status we're getting for debugging
                http_status=$(curl -I -s -w "%{http_code}" http://localhost:7071/api/lookuptables/list 2>/dev/null | tail -1)
                print_info "Still waiting for HTTP 200... (attempt $attempt/60, status: $http_status)"
            fi
            sleep 5
        done
        
        print_error "API not responding after 5 minutes"
        print_info "Final API response: '$api_response'"
        print_info "Collecting complete API container logs for analysis..."
        docker logs "$API_CONTAINER_NAME" > debug-logs/api-complete.log 2>&1
        print_info "Complete API logs saved to debug-logs/api-complete.log"
        exit 1
    fi
    
    # For gradle API, use port and HTTP checks
    wait_for_service "localhost" "7071" "Prime Router API port"
    
    # Extended HTTP server initialization check with timing
    print_info "Waiting for HTTP server initialization (up to 3 minutes)..."
    local http_check_start=$(date +%s)
    
    for attempt in {1..36}; do
        # Check if API responds (accept 401/403 as working response - means endpoint exists)
        api_response=$(curl -s -w "%{http_code}" --max-time 10 http://localhost:7071/api/lookuptables/list 2>/dev/null)
        http_code="${api_response: -3}"
        if [[ "$http_code" == "200" || "$http_code" == "400" || "$http_code" == "401" || "$http_code" == "403" ]]; then
            local api_ready_time=$(date +%s)
            local total_startup_time=$((api_ready_time - API_START_TIME))
            local http_ready_time=$((api_ready_time - http_check_start))
            
            print_info "API HTTP server ready for operations"
            print_info "Timing: Total API startup: ${total_startup_time}s, HTTP ready: ${http_ready_time}s"
            print_info "API became ready at: $(date)"
            return 0
        fi
        
        # Show progress every 30 seconds with timing diagnostics
        if [ $((attempt % 6)) -eq 0 ]; then
            local elapsed_total=$(($(date +%s) - API_START_TIME))
            local elapsed_http=$(($(date +%s) - http_check_start))
            print_info "Still waiting for HTTP server... (attempt $attempt/36)"
            print_info "Timing: Total elapsed: ${elapsed_total}s, HTTP check: ${elapsed_http}s"
            
            # Show recent gradle output for debugging
            if [ -f "gradle-output.log" ]; then
                print_info "Recent gradle output:"
                tail -3 gradle-output.log 2>/dev/null | sed 's/^/  /' || echo "  No output available"
            fi
        fi
        
        sleep 5
    done
    
    print_error "API HTTP server not ready after 3 minutes"
    print_info "Gradle diagnostics:"
    if [ -f "gradle-output.log" ]; then
        tail -10 gradle-output.log | sed 's/^/  /'
    fi
    if [ -f "gradle-errors.log" ]; then
        tail -5 gradle-errors.log | sed 's/^/  /'
    fi
    
    print_error "API HTTP endpoints not accessible - cannot run meaningful tests"
    print_error "Tests require working API - fix API startup before proceeding"
    exit 1  # Exit with error - API is mandatory for test execution
}

#
# Load application data using direct JAR execution
#
load_application_data() {
    print_header "Loading Application Data"
    
    # Verify API is responsive before loading data
    if ! curl -s -f http://localhost:7071/api/check >/dev/null 2>&1; then
        print_error "API not ready - cannot load data"
        exit 1
    fi
    
    # Load lookup tables
    print_info "Loading lookup tables..."
    if ! java -jar "$PRIME_JAR_PATH" lookuptables loadall -d metadata/tables/local >/dev/null 2>&1; then
        print_error "Lookup table loading failed"
        exit 1
    fi
    print_info "Lookup tables loaded"
    
    # Load organization settings  
    print_info "Loading organization settings..."
    if ! java -jar "$PRIME_JAR_PATH" multiple-settings set -s -i settings/organizations.yml >/dev/null 2>&1; then
        print_error "Organization settings loading failed"
        exit 1
    fi
    print_info "Organization settings loaded"
    
    # Verify data loaded via API
    response=$(curl -s http://localhost:7071/api/lookuptables/list 2>/dev/null)
    table_count=$(echo "$response" | grep -o "tableName" | wc -l | tr -d ' ')
    if [ "$table_count" -gt 0 ]; then
        print_info "Data loading verified: $table_count lookup tables available"
    else
        print_error "Data loading verification failed"
        exit 1
    fi
}

# Run only test suites (skip database prep since data is loaded)
run_full_test_suites() {
    print_header "Running test suites (smoke - integration - unit - e2e - e2e-up)"
    
    local overall_start_time
    overall_start_time=$(date +%s)
    
    # Test suites 
    run_test_suite_with_tracking "testSmoke" "testSmoke -Pforcetest" "smoke tests"
    run_test_suite_with_tracking "testIntegration" "testIntegration -Pforcetest" "integration tests"
    run_test_suite_with_tracking "test" "test -Pforcetest" "unit tests"
    run_test_suite_with_tracking "testEnd2End" "testEnd2End -Pforcetest" "end-to-end tests"
    run_test_suite_with_tracking "testEnd2EndUP" "testEnd2EndUP -Pforcetest" "end-to-end UP tests"
    
    local overall_duration=$(($(date +%s) - overall_start_time))
    
    # Generate complete summary
    generate_complete_summary $overall_duration
}

# --- Test Execution ---
# Execute test command directly using Prime CLI (avoids gradle configuration conflicts)
run_gradle_test() {
    local task=$1
    
    # Map gradle tasks to CLI commands
    case $task in
        "testSmoke -Pforcetest"|"testSmoke")
            # Run smoke tests that don't require authentication first
            java -jar "$PRIME_JAR_PATH" test --run removepiicheck,qualityfilter,hl7null,toomanycols,badcsv,livdApi --sequential || true
            # Then run tests that may have auth issues but continue anyway
            java -jar "$PRIME_JAR_PATH" test --run ping,merge,waters,jti,sender-settings,history --sequential || true
            ;;
        "testIntegration -Pforcetest"|"testIntegration")  
            # Integration tests require gradle execution due to Testcontainers
            pushd .. > /dev/null
            ./gradlew :prime-router:testIntegration -Pforcetest -x composeUp
            local exit_code=$?
            popd > /dev/null
            return $exit_code
            ;;
        "test -Pforcetest"|"test")
            # Unit tests require gradle execution 
            pushd .. > /dev/null
            ./gradlew :prime-router:test -Pforcetest -x composeUp
            local exit_code=$?
            popd > /dev/null
            return $exit_code
            ;;
        "testEnd2End -Pforcetest"|"testEnd2End")
            java -jar "$PRIME_JAR_PATH" test --run end2end --sequential || true
            ;;
        "testEnd2EndUP -Pforcetest"|"testEnd2EndUP")
            java -jar "$PRIME_JAR_PATH" test --run end2end_up --sequential || true
            ;;
        *)
            # Fall back to gradle for non-test tasks
            pushd .. > /dev/null
            ./gradlew :prime-router:$task -x composeUp
            local exit_code=$?
            popd > /dev/null
            return $exit_code
            ;;
    esac
}

# Execute all Gradle-based test suites.
run_test_suite_with_tracking() {
    local test_name=$1
    local gradle_task=$2
    local description=$3
    
    print_info "Running $description... (executing: :prime-router:$gradle_task)"
    local start_time
    start_time=$(date +%s)
    
    # Execute test and capture output
    # Execute test and capture both output and actual exit status
    run_gradle_test "$gradle_task" > "test-output-$test_name.log" 2> "test-errors-$test_name.log"
    local exit_code=$?
    
    # Parse results first to determine actual success/failure
    parse_gradle_test_results "$test_name" "test-output-$test_name.log" $exit_code
    
    # Report based on actual test results, not just exit code
    local actual_status=$(echo "$TEST_SUITE_RESULTS" | grep -o "$test_name:[^;]*" | cut -d: -f2 | tail -1)
    if [ "$actual_status" = "PASS" ]; then
        if [[ "$test_name" =~ ^(test|testSmoke|testIntegration|testEnd2End|testEnd2EndUP)$ ]]; then
            print_success "$description completed successfully."
        else
            print_info "$description completed."
        fi
    else
        if [[ "$test_name" =~ ^(test|testSmoke|testIntegration|testEnd2End|testEnd2EndUP)$ ]]; then
            print_warning "$description failed - continuing with remaining tests"
        else
            print_error "$description failed."
        fi
    fi
    
    local end_time
    end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    # Update results string with duration (results already parsed above)
    TEST_SUITE_RESULTS="${TEST_SUITE_RESULTS%?}:${duration};"
    
    # Report individual result based on actual test results, not exit code
    report_test_suite_result "$test_name" $duration
    
    # Return actual failure status for test suites based on test results
    if [[ "$test_name" =~ ^(test|testSmoke|testIntegration|testEnd2End|testEnd2EndUP)$ ]]; then
        # Extract actual status from results
        local actual_status=$(echo "$TEST_SUITE_RESULTS" | grep -o "$test_name:[^;]*" | cut -d: -f2)
        if [ "$actual_status" = "FAIL" ]; then
            return 1
        else
            return 0
        fi
    else
        return $exit_code
    fi
}

run_all_test_suites() {
    print_header "Running test suites (db-prep - smoke - integration - unit - e2e - e2e-up)"
    
    local overall_start_time
    overall_start_time=$(date +%s)
    
    print_info "Database preparation phase..."
    run_test_suite_with_tracking "clearDB" "clearDB" "database cleanup"
    
    print_info "Allowing database to stabilize after cleanup..."
    sleep 3
    
    run_test_suite_with_tracking "reloadTables" "reloadTables" "lookup tables loading"
    
    # Validate table operations completed before proceeding
    print_info "Verifying lookup tables were loaded successfully..."
    for attempt in {1..10}; do
        table_count=$(PGPASSWORD="changeIT!" psql -U prime -h localhost -p 5432 -d prime_data_hub -t -c "SELECT COUNT(*) FROM lookup_table_row;" 2>/dev/null | tr -d ' ')
        if [ "$table_count" -gt 0 ]; then
            print_info "Lookup tables verified: $table_count rows loaded"
            break
        elif [ $attempt -eq 10 ]; then
            print_warning "Lookup tables not detected after 20 seconds - proceeding anyway"
        else
            print_info "Waiting for lookup tables to be committed... (attempt $attempt/10)"
            sleep 2
        fi
    done
    
    # Only settings loading requires vault (for credential storage)
    ensure_vault_healthy "organization settings loading"
    if ! run_test_suite_with_tracking "reloadSettings" "reloadSettings" "organization settings loading"; then
        print_warning "Organization settings loading failed - retrying after vault stabilization..."
        sleep 5
        ensure_vault_healthy "organization settings loading retry"
        run_test_suite_with_tracking "reloadSettings-retry" "reloadSettings" "organization settings loading (retry)"
    fi
    
    # Test suites 
    run_test_suite_with_tracking "testSmoke" "-Pforcetest testSmoke" "smoke tests"
    run_test_suite_with_tracking "testIntegration" "-Pforcetest testIntegration" "integration tests"
    run_test_suite_with_tracking "test" "-Pforcetest test" "unit tests"
    run_test_suite_with_tracking "testEnd2End" "-Pforcetest testEnd2End" "end-to-end tests"
    run_test_suite_with_tracking "testEnd2EndUP" "-Pforcetest testEnd2EndUP" "end-to-end UP tests"
    
    local overall_duration=$(($(date +%s) - overall_start_time))
    
    # Generate complete summary
    generate_complete_summary $overall_duration
}

# Create test results summary
generate_complete_summary() {
    local total_duration=$1
    
    echo
    print_header "FULL TEST RESULTS SUMMARY"
    
    # Overall statistics  
    local total_suites=$((TOTAL_SUITES_PASSED + TOTAL_SUITES_FAILED))
    local success_rate=0
    if [ $TOTAL_TESTS_RUN -gt 0 ]; then
        success_rate=$((TOTAL_TESTS_PASSED * 100 / TOTAL_TESTS_RUN))
    fi
    
    print_info "Total Test Suites: $total_suites"
    print_info "Passed Suites: $TOTAL_SUITES_PASSED"
    print_info "Failed Suites: $TOTAL_SUITES_FAILED"
    print_info "Overall Test Success Rate: $success_rate% ($TOTAL_TESTS_PASSED/$TOTAL_TESTS_RUN)"
    print_info "Total Duration: $(printf '%02d:%02d' $((total_duration/60)) $((total_duration%60)))"
    
    echo
    print_info "Individual Suite Results:"
    
    # Parse and display each test suite result
    echo "$TEST_SUITE_RESULTS" | tr ';' '\n' | while IFS=: read -r suite status counts duration; do
        if [ -n "$suite" ]; then
            if [ "$status" = "PASS" ]; then
                printf "  PASS %-15s: %s (%s tests, %ss)\n" "$suite" "$status" "$counts" "$duration"
            elif [ "$status" = "FAIL" ]; then
                printf "  FAIL %-15s: %s (%s tests, %ss)\n" "$suite" "$status" "$counts" "$duration"
            fi
        fi
    done
    
    echo
    # Final validation result with Karen notification :D
    if [ $TOTAL_SUITES_FAILED -eq 0 ] && [ $TOTAL_TESTS_FAILED -eq 0 ]; then
        print_success "FULL VALIDATION: SUCCESSFUL"
        print_success "All test suites passed with zero failures"
        say -v Karen "testing complete, all good" 2>/dev/null || true
    else
        print_error "FULL VALIDATION: PARTIAL SUCCESS"
        print_error "$TOTAL_SUITES_FAILED suite(s) failed, $TOTAL_TESTS_FAILED individual test(s) failed"
        if [ $TOTAL_SUITES_FAILED -gt 0 ]; then
            say -v Karen "testing complete, $TOTAL_SUITES_FAILED suites failed" 2>/dev/null || true
        fi
    fi
}

# --- Main Orchestration ---
# Parse command line arguments
for arg in "$@"; do
    case $arg in
        --debug)
            DEBUG_MODE=true
            ;;
        --help|-h)
            echo "Usage: ./validate-matts-testing.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --debug    Enable debug mode with container logging"
            echo "  --help, -h Show this help message"
            exit 0
            ;;
    esac
done

# Debug logging function
setup_debug_logging() {
    if [ "$DEBUG_MODE" = true ]; then
        print_info "Debug mode enabled - setting up complete container logging..."
        mkdir -p debug-logs
        
        # Collect complete container logs for all services (no truncation)
        docker logs "$POSTGRESQL_CONTAINER_NAME" > debug-logs/postgresql.log 2>&1 &
        docker logs "$VAULT_CONTAINER_NAME" > debug-logs/vault.log 2>&1 &
        docker logs rs-azurite > debug-logs/azurite.log 2>&1 &
        docker logs "$SFTP_CONTAINER_NAME" > debug-logs/sftp.log 2>&1 &
        
        if [ "$API_MODE" = "container" ]; then
            docker logs "$API_CONTAINER_NAME" > debug-logs/api.log 2>&1 &
        fi
        
        print_info "Complete container logs will be written to debug-logs/ directory"
    fi
}

main() {
    # Ensure cleanup is called on exit
    trap cleanup EXIT

    print_header "Starting Complete Validation"
    
    # Set up debug logging if enabled
    if [ "$DEBUG_MODE" = true ]; then
        print_info "Debug mode: Enhanced logging and diagnostics enabled"
    fi

    # Start with a clean slate
    cleanup
    start_docker_infrastructure
    build_prime_router_app
    
    # Run database migration before API startup for container mode
    if [ "$API_MODE" = "container" ]; then
        print_header "Running Database Migration"
        pushd .. > /dev/null
        ./gradlew :prime-router:flywayMigrate
        popd > /dev/null
        print_info "Database migration completed"
    fi
    
    verify_infrastructure_ready
    start_api
    wait_for_api_ready
    print_header "Loading Application Data"
    print_info "Loading application data using start_func.sh load_config function..."
    
    # prevent interactive prompts
    export GITHUB_ACTIONS=true
        
    # Load data directly
    print_info "Loading lookup tables..."
    if java -jar "$PRIME_JAR_PATH" lookuptables loadall -d metadata/tables/local -r 60; then
        print_info "Loading organization settings..."
        if java -jar "$PRIME_JAR_PATH" multiple-settings set -s -i settings/organizations.yml -r 60; then
            print_info "Data loading completed successfully"
        else
            print_error "Organization settings loading failed"
            exit 1
        fi
    else
        print_error "Lookup tables loading failed"
        exit 1
    fi
    
    print_info "Application data loaded via start_func.sh"
    
    # Load SFTP credentials for smoke tests (required for authentication)
    print_info "Loading SFTP credentials for smoke tests..."
    
    # Fix SFTP permissions (matching CI approach)
    print_info "Setting SFTP permissions..."
    if docker exec "$SFTP_CONTAINER_NAME" chmod 777 /home/foo/upload 2>/dev/null; then
        print_info "SFTP permissions set successfully"
    else
        print_warning "SFTP permission fix failed - may affect some tests"
    fi
    
    # Load credentials using CLI directly (avoiding gradle config conflicts)
    print_info "Creating SFTP credentials in Vault..."
    if java -jar "$PRIME_JAR_PATH" create-credential --type=UserPass --persist=DEFAULT-SFTP --user foo --pass pass; then
        print_info "SFTP credentials loaded successfully"
    else
        print_warning "SFTP credentials loading failed - smoke tests may have authentication issues"
    fi
    
    # Run comprehensive test suites (continue even if tests fail)
    run_full_test_suites || true

    # Final status based on actual results
    if [ $TOTAL_SUITES_FAILED -eq 0 ] && [ $TOTAL_TESTS_FAILED -eq 0 ]; then
        print_header "Complete Validation Successful"
        print_success "All steps completed successfully."
    else
        print_header "Complete Validation Finished"
        print_warning "Validation completed with $TOTAL_SUITES_FAILED failed suite(s) and $TOTAL_TESTS_FAILED failed test(s)."
    fi
}

# Run the main function
main "$@"
