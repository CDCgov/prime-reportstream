# How to Onboard a New Organization to Send Data

Add subsections that mimic the linked resources: ([Brandon’s version](https://docs.google.com/document/d/1noB3lK2Nc_vbD4s5ZHgdTjgIjhCii63x_2bjBz7GM1I/edit#heading=h.be9yxi8thtdw), [Github version](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/how-to-onboard-a-sender.md))
Note for content writer: If there is overlap with the previous "Pipeline Configuration" section, hyperlink and reference the content instead of rewriting it..

## Sender Onboarding Process Overview

- Kickoff call
- [Answer Technical Questions](https://docs.google.com/spreadsheets/d/1iKYB6OmqXMFkwGp960EUU8Y78pWvwQRs1YYYhObsEKI/edit#gid=0)
- Get sample files(non-PII) from sender with different reportable conditions
- Sender validates messages using ReportStream validation tool (Not yet implemented)
- [Sender Configuration](#sender-configuration)
- [Test sample files through the Universal Pipeline](#testing)
- [Sender sends data to RS staging environment](#sending-data-to-reportstream)
- Conduct end-to-end test in staging environment

## Sender Configuration

### Get Ready

Create a new branch to store your work on the new sender.

### Set up a New Organization

Create organization .yml files in your working branch in:
- prime-router -> settings -> staging
Ensure the file begins with “---”.

Example:
```yaml
- name: yoyodyne
  description: Yoyodyne Propulsion Laboratories, the Future Starts Tomorrow!
  jurisdiction: FEDERAL
  senders:
    - name: default
      organizationName: yoyodyne
      topic: full-elr
      schemaName: metadata/fhir_transforms/senders/original-pipeline-transforms
      format: HL7
```

Use the following command to load the information from the .yml file into your local database

`./prime multiple-settings set –env local –input <file-location>`

A few things to note here:

- The name of the organization must be unique. It cannot share a name with a state or other organization
- The jurisdiction should be FEDERAL since they are not a STLT
- The organizationName under `senders` must match the name of the org above
- The format here is `FHIR`, though it is possible it could another format, such as `HL7`
- The schema name should use `metadata/fhir_transforms/senders/original-pipeline-transforms` by default
- Pay attention to the value in the topic field to make sure it is correct for your use case

### Set up a New Schema

**You should only create new schemas when an existing schema will not fit your use case. Please ensure that an
existing schema cannot be used prior to creating a new schema.**

Once you've added a setting file the sender you next need to create a schema file.

The schema provides additional transforms required so that the senders data can be routed correctly.

Note: When creating a transform, please reference [Changing/Updating Sender/Receiver Transforms](../../getting-started/standard-operating-procedures/changing-transforms.md) for guidance.

### Authenticating to ReportStream’s REST API
**Note: This is legacy authentication and should not be used for onboarding new users**

Shared secret key authorization

Prior to connecting to the endpoint, you’ll need a public/private keypair. There are many ways to do this. The steps below show how to create a key pair using `openssl`.

EC
```
openssl ecparam -genkey -name secp384r1 -noout -out my-es-keypair.pem
openssl ec -in my-es-keypair.pem -pubout -out  my-es-public-key.pem
```

RSA
```
openssl genrsa -out my-rsa-keypair.pem 2048
openssl rsa -in my-rsa-keypair.pem -outform PEM -pubout -out my-rsa-public-key.pem
```

Send the public key to the ReportStream team using [our public key tool](/manage-public-key).
Note: you’ll need to login to use that feature. If you do not have a login contact ReportStream support at [reportstream@cdc.gov](mailto:reportstream@cdc.gov). ReportStream will associate the key with your configuration within ReportStream.

You only need to do this step once, not every time you submit reports. If you need to change your keys at any time, contact ReportStream support.

## Testing

### Mapping sender codes to observation-mapping table

Prior to testing we need to make sure the codes that the sender will be sending are mapped in the observation-mapping table. In order to accomplish this we will need the list of LOINC/SNOMED order and result codes the sender will be sending (also known as a "compendium") in a CSV file. More info on this process can be found in the [mapping-sender-codes-to-condition document](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/sender-onboarding/mapping-sender-codes-to-condition.md).

### Note
In order to test a sender, you must create (or have in your possession) a file that matches the format
you have laid out in the schema, and that matches the format the sender will provide.  Also, it is
good to have at least one receiver that will match what the sender provides. For example, if Yoyodyne
is sending information for a lab in NJ, but you have no receivers set up to receive information
from NJ, then you will be unable to complete testing. The sender and receiver also need to have the same topic.

### Testing Locally
Assuming you have a receiver set up for your sender, follow the below steps to build and test your solution:

```
./git checkout “branch_name”
./gradlew clean package
```


Once that has completed successfully, the next step is to manually check the Universal Pipeline can process 
the message with no errors and not lose any data while converting it.

#### Testing sender transforms
```shell
./prime fhirdata --input-file "PATH-TO-SAMPLE-FILE.hl7" -s metadata/fhir_transforms/senders/original-pipeline-transforms.yaml --output-format FHIR --output-file "PATH-TO-OUTPUT-FILE.fhir"
```

This call will take in your sample input file, and apply any sender transforms specified in the schema passed in,
and will output a FHIR bundle. If there are any mapping issues, if
there are any problems with your schema, they should become readily apparent at this point.

#### Testing receiver transforms
```shell
./prime fhirdata --input-file "PATH-TO-SAMPLE-FILE.fhir" -s metadata/hl7_mapping/ORU_R01/ORU_R01-base.yaml --output-format HL7 --output-file "PATH-TO-OUTPUT-FILE.hl7"
```
This call will take in your sample input file, and convert it to HL7 applying any receiver transforms specified in the 
schema passed in. If there are any mapping issues, if
there are any problems with your schema, they should become readily apparent at this point.

#### Testing both receiver and sender transforms
```shell
./prime fhirdata --input-file "PATH-TO-SAMPLE-FILE.hl7" -s metadata/hl7_mapping/ORU_R01/ORU_R01-base.yaml --output-format HL7 --output-file "PATH-TO-OUTPUT-FILE.hl7"
```
This call will take in your sample input file, and convert it to HL7 applying any receiver transforms specified in the
schema passed in. If there are any mapping issues, if there are any problems with your schema, they should become readily apparent at this point.


### Testing using API
Once your schema has passed running locally, the next step is to run it using the report REST API and see if it not
only parses correctly, but also will route as you expect.

The first step, as always, is to build the package:

`mvn clean package`

Then run:

`./gradlew quickRun`

Next, load the new organization and schema into the local DB using the below command:

`./gradlew reloadSettings`

At this point, once the container is loaded you can submit a file via curl:
```shell
curl -X POST -H 'client: yoyodyne' -H 'Content-Type: application/hl7-v2' --data-binary '@/Path/to/test/file.hl7' 'http://localhost:7071/api/reports'
```
Depending on the contents of the sample file the `Content-Type` can be `application/hl7-v2`,`application/fhir+ndjson`, or `text/csv`

You will then see a JSON object reported back with the result of your post to the local container.

You will then see a report of the result of your post to the local container.  After a few minutes, you can view the
output here: `/prime-router/build/sftp`

You absolutely should pull them down and review them to verify that the data you sent matches
what the receivers are getting.

If there are any exceptions, you will see them output in the console for Azure.

## Sending data to ReportStream

Using the .yml file you created previously and load it into the staging database.

Use the following commands to load the information from the .yml files into the staging database

First obtain a login token for staging
`./prime login –env staging`

Next update the staging DB
`./prime multiple-settings set –env staging –input <file-location>`

### Testing in Staging
After the sender has been configured to send data in staging. The next step is to have to send fake data to the staging 
environment via our reports REST API. 
In order to send to the staging environment, the sender will need to authenticate via either a bearer token obtained by 
using a JWT in combination with a public/private keypair or a shared secret key. Details can be found in the 
ReportStream Programmer’s Guide (https://staging.reportstream.cdc.gov/resources/programmers-guide).

### Authenticating to ReportStream’s REST API

There are two methods of authenticating to ReportStream’s REST API:
1. Token-based authentication with a public/private key pair
   Note: This method is the recommended best practice.

2. Using a shared secret API key
**Note: This is legacy authentication and is no longer used for onboarding new users. This information is here to support legacy senders**

The examples below use the fake client-id healthy-labs, that you will change for your
submissions. The examples submit the payload contained in the file
./healthy-labs-nonPII-data.csv (or .hl7). In the examples, data are submitted via
an HTTP POST to the ReportStream staging system reports endpoint. The data submitted
are sent as the payload of the POST, as is, with no changes.

**Token Auth:**

Prior to connecting to the endpoint, the sender needs a public/private keypair. There are many ways to do this. The steps below show how to create a key pair using `openssl`.

EC
```
openssl ecparam -genkey -name secp384r1 -noout -out my-es-keypair.pem
openssl ec -in my-es-keypair.pem -pubout -out  my-es-public-key.pem
```

RSA
```
openssl genrsa -out my-rsa-keypair.pem 2048
openssl rsa -in my-rsa-keypair.pem -outform PEM -pubout -out my-rsa-public-key.pem
```

The sender should send **only** the public key to the ReportStream team using either the public key upload functionality on the site or via email to the onboarding ReportStream Engineer.
Note: Senders will need to login to use the public key upload feature. If they do not have a login see the [okta-account-creation documentation](prime-reportstream/prime-router/docs/onboarding-users/okta-account-creation.md). 
This step only need to be completed once unless the private key portion is lost or compromised.

**Shared-Secret Authentication**
**Note: This is legacy authentication and is no longer used for onboarding new users. This information is here to support legacy senders**

Some legacy senders will use azure function keys as shared-secret auth. This is no longer supported for new senders.

Here’s an example bash shell curl command submission to ReportStream using a
shared secret API key. The example command submits the contents of the file
‘./healthy-labs-nonPII-data.csv‘ to the endpoint using the client name healthy-labs.

The sender's orgName is the client name and the x-functions-key-value is an azure fucntion key stored under the "reports" function

CSV example:<br>
"curl -X POST -H “client:healthy-labs” -H “content-type:text/csv” –data-binary “@./healthy-labs-nonPII- data.csv” -H “x functions-key:<place-token-here>” https://staging.prime.cdc.gov/api/waters

HL7 example:<br>
"curl -X POST -H “client:super-labs” -H “content-type:application/hl7-v2” –data-binary “@./super-labs-nonPII- data.hl7” -H “x-functions-key:<place-token-here>” https://staging.prime.cdc.gov/api/waters

#### Submission status

The Reports API will return a report id that can be used keep track of the submission 
status via our Submission History REST API.
To view all the submissions a sender has sent should also be available to run this query in the database:

`SELECT * FROM report_file WHERE sending_org = "{sending_org}"`

To view all the report descendants for a given report this query can be used:

`select * from report_file where report_id in (select * from report_descendants('{REPORT_ID}'))`

That query will return where that report got routed to and also the BLOB storage URL. That URL can be used to view the actual 
file contents of the file the sender sent, and how the file looks through the different steps in the pipeline.


## Rhapsody Configuration

Rhapsody is a health data pipeline that provides a visual interface representing various flows of health data. It uses
`Communication Points` of various types to interface with external systems.

