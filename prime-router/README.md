# PRIME Data Hub

**General disclaimer** This repository was created for use by CDC programs to collaborate on public health related projects in support of the [CDC mission](https://www.cdc.gov/about/organization/mission.htm).  GitHub is not hosted by the CDC, but is a third party website used by CDC and its partners to share information and collaborate on software. CDC use of GitHub does not imply an endorsement of any one particular service, product, or enterprise. 

## Overview

The PRIME data hub project is the part of the Pandemic Ready Interoperable Modernization Effort that works with state and local public health departments. 
The project is a joint effort between the CDC and USDS. 
Currently, we are focusing on the problem of delivering COVID-19 test data to public health departments. 
Later, we will work on other tools to analyze and explore this data and different types of health data.  

Other PRIME repositories include
- [PRIME-Central](https://github.com/CDCgov/prime-central): a place we keep common files and documents
- [PRIME-Data-Input-Client](https://github.com/CDCgov/prime-data-input-client): The POC COVID-19 test data input application that will use the data router

## Current Status

Our current goal is to support data from the POC app and sends it to a public health department. 
Features include:

- Ability to route standard CSV from the POC app
- Translate the POC CSV into CSV accepted by PHDs
- Ability to convert to HL7 messages
- Ability to send to an SFTP folder

The full feature set is kept in the repositories project folder. 

## Running a Demo in a Command Line

One a Mac with [Homebrew](https://brew.sh/) installed. 

First, setup Java 11 and Maven
```
brew install openjdk@11
brew install maven
```

Next, build the project
```
mvn clean package
```

Run the router in the prime_router directory using the command-line interface.
```
mkdir routed_files
./prime --input_schema=sample/phd1-sample --input=src/test/unit_test_files/lab1-test_results-17-42-31.csv --route --output_dir=routed_files
```

Create a set of 20 PrimeDataInput results with fake values

 ```
 mkdir result_files
 ./prime --input_schema=primedatainput/pdi-covid-19 --input_fake 20 --output_dir=result_files
 ```

Route the results from a PDI CSV file to the files for specific public health departments specified the `receivers.yml` file

```
mkdir routed_files
./prime --input_schema=PrimeDataInput/pdi-covid-19 --input=result_files/fake-pdi-covid-19.csv --route --output_dir=routed_files
```
## Azure Function Development with Docker Compose
An Azure Function can also contain the PRIME router. This configuration is how the POC app will use the PRIME router. When hosted in Azure, the PRIME router uses Docker containers. The `DockerFile` describes how to build this container. 

Developers can also run the router locally with the same Azure runtime and libraries to help develop and debug Azure code. In this case, a developer can use a local Azure storage emulator, called Azurite.

Install Azure tools on a Mac. 
```
brew update && brew install azure-cli
brew tap azure/functions
brew install azure-functions-core-tools@3

```

To orchestrate running the Azure function code and Azurite, Docker Compose is a useful tool. After installing [Docker Desktop](https://www.docker.com/get-started) or the equivalent, build the project using `Maven` and then run the project in Docker containers using `docker-compose.`
```
mvn clean package 
docker-compose up
```
Docker-compose will build a `prime_dev` container with the output of the `mvn package` command as well as launch an Azurite container. The first time this command is run, it builds a whole new image, which may take a while. However, after the first time `docker-compse` is run, `docker-compose` should start up in few seconds. The output should look like.

![Docker Compose](docs/assets/docker_compose_log.png)

Looking at the log above, you may notice that container has a debug open at port `5005`. This configuration allows you to attach a Java debugger (IntelliJ, VSCode, or ...) to debug your code. 

