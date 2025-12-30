# Local Development Environment Validation Script

Reference for `validate-secure-multiarch.sh` - the Prime Router local development environment validation and testing framework.

---

## Quick Start

```bash
# Initial setup
./validate-secure-multiarch.sh

# Fast iteration
./validate-secure-multiarch.sh --fast

# Full cleanup
./validate-secure-multiarch.sh --clean
```

---

## Command Line Flags

### Infrastructure Management

**--clean**

- Stops all containers (PostgreSQL, Vault, Azurite, SFTP, webservices)
- Stops all Gradle and Azure Functions processes
- Removes Vault credentials and volumes
- Complete teardown for clean slate

**--clean-tests**

- Removes test output directories only
- Preserves build artifacts and running containers
- Quick cleanup between test runs

**--infra**

- Shows running container status
- Lists configured services
- Displays port usage
- Shows volume status
- Detects orphan containers
- Provides context-aware commands

---

### Execution Modes

**--fast**

- Skips infrastructure setup
- Reuses running containers
- Regenerates jOOQ classes
- Rebuilds JAR and Docker image
- Runs quick unit tests by default
- Fast iteration for code changes

**--use-container**

- Runs API in rs-prime-router-hardened Docker container
- Production-like environment
- Tests actual hardened image
- Slower startup but validates deployment artifact
- Use for pre-deployment validation

---

### Testing Flags

**--e2e-tests**

- Runs smoke tests
- Runs end-to-end tests
- Runs end-to-end UP tests
- Tests HL7 and FHIR report submissions
- Validates complete data pipeline

**--full-gradle-tests**

- Runs complete unit test suite (1300+ tests)
- Runs integration tests
- Generates code coverage reports
- Forces test execution even if up-to-date

---

### Code Quality

**--lint**

- Runs ktlintCheck on all source files
- Reports formatting violations
- Exits with error if violations found
- Provides fix commands

**--lint-fix**

- Runs ktlintFormat to auto-fix violations
- Verifies all checks pass after fixes
- Reports any remaining manual fixes needed
- Modifies source code

---

### Database Management

**--reset-db**

- Runs flywayClean (drops all database objects)
- Runs flywayMigrate (recreates schema)
- Ensures truly fresh database state
- Use when schema drift suspected

---

### Security Scanning

**--sec**

- Scans running container images with Trivy
- Scans docker-compose configured images
- Reports MEDIUM, HIGH, and CRITICAL vulnerabilities
- Skips if Trivy not installed

**--rs-infra**

- Used with --sec flag
- Scans all local rs- images
- Scans all docker-compose infrastructure images
- Includes stopped containers and available images
- Complete infrastructure security coverage

---

### Debugging

**--verbose**

- Shows filtered Gradle output in real-time
- Displays task execution and errors
- Streams to console during execution

**--debug**

- Full Gradle debug output
- Very verbose logging
- All output to console and log file
- Use for deep troubleshooting

---

### Platform

**--platform=PLATFORM**

- Force specific platform (linux/amd64 or linux/arm64)
- Auto-detects by default
- AMD64 required for Azure Functions

---

## Run Modes

### Default Mode (Gradle API)

```bash
./validate-secure-multiarch.sh
```

**What happens:**

- Builds custom images (PostgreSQL, SFTP, Prime Router)
- Starts full infrastructure stack
- Runs database migrations
- Generates jOOQ classes
- Builds JAR file
- Starts API with Gradle (host-based)
- Loads lookup tables
- Loads organization settings
- Loads SFTP credentials to Vault
- Tests API endpoints
- Infrastructure remains running

**API:** Gradle process on host (fast, debuggable)

---

### Containerized Mode

```bash
./validate-secure-multiarch.sh --use-container
```

**What happens:**

- Same infrastructure setup as default
- Starts API in Docker container (rs-prime-router-app-hardened)
- Tests actual hardened image
- Production-like validation
- Infrastructure remains running

**API:** Container (production parity)

---

### Fast Iteration Mode

```bash
./validate-secure-multiarch.sh --fast
```

**Prerequisites:** Infrastructure already running

**What happens:**

- Skips infrastructure setup
- Regenerates jOOQ if needed
- Rebuilds JAR with code changes
- Rebuilds hardened image
- Runs quick unit tests
- Infrastructure stays running

**Use for:** Rapid code-test cycles

---

### Security Scanning Mode

```bash
./validate-secure-multiarch.sh --sec
./validate-secure-multiarch.sh --sec --rs-infra
```

**What happens:**

- Scans container images with Trivy
- Reports security vulnerabilities
- No infrastructure changes
- --rs-infra: Comprehensive scan of all infrastructure images

**Use for:** Security validation, CVE tracking

---

## Common Workflows

### Development Session

```bash
# Start
./validate-secure-multiarch.sh

# Iterate
# Edit code
./validate-secure-multiarch.sh --fast
# Edit more
./validate-secure-multiarch.sh --fast

# Full tests before commit
./validate-secure-multiarch.sh --fast --full-gradle-tests

# End
./validate-secure-multiarch.sh --clean
```

---

### Pre-Commit Validation

```bash
# Code validation
./validate-secure-multiarch.sh --fast --full-gradle-tests

# Lint check
./validate-secure-multiarch.sh --lint-fix

# Container validation
./validate-secure-multiarch.sh --use-container
```

---

### Pre-Deployment Validation

```bash
# Complete validation with container
./validate-secure-multiarch.sh --use-container --e2e-tests --sec --rs-infra

# Verify infrastructure
./validate-secure-multiarch.sh --infra
```

---

### Troubleshooting

```bash
# Check status
./validate-secure-multiarch.sh --infra

# Debug Gradle issues
./validate-secure-multiarch.sh --debug

# Fresh database
./validate-secure-multiarch.sh --reset-db

# Complete rebuild
./validate-secure-multiarch.sh --clean
./validate-secure-multiarch.sh
```

---

## Flag Combinations

**Fast iteration with tests:**

```bash
./validate-secure-multiarch.sh --fast --full-gradle-tests
./validate-secure-multiarch.sh --fast --e2e-tests
```

**Container with tests:**

```bash
./validate-secure-multiarch.sh --use-container --e2e-tests
./validate-secure-multiarch.sh --use-container --full-gradle-tests
```

**Security with different scopes:**

```bash
./validate-secure-multiarch.sh --sec                    # Running containers
./validate-secure-multiarch.sh --sec --rs-infra         # All infrastructure
```

**Database management:**

```bash
./validate-secure-multiarch.sh --reset-db               # Full rebuild
./validate-secure-multiarch.sh --fast --reset-db        # Quick reset
```

---

## Infrastructure Components

### Services Started

- **rs-postgresql** - Custom PostgreSQL 16.6 (Wolfi-based)
- **rs-vault** - HashiCorp Vault (credential storage)
- **rs-azurite** - Azure Storage Emulator
- **rs-sftp** - Custom SFTP server (Wolfi-based)
- **rs-soap-webservice** - CastleMock (SOAP testing)
- **rs-rest-webservice** - Mockoon (REST testing)
- **rs-prime-router-app-hardened** - Containerized API (with --use-container)

### Ports Used

- 5432 - PostgreSQL
- 8200 - Vault
- 10000-10002 - Azurite (Blob, Queue, Table)
- 2222 - SFTP
- 8087 - SOAP webservice
- 3001 - REST webservice
- 7071 - Prime Router API
- 5005 - Java debug port
- 9090 - JMX port

---

## Generated Files

### Build Artifacts

- `build/libs/prime-router-0.2-SNAPSHOT-all.jar` - Application JAR
- `build/generated-src/jooq/` - jOOQ generated classes
- `build/azure-functions/` - Azure Functions structure

### Logs

- `../gradle-output.log` - Gradle execution output
- `validation-report.txt` - Validation summary

### Reports

- `build/reports/tests/test/index.html` - Unit test results
- `build/reports/jacoco/test/html/index.html` - Code coverage
- `build/reports/ktlint/` - Linting reports

### Configuration

- `.vault/env/.env.local` - Vault credentials (generated)
- `.vault/env/key` - Vault key file (generated)

---

## Requirements

- Docker Desktop running
- Azure Functions Core Tools 4+ (for Gradle API mode)
- Trivy (optional, for --sec flag)
- Sufficient disk space (10GB+ recommended)

---

## Notes

### API Execution Modes

**Gradle API (Default):**

- Fast startup (~30 seconds)
- Host-based Java process
- Direct log access
- Best for development iteration

**Containerized API (--use-container):**

- Slower startup (~60-90 seconds)
- Container-based execution
- Tests production artifact
- Best for validation and deployment prep

### Infrastructure Persistence

After validation completes, infrastructure containers remain running by default. This enables fast iterations with --fast flag. Use --clean to stop everything.

### Database State

Default behavior removes Docker volumes for fresh database. Use --reset-db with existing volumes to clean and migrate without container recreation.

### SFTP Credentials

Automatically loaded to Vault during setup:

- Credential name: DEFAULT-SFTP
- Username: foo
- Password: pass
- Upload directory: /home/foo/upload

Required for SFTP integration tests.

---

## Exit Codes

- 0: Success
- 1: Validation failed, errors detected
- Port conflicts, missing dependencies, build failures, or test failures

---

## Configuration Files

**Primary:**

- `docker-compose.secure-multiarch.yml` - Infrastructure definition
- `Dockerfile.hardened` - Hardened Prime Router image
- `operations/utils/postgres/Dockerfile.postgres` - PostgreSQL image
- `operations/utils/sftp/Dockerfile` - SFTP image

**Data:**

- `settings/organizations.yml` - Organization configuration
- `metadata/tables/local/` - Lookup tables

---

**Version:** 1.0

**Last Updated:** December 2025

**Script:** validate-secure-multiarch.sh
