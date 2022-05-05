# Programmer’s Guide for Organizations and Testing Facilities

VERSION 2 .1 – MARCH 2022

## Contents

## Introduction
ReportStream is a free, open-source data platform that makes it easy for public health
data to be transferred from testing facilities to public health departments.

This programmer’s guide enables those who are writing automated systems and tools to
send laboratory and other health-related data to local, state, and federal jurisdictions. It
helps you, the technical user at the testing facility or sending location, learn how to send
data using the ReportStream Restful (REST) API.

Examples in this guide use curl commands for simplicity with the assumption you’ll be
coding these calls into your sending system. You can also use a program like Postman
to test submissions.

The **Waters API**—the primary secure entry point to ReportStream—is named in memory of Dr.
Michael Stephan Waters (1973-2020) whose tireless work at the U.S. Food and Drug
Administration championed diagnostic data interoperability efforts nationwide. ReportStream
honors Dr. Waters through continuation and elevation of his work.

## Release Notes
You can find ReportStream release notes here:
https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/release-notes.md

## Onboarding Process Summary

![USDS ReportStream Architecture!](/assets/rs-diagram.svg "USDS ReportStream Architecture")

The above diagram represents a high-level outline of the steps involved in a typical
ReportStream interaction for organizations and testing facilities.

## Onboarding Steps
### Step 1: Sample Data
You’ll share artificially created data (“fake data”)/non-PII example data with the
ReportStream team via email. Currently, ReportStream can accept either a CSV file or
HL7 input data. We’ll work together to help you use one of our existing standard data
models or derive new data models as needed. We’ll provide detailed documentation
for expected data types and values in your data model, as well as fake data or
synthetic data using that model, if needed.


## Sending to ReportStream
There are two methods of authenticating to ReportStream’s REST API token-based
authentication with a public/private key pair and using a shared secret API key. Token-based
authentication is recommended best practice.

The examples below assume a ReportStream client “healthy-labs” and submit the payload
contained in the file ./healthy-labs-nonPII-data.csv (or .hl7). As part of the onboarding process, the ReportStream team will pre-configure ReportStream with your client information and give you a unique client-id. The client configuration tells ReportStream what type of data to expect
for that client-id. ReportStream will look up the associated data model and format (CSV, HL7),
and validate the attached payload.
In the examples, data are submitted via an HTTP POST to the ReportStream Staging (test)
system "reports" endpoint. The data submitted are sent as the payload of the POST, as is, with
no changes.

*Example: Token-based authentication with public/private key pair*
This method uses FHIR style authentication. Prior to connecting to the endpoint, you’ll need a
public/private keypair. The steps below show how to create a key pair using OpenSSL.

EC
```bash
  openssl ecparam -genkey -name secp384r1 -noout -out my-es-keypair.pem
  openssl ec -in my-es-keypair.pem -pubout -out my-es-public-key.pem
```

RSA
```bash
  openssl genrsa -out my-rsa-keypair.pem 2048
  openssl rsa -in my-rsa-keypair.pem -outform PEM -pubout -out my-rsa-public-key.pem
```

Send the public key to the ReportStream team (they’ll associate it with y


## Appendix A: Fields List for a Typical Covid-19
### Submission

**API CSV AND HL7 FIELD REQUIREMENTS**

Legend:
* “Yes” means this is a required field for acceptance
* “Yes – Conditional” means this field is required, but only under certain circumstances. Review the field’s
Data Requirements and Additional Guidance for more information.
* “Requested” means that this field should be populated if available. In addition, some states may treat
this as a required field.
* “No” means that this field is not a hard requirement. In the interest of providing complete information to
public health agencies, please populate the field if data is available.

Special notes:
* Two of the most important and often overlooked pieces of required data are the deviceIdentifier
(OBX-17.1) and testPerformed (OBX-3.1). These fields must match exactly to the appropriate row in the
LOINC In Vitro Diagnostic (LIVD) test code mapping. The most updated mapping can be found here.
Specifics about each field are detailed below.
* The preferred timestamp formatting for CSV and HL7 is yyyyMMddhhmmss+/-zzzz. If the UTC offset
(+/-zzzz) is not present, results should be normalized to a single time zone, agreed to during the
onboarding process.

**Patient Data Elements**
| CSV Column Names | HL7 Field / Component | Fed Required? | State Required? | Data Requirements  | Additional Guidance |
| ------------- | ------------- | ------------- | ------------- | ------------- | ------------- |
| patientUniqueId | PID-3.1  | No | Requested  | Optional - Requested: Enter unique patient identifier. This is typically the Medical Record Number. **_Do not send a SocialSecurity Number._** | This value is optional and can be left blank if no information is provided. Some jurisdictions may require this field, ReportStream will notify you if this is the case.  |
| patientNameLast | PID-5.1  | No | Yes | Enter patient's last name.  | File will fail if field left blank.  |
| patientNameFirst | PID-5.2  | No | Yes  | Enter patient's first name. | File will fail if field left blank.  |
| patientNameMiddle | PID-5.3  | No | No  | **Optional:** Enter patient's middle name, if known. | This value is optional and can be left blank if no information is provided.  |
