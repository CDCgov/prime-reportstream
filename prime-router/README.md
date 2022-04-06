# PRIME ReportStream

**General disclaimer** This repository was created for use by CDC programs to collaborate on public health related projects in support of the [CDC mission](https://www.cdc.gov/about/organization/mission.htm).  GitHub is not hosted by the CDC, but is a third party website used by CDC and its partners to share information and collaborate on software. CDC use of GitHub does not imply an endorsement of any one particular service, product, or enterprise.

## Overview

The PRIME ReportStream project is the part of the Pandemic Ready Interoperable Modernization Effort that works with state and local public health departments. 
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

## Learn More
To continue the developer orientation, please read
- [Contributing](../contributing.md) to see the contributions rules for the project
- [Getting Started](docs/getting-started/getting-started.md) to continue the developer machine setup
- [Release Notes](docs/release-notes.md)
