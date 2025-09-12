# Prime Router Hardened Infrastructure Validation

---

## Quick Start

### Essential Commands

```bash
# Basic validation (45 seconds)
./validate-secure-multiarch.sh

# Security validation (3 minutes)  
./validate-secure-multiarch.sh --sec --platform=linux/amd64

# Complete test suite (7 minutes)
./validate-secure-multiarch.sh --full-gradle-tests

# End-to-end API testing (5 minutes)
./validate-secure-multiarch.sh --e2e-tests
```

### Prerequisites

- Docker Desktop running
- Azure Functions Core Tools 4.2.2+ installed
- Suffficient disk space (4GB+ for image builds)
- Ensure your shell is in `prime-router` directory before execution

---

## Validation Modes

### Basic Validation (No Flags)

**Purpose**: Daily development validation  
**Infrastructure**: PostgreSQL + standard Docker Compose services  
**Testing**: API responsiveness, lookup tables, HL7/FHIR submissions, organization settings

**Use Cases**:

- Pre-commit validation
- Quick API health check  
- Daily development workflow
- Basic infrastructure verification

### Security Validation (--sec)

**Purpose**: Vulnerability assessment and compliance  
**Infrastructure**: None (image-only scanning)  
**Analysis**: JAR-by-JAR CVE analysis with MEDIUM,HIGH,CRITICAL severities

**Platform Options**:

```bash
--sec --platform=linux/amd64    # Full image + Azure Functions scanning
--sec --platform=linux/arm64    # Base components only (Apple Silicon)
```

**Use Cases**:

- Pre-deployment security validation
- CI/CD security gates  
- Compliance reporting
- CVE tracking and reduction verification

### Full Gradle Test Suite (--full-gradle-tests)  

**Purpose**: Complete code quality validation  
**Infrastructure**: PostgreSQL only (test-optimized)  
**Testing**: 1338 unit tests + 430 integration tests + JaCoCo coverage

**Use Cases**:

- Pre-commit complete validation
- Release qualification testing
- Code coverage analysis
- Quality assurance workflows

### End-to-End Testing (--e2e-tests)

**Purpose**: Data pipeline and API integration validation  
**Infrastructure**: Full Docker Compose stack  
**Testing**: API health, database ops, HL7/FHIR submissions, infrastructure integration

**Use Cases**:

- Feature validation after changes
- Integration testing
- Data pipeline verification  
- API functionality validation

---

## Advanced Usage

### Flag Combinations

```bash
# Security + E2E (runs security only - precedence rules apply)
./validate-secure-multiarch.sh --e2e-tests --sec --platform=linux/arm64

# Complete testing (E2E + full test suite)
./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests

# Verbose output for debugging
./validate-secure-multiarch.sh --e2e-tests --verbose
```

### Platform-Specific Usage

**Apple Silicon**:

```bash
# Security scanning (native ARM64)
./validate-secure-multiarch.sh --sec --platform=linux/arm64

# API testing (auto-corrected to AMD64 for Azure Functions)  
./validate-secure-multiarch.sh --e2e-tests

# All modes should auto-detect and handle platform appropriately
```

**Intel/AMD Machines**:

No `--platform=...` flag necessarty.

```bash
# All modes work natively without platform specification
./validate-secure-multiarch.sh --full-gradle-tests
./validate-secure-multiarch.sh --e2e-tests --sec
```

---

## Integration with Development Workflow

**Josh, Bill** - please keep me honest here, it's how I feel, so please let me know if your day-to-day workflow is different than the below

### Pre-Commit Validation

```bash
# Quick validation before commits
./validate-secure-multiarch.sh

# Complete validation for major changes
./validate-secure-multiarch.sh --full-gradle-tests
```

### Feature Development

```bash
# After API changes
./validate-secure-multiarch.sh --e2e-tests

# After dependency updates
./validate-secure-multiarch.sh --sec --platform=linux/amd64
```

### Release Preparation  

```bash
# Complete validation suite
./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests

# Security compliance check
./validate-secure-multiarch.sh --sec --platform=linux/amd64
```

---

## Output and Reporting

Once this is validated and matured, I can hide some testing output to only show tests that error out.

### Validation Reports

- **validation-report.txt**: Validation summary
- **build/reports/tests/test/index.html**: Unit test results (after --full-gradle-tests)
- **build/reports/jacoco/test/html/index.html**: Code coverage analysis

### Log Files (Auto-Generated)

- **gradle-output.log**: Gradle execution details (API testing)
- **full-security-scan.log**: Detailed Trivy output (after --sec)

### Cleanup

```bash
# Clean test artifacts (preserves build cache)
./validate-secure-multiarch.sh --clean-tests
```

---

## Understanding Validation Results

### Success Indicators

- **Clean execution**: No warnings or errors in output
- **Validation summary**: "Multi-architecture hardened infrastructure validation PASSED"  
- **Test results**: Specific test counts and success rates displayed
- **Security results**: CVE counts and vulnerability details shown

### Infrastructure Components Validated

- **Hardened PostgreSQL**: Custom Wolfi-based build (0 CVEs)
- **Prime Router API**: Azure Functions with 75 endpoint configurations
- **Docker infrastructure**: Vault, Azurite, SFTP, SOAP/REST webservices
- **Multi-platform images**: ARM64 and AMD64 hardened containers

### API Testing Scope

- **Health verification**: Endpoint responsiveness and connectivity
- **Data operations**: Lookup table loading and organization settings
- **Message processing**: Real HL7 and FHIR message submissions  
- **Response validation**: Report IDs, submission IDs, status verification
- **Pipeline integration**: End-to-end data flow through infrastructure

---

## Troubleshooting

### Port Conflicts

```bash
# Check for conflicts
./validate-secure-multiarch.sh  # Will detect, attempt to fix and report exotic conflicts

# Resolve conflicts  
docker-compose down              # Stop this project's containers
pkill -f "gradlew.*run"         # Stop Gradle processes
```

### Performance Issues

```bash
# Check Docker space
docker system df

# Clean if needed (CAREFUL - only if no other projects affected)
docker system prune -f
```

### Build Issues

```bash
# Force clean rebuild
./validate-secure-multiarch.sh --clean-tests
docker-compose down --remove-orphans
```

---

## Security Scanning Details

- **Severity levels**: MEDIUM, HIGH, CRITICAL (project standard)
- **Scan coverage**: OS packages, .NET components, Java JARs
- **Platform-specific**: ARM64 base components, AMD64 full stack
- **Infrastructure images**: PostgreSQL, Vault, Azurite, SFTP

### Resource Requirements

- **Memory**: 4-8GB available for Docker builds and test execution
- **CPU**: Multi-core recommended for parallel Gradle compilation
- **Storage**: 5GB+ free space for Docker images and test artifacts
- **Network**: Required for base image pulls and Trivy database updates

---

## Integration Notes

### Gradle Integration

- **Custom configuration**: Uses matts-testing build files for rs- infrastructure
- **Environment variables**: COMPOSE_PROJECT_NAME and COMPOSE_FILE set automatically
- **Test isolation**: Unit tests run with test-only infrastructure (PostgreSQL only)
- **API tests**: Full infrastructure with Azure Functions for E2E validation

### Docker Integration  

- **Project isolation**: rs- prefixed containers prevent conflicts with other projects
- **Multi-platform builds**: Supports both Apple Silicon and Intel/AMD development
- **Security hardening**: Non-root execution, read-only mounts, minimal attack surface
- **Clean teardown**: Project-specific cleanup prevents disrupting other work

### Azure Functions Integration

- **Version compatibility**: Azure Functions Core Tools 4.2.2+
- **Platform requirements**: AMD64 for runtime (auto-corrected on Apple Silicon)
- **Function generation**: 75 API endpoints automatically configured
- **Development parity**: Same functions and behavior as production Azure deployment
  