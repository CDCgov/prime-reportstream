# Prime Router Secure Multi-Architecture Validation Guide

## Summary

Enable rapid adoption of hardened infrastructure across Intel and Apple Silicon machines for development and testing

---

## Quick Start Commands

### Basic Validation

```bash
# Infrastructure + basic API testing
./validate-secure-multiarch.sh --platform=linux/amd64

# Security scan only 
./validate-secure-multiarch.sh --sec --platform=linux/amd64
```

### e2e Testing

```bash
# End-to-end tests with all documented test suites
./validate-secure-multiarch.sh --e2e-tests --platform=linux/amd64

# Complete validation (security + E2E + full Gradle tests)
./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests --sec --platform=linux/amd64
```

### Local Development

```bash
# Start teh complete local infrastructure for development
./run-with-rs-infrastructure.sh

# Then use standard Gradle commands with hardened infrastructure
./gradlew run                    # API with hardened PostgreSQL
./gradlew test                   # All tests with hardened containers  
./gradlew testSmoke             # Smoke tests
./gradlew testEnd2End           # End-to-end tests
```

---

## All Available Validation Options

### Script Flags Reference

You can run `validate-secure-multiarch.sh` with any combination of flags listed below on Intel or Silicon machiones, but bee mindful of the `--platform` flag - details below.

| Flag | Purpose | Duration | Use Case |
|------|---------|----------|----------|
| `--sec` | Security scanning only | ~3 min | Pre-deployment security validation |
| `--e2e-tests` | E2E + documented test suites | ~15 min | Feature validation, integration testing |
| `--full-gradle-tests` | Complete unit + integration test suite | ~20 min | Development validation, CI/CD |
| `--api-test` | Containerized API testing (advanced) | ~10 min | Container validation, debugging |
| `--platform=ARCH` | Force specific architecture | N/A | Cross-platform testing |
| `--verbose` | Detailed output | + ~20% time | Debugging, detailed analysis |
| `--clean-tests` | Clean test artifacts only | seconds | Cleanup before fresh testing |

### Common Flag Combinations

```bash
# Pre-deployment validation (recommended for CI/CD)
./validate-secure-multiarch.sh --sec --platform=linux/amd64

# Feature development validation  
./validate-secure-multiarch.sh --e2e-tests --verbose

# Complete e2e validation
./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests --sec --platform=linux/amd64

# Quick infrastructure check
./validate-secure-multiarch.sh

# Debugging with detailed output
./validate-secure-multiarch.sh --e2e-tests --verbose --platform=linux/amd64
```

---

## Architecture Support: Intel vs Apple Silicon

### Platform Detection & Override

The script auto-detects your architecture:

- **Apple Silicon (M-chips)**: Detected as `linux/arm64`
- **Intel Machines**: Detected as `linux/amd64`

#### Platform Override (Recommended)

```bash
# Force AMD64 (recommended for all API testing)
./validate-secure-multiarch.sh --platform=linux/amd64

# Use ARM64 (for security scanning only)  
./validate-secure-multiarch.sh --sec --platform=linux/arm64
```

### Key Architecture Differences

#### Apple Silicon (ARM64)

- **Infrastructure**: All containers work natively
- **Security scanning**: Full Trivy scanning of ARM64 components
- **Azure Functions**: Runtime requires emulation (AMD64 forced by Microsoft's limite arch availability)
- **Recommendation**: Use `--platform=linux/amd64` for API/E2E testing

**Silicon Commands**:

```bash
# Security validation (native ARM64)
./validate-secure-multiarch.sh --sec --platform=linux/arm64

# API/E2E testing (AMD64 emulation)  
./validate-secure-multiarch.sh --e2e-tests --platform=linux/amd64
```

#### Intel/AMD Machines

- **All features**: Native AMD64 support for everything
- **Azure Functions**: Native runtime performance
- **Full compatibility**: All validation modes work natively

**Intel Commands**:

```bash
# All validation modes work natively
./validate-secure-multiarch.sh --e2e-tests --sec
./validate-secure-multiarch.sh --full-gradle-tests
```

### Platform Validation Logic

The script automatically handles platform compatibility:

- **Auto-correction**: ARM64 → AMD64 when API testing is requested
- **Warning messages**: Clear guidance on platform requirements
- **Performance notes**: Emulation vs native execution information

---

## What Gets Validated

### Basic Validation (No Flags)

- Multi-architecture hardened Docker image build
- RS- prefixed infrastructure startup (7 services)  
- PostgreSQL compatibility with Gradle (hardened Wolfi-based)
- Basic API connectivity testing
- Infrastructure health verification

### Security Validation (`--sec`)

- Trivy scanning (MEDIUM,HIGH,CRITICAL severities)
- JAR-by-JAR vulnerability analysis
- Infrastructure CVE elimination verification (327 → 0)
- Application CVE reduction confirmation (327 → 25)
- Platform-specific Docker socket configuration

### E2E Validation (`--e2e-tests`)

- All basic validation capabilities  
- HL7 report submission with real data pipeline
- FHIR report submission with response validation
- Database operations via API (lookup tables)
- Report history API testing
- Infrastructure integration (cross-service connectivity)
- Documented test suites from `running-tests.md`:
- Smoke tests (`./gradlew testSmoke`)
- End-to-end tests (`./gradlew testEnd2End`, `testEnd2EndUP`)
- Prime CLI tests (`./prime test --run end2end`)

### Full Gradle Validation (`--full-gradle-tests`)

- Complete unit test suite (`./gradlew test -Pforcetest`)
- Integration tests (`./gradlew testIntegration`)
- Test report generation with coverage analysis
- All 1338+ tests with hardened infrastructure

---

## Performance Guidelines

### Recommended Usage by Scenario

#### Fast Pre-Commit Validation

```bash
./validate-secure-multiarch.sh --sec --platform=linux/amd64
```

#### Feature Development

```bash
./validate-secure-multiarch.sh --e2e-tests --platform=linux/amd64
```

#### Complete Pre-release Validation  (all test ssuites)

```bash  
./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests --sec --platform=linux/amd64
```

#### CI/CD Optimized Pipeline

```bash
# Parallel execution:
./validate-secure-multiarch.sh --sec --platform=linux/amd64 &
./validate-secure-multiarch.sh --e2e-tests --platform=linux/amd64 &
wait
```

### Approximate Execution Time Breakdown (M3 MAX)

| Component | Duration | Optimization |
|-----------|----------|--------------|
| Infrastructure startup | ~2 min | Parallel service startup |
| Fat JAR build | ~1 min | Infrastructure-first eliminates failures |
| Hardened image build | ~2 min | Docker layer caching |
| Security scanning | ~3 min | JAR analysis |
| E2E tests | ~10 min | Real data pipeline validation |
| Full Gradle tests | ~15 min | ~1338 test suite execution |

---

## Developer Workflow Integration

### Local Development Setup

```bash
# One-time setup: Start RS infrastructure
./run-with-rs-infrastructure.sh

# Daily development (infrastructure stays running)
./gradlew run                    # API with hardened PostgreSQL
./gradlew test                   # Tests with secure containers
./prime test --run end2end      # E2E validation

# Clean shutdown when done
docker-compose -f docker-compose.secure-multiarch.yml down
```

### Team Validation Workflow

```bash
# Before merging features
./validate-secure-multiarch.sh --e2e-tests --platform=linux/amd64

# Before releases
./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests --sec --platform=linux/amd64

# Security-only checks (fast feedback)
./validate-secure-multiarch.sh --sec --platform=linux/amd64
```

---

## Platform-Specific Notes

### Apple Silicon (M1-3 chip) Users

- **Auto-detection**: Script detects ARM64 and may auto-correct to AMD64 for API testing
- **Performance**: Emulation adds ~20% overhead for Azure Functions
- **Recommendation**: Always use `--platform=linux/amd64` for API/E2E testing
- **Native ARM64**: Use only for security scanning (`--sec --platform=linux/arm64`)

### Intel/AMD Users

- **Native performance**: All features work at full speed
- **Platform flexibility**: Both ARM64 and AMD64 builds supported
- **CI/CD ready**: Ideal for Linux-based build environments

### Cross-Platform Development

```bash
# Test both architectures
./validate-secure-multiarch.sh --sec --platform=linux/arm64   # ARM64 security scan
./validate-secure-multiarch.sh --sec --platform=linux/amd64   # AMD64 security scan
./validate-secure-multiarch.sh --e2e-tests --platform=linux/amd64  # API testing (AMD64 only (azure functions compatibility))
```

---

## Troubleshooting

### Common Issues & Solutions

```bash
# Issue: "No space left on device"
docker system prune -af --volumes

# Issue: Port conflicts
docker ps | grep :5432                    # Check for conflicts
docker-compose down                        # Stop this project's containers only

# Issue: Network conflicts (project-specific cleanup)
docker-compose down --remove-orphans       # Safe project cleanup
docker network ls | grep prime-router      # Check project networks only

# Issue: Permission errors
./validate-secure-multiarch.sh --clean-tests    # Clean test artifacts
```

### Verification Commands

```bash
# Check RS infrastructure status
docker ps | grep rs-

# Verify services are responding
curl http://localhost:8200/v1/sys/health    # Vault
curl http://localhost:10000/                # Azurite  
nc -z localhost 5432                       # PostgreSQL

# Check validation script help
./validate-secure-multiarch.sh --help
```

---

## Success Metrics

### Security Posture Improvementds

- **100% Infrastructure CVE elimination** (327 → 0 vulnerabilities)
- **92.3% Overall application + dependenncies CVE reduction** (327 → 25 vulnerabilities)  
- **Zero-CVE hardened PostgreSQL** (Wolfi-based)
- **Security-hardened containers** (no-new-privileges, read-only mounts)

### Development Niotes

- **No conflicts**: rs- prefix isolates resources from other projects
- **Parallel development**: Multiple teams can use different prefixes
- **Production parity**: Same security in dev, staging, and production
- **Updated Azure Functions to 4.2.2**: No version upgrade issues

---

## Getting Started Checklist

### First-Time Setup

- [ ] Clone repository and navigate to `prime-router` directory
- [ ] Ensure Docker Desktop is running  
- [ ] Install Azure Functions Core Tools 4.2.2+ with `brew` (`func --version`)
- [ ] Run security validation: `./validate-secure-multiarch.sh --sec --platform=linux/amd64`

### Daily Development

- [ ] Start RS local infrastructure: `./run-with-rs-infrastructure.sh`
- [ ] Use standard Gradle commands with hardened containers
- [ ] Run validation before commits: `./validate-secure-multiarch.sh --e2e-tests`

### Release Preparation

- [ ] Complete validation: `./validate-secure-multiarch.sh --e2e-tests --full-gradle-tests --sec`
- [ ] Review security scan results
- [ ] Verify all documented test suites pass

