# Developer Getting Started Guide

This document will walk you through the setup instructions to get a functioning development environment.

# Table of contents
- [Table of contents](#table-of-contents)
- [Locally installed software prerequisites](#locally-installed-software-prerequisites)
- [Bulding the Baseline](#bulding-the-baseline)
    * [First Build](#first-build)
    * [Build Dependencies](#build-dependencies)
- [Building the Baseline](#building-the-baseline)
- [Committing to this repository](#committing-to-this-repository)
    * [Git Hooks](#git-hooks)
        + [pre-commit: Gitleaks](#pre-commit-gitleaks)
    * [Updating schema documentation](#updating-schema-documentation)
- [Running ReportStream](#running-reportstream)
    * [Restarting After a Code Update](#restarting-after-a-code-update)
        + [Inspecting the Logs](#inspecting-the-logs)
        + [Debugging ReportStream](#debugging-reportstream)
    * [Finding misconfigurations](#finding-misconfigurations)
    * [Getting around SSL errors](#getting-around-ssl-errors)
- [Function development with docker-compose](#function-development-with-docker-compose)
    * [Running ReportStream locally](#running-reportstream-locally)
- [How to use the CLI](#how-to-use-the-cli)
- [Credentials and secrets vault](#credentials-and-secrets-vault)
    * [Initializing the vault](#initializing-the-vault)
    * [Re-initializing the vault](#re-initializing-the-vault)
    * [Using the vault locally](#using-the-vault-locally)
- [Testing](#testing)
    * [Running the unit tests](#running-the-unit-tests)
    * [Data conversion quick test](#data-conversion-quick-test)
    * [Running the end-to-end tests](#running-the-end-to-end-tests)
- [Resetting your environment](#resetting-your-environment)
    * [Resetting the Database](#resetting-the-database)
- [Additional tooling](#additional-tooling)
- [Miscellaneous subjects](#micellaneous-subjects)
    * [Using different database credentials than the default](#using-different-database-credentials-than-the-default)
    * [Using local configuration for organizations.yml](#using-local-configuration-for-organizationsyml)
    * [`PRIME_DATA_HUB_INSECURE_SSL` environment variable](#-prime-data-hub-insecure-ssl--environment-variable)

# Locally installed software prerequisites

You will need to have at least the following pieces of software installed _locally_ in order to be able to build and/or debug this baseline:

* [git](install-git.md) including git-bash if you're on Windows
* [Docker or Docker Desktop](install-docker.md)
* [OpenJDK](install-openjdk.md) (currently targetting 11 through 15)
* [Azure Functions Core Tools](install-afct.md) (currently targetting 4)

The following are optional tools that can aid you during development or debugging:

* [Azure Storage Explorer](https://docs.microsoft.com/en-us/azure/vs-azure-tools-storage-manage-with-storage-explorer)
* [AzureCLI](install-azurecli.md)
* [Gradle](install-gradle.md)
* One or more [PostgreSQL Clients](psql-clients.md)

# Bulding the Baseline

## First Build

1. [Clone the prime-reportstream repository](https://docs.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository-from-github/cloning-a-repository)
   to your workstation using git.

1. If you are using Docker Desktop, verify that it is running prior to building or running ReportStream locally.

1. Initialize your environment and run an initial build by running the following command using a Linux shell.
   Note you can run `cleanslate.sh` script to reset your environment as well (run `./cleanslate.sh --help` for more
   information). The `cleanslate.sh` script not only cleans, but also performs a first build and setup of all artifacts
   needed for the build

```bash
cd ./prime-router
./cleanslate.sh
```

> Note: If you are working on an Apple Silicon Mac, stop here at this step and 
> continue on with the instructions in [Using an Apple Silicon Mac](Using-an-apple-silicon-mac.md).

## Build Dependencies
1. If you are using Docker Desktop, verify that it is running prior to building or running ReportStream locally.
1. Building and running ReportStream requires a locally accessible PostgreSQL database instance that is initially setup
   and run by the `cleanslate.sh` script.  This database instance runs as a Docker container defined by the
   `docker-compose.build.yml` file.  You will need to start this database instance upon a workstation reboot by
   using the following command:

```bash
cd ./prime-router
docker-compose --file "docker-compose.build.yml" up --detach
```

# Building the Baseline

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

# Committing to this repository

* Commits _must_ be signed or will not be mergeable into `master` or `production` without Repository Administrator intervention. You can find detailed instructions on how to set this up in the [Signing Commits](../signing-commits.md) document.
* You will also need to connect to GitHub with an SSH key. You can find instructions on generating and adding an SSH key [here](https://docs.github.com/en/authentication/connecting-to-github-with-ssh).
* Make your changes in topic/feature branches and file a [new Pull Request](https://github.com/CDCgov/prime-reportstream/pulls) to merge your changes in to your desired target branch.

## Git Hooks

We make use of git hooks in this repository and rely on them for certain levels of protections against CI/CD failures and other incidents. Install/activate these hooks by invoking either `prime-router/cleanslate.sh` or by directly invoking `.environment/githooks.sh install`. This is a _repository-level_ setting, you _must_ activate the git hooks in every clone on every device you have.

### pre-commit: Docker

The first hook we'll invoke is to ensure Docker is running. If it's not we'll short-circuit the remainder of the hooks and let you know why.


### pre-commit: Gitleaks

Gitleaks is one of the checks that are run as part of the `pre-commit` hook. It must pass successfully for the commit to proceed (i.e. for the commit to actually happen, failure will prevent the commit from being made and will leave your staged files in staged status). Gitleaks scans files that are marked as "staged" (i.e. `git add`) for known patterns of secrets or keys.

The output of this tool consists of 2 files, both in the root of your repository, which can be inspected for more information about the check:
* `gitleaks.report.json`: the details about any leaks it finds, serialized as JSON. If no leaks are found, this file contains the literal "`null`"; if leaks are found, then this file will contain an array of found leak candidates.
* `gitleaks.log`: the simplified logging output of the gitleaks tool

When gitleaks reports leaks/violations, the right course of action is typically to remove the leak and replace it with a value that is collected at run-time. There are limited cases where the leak is a false positive, in which case a _strict and narrow_ exemption may be added to the `.environment/gitleaks/gitleaks-config.toml` configuration file. _If an exemption is added, it must be signed off on by a member of the DevOps team_.

This tool can also be manually invoked through `.environment/gitleaks/run-gitleaks.sh` which may be useful to validate the lack of leaks without the need of risking a commit. Invoke the tool with `--help` to find out more about its different run modes.

See [Allow-listing Gitleaks False Positives](../allowlist-gitleaks-false-positives.md) for more details on how to prevent False Positives!

### pre-commit: Terraform formatting

If you've changed any terraform files in your commit we'll run
`terraform fmt -check` against the directory of files. If any file's format is invalid 
the pre-commit hook will fail. You may be able to fix the issues with:

```bash
$ terraform fmt -recursive
```

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
docker-compose restart prime_dev
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

# How to use the CLI

The PRIME command line interface allows you to interact with certain parts of report stream functionality without using the API or running all of ReportStream.  A common use case for the CLI is testing while developing mappers for the new FHIR pipeline.

The primary way to access the cli is through the gradle command (although a deprecated bash script exists as well). If you are an IntelliJ user, you can set up the gradle command to be run through your IDE and be run in debug mode to step through your code line by line.
```bash
cd ./prime-router
# Prints out all the available commands
./gradlew primeCLI
#  data                      process data
#  list                      list known schemas, senders, and receivers
#  livd-table-download       This updates the LIVD lookup table with a new version.
#  generate-docs             generate documentation for schemas
#  create-credential         create credential JSON or persist to store
#  compare                   compares two CSV files so you can view the
#                            differences within them
#  test                      Run tests of the Router functions
#  login                     Login to the HHS-PRIME authorization service
#  logout                    Logout of the HHS-PRIME authorization service
#  organization              Fetch and update settings for an organization
#  sender                    Fetch and update settings for a sender
#  receiver                  Fetch and update settings for a receiver
#  multiple-settings         Fetch and update multiple settings
#  lookuptables              Manage lookup tables
#  convert-file
#  sender-files              For a specified report, trace each item's ancestry
#                            and retrieve the source files submitted by
#                            senders.
#  fhirdata                  Process data into/from FHIR
#  fhirpath                  Input FHIR paths to be resolved using the input
#                            FHIR bundle
#  convert-valuesets-to-csv  This is a development tool that converts
#                            sender-automation.valuesets to two CSV files

# Converts HL7 to FHIR (IN DEV MODE)
./gradlew primeCLI --args='fhirdata --input-file "src/testIntegration/resources/datatests/HL7_to_FHIR/sample_co_1_20220518-0001.hl7"'

# Converts the FHIR file to HL7 using the provided schema (IN DEV MODE)
./gradlew primeCLI --args='fhirdata --input-file "src/testIntegration/resources/datatests/HL7_to_FHIR/sample_co_1_20220518-0001.fhir" -s metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml'
```

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


## Running the end-to-end tests locally

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
    or 
    ```bash
    ./prime test --run end2end
    ```
    Or to run the entire smoke test suite locally:
    ```
    ./prime test
    ```

Upon completion, the process should report success.

## Running the end2end test on Staging

To run the end2end test on Staging you'll need a `<postgres-user>` and `<postgres-password>`, VPN tunnel access, and a `<reports-endpoint-function-key>` 

With your VPN running, do the following:
    
```    
export POSTGRES_PASSWORD=<postgres-password>
export POSTGRES_USER= <postgres-user>@pdhstaging-pgsql                                                                                                          
export POSTGRES_URL=jdbc:postgresql://pdhstaging-pgsql.postgres.database.azure.com:5432/prime_data_hub
./prime test --run end2end --env staging --key <reports-endpoint-function-key> 
```    

To run the entire smoke test suite on Staging use this:

```
    ./prime test -env staging --key <reports-endpoint-function-key>
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
    docker-compose down
    ```
1. Run the following command to delete all ReportStream related tables from the database and recreate them.  This
is very useful to reset your database to a clean state.  Note that the database will be re-populated the
next time you run ReportStream using docker-compose up.
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
PRIME_DATA_HUB_INSECURE_SSL=true docker-compose build
```

# Troubleshooting
## Local SFTP Issues
1. SFTP Upload Permission denied - If you get a Permission Denied exception in the logs then it is most likely the atmoz/sftp
   Docker container has the incorrect permissions for the folder used by the local SFTP server.

`FAILED Sftp upload of inputReportId xxxx to SFTPTransportType(...) (orgService = ignore.HL7), Exception: Permission denied`

Run the following command to change the permissions for the folder:
```bash
docker exec -it prime-router_sftp_1 chmod 777 /home/foo/upload
```
