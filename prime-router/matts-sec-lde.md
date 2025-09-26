# Prime Router Infrastructure Validation

---

## Quick Start

### Essential Commands

```bash
# Basic validation (45 seconds)
./validate-secure-multiarch.sh

# Security validation (3 minutes) - auto-selects rs-infra
./validate-secure-multiarch.sh --sec --platform=linux/amd64

# Complete test suite (7 minutes)
./validate-secure-multiarch.sh --full-gradle-tests

# End-to-end API testing (5 minutes) - auto-selects gradle-infra
./validate-secure-multiarch.sh --e2e-tests

# Fast E2E with rs-infrastructure (~3 minutes)
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Infrastructure mode override examples
./validate-secure-multiarch.sh --sec --gradle-infra   # Override auto-selection
./validate-secure-multiarch.sh --e2e-tests --rs-infra  # Fast vault mode
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
--sec --platform=linux/amd64  # Full image + Azure Functions scanning
--sec --platform=linux/arm64  # Base components only (Apple Silicon)
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

### Infrastructure Selection Flags

**New in 2025**: Infrastructure-based testing division with intelligent switching

```bash
# Force rs-infrastructure (pure rs- ecosystem)
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Force gradle-infrastructure (mixed ecosystem)
./validate-secure-multiarch.sh --e2e-tests --gradle-infra

# Auto-selection (recommended - chooses best infrastructure)
./validate-secure-multiarch.sh --sec    # → rs-infra
./validate-secure-multiarch.sh --e2e-tests # → gradle-infra
```

#### Infrastructure Modes

**Rs-Infrastructure (`--rs-infra`)**:

- **Components**: rs-postgresql, rs-vault (dev mode), rs-azurite, rs-sftp
- **Benefits**: Fast vault, predictable timing
- **Use Cases**: Security scanning, container testing, performance validation

**Gradle-Infrastructure (`--gradle-infra`)**:

- **Components**: rs-postgresql + prime-router-vault + standard services
- **Benefits**: CLI compatibility, existing test integration
- **Use Cases**: Standard development, E2E testing, Gradle workflows

### Flag Combinations

```bash
# Security scanning with rs-infrastructure (recommended)
./validate-secure-multiarch.sh --sec --rs-infra

# E2E testing with fast vault
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Standard development workflow
./validate-secure-multiarch.sh --e2e-tests --gradle-infra

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

## Infrastructure Testing Scenarios

### Security-Focused Testing

```bash
# Security scanning (auto-selects rs-infra)
./validate-secure-multiarch.sh --sec

# Container security validation
./validate-secure-multiarch.sh --container-api-tests --rs-infra

# Override for standard environment testing
./validate-secure-multiarch.sh --sec --gradle-infra
```

### Performance Testing

```bash
# Fast E2E with reliable vault (~20 minutes)
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Standard E2E with full features
./validate-secure-multiarch.sh --e2e-tests --gradle-infra

# Lightweight testing
./validate-secure-multiarch.sh --e2e-lite --rs-infra
```

### Development Workflows

```bash
# Fast development cycle
./validate-secure-multiarch.sh --e2e-lite --rs-infra

# Standard development
./validate-secure-multiarch.sh --e2e-lite --gradle-infra

# Gradle compatibility testing
./validate-secure-multiarch.sh --full-gradle-tests --gradle-infra
```

---

## Integration with Development Workflow

**Josh, Bill** - please keep me honest here, it's how I feel, so please let me know if your day-to-day workflow is different than the below

### Pre-Commit Validation

```bash
# Quick validation before commits
./validate-secure-multiarch.sh

# Complete validation for major changes
./validate-secure-multiarch.sh --full-gradle-tests --gradle-infra

# Security validation before commits
./validate-secure-multiarch.sh --sec
```

### Feature Development

```bash
# After API changes (fast validation)
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# After API changes (standard validation)
./validate-secure-multiarch.sh --e2e-tests --gradle-infra

# After dependency updates
./validate-secure-multiarch.sh --sec --platform=linux/amd64

# Infrastructure changes validation
./validate-secure-multiarch.sh --container-api-tests --rs-infra
```

### Release Preparation 

```bash
# Complete validation suite (fast)
./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests --rs-infra

# Complete validation suite (standard)
./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests --gradle-infra

# Security compliance check
./validate-secure-multiarch.sh --sec --platform=linux/amd64

# Infrastructure validation
./validate-secure-multiarch.sh --container-api-tests --rs-infra
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
- **Validation summary**: "Multi-architecture infrastructure validation PASSED" 
- **Test results**: Specific test counts and success rates displayed
- **Security results**: CVE counts and vulnerability details shown

### Infrastructure Components Validated

- **PostgreSQL**: Wolfi-based build
- **Prime Router API**: Azure Functions with 75 endpoint configurations
- **Docker infrastructure**: Vault, Azurite, SFTP, SOAP/REST webservices
- **Multi-platform images**: ARM64 and AMD64 containers

### API Testing Scope

- **Health verification**: Endpoint responsiveness and connectivity
- **Data operations**: Lookup table loading and organization settings
- **Message processing**: Real HL7 and FHIR message submissions 
- **Response validation**: Report IDs, submission IDs, status verification
- **Pipeline integration**: End-to-end data flow through infrastructure

---

## Troubleshooting

### Infrastructure Mode Issues

```bash
# Switch between infrastructure modes
./validate-secure-multiarch.sh --e2e-tests --rs-infra   # Use rs-infrastructure
./validate-secure-multiarch.sh --e2e-tests --gradle-infra # Use gradle-infrastructure

# Clean up before switching modes
./validate-secure-multiarch.sh --clean-tests
docker-compose -f docker-compose.secure-multiarch.yml down
```

### Vault Issues

```bash
# Rs-infrastructure: Fast, reliable dev mode vault
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Gradle-infrastructure: Feature-rich vault with init.sh
./validate-secure-multiarch.sh --e2e-tests --gradle-infra

# If vault crashes (use rs-infra for stability)
./validate-secure-multiarch.sh --e2e-tests --rs-infra
```

### Port Conflicts

```bash
# Check for conflicts (auto-detects and reports)
./validate-secure-multiarch.sh

# Manual cleanup
docker-compose down --remove-orphans # Stop this project's containers
pkill -f "gradlew.*run"        # Stop Gradle processes

# Clean up rs-infrastructure
docker-compose -f docker-compose.secure-multiarch.yml down
```

### Performance Issues

```bash
# For faster testing, use rs-infrastructure
./validate-secure-multiarch.sh --e2e-tests --rs-infra

# Check Docker space
docker system df

# Clean if needed (CAREFUL - only if no other projects affected)
docker system prune -f

# Infrastructure-specific performance
# Rs-infra: ~20 minutes for E2E (fast vault)
# Gradle-infra: Longer due to vault complexity
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
 