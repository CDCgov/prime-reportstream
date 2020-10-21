# CDCgov GitHub Organization Open Source Project Template

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

Our current goal is building a prototype that takes fake data form the POC app and sends it to a public health department. 
Features of the prototype include:

- Ability to route standard CSV from AZ and FL
- Ability to convert CSV to HL7 message
- Ability to send to an SFTP folder

The full feature set is kept in the repositories project folder. 

## Running a demo

First, setup Java11, Maven, ...
1. `brew install java11`
2. `brew install maven`

Next, build
1. `mvn clean package`

Finally, run the router
`prime --input_schema=sample/phd1-covid-19 --input=result_files/lab1-test_results-17-42-31.csv --route --output_dir=routed_files`