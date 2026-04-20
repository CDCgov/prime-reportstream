# Matt's Local Dev Quick Start

## Quick Infrastructure Testing

Everything here is completely separate from current local dev (`docker-compose.yml`, `Dockerfile.dev`) as I want to replicate Azure to the extent I can, without stepping on anything that may be currently in use.
The `validate-*.sh` scripts set up all local testing infra via docker-compose at the test start and tear it down at the test end.  It does leave the docker images on your system, so they do not have to be pulled/rebuilt onn every run.
If you do not have a local postgrees image for these tests, start with:

```bash
./validate-secure-infrastructure-gradle-tests.sh --rebuild-postgres
```

**Important**: This is just a strating point to having leaner and more up to date test environment, thhat allows validation of the router by running tests.
Once this approach has been thoeroughly validated, it will need to be tested by deploying to a lower environment, but it needs rigorous testing first.

### Prerequisites

- Docker and Docker Compose
- Java 17
- local `prime-router-postgresql:latest` image

### Fast Start

```bash
# Navigate to prime-router directory
cd prime-router

# Quick validation with Gradle tests (fast)
./validate-secure-infrastructure-gradle-tests.sh --full

# OR: Full e2e validation (slower but comprehensive)  
./validate-secure-infrastructure.sh
```

## PostgreSQL Setup

### If PostgreSQL Image Missing

```bash
# Check if image exists
docker image inspect prime-router-postgresql:latest

# If not found, build it:
cd ../operations/utils/postgres
docker build -f Dockerfile.postgres -t prime-router-postgresql:latest .
cd ../../../prime-router
```

### Manual Infrastructure Control

```bash
# Start infrastructure only
docker-compose -f docker-compose.secure-working.yml up -d

# Check status
docker ps | grep prime-router

# Stop infrastructure
docker-compose -f docker-compose.secure-working.yml down
```

## Test Options

### Gradle-Based Testing (Fast)

```bash
# Basic CLI tests only
./validate-secure-infrastructure-gradle-tests.sh

# Full Gradle test suite (1330+ tests) 
./validate-secure-infrastructure-gradle-tests.sh --full

# Available flags (can be combined):
./validate-secure-infrastructure-gradle-tests.sh --help           # Show help message
./validate-secure-infrastructure-gradle-tests.sh --verbose        # Verbose output
./validate-secure-infrastructure-gradle-tests.sh --sec            # Run security scan  
./validate-secure-infrastructure-gradle-tests.sh --rebuild-postgres # Force rebuild PostgreSQL image
./validate-secure-infrastructure-gradle-tests.sh --full --verbose --sec
```

### E2E API Testing (Comprehensive)

```bash
# Full end-to-end API validation
./validate-secure-infrastructure.sh

# Available flags (can be combined):
./validate-secure-infrastructure.sh --help         # Show help message
./validate-secure-infrastructure.sh --verbose      # Verbose output  
./validate-secure-infrastructure.sh --sec          # Run security scan
./validate-secure-infrastructure.sh --verbose --sec
```

## Troubleshooting

### Common Issues

- **Fat JAR missing**: Run `./gradlew :prime-router:fatJar`
- **Permission denied**: Check Docker is running
- **Port conflicts**: Stop other local services on ports 5432, 8200, 10000-10002, 2222

### Failed Tests

Scripts now show **exact failed test names** and details automatically.

### Clean Restart

```bash
docker-compose -f docker-compose.secure-working.yml down -v
docker system prune -f
```
