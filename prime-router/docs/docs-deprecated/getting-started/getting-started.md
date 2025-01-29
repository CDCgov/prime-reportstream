# Developer Getting Started Guide

> Note: All information duplicated by the [Backend Setup](../../getting-started/README.md) has been removed.

This document will walk you through the setup instructions to get a functioning development environment.

# Table of contents
- [Committing to this repository](#committing-to-this-repository)
    * [Updating schema documentation](#updating-schema-documentation)
- [Running ReportStream](#running-reportstream)
    * [Restarting After a Code Update](#restarting-after-a-code-update)
        + [Inspecting the Logs](#inspecting-the-logs)
        + [How to change Logging Levels](#how-to-change-logging-levels)
        + [Debugging ReportStream](#debugging-reportstream)
    * [Finding misconfigurations](#finding-misconfigurations)
    * [Getting around SSL errors](#getting-around-ssl-errors)
- [Function development with docker-compose](#function-development-with-docker-compose)
    * [Running ReportStream locally](#running-reportstream-locally)
- [Credentials and secrets vault](#credentials-and-secrets-vault)
    * [Initializing the vault](#initializing-the-vault)
    * [Re-initializing the vault](#re-initializing-the-vault)
    * [Using the vault locally](#using-the-vault-locally)
- [Resetting your environment](#resetting-your-environment)
    * [Resetting the Database](#resetting-the-database)
- [Additional tooling](#additional-tooling)
- [Miscellaneous subjects](#micellaneous-subjects)
    * [Using different database credentials than the default](#using-different-database-credentials-than-the-default)
    * [Using local configuration for organizations.yml](#using-local-configuration-for-organizationsyml)
    * [`PRIME_DATA_HUB_INSECURE_SSL` environment variable](#-prime-data-hub-insecure-ssl--environment-variable)

# Committing to this repository

## Updating schema documentation
You must run the schema document generator after a schema file is updated.  The updated documents are stored in
`generated/schema-documentation` and must be included with your schema changes. The CI/CD pipeline checks for the need to update
schema documentation and the build will fail if the schema documentation updates are not included.

```bash
./gradlew generateDocs
```

# Running ReportStream

You can bring up the entire ReportStream environment by running the `devenv-infrastructure.sh` script after building
the baseline (see "[First Build](#first-build)")

```bash
cd ./prime-router
./gradlew clean package
./devenv-infrastructure.sh
```
If you see any SSL errors during this step, follow the directions in [Getting Around SSL Errors](#getting-around-ssl-errors).

## Restarting After a Code Update
You must re-package the build and restart the prime_dev container to see any modifications you have made to the files:

```bash
cd ./prime-router
./gradlew package
docker compose restart prime_dev
```

### Inspecting the Logs

The docker containers produce logging output. When dealing with failures or bugs, it can be very useful to inspect this output. You can inspect the output of each container using ('`$`' indicates your prompt):

```bash
# List PRIME containers:
$ docker ps --format '{{.Names}}' | grep ^prime-router
prime-router_web_receiver_1
prime-router_prime_dev_1
prime-router_sftp_1
prime-router_azurite_1
prime-router_vault_1
prime-router_postgresql_1
# Show the log of (e.g.) prime-router_postgresql_1 until now
docker logs prime-router_postgresql_1
# Show the log output of (e.g.) prime-router-prime_dev_1 and stay on it
docker logs prime-router_prime_dev_1 --follow
```

### How to change Logging Levels

To change the level of logging in our kotlin code, edit the src/main/resources/log4j2.xml file.  For example, to get very verbose logging across all classes:
```
        <Logger name="gov.cdc.prime.router" level="trace"/>
```
To increase the level of Azure Function logging (Microsoft's logging), edit the 'logging' section of the host.json file and add a logLevel section, like this:
```
  "logging": {
    "logLevel": {
      "default": "Trace"
    },
    "applicationInsights": {
      "samplingSettings": {
        "isEnabled": true
      }
    }
  }
```

### Debugging ReportStream

The Docker container running ReportStream exposes local port `5005` for remote Java debugging. Connect your
debugger to `localhost:5005` while the Docker container is running and set the necessary breakpoints.

## Finding misconfigurations

ReportStream comes packaged with a executable that can help with finding misconfigurations and other problems with the appliciation. Use the following command to launch the tool locally while the ReportStream container is running:

```bash
cd prime-router

# Specify these explicitly as exports or as command-scope variables
export POSTGRES_PASSWORD='changeIT!'
export POSTGRES_URL=jdbc:postgresql://localhost:5432/prime_data_hub
export POSTGRES_USER=prime
./prime test
```

Running this test command (pointed at the right database) should "repair" a running ReportStream process and should persist through subsequent runs.

## Getting around SSL errors

If your agency's network intercepts SSL requests, you might have to disable SSL verifications to get around invalid certificate errors.

# Function development with docker-compose

## Running ReportStream locally

Most uses of the PRIME router will be in the Microsoft Azure cloud. The router runs as a container in Azure. The [`DockerFile`](../../Dockerfile) describes what goes in this container.

Developers can also run the router locally with the same Azure runtime and libraries to help develop and debug in an environment that mimics the Azure environment as closely as we can on your local machine. In this case, a developer can use a local Azure storage emulator, called Azurite.

We use docker-compose' to orchestrate running the Azure function(s) code and Azurite. See sections "[Running ReportStream](#running-reportstream)" for more information on building and bringing your environment up.

If you see any SSL errors during this step, follow the directions in [Getting Around SSL Errors](#getting-around-ssl-errors).

# Credentials and secrets vault

Our `docker-compose.yml` includes a Hashicorp Vault instance alongside our other containers to enable local secrets storage. Under normal circumstances, developers will not have to interact directly with the Vault configuration.
This vault is used locally to provide the SFTP credentials used by the Send function to upload files to the locally running atmoz/sftp SFTP server.

## Initializing the vault

NOTE: the cleanslate.sh script will set this up for you (see also "[First build](#first-build)" and "[Resetting your environment](#resetting-your-environment)").

Run the following commands to initialize vault:
```bash
mkdir -p .vault/env
cat /dev/null > .vault/env/.env.local
```

When starting up our containers with `docker compose up` on first-run, the container will create a new Vault database and once initialized (which may take a couple of seconds) store the following files in `.vault/env`:

* `key`: unseal key for decrypting the database
* `.env.local`: the root token in envfile format for using the Vault api/command line

The database is stored in a docker compose container `vault` which is persisted across up and down events. All files are excluded in `.gitignore` and should never be persisted to source control.

## Re-initializing the vault

NOTE: the cleanslate.sh script will re-initialize your vault for you (see also "[Resetting your environment](#resetting-your-environment)").

If you would like to start with a fresh Vault database, you can clear the Vault database with one of the following commands sets:

Using `cleanslate.sh`:
```bash
cd ./prime-router
./cleanslate.sh --keep-images --keep-build-artifacts
```

Manually:
```bash
cd prime-router
# -v removes ALL volumes associated with the environment
docker compose down -v
rm -rf .vault/env/{key,.env.local}
cat /dev/null > .vault/env/.env.local
```

## Using the vault locally

Our `docker-compose.yml` will automatically load the environment variables needed for the Vault. If you need to use the Vault outside Docker, you can find the environment variables you need in `.vault/env/.env.local`.
When your Vault is up and running (exemplified by `.vault/env/.env.local` being populated with two environment variables: `VAULT_TOKEN` and `CREDENTIAL_STORAGE_METHOD`), you can interact with it in a couple of ways:

* Graphical/Web UI: Navigate to [http://localhost:8200](http://localhost:8200) and provide the value of `VAULT_TOKEN` to log into the vault.
* curl/HTTP API to get JSON back, example:

    ```bash
    export $(xargs <.vault/env/.env.local)
    SECRET_NAME=DEFAULT-SFTP
    URI=http://localhost:8200/v1/secret/${SECRET_NAME?}
    curl --header "X-Vault-Token: ${VAULT_TOKEN?}" "${URI?}"
    ```

The values from the .vault/env/.env.local file can also be automatically loaded into most IDEs:

* [IntelliJ](https://plugins.jetbrains.com/plugin/7861-envfile)
* [VSCode](https://dev.to/andreasbergstrom/placeholder-post-1klo)

Alternatively, you can inject them into your terminal via:

```bash
export $(xargs <./.vault/env/.env.local)
```

# Resetting your environment

You can run the `./cleanslate.sh` script to recover from an unreliable or messed up environment. Run the script with `--help` to learn about its different levels of 'forcefulness' and 'graciousness' in its cleaning repertoire:

```bash
cd ./prime-router

# default mode:
./cleanslate.sh

# most forceful mode
./cleanslate.sh --prune-volumes

# Show the different modes for 'graciousness'
./cleanslate.sh --help
```

When invoked with `--prune-volumes`, this script will also reset your PostgreSQL database. This can be useful to get back to a known and/or empty state.

## Resetting the Database
1. Stop your ReportStream container if it is running.
    ```bash
    docker compose down
    ```
1. Run the following command to delete all ReportStream related tables from the database and recreate them.  This
is very useful to reset your database to a clean state.  Note that the database will be re-populated the
next time you run ReportStream using docker compose up.
    ```bash
    ./gradlew resetDB
    ```
1. Run ReportStream and run the following commands to load the tables and organization settings into the database:
    ```bash
    ./gradlew reloadTables
    ./gradlew reloadSettings
    ```

# Additional tooling

Use any other tools that are accessible to you to develop the code. Be productive. Modify this document if you have a practice that will be useful.

Some useful tools for Kotlin/Java development include:

* [Azure Storage Explorer](https://azure.microsoft.com/en-us/features/storage-explorer/)
* [JetBrains IntelliJ](https://www.jetbrains.com/idea/download/)
* [KTLint](https://ktlint.github.io/): the Kotlin linter that we use to format our KT code
    * After installing IntelliJ and cloning the repo, run the following command to install the ktlint rules: `cd ./prime-router && ./gradlew ktlintApplyToIdea`
    * DO NOT install the IntelliJ KTLint plugin or any other KTLint tool as the rule versions may conflict.
* [Microsoft VSCode](https://code.visualstudio.com/Download) with the available Kotlin extension
* [Java Profiling in ReportStream](./java-profiling.md)
* [Tips for faster development](./faster-development.md)

# Miscellaneous subjects

## Using different database credentials than the default

In cases where you want to change which credentials are used by any of our tooling to connect to the PostgreSQL database, you can do so by specifying some environment variables or specifying project properties to the build. The order of precendence of evaluation is: `Project Property beats Environment Variable beats Default`. You can specify the following variables:

* `DB_USER`: PostgreSQL database username (defaults to `prime`)
* `DB_PASSWORD`: PostgreSQL database password (defaults to `changeIT!`)
* `DB_URL`: PostgreSQL database URL (defaults to \
  `jdbc:postgresql://localhost:5432/prime_data_hub`)

Example:

```bash
 # exported environment variable DB_URL
export DB_URL=jdbc:postgresql://postgresql:5432/prime_data_hub
# Command-level environment variable (DB_PASSWORD)
# combined with project property (DB_USER)
DB_PASSWORD=mypassword ./gradlew testEnd2End -PDB_USER=prime
```

Alternatively, you can specify values for project properties via environment variables per the Gradle project properties environment `ORG_GRADLE_PROJECT_<property>`:

```bash
export ORG_GRADLE_PROJECT_DB_USER=prime
export ORG_GRADLE_PROJECT_DB_PASSWORD=mypass
./gradlew testEnd2End -PDB_URL=...
```

## Using local configuration for organizations.yml

By default, the functions will pull their configuration for organizations from the `organizations.yml` file. This can be overridden locally or in test by declaring an environment variable `PRIME_ENVIRONMENT` to specify the suffix of the yml file to use. This enables setting up local SFTP routing without impacting the 'production' `organizations.yml` configuration file.

```bash
# use organizations-mylocal.yml instead
export PRIME_ENVIRONMENT=mylocal

# use organizations-foo.yml instead
export PRIME_ENVIRONMENT=foo
```

## `PRIME_DATA_HUB_INSECURE_SSL` environment variable

When building the ReportStream container, you can set this value to `true` to enable insecure SSL:

```bash
PRIME_DATA_HUB_INSECURE_SSL=true docker compose build
```

## `ETOR_TI_baseurl` service setup

> WARNING: This project has been paused. This documentation remains in case it is brought back.

To run the service associated with this environment variable locally, please visit the instructions located 
[at the intermediary project](https://github.com/CDCgov/trusted-intermediary/blob/main/README.md)

# Troubleshooting
## Local SFTP Issues
1. SFTP Upload Permission denied - If you get a Permission Denied exception in the logs then it is most likely the atmoz/sftp
   Docker container has the incorrect permissions for the folder used by the local SFTP server.

`FAILED Sftp upload of inputReportId xxxx to SFTPTransportType(...) (orgService = ignore.HL7), Exception: Permission denied`

Run the following command to change the permissions for the folder:
```bash
docker exec -it prime-router_sftp_1 chmod 777 /home/foo/upload
```
