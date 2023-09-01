# How to Onboard a New Organization to Send Data

The goal of this document is to define the steps that an engineer on the ReportStream project should take to onboard a
new data sender to the Universal Pipeline.

## Sender Onboarding Process Overview

- Kickoff call
- [Answer Technical Questions](https://docs.google.com/spreadsheets/d/1iKYB6OmqXMFkwGp960EUU8Y78pWvwQRs1YYYhObsEKI/edit#gid=0)
- Get sample files(non-PII) from sender with different reportable conditions
- [Sender Configuration](#sender-configuration)
- [Test sample files through the Universal Pipeline](#testing)
- Sender sends data to RS staging environment
- Conduct end-to-end test in staging environment

## Sender Configuration

### Get Ready

Create a new branch to store your work on the new sender.

### Set up a New Organization

In your `organization.yml` file create a new organization.

Example organization with one sender entry:

```yaml
- name: yoyodyne
  description: Yoyodyne Propulsion Laboratories, the Future Starts Tomorrow!
  jurisdiction: FEDERAL
  senders:
    - name: default
      organizationName: yoyodyne
      topic: full-elr
      schemaName: metadata/fhir_transforms/senders/default-sender-transform
      format: FHIR
```

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

Once you've added the sender to the `organizations.yml` file you next need to create a schema file.

The schema provides additional transforms required so that the senders data can be routed correctly.

Note: When creating a transform, please reference [Changing/Updating Sender/Receiver Transforms](../getting-started/standard-operating-procedures/changing-transforms.md) for guidance.

## Testing

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

```shell
./prime data --input-schema ydp/ydp-covid-19 --input "PATH-TO-SAMPLE-FILE" --output-format CSV --output-schema ydp/ydp-covid-19 --output "PATH-TO-OUTPUT-FILE"
```

This call will take in your sample file, try to read it according to the schema you have defined,
and then will output it in the same schema for you to examine. If there are any mapping issues, if
there are any problems with your schema, they should become readily apparent at this point.

### Testing in Docker
Once your schema has passed running locally, the next step is to run it in Docker and see if it not
only parses correctly, but also will route as you expect.

The first step, as always, is to build the package:

`mvn clean package`

Then build the docker image:

`docker-compose build`

Note, you might need to add `--force` to the call for `docker-compose build` if you want to make sure it rebuilds
the solution: `docker-compose build --force`

Then start the docker container:

`docker-compose up`

Next, load the new organization and schema into the local DB using the below command:

`./gradlew reloadSettings`

At this point, once the container is loaded you can submit a file via curl:
```shell
curl -X POST -H 'client: yoyodyne' -H 'Content-Type: text/csv' --data-binary '@/Path/to/test/file.csv' 'http://localhost:7071/api/reports'
```
You will then see a JSON object reported back with the result of your post to the local container.

You will then see a report of the result of your post to the local container.  After a few minutes, you can view the
output here: `/prime-router/build/sftp`

You absolutely should pull them down and review them to verify that the data you sent matches
what the receivers are getting.

If there are any exceptions, you will see them output in the console for Azure.

## Sending data to ReportStream

- (Exists in API programmer's guide)
- Authentication - explain how to set up server-server auth to connect with RS (Exists in API programmer's guide and elsewhere)
- API key management - explain how to manage keys in UI or through API
- Report endpoint - explain how to submit data (Exists in API programmer's guide)
- History endpoint - explain how to use + example
- Swagger API Documentation
