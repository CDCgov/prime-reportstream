# Local Prime Router Development Environemnt

[WiP] validation script for Prime Router with containerized infrastructure, security scanning, and test execution.

## Overview

`validate-matts-testing.sh` provides a complete testing framework using hardened Docker containers. It automates infrastructure management, database migration, data loading, and test execution with accurate result reporting.

## API Deployment Modes

### Container Mode (Default)

Deploys API using hardened multi-architecture Docker container

### Gradle Mode (Alternative)  

Deploys API using Azure Functions host via Gradle

## Configuration

### Switching Deployment Modes

Edit API_MODE var in `validate-matts-testing.sh`:

```bash
# Container mode (default)
API_MODE="container"

# Gradle mode (alternative)  
API_MODE="gradle"
```

### Core Parameters

All key references are parameterized at the top of the script for easy maintenance:

```bash
PRIME_JAR_NAME="prime-router-0.2-SNAPSHOT-all.jar"
API_IMAGE_NAME="rs-prime-router-api:latest"
POSTGRESQL_IMAGE_NAME="rs-postgresql:latest"
API_CONTAINER_NAME="rs-prime-router-api"
```

## Usage

```bash
# Basic validation
./validate-matts-testing.sh

# Debug mode with container logging
./validate-matts-testing.sh --debug
```

## Test Coverage

Executes 5 complete test suites:

- **Smoke Tests**: API functionality and data processing
- **Integration Tests**: Database and service integration  
- **Unit Tests**: Core application logic
- **End-to-End Tests**: Complete data flow validation
- **End-to-End UP Tests**: Upload and processing validation

## Infrastructure

- **PostgreSQL**: Custom Wolfi-based image
- **Vault**: Latest HashiCorp Vault immage
- **Azurite**: Latest Azure storage emulator image
- **SFTP**: Custom Wolfi-based image
- **API**: Multi-architecture hardened image
