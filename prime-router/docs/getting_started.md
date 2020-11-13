# Developers Getting Started

## Setting up your Mac developer machine

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

Clone the project 
```
git clone https://github.com/CDCgov/prime-data-hub.git
```

Change the working directory to the `prime-router` directory. 
```
cd <your_path>/prime-router
```

Install the [Docker Desktop](https://www.docker.com/get-started) or the equivalent. 

Use any other tools that you want to develop the code. Be productive. Modify this document if you have a practice that will be useful. 

Some useful tools for Kotlin/Java development include:
- [Microsoft VSCode](https://code.visualstudio.com/Download)
- [JetBrains IntelliJ](https://www.jetbrains.com/idea/download/#section=mac) 

A useful Azure tool to examine Azurite and Azure storage is (Storage Explorer)[https://azure.microsoft.com/en-us/features/storage-explorer/] from Microsoft. 

## Function Development with Docker Compose

The project's [README](../readme.md) file contains some steps to use the PRIME router in a CLI. However, for the POC app and most other users of the PRIME router will the router in the Microsoft Azure cloud. When hosted in Azure, the PRIME router uses Docker containers. The `DockerFile` describes how to build this container. 

Developers can also run the router locally with the same Azure runtime and libraries to help develop and debug Azure code. In this case, a developer can use a local Azure storage emulator, called Azurite.

To orchestrate running the Azure function code and Azurite, Docker Compose is a useful tool. After installing or the equivalent, build the project using `Maven` and then run the project in Docker containers using `docker-compose.`
```
mvn clean package  
docker-compose up
```
Docker-compose will build a `prime_dev` container with the output of the `mvn package` command and launch an Azurite container. The first time you run this command, it builds a whole new image, which may take a while. However, after the first time `docker-compse` is run, `docker-compose` should start up in a few seconds. The output should look like:

![Docker Compose](assets/docker_compose_log.png)

Looking at the log above, you may notice that container has a debug open at port `5005`. This configuration allows you to attach a Java debugger to debug your code.

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
docker build --tag rhawesprimedevregistry.azurecr.io/prime-data-hub . 
docker push rhawesprimedevregistry.azurecr.io/prime-data-hub 
```