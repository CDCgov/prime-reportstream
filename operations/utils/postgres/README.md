## PostgreSQL 16.6 Docker Image (Local Testing Only)

> **WARNING: This image is for local testing only. Do NOT deploy it to Azure, AWS, or any public cloud environment.**

### Overview

This Docker image builds PostgreSQL 16.6 from source using Wolfi Linux and provides:

A pre-configured PostgreSQL instance with ICU and Readline support.
A default database and user for quick testing.
Networking enabled for local connections and other Docker containers.

### Usage

Read through the Makefile, or run:

```shell
    make help
```

#### Connect From Another Container

```shell
    docker run --rm --network pg_network postgres:16 psql -h rs-postgres -U rsuser -d mydb
```

#### Connect From Host Machine

```shell
    psql -h localhost -U rsuser -d mydb -p 5432
```

### Important Security Warnings

DO NOT use this image in production or any public environment.

DO NOT deploy to Azure, AWS, or any other cloud platform.

While CVE-free at the time of creation, this image lacks sufficient security hardening, making it unsafe for external deployments.
