# Developers Getting Started

## Setting up your Mac or Linux developer machine

First, set up Java 11 and Maven by opening up a Terminal session and
entering:
```
brew install openjdk@11
brew install maven
```

Install Azure tools
```
brew update && brew install azure-cli
brew tap azure/functions
brew install azure-functions-core-tools@3
```

Install the [Docker Desktop](https://www.docker.com/get-started) or the equivalent. 

Clone the project 
```
git clone https://github.com/CDCgov/prime-data-hub.git
```

Change the working directory to the `prime-router` directory. 
```
cd <your_path>/prime-router
```

### Dependencies

#### PostgreSQL

You need a running local PostgreSQL database to **compile** the project.

##### Option 1: PostgreSQL via Brew

One way is to use [brew](https://brew.sh) again to get this database. (Mac or Linux)
```
brew install postgresql@11
brew install flyway
brew services start postgresql@11

Add /usr/local/opt/postgresql@11/bin to your PATH 

# Run createuser.  When prompted set the password as changeIT!
createuser -P prime 
createdb --owner=prime prime_data_hub
```

##### Option 2: PostgreSQL via `apt` on Ubuntu/Debian

Installing PostgreSQL and Flyway on Ubuntu
```
sudo apt install postgresql
cd /usr/local/lib
wget -qO- https://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/7.3.0/flyway-commandline-7.3.0-linux-x64.tar.gz | tar xvz && sudo ln -s `pwd`/flyway-7.3.0/flyway /usr/local/bin

# Run createuser - when prompted set the password as changeIT!
sudo -u postgres createuser -P prime
sudo -u postgres createdb --owner=prime prime_data_hub
```

##### Option 3: PostgreSQL via Docker

In [`devenv-infrastructure.sh`](../devenv-infrastructure.sh)
```sh
docker-compose -f ./docker-prime-infra.yml up --detach
```

If you need need Flyway, you can install it via `apt` or `brew` as above.



### Compiling

You should be able to compile the project now. Check if it works. 

```
mvn clean package
```

If you see any SSL errors during this step, follow the directions in [Getting Around SSL Errors](#getting-around-ssl-errors).

Check out the database if you like:
```
psql prime_data_hub
    select * from task;
```

There are a few database commands that are helpful while developing.
```
# drop and recreate the local database
dropdb prime_data_hub # to trash your local database
createdb --owner=prime prime_data_hub

# migrate the local database by hand
flyway -user=prime -password=change1T! -url=jdbc:postgresql://localhost:5432/prime_data_hub -locations=filesystem:./src/main/resources/db/migrate migrate
```

Use any other tools that you want to develop the code. Be productive. Modify this document if you have a practice that will be useful. 

Some useful tools for Kotlin/Java development include:
- [KTLint](https://ktlint.github.io/) the Kotlin linter that we use to format our KT code
- [Microsoft VSCode](https://code.visualstudio.com/Download) 
- [JetBrains IntelliJ](https://www.jetbrains.com/idea/download/#section=mac) 

If you are using IntelliJ, you can configure it to follow standard Kotlin conventions by
```
cd prime_router
brew install ktlint
ktlint applyToIDEAProject
```


A useful Azure tool to examine Azurite and Azure storage is (Storage Explorer)[https://azure.microsoft.com/en-us/features/storage-explorer/] from Microsoft. 

## Function Development with Docker Compose

The project's [README](../readme.md) file contains some steps to use the PRIME router in a CLI. However, for the POC app and most other users of the PRIME router will the router in the Microsoft Azure cloud. When hosted in Azure, the PRIME router uses Docker containers. The `DockerFile` describes how to build this container. 

Developers can also run the router locally with the same Azure runtime and libraries to help develop and debug Azure code. In this case, a developer can use a local Azure storage emulator, called Azurite.

To orchestrate running the Azure function code and Azurite, Docker Compose is a useful tool. After installing or the equivalent, build the project using `Maven` and then run the project in Docker containers using `docker-compose.`
```
mvn clean package  
docker-compose up
```
Docker-compose will build a `prime_dev` container with the output of the `mvn package` command and launch an Azurite container. The first time you run this command, it builds a whole new image, which may take a while. However, after the first time `docker-compose` is run, `docker-compose` should start up in a few seconds. The output should look like:

![Docker Compose](assets/docker_compose_log.png)

Looking at the log above, you may notice that container has a debug open at port `5005`. This configuration allows you to attach a Java debugger to debug your code.

If you see any SSL errors during this step, follow the directions in [Getting Around SSL Errors](#getting-around-ssl-errors).

## Setup Azure to deploy your locally built container

Each developer is given an Azure resource group in the project's Azure subscription. 
The group's name follows the pattern of `prime-dev-<developer_name>`. 
Use this resource group for your experiments and development. 
Note: please shut stuff down after you are done to avoid running up a bill. 

This section will show you how to set up a simple pipeline to build a container and deploy it to Azure semi-automatically. 
This diagram illustrates the concepts: a local build, which is pushed to a private container registry, and then deployed automatically to an Azure function in the developer's resource group. 

![deploy_pipeline](assets/deploy_pipeline.png)

To set up the pipeline, first, login to Azure
```
az login
```
Next, set a `PRIME_DEV_NAME` environment variable with your dev name used in your resource group. 
This name is usually your first name's intial and your last name. 
I recommend setting this variable in your shell's profile, so you do not have to set it by hand. 
```
export PRIME_DEV_NAME rhawes
```
Now you are ready to create a deploy pipeline. To help you get going, we've created the `setup_resource_group.sh` script. 
This script will take you step-by-step through the process of setting up your private container registry and building a container. 
This [article](https://docs.microsoft.com/en-us/azure/azure-functions/functions-create-function-linux-custom-image?tabs=bash%2Cportal&pivots=programming-language-java) from Microsoft describes the process. 
The article is long but goes through the process in detail. Note: you can stop the script at any point and just rerun the script from the beginning. 
```
bash setup_resource_group.sh
```
Now you can develop, build and push your Docker containers to your resource group in Azure. 
```
az acr login --name rhawesprimedevregistry
docker build --tag rhawesprimedevregistry.azurecr.io/prime-data-hub . 
docker push rhawesprimedevregistry.azurecr.io/prime-data-hub 
```

## Using local configuration for organizations.yml

By default, the functions will pull their configuration for organizations from the `organizations.yml` file.  You can override this locally or in test by declaring an environment variable `PRIME_ENVIRONMENT`.  If you declare something like, `export PRIME_ENVIRONMENT=mylocal` then the system will look for a configuration file `organizations-mylocal.yml` and will use that, even if the `organizations.yml` file exists.  In this way, you can set up local SFTP routing, etc. without impacting the production (`organizations.yml`) config.  Note that depending on the OS - case matters.

## Getting Around SSL Errors

If your agency's network intercepts SSL requests, you might have to disable SSL verifications to get around invalid certificate errors.

### Maven Builds

For Maven builds, you can add the parameter `-Dmaven.wagon.http.ssl.insecure=true` as follows:

```bash
mvn clean package -Dmaven.wagon.http.ssl.insecure=true
```

If you want to permanently set this, add the following to your `.bash_profile`:

```bash
export MAVEN_OPTS="-Dmaven.wagon.http.ssl.insecure=true $MAVEN_OPTS"
```

### Docker Builds

This can be accomplished by setting an environment variable `PRIME_DATA_HUB_INSECURE_SSL=true`. You can pass this in as a one-off when you build a component, for example:

```bash
PRIME_DATA_HUB_INSECURE_SSL=true docker-compose up
```

Or you can add this line in your `~/.bash_profile` to ensure your local builds will always disable SSL verification:

```bash
export PRIME_DATA_HUB_INSECURE_SSL=true
```

## Managing the local Hashicorp Vault secrets database

Our `docker-compose.yml` includes Hashicorp Vault alongside our other containers to enable local secrets storage. Under normal circumstances, developers will not have to interact directly with the Vault configuration, but some helpful guidance is provided below for troubleshooting.

### Initialize the Vault

When starting up our containers with `docker-compose up` on first-run, the container will create a new Vault database and store the following files in `.vault/env`:

* `key` - unseal key for decrypting the database
* `.env.local` - the root token in envfile format for using the Vault api / command line

The database is stored in a docker-compose container `vault` that persists across up and down events. All files are excluded in `.gitignore` and should never be persisted to source control.

## Re-initialize the Vault

If you would like to start with a fresh Vault database, you can clear the Vault database with the following commands:

```bash
cd prime_router
docker-compose down -v
rm -rf .vault/env/{key,.env.local}
```

Note: The `docker-compose down -v` option deletes all volumes associated with our docker-compose file.

## Using the Vault locally

Our `docker-compose.yml` will automatically load the environment variables needed for the Vault. If you need to use the Vault outside Docker, you can find the environment variables you need in:

```
.vault/env/.env.local
```

They can automatically be loaded in most IDEs:
- IntelliJ: https://plugins.jetbrains.com/plugin/7861-envfile
- VSCode: https://dev.to/andreasbergstrom/placeholder-post-1klo

Alternatively, inject them in your terminal with (useful for using the CLI):

```bash
export $(cat ./.vault/env/.env.local | xargs)
```