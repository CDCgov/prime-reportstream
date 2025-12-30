# Prime Router Dockerized Local Development Environment

Fully containerized Router, with complete supporting infrastructure for local development and testing.  All modes should auto-detect and handle platform appropriately architecture.  Azure Functions are not available in ARM64 flavor, but AMD64 will work fine on Apple Silicon.

---

## Prerequisites

- Docker Desktop running
- Azure Functions Core Tools 4.2.2+ installed
- Suffficient disk space (5GB+ for image builds)
- Ensure your shell is in the `prime-router/` directory before execution
- [Chainguard free account with credentials for base images](https://console.chainguard.dev/) (SSO with Github)

---

### Supported Architectures

Supports and auto-selects appropriate amd64 and arm64 architectures, the following flags can be used to force architecture.

```sh
 --platform=linux/amd64  # Full image + Azure Functions scanning
 --platform=linux/arm64  # Base components only (Apple Silicon)
```

## Iterative Development

### Initial Setup with LDE Validation

```sh
  ./validate-secure-multiarch.sh
```

What happens:

- Builds all images (PostgreSQL, Prime Router, SFTP)
- Runs migrations and generates jOOQ classes
- Starts infrastructure (PostgreSQL, Vault, Azurite, SFTP)
- Loads lookup tables and organization settings
- Loads SFTP credentials to Vault (DEFAULT-SFTP: foo/pass)
- Builds the router and runs tests against it
- Keeps the Docker infrastructure running at the end

### Iteration (code/build/test cycles)

```sh
  ./validate-secure-multiarch.sh --fast
```

What happens:

- Skips: Infrastructure setup (reuses running containers)
- Skips: Database migrations (schema unchanged)
- Regenerates: jOOQ classes (fast, usually cached)
- Rebuilds: Fat JAR with your code changes
- Rebuilds: Hardened Docker image
- Runs: Quick unit tests by default
- Keeps: Infrastructure running for next iteration

### API Execution Modes

The script supports two ways to run the Prime Router API:

#### Gradle API (Default - Fast Development)

```sh
./validate-secure-multiarch.sh
./validate-secure-multiarch.sh --fast
```

Uses:
- Java process on host (not containerized)
- Fast startup (~30 seconds)
- Direct log output to gradle-output.log
- Easy debugging with IDE

Best for:
- Code iteration during development
- Quick testing of code changes
- Debugging issues
- Fast feedback loops

#### Containerized API (Production-Like Validation)

```sh
./validate-secure-multiarch.sh --use-container
./validate-secure-multiarch.sh --fast --use-container
```

Uses:
- rs-prime-router-hardened Docker container
- Slower startup (~60-90 seconds)
- Tests actual hardened image
- Production-like environment

Best for:
- Validating hardened Docker image
- Pre-deployment testing
- Container configuration validation
- Production parity testing
- Security validation of final artifact

#### Choosing the Right Mode

During development:
- Use Gradle API (default) for fast iterations
- Switch to container before commits for image validation

Before committing:
```sh
# Test code with Gradle
./validate-secure-multiarch.sh --fast --full-gradle-tests

# Validate container image
./validate-secure-multiarch.sh --use-container
```

Before deploying:
```sh
# Full container validation
./validate-secure-multiarch.sh --use-container --e2e-tests --sec
```

### Testing options

Full Gradle test suite:

```sh
  ./validate-secure-multiarch.sh --fast --full-gradle-tests
```

E2E tests:

```sh
  ./validate-secure-multiarch.sh --fast --e2e-tests
```

Just linting:

```sh
  ./validate-secure-multiarch.sh --fast --lint
```

With security scanning:

```sh
  ./validate-secure-multiarch.sh --fast --sec
```

Auto-fix code style violations:

```sh
  ./validate-secure-multiarch.sh --lint-fix
```

What happens:

- Runs ktlintFormat to auto-fix violations
- Verifies all checks pass after fixes
- Reports any remaining issues that need manual intervention

Reset database (clean + migrate):

```sh
  ./validate-secure-multiarch.sh --reset-db
  ./validate-secure-multiarch.sh --fast --reset-db
```

What happens:

- Drops all database objects (flywayClean)
- Re-creates schema from migrations (flywayMigrate)
- Ensures truly fresh database state
- Useful when schema drift suspected

Check Infrastructure Status

```sh
  ./validate-secure-multiarch.sh --infra
```

What happens:

- Shows running containers and their status
- Lists all configured services in docker-compose
- Checks port availability (5432, 8200, 10000-10002, 2222, etc.)
- Displays helpful commands for next steps
- Exits without making changes

Full Cleanup When Done

```sh
  ./validate-secure-multiarch.sh --clean
```

What happens:

- Stops all Gradle/API processes
- Stops all containers (PostgreSQL, Vault, Azurite, SFTP)
- Complete teardown
- Clean slate

---

### Typical Development Workflow

1. Initial setup (once per session)

```sh
  ./validate-secure-multiarch.sh
```

2. Edit code in src/main/kotlin/

3. Quick test

```sh
  ./validate-secure-multiarch.sh --fast
```

4. Edit more code

5. Quick test again

```sh
  ./validate-secure-multiarch.sh --fast
```

6. Run full tests before committing

```sh
  ./validate-secure-multiarch.sh --fast --full-gradle-tests
```

7. Check infrastructure status (as needed)

```sh
# Check what's running
./validate-secure-multiarch.sh --infra
```

8. Clean tests (as needed)

```sh
# Clean test artifacts (preserves build cache)
./validate-secure-multiarch.sh --clean-tests
```

9. When done, clean up

```sh
 ./validate-secure-multiarch.sh --clean
```

#### What Gets Cleaned Out vs Kept

Running the script without any flags, the script performs a minimal cleanup:

- STOPPED: Gradle API process, Azure Functions, JMX, debug processes
- KEPT RUNNING: PostgreSQL, Vault, Azurite, SFTP containers
- KEPT: Build artifacts, jOOQ classes, Docker images


After `--fast` iteration:

- STOPPED: Gradle processes (if any were started)
- KEPT RUNNING: All infrastructure
- UPDATED: JAR file, Docker image with code changes

After `--clean`:

- STOPPED: Everything
- REMOVED: All containers
- KEPT: Build artifacts

After `--infra`:

- READ-ONLY: Shows status without changes
- Displays: Running containers, configured services, port usage
- Exits: No cleanup or modifications

### Security Validation (--sec)

Vulnerability assessment and compliance:
Image scanning, JAR-by-JAR CVE analysis with MEDIUM,HIGH,CRITICAL severities

### Full Gradle Test Suite (--full-gradle-tests)

Complete code quality validation with PostgreSQL only,unit tests + integration tests + JaCoCo coverage

### End-to-End Testing (--e2e-tests)

Data pipeline and API integration validation using full Docker Compose stack, checks API health, database ops, HL7/FHIR submissions, infrastructure integration.

### Code Quality Validation (--lint, --lint-fix)

#### Run the linter

```sh
./validate-secure-multiarch.sh --lint
```

Validates code formatting with ktlintCheck, reports violations, exits with error if issues found.

#### Auto-fix code style

```sh
./validate-secure-multiarch.sh --lint-fix
```

Automatically fixes formatting violations with ktlintFormat, then verifies all checks pass. Reports any remaining issues that need manual intervention.

### Database Management (--reset-db)

Resetting the DB dataset

```sh
# quick DB reset + reload data without stopping the Postgres container
./validate-secure-multiarch.sh --fast --reset-db
```


```sh
# full DB container reset, flyway migration
./validate-secure-multiarch.sh --reset-db
```


Drops all database objects and re-creates schema from migrations. Useful when:

- Schema drift suspected
- Need to test migrations from clean state
- Stale objects causing test failures

Note: Default behavior removes Docker volumes for fresh database. Use --reset-db when keeping existing volumes but wanting fresh schema.

### SFTP Credentials

The script automatically loads SFTP credentials to Vault during initial setup:

- Credential name: DEFAULT-SFTP
- Username: foo
- Password: pass
- Upload directory: /home/foo/upload

Required for some SFTP delivery tests.

Verify credential loaded:

```sh
docker exec rs-vault vault kv get secret/DEFAULT-SFTP
```

Test SFTP access:
```sh
sftp -P 2222 foo@localhost
# Password: pass
cd upload
put test.txt
```

---

## Advanced Usage

### Infrastructure Selection Flags

Infrastructure-based testing division with automatic environment switching.

Rs-Infrastructure (`--rs-infra`) uses: rs-postgresql, rs-vault, rs-azurite, rs-sftp.

Gradle-Infrastructure (`--gradle-infra`) uses rs-postgresql + rs-vault + standard services.

```sh
# Force rs-infrastructure (pure rs- ecosystem)
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Force gradle-infrastructure (mixed ecosystem)
./validate-secure-multiarch.sh --e2e-tests --gradle-infra

# Auto-selection (recommended - chooses best infrastructure)
./validate-secure-multiarch.sh --sec    # → rs-infra
./validate-secure-multiarch.sh --e2e-tests # → gradle-infra
```

### Flag Combinations

```sh
# Security scanning incl. rs-infrastructure
./validate-secure-multiarch.sh --sec --rs-infra

# E2E testing with containerized infra
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Standard development workflow
./validate-secure-multiarch.sh --e2e-tests --gradle-infra

# Verbose output for debugging
./validate-secure-multiarch.sh --e2e-tests --verbose
```

---

## Troubleshooting

### Infrastructure Mode Issues

```sh
# Switch between infrastructure modes
./validate-secure-multiarch.sh --e2e-tests --rs-infra   # Use rs-infrastructure
./validate-secure-multiarch.sh --e2e-tests --gradle-infra # Use gradle-infrastructure

# Clean up before switching modes
./validate-secure-multiarch.sh --clean-tests
docker-compose -f docker-compose.secure-multiarch.yml down
```

### Vault Issues

```sh
# Rs-infrastructure: Fast, reliable dev mode vault
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Gradle-infrastructure: Feature-rich vault with init.sh
./validate-secure-multiarch.sh --e2e-tests --gradle-infra

# If vault crashes (use rs-infra for stability)
./validate-secure-multiarch.sh --e2e-tests --rs-infra
```

### Port Conflicts

```sh
# Check for conflicts (auto-detects and reports)
./validate-secure-multiarch.sh

# Manual cleanup
docker-compose -f docker-compose.secure-multiarch.yml down --remove-orphans 
# Stop this project's containers
./validate-secure-multiarch.sh --clean
# Stop Gradle processes
pkill -f "gradlew.*run"        

# Clean up rs-infrastructure
docker-compose -f docker-compose.secure-multiarch.yml down
```

### Build Issues

```sh
# Force clean rebuild
./validate-secure-multiarch.sh --clean-tests
docker-compose down --remove-orphans
```

### Code Style Issues

```sh
# Check for violations
./validate-secure-multiarch.sh --lint

# Auto-fix violations
./validate-secure-multiarch.sh --lint-fix

# Review changes before committing
git diff
```

### Database Schema Issues

```sh
# Force fresh schema (drops + recreates)
./validate-secure-multiarch.sh --reset-db

# Verify schema
docker exec rs-postgresql psql -U prime -d prime_data_hub -c "\dt"
```

### SFTP Authentication Issues

```sh
# Verify credential in Vault
docker exec rs-vault vault kv get secret/DEFAULT-SFTP

# Reload credential if missing
java -jar build/libs/prime-router-0.2-SNAPSHOT-all.jar \
  create-credential --type=UserPass --persist=DEFAULT-SFTP \
  --user foo --pass pass

# Test SFTP login
sftp -P 2222 foo@localhost
```
