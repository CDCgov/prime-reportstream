# How to Onboard a New Organization to Send Data

Add subsections that mimic the linked resources: ([Brandon’s version](https://docs.google.com/document/d/1noB3lK2Nc_vbD4s5ZHgdTjgIjhCii63x_2bjBz7GM1I/edit#heading=h.be9yxi8thtdw), [Github version](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/how-to-onboard-a-sender.md))
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
      schemaName: metadata/fhir_transforms/senders/default-sender-transform
      format: HL7
```

Use the following command to load the information from the .yml file into your local database

`./prime multiple-settings set –env local –input <file-location>`

A few things to note here:

- The name of the organization must be unique. It cannot share a name with a state or other organization
- The jurisdiction should be FEDERAL since they are not a STLT
- The organizationName under `senders` must match the name of the org above
- The format here is `FHIR`, though it is possible it could another format, such as `HL7`
- The schema name should use `metadata/fhir_transforms/senders/default-sender-transform` by default
- Pay attention to the value in the topic field to make sure it is correct for your use case

### Set up a New Schema

**You should only create new schemas when an existing schema will not fit your use case. Please ensure that an
existing schema cannot be used prior to creating a new schema.**

Once you've added a setting file the sender you next need to create a schema file.

The schema provides additional transforms required so that the senders data can be routed correctly.

Note: When creating a transform, please reference [Changing/Updating Sender/Receiver Transforms](../getting-started/standard-operating-procedures/changing-transforms.md) for guidance.

## Testing

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
./prime fhirdata --input-file "PATH-TO-SAMPLE-FILE.hl7" -s metadata/fhir_transforms/senders/default-sender-transform.yaml --output-format FHIR --output-file "PATH-TO-OUTPUT-FILE.fhir"
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

#### Setting up public/private keypair

Details on how to set up a keypair can be found in the “how-to-use-token-auth.md” documentation in the repo. Keypair is 
the preferred authentication method. Shared secret can be used as an alternative in cases where keypair presents a 
barrier to the sender.

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

### Using x-functions-key

The `HTTP Communications Point` uses a standard HTTP request to send data externally. It has been used in conjunction
with the `x-functions-key` authentication flow to connect clients in the `staging` environment. However, this auth flow is
deprecated and should not be used to onboard any new clients moving forward (documentation kept for posterity).

It is recommended to use oauth2 or server-to-server auth instead. The observed versions of Rhapsody cannot support the
OAuth2 authentication flow (javascript version too old).

#### HTTP Communications Point Configuration

| Name                             | Value                                     |
|----------------------------------|-------------------------------------------|
| COMMUNICATION POINT              | HTTP CLIENT                               |
| MODE                             | Out->In                                   |
| URL                              | https://staging.prime.cdc.gov/api/reports |
| HTTP METHOD                      | POST                                      |
| FOLLOW REDIRECTS                 | YES                                       |
| USE HTTPS                        | YES                                       |
| SSL PROTOCOL MODE                | TLSv1.2                                   |
| SPECIFY EXACT TLS VERSION        | YES                                       |
| HOSTNAME VERIFICATION            | YES                                       |
| PROTOCOL SUPPORT                 | Standard HTTP Only                        |
| READ TIMEOUT (MS)                | 10,000                                    |
| CONNECTION TIMEOUT (MS)          | 60,000                                    |
| PROXY TYPE                       | System                                    |
| REFRESH RATE (MS)                | 60,000                                    |
| MESSAGE CONTENT                  | Message Body                              |
| CONTENT TYPE                     | application/hl7-v2                        |
| ON 4xx ERROR RESPONSE            | Mark as connection failed                 |
| ON 5xx ERROR RESPONSE            | Mark as connection failed                 |
| DYNAMIC CONNECTION FAILED ACTION | Treat as message error                    |

##### Request Headers
| Name            | Value         |
| --------------- | ------------- |
| x-functions-key | <suppressed>  |
| client          | CDC-ELIMS-HL7 |
