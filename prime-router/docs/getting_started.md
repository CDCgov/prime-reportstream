% Developer Getting Started Guide

This document will walk you through the setup instructions to get a functioning development environment from first clone to completion of your first run of the end-to-end tests.

# Table of contents

* [Locally installed software prerequisites](#locally-installed-software-prerequisites)
* [First build](#first-build)
    * [Build dependencies](#build-dependencies)
* [Committing to this repository](#committing-to-this-repository)
    * [Git Hooks](#git-hooks)
* [Building in the course of development](#building-in-the-course-of-development)
    * [Updating schema documentation](#updating-schema-documentation)
* [Running ReportStream](#running-reportstream)
    * [Inspecting Logs](#inspecting-logs)
    * [Debugging ReportStream](#debugging-reportstream)
    * [Finding misconfigurations](#finding-misconfigurations)
    * [Getting around SSL errors](#getting-around-ssl-errors)
* [Function development with docker-compose](#function-development-with-docker-compose)
    * [Running ReportStream locally](#running-reportstream-locally)
* [Credentials and secrets vault](#credentials-and-secrets-vault)
    * [Initializing the vault](#initializing-the-vault)
    * [Re-initializing the vault](#re-initializing-the-vault)
    * [Using the vault locally](#using-the-vault-locally)
* [Testing](#testing)
    * [Running the unit tests](#running-the-unit-tests)
    * [Data conversion quick test](#data-conversion-quick-test)
    * [Running the end-to-end tests](#running-the-end-to-end-tests)
* [Resetting your environment](#resetting-your-environment)
    * [Resetting just your database](#resetting-just-your-database)
* [Additional tooling](#additional-tooling)
* [Miscelanious subjects](#miscelanious-subjects)
    * [Using different database credentials than the default](#using-different-database-credentials-than-the-default)
    * [Using local configuration for organizations.yml](#using-local-configuration-for-organizations.yml)
    * [PRIME_DATA_HUB_INSECURE_SSL environment variable](#prime_data_hub_insecure_ssl-environment-variable)

# Locally installed software prerequisites

You will need to have at least the following pieces of software installed _locally_ in order to be able to build and/or debug the product:

* [git](getting-started/install-git.md) including git-bash if you're on Windows
* [Docker](getting-started/install-docker.md)
* [OpenJDK](getting-started/install-openjdk.md) (currently targetting 11 through 15)
* [Azure Functions Core Tools](getting-started/install-afct.md) (currently targetting 3)

The following tools may be installed for development or debugging purposes:

* [Azure Storage Explorer](https://docs.microsoft.com/en-us/azure/vs-azure-tools-storage-manage-with-storage-explorer)
* [AzureCLI](getting-started/install-azurecli.md)
* [Gradle](getting-started/install-gradle.md)
* One or more [PostgreSQL Clients](getting-started/psql-clients.md)

# First build

After having installed all software dependencies, you should be able to successfully run your first build by executing:

```bash
cd ./prime-router
./cleanslate.sh
```
The `cleanslate.sh` script not only cleans, but also performs a first build and setup of all artifacts as well: it sets you up from clone to first-run in a single step. Upon successful completion of `./cleanslate.sh`, you should be able to run the instructions advertised the end of the script, i.e. loading your vault information as environment variables followed by running the end-to-end tests ('`$`' indicates your prompt):

```bash
$ cd ./prime-router
$ ./cleanslate.sh --instructions
Please run the following command to load your credentials and run the End-to-End tests:

    $ export $(xargs < .vault/env/.env.local)
    $ ./gradlew testEnd2End

# This loads the credentials to the vault as exported environment variables
$ export $(xargs < .vault/env/.env.local)
# Runs the end-to-end tests
$ ./gradlew testEnd2End
```

You can execute `./cleanslate.sh --help` for more information. If this is truly your first time building anything, you do not have to specify any arguments to the script.

## Build dependencies

The `./cleanslate.sh` script will bring up any build dependencies needed for you. But it is still important to understand what these are.

In order to successfully build, you will need to have a locally accessible PostgreSQL instance. **We highly recommend running this instance as a docker container** that is brought up through docker-compose. Without this locally accessible instance, your build will fail due to not being able to migrate your schema.

The `./prime-router` directory contains a docker-compose file(*) that will bring up a properly configured PostgreSQL instance for you. The `docker-compose.build.yml` file contains the default credentials that are provisioned as 'root' user (with equivalent powers as '`postgres`') in this PostgreSQL instance.

```bash
cd ./prime-router
# Note the --file argument!
docker-compose --file "docker-compose.build.yml" up --detach
```

Running this instance as a docker container enables you to easily clean it (and its data) up without affecting any other parts of your local system.

(*) _Note that this `docker-compose.build.yml` file contains other artifacts that are used in our CI/CD build pipeline. Specifically there is a '`builder`' service which will start and terminate on bringing this docker-compose environment. For all intents and purposes, you can ignore this service._

# Committing to this repository

* Commits _must_ be signed or will not be mergeable into `master` or `production` without Repository Administrator intervention. You can find detailed instructions on how to set this up in the [Signing Commits](signing-commits.md) document.
* Make your changes in topic/feature branches and file a [new Pull Request](https://github.com/CDCgov/prime-reportstream/pulls) to merge your changes in to your desired target branch.

## Git Hooks

We make use of git hooks in this repository and rely on them for certain levels of protections against CI/CD failures and other incidents. Install/activate these hooks by invoking either `prime-router/cleanslate.sh` or by directly invoking `.environment/githooks.sh install`. This is a _repository-level_ setting, you _must_ activate the git hooks in every clone on every device you have.

### pre-commit: Gitleaks

Gitleaks is one of the checks that are run as part of the `pre-commit` hook. It must pass successfully for the commit to proceed (i.e. for the commit to actually happen, failure will prevent the commit from being made and will leave your staged files in staged status). Gitleaks scans files that are marked as "staged" (i.e. `git add`) for known patterns of secrets or keys.

The output of this tool consists of 2 files, both in the root of your repository, which can be inspected for more information about the check:
        * `gitleaks.report.json`: the details about any leaks it finds, serialized as JSON. If no leaks are found, this file contains the literal "`null`"; if leaks are found, then this file will contain an array of found leak candidates.
        * `gitleaks.log`: the simplified logging output of the gitleaks tool

When gitleaks reports leaks/violations, the right course of action is typically to remove the leak and replace it with a value that is collected at run-time. There are limited cases where the leak is a false positive, in which case a _strict and narrow_ exemption may be added to the `.environment/gitleaks/gitleaks-config.toml` configuration file. _If an exemption is added, it must be signed off on by a member of the DevOps team_.

This tool can also be manually invoked through `.environment/gitleaks/run-gitleaks.sh` which may be useful to validate the lack of leaks without the need of risking a commit. Invoke the tool with `--help` to find out more about its different run modes.

# Building in the course of development

You can invoke gradle from the `./prime-router` directory to build the product:

```bash
# if you have gradle (>=7.0) installed locally, use 'gradle' instead of './gradlew'
# Your most invoked build command
./gradlew package

# cleans up build artifacts (not as thorough as ./cleanslate.sh though)
./gradlew clean
```

Your most used gradle tasks will be:

* `clean`: deletes the build artifacts
* `compile`: compiles the code
* `migrate`: loads the database with the current schema
* `package`: packages the build artifacts for deployment
* `primeCLI`: run the prime CLI.  Specify arguments with `"--args=<args>"`
* `test`: runs the unit tests
* `testEnd2End`: runs the end to end tests; this requires [that you are running ReportStream](#running-reportstream)
* `testIntegration`: runs the integration tests; this requires [that you are running ReportStream](#running-reportstream)

If you see any SSL errors during this step, follow the directions in [Getting Around SSL Errors](#getting-around-ssl-errors).

## Updating schema documentation
Run the following gradle command to generate the schema documentation. This documentation is written to `docs/schema-documentation`

```bash
./gradlew generateDocs
```

The CI/CD pipeline checks for the need to update schema documentation and will fail the build if this documentation is lacking.

# Running ReportStream

You can bring up an entire ReportStream environment by running the command listed underneath. Note that this requires that you have successfully built in the past (see "[Building in the course of development](#building-in-the-course-of-development)" and/or "[First Build](#first-build)") as it tries to bring the ReportStream container itself up as well.

```bash
cd ./prime-router
./devenv-infrastructure.sh
```

If you have made modifications and would like to see these reflected in your running ReportStream, restart the service as follows:

```bash
cd ./prime-router
# restart just the prime_dev service (which is the one running ReportStream)
docker-compose restart prime_dev
```

The `devenv-infrastructure.sh` script can also be used to restart your _entire_ environment:

```bash
cd ./prime
./devenv-infrastructure.sh restart
```

Note that this script '_detaches_' from the docker containers it fires up.

## Inspecting logs

The docker containers produce logging output. When dealing with failures or bugs, it can be very useful to inspect this output. You can inspect the output of each container using ('`$`' indicates your prompt):

```bash
# List PRIME containers:
$ docker ps --format '{{.Names}}' | grep ^prime-router
prime-router_web_receiver_1
prime-router_prime_dev_1
prime-router_sftp_1
prime-router_redox_1
prime-router_azurite_1
prime-router_vault_1
prime-router_postgresql_1
# Show the log of (e.g.) prime-router_postgresql_1 until now
docker logs prime-router_postgresql_1
# Show the log output of (e.g.) prime-router-prime_dev_1 and stay on it
docker logs prime-router_prime_dev_1 --follow
```

## Debugging ReportStream

The '`prime_dev`' service from the [`docker-compose.yml`](../docker-compose.yml) file exposes a local port `5005` to which you can attach a Java debugger (i.e. `localhost:5005`).

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

The project's [README](../README.md) file contains some steps on how to use the PRIME router in a CLI. However, most uses of the PRIME router will be in the Microsoft Azure cloud. The router runs as a container in Azure. The [`DockerFile`](../Dockerfile) describes what goes in this container.

Developers can also run the router locally with the same Azure runtime and libraries to help develop and debug in an environment that mimics the Azure environment as closely as we can on your local machine. In this case, a developer can use a local Azure storage emulator, called Azurite.

We use docker-compose' to orchestrate running the Azure function(s) code and Azurite. See sections "[Running ReportStream](#running-reportstream)" for more information on building and bringing your environment up.

If you see any SSL errors during this step, follow the directions in [Getting Around SSL Errors](#getting-around-ssl-errors).

# Credentials and secrets vault

Our `docker-compose.yml` includes a Hashicorp Vault instance alongside our other containers to enable local secrets storage. Under normal circumstances, developers will not have to interact directly with the Vault configuration.

## Initializing the vault

NOTE: the cleanslate.sh script will set this up for you (see also "[First build](#first-build)" and "[Resetting your environment](#resetting-your-environment)").

Run the following commands to initialize vault:
```bash
mkdir -p .vault/env
cat /dev/null > .vault/env/.env.local
```

When starting up our containers with `docker-compose up` on first-run, the container will create a new Vault database and once initialized (which may take a couple of seconds) store the following files in `.vault/env`:

* `key`: unseal key for decrypting the database
* `.env.local`: the root token in envfile format for using the Vault api/command line

The database is stored in a docker-compose container `vault` which is persisted across up and down events. All files are excluded in `.gitignore` and should never be persisted to source control.

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
docker-compose down -v
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

# Testing

## Running the unit tests

The build will run the unit tests for you when you invoke `./gradlew package`. However, you may sometimes want to invoke them explicitly. Use this command to do run the Unit Tests manually:

```bash
cd ./prime-router
./gradlew test
# Or to force the tests to run
./gradlew test -Pforcetest
```

## Data conversion quick test

The quick test is meant to test the data conversion and generation code. Use this following command to run all quick tests, which you should do as part of a Pull Request:

```bash
./quick-test.sh all
```


## Running the end-to-end tests

End-to-end tests check if the deployed system is configured correctly. The tests use an organization called IGNORE for running the tests. In order to successfully run the end-to-end tests, you will need to:

1. Have built successfully
2. Export the vault's credentials

    ```bash
    cd ./prime-router
    export $(xargs < .vault/env/.env.local)
    ```

3. Create the SFTP credentials and upload organizations' settings

    ```bash
    cd ./prime-router
    ./prime create-credential --type=UserPass \
            --persist=DEFAULT-SFTP \
            --user foo \
            --pass pass
    ./prime multiple-settings \
            set --input settings/organizations.yml
    ```

4. Ensure that your docker containers are running (see also "[Running ReportStream](#running-reportstream)")

    ```bash
    cd ./prime-router
    # Specify restart if they are already running and you want
    # them to pick up new bianries
    # i.e. ./devenv-infrastructure.sh restart
    ./devenv-infrastructure.sh
    ```

5. Run the tests

    ```bash
    ./gradlew testEnd2End
    ```

Upon completion, the process should report success.


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

## Resetting just your database

You can also use [Flyway](https://flywaydb.org/) to reset your database:
```bash
# drop and recreate the database; you may be prompted for a password
export PGHOST=localhost
export PGUSER=prime
export PGDATABASE=prime_data_hub
dropdb --force ${PGDATABASE?}
createdb --owner=${PGUSER?} ${PGDATABASE?}

# migrate the local database by hand
flyway "-user=${PGUSER?}" -password=changeIT! \
    -url=jdbc:postgresql://${PGHOST?}:5432/${PGDATABASE?} \
    -locations=filesystem:./src/main/resources/db/migration migrate
```

# Additional tooling

Use any other tools that are accessible to you to develop the code. Be productive. Modify this document if you have a practice that will be useful.

Some useful tools for Kotlin/Java development include:

* [Azure Storage Explorer](https://azure.microsoft.com/en-us/features/storage-explorer/)
* [JetBrains IntelliJ](https://www.jetbrains.com/idea/download/)
* [KTLint](https://ktlint.github.io/): the Kotlin linter that we use to format our KT code
    * Install the [IntelliJ KLint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint-unofficial-) or configure it to follow standard Kotlin conventions as follows on a mac: `cd ./prime-router && brew install ktlint && ktlint applyToIDEAProject`
* [Microsoft VSCode](https://code.visualstudio.com/Download) with the available Kotlin extension

# Miscelanious subjects

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
PRIME_DATA_HUB_INSECURE_SSL=true docker-compose build
```