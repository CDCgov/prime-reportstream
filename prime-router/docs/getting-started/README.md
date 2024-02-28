# Getting Started as a ReportStream Developer

## Overview

These pages are a work in progress, please see the [old pages](../docs-deprecated/getting-started/) if something is missing

## Table of Contents
todo

## Locally Installed Software Prerequisites

You will need to have at least the following pieces of software installed _locally_ in order to be able to build and/or debug this baseline:

* [git](install-git.md) including git-bash if you're on Windows
* [Docker or Docker Desktop](install-docker.md)
* [OpenJDK](install-openjdk.md) (currently targetting 17)
    * See the linked docs for important instructions on setting `JAVA_HOME`
* An IDE. IntelliJ is recommended for Kotlin/debugging support. 

To reduce the build-debug cycle time you can install these tools to run the code directly. These tools are required if you are using an Apple Silicon computer, otherwise they are optional.
* [Azure Functions Core Tools](install-afct.md) (currently targetting 4)
* [Azure Storage Explorer](https://docs.microsoft.com/en-us/azure/vs-azure-tools-storage-manage-with-storage-explorer)
* [AzureCLI](install-azurecli.md)
* [Gradle](install-gradle.md)
* One or more [PostgreSQL Clients](psql-clients.md)

> [!Warning]
> If you are using an Apple Silicon computer you will need to use Gradle as Microsoft's Azure Function Docker image is only `amd64` compatible. There are also [known issues running Intel Docker images on Apple Silicon](https://docs.docker.com/desktop/mac/apple-silicon/#known-issues). Both approaches to running the project are documented here. 
> Many of our local developer tools are set up to run in Docker containers. Looking at our `docker-compose.yml` file, the `web_receiver`, `prime_dev`, and `settings` services do not work on Apple Silicon. Likewise, the `builder` service in our `docker-compose.build.yml` does not work.

## Bulding the Baseline

### First Build

1. The `cleanslate.sh` script does the base work needed to start developing for ReportStream. It only needs to be run once.   This script runs on Apple processors, but it skips a few steps. We will need to do these missing steps by hand.

1. [Clone the prime-reportstream repository](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository-from-github/cloning-a-repository) to your workstation using git.

1. If you are using Docker Desktop, verify that it is running prior to building or running ReportStream locally.

1. Initialize your environment and run an initial build by running the following command using a Linux shell.
   Note you can run `cleanslate.sh` script to reset your environment as well (run `./cleanslate.sh --help` for more
   information). The `cleanslate.sh` script not only cleans, but also performs a first build and setup of all artifacts
   needed for the build

```bash
cd ./prime-router
./cleanslate.sh
```

## Running ReportStream

1. If you are using Docker Desktop, verify that it is running prior to building or running ReportStream locally.
- Explanation of all the pieces


⋅⋅* Building and running ReportStream requires a locally accessible PostgreSQL database instance that is initially setup
   and run by the `cleanslate.sh` script.  This database instance runs as a Docker container defined by the
   `docker-compose.build.yml` file.  You will need to start this database instance upon a workstation reboot by
   using the following command:

```bash
cd ./prime-router
docker-compose --file "docker-compose.build.yml" up --detach
```

### Running the Backend

#### Running Natively

**Must use this option if on a silicon mac**

#### Running via docker

### Running the frontend

### Running the static site

## Troubleshooting

## What to read next

- [Contributing](./contributing.md)
- [About the azure environment](./azure.md)
- [Working with Docker](./docker.md)
- [Common database commands](./postgres-database.md)
- [PrimeCLI](./prime-cli.md)
- [FHIR Functions](./fhir-functions.md)
- [Gradle](./gradle.md)
- [Metabase](./metabase.md)
- [VPN](./vpn.md)
- [Pipeline Configuration](./universal-pipeline-configuration.md)
- [Swagger](./swagger.md)
- [Kotlin](./kotlin.md)


### Gradle Commands

You can invoke `gradlew` from the `./prime-router` directory to build the baseline as follows:

```bash
./gradlew clean package
```

The most useful gradle tasks are:

* `clean`: deletes the build artifacts
* `compile`: compiles the code
* `test`: runs the unit tests
* `testIntegration`: runs the integration tests
* `package`: packages the build artifacts for deployment
* `quickpackage`: re-packages the build artifacts for deployment without running the tests
* `testSmoke`: runs all the smoke tests; this requires [that you are running ReportStream](#running-reportstream)
* `testEnd2End`: runs the end-to-end test; this requires [that you are running ReportStream](#running-reportstream)
* `primeCLI`: run the prime CLI.  Specify arguments with `"--args=<args>"`