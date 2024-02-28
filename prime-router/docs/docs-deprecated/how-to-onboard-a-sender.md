# How to Onboard a new Organization to Send Data

## Welcome

The goal of this document is to define the steps that an engineer on the ReportStream project should take to onboard a 
new data sender to the platform via our RESTful API.

We will use an example organization named Yoyodyne Propulsion (YDP). We will start by creating a sender for Yoyodyne 
Propulsion and then walk through the steps for testing in our staging environment.

Assumptions:
- Knowledge of GitHub and ReportStream repositories
- A working local instance of the ReportStream Application
- Has an active Okta admin account

## Steps

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
      topic: covid-19
      schemaName: ydp/ydp-covid-19
      format: CSV
```

A few things to note here:

- The name of the organization must be unique. It cannot share a name with a state or other organization
- The jurisdiction should be FEDERAL since they are not a STLT
- The organizationName under `senders` must match the name of the org above
- The format here is `CSV`, though it is possible it could another format, such as `HL7`
- The schema name must match the value in the “name” parameter of the corresponding schema
- Pay attention to the value in the topic field to make sure it is correct for your use case

### Set up a New Schema

**You should only create new schemas when an existing master schema will not fit your use case. Please ensure that an 
existing schema cannot be used prior to creating a new schema.**

Once you've added the sender to the `organizations.yml` file you next need to create a schema file.

The schema describes the data coming in from the sender and maps back to the base schema, which for `covid-19`
is named `covid-19`. Schemas live in `metadata/schemas` so the one for Yoyodyne would be at
`metadata/schemas/YDP/ydp-covid-19.schema`

#### Schema Header

The header for a schema looks like this:

```yaml
name: ydp-covid-19
description: A COVID-19 schema for Yoyodyne Propulsion Lab
trackingElement: message_id
topic: covid-19
basedOn: covid-19
elements:
```

In the example above, the name is what the schema is known by in the system. If you execute the following command in 
the `prime-router` folder you will see a list of the schemas, clients, and receivers that we currently have loaded in 
PRIME.

```./prime list```

`description` is used to give a more descriptive name to the schema.

`topic` ties the schema to a specific topic for routing information. If you refer back to the organization example 
above, you will see that a topic is provided. These must match. If the topic of the schema does not match the schema 
being used to translate information for a receiver, you will get an error.

`trackingElement` is the name of the unique key for each row being processed

`basedOn` is one of two ways we can inherit from another schema. The other potential option is `extends`. The difference 
between the two is subtle but important.

#### Schema Element

Given a simplistic CSV file that looks like this:
```csv
"Message ID","Patient First Name","Patient Last Name"
"1","Buckaroo","Banzai"
"2","Sidney","Zweibel"
```

The elements in the sender file would look like this:
```yaml
- name: message_id
  csvFields:
      - name: "Message ID"

- name: patient_first_name
  csvFields:
      - name: "Patient First Name"

- name: patient_last_name
  csvFields:
      - name: "Patient Last Name"
```


**NOTE** - A schema can either be `basedOn` the `covid-19` schema or `extends` it. If you choose `extends`,
ReportStream will import ALL the elements from the parent schema into your schema which could cause errors.
For the purpose of senders, it is better to use `basedOn`

When a file is sent from Yoyodyne, ReportStream will match columns in the document based on the `csvFields.name`
property. The matching is done in a case-sensitive matter, though order isn't strictly enforced. We do try to
match the order of the CSV file when creating schemas.

Each of the fields above, `message_id`, `patient_first_name`, `patient_last_name` are imported from the `covid-19`
parent schema, which defines additional information about the fields, like datatype, documentation, etc

There is a lot to go over here, so we'll take this a piece at a time. Note, not all fields are required for every 
element, especially once you're inheriting from another schema, such as the base `covid-19` schema where this came from.

`name` is the name of the field

`type` is the data type of the field. We have any available types for elements. Refer back to the code in Element for a 
complete list. In this case, CODE means it refers to a valueSet we have defined in the application elsewhere.

`default` is a default value is one is not provided

`cardinality` allows us to specify if a field is required, or can accept only one, or multiple values

`valueSet` is the name of the value set the element ties back to

`hl7Field` and `hl7OutputFields` can be used by themselves or in conjunction with each other. In some cases, a data 
point might be needed in multiple different locations, for example, the specimen ID might show up in multiple segments, 
so they would be put into the list for hl7OutputFields, but if it's only used in a single location, then you can omit it 
and just use hl7Field.

`csvFields` is similar to `hl7OutputFields` in that it takes a list of the column header name the report will write out 
for this data point, and you can have multiple in case you need to duplicate the data. CSV fields also let you output 
different formats, so for example, with a value set you might want to output both the code and the description, which 
you could do by providing multiple CSV fields and different format specifiers.

`documentation` lets you add documentation to the element, which is then carried over into the documentation generator 
we have built into prime. This can be used in conjunction with the `referenceUrl` which points to a website with 
additional information about the element.

### Mapped Fields and Valuesets
In some cases, the information from a sender will need to match one of the predefined valuesets used by
receivers. One example would be `patient_gender`. Valueset `hl70001` defines seven values for gender:
```yaml
    - code: M
      display: Male
    - code: F
      display: Female
    - code: O
      display: Other
    - code: A
      display: Ambiguous
    - code: U
      display: Unknown
    - code: N
      display: Not applicable
```

The values sent from Yoyodyne might not match up with what `patient_gender` is expecting. Therefore
you have two options:
1. Create altValues for the field
2. Create a mapper

#### AltValues
Creating alternate values for a schema element is relatively straightforward:
```yaml
- name: patient_gender
  altValues:
      - diplay: Male
        code: M
      - display: Female
        code: F
      - display: Unk
        code: U
```

In the above case, the values coming in from Yoyodyne will be checked against the altValues and converted
to the mapped code.

#### Mapper
ReportStream allows for mappers to take a value and then convert it to a different value programmatically.

Mappers live in `Mappers.kt` and must implement the `Mapper` interface. You then must also
initialize the mapper and add it to the collection of mappers in `Metadata` so that it can be invoked. There
is only one copy of a mapper created in ReportStream, and they should be treated as a pure function
with no side effects.

## Testing

### Note
In order to test a sender, you must create (or have in your possession) a file that matches the format
you have laid out in the schema, and that matches the format the sender will provide.  Also, it is
good to have at least one receiver that will match what the sender provides. For example, if Yoyodyne
is sending information for a lab in NJ, but you have no receivers set up to receive information
from NJ, then you will be unable to complete testing.

### Testing Locally
Assuming you have a receiver set up for your sender, follow the below steps to build and test your solution:

```
./git checkout “branch_name”
./gradlew clean package
```


Once that has completed successfully, the next step is to manually run Prime and see if it can ingest
the file from the sender:

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

## Sender Testing

### Initial Sender File Testing - Validation Tool

A sender will first utilize the validation tool
- https://staging.reportstream.cdc.gov/file-handler/validate

to validate against one of the three existing master schemas. In order to validate against a custom schema in the 
validation tool, that organization will need to be given an Okta login and their organization and sender settings need 
to be updated in the staging and production databases.

Update organization and sender settings in DB

Create .yml files in your working branch in:
- prime-router -> settings -> staging

The .yml files should contain the same information as you used above to create the organization and sender in the 
organization.yml files. Ensure the file begins with “---”.

Example:

```agsl
- name: yoyodyne
  description: Yoyodyne Propulsion Laboratories, the Future Starts Tomorrow!
  jurisdiction: FEDERAL
  senders:
    - name: default
      organizationName: yoyodyne
      topic: covid-19
      schemaName: ydp/ydp-covid-19
      format: CSV
```
Use the following commands to load the information from the .yml files into the staging database

First obtain a login token for staging
`./prime login –env staging`

Next update the staging DB
`./prime multiple-settings set –env staging –input <file-location>`

### Testing in Staging

Once the sender has successfully completed testing with the validation tool, the next step will be to have them test by 
sending fake data (NO PII OR PHI) to the ReportStream staging environment. If you have not already done so, update the 
staging DB using the process outlined above.

In order to send to the staging environment, the sender will need to authenticate via either a bearer token obtained by 
using a JWT in combination with a public/private keypair or a shared secret key. Details can be found in the 
ReportStream Programmer’s Guide (https://staging.reportstream.cdc.gov/resources/programmers-guide).

#### Setting up public/private keypair

Details on how to set up a keypair can be found in the “how-to-use-token-auth.md” documentation in the repo. Keypair is 
the preferred authentication method. Shared secret can be used as an alternative in cases where keypair presents a 
barrier to the sender.

#### Setting up a shared secret

Shared secret keys can be generated or modified in the “reports” function in the relevant Function App in Azure. You 
will need a superuser level account to access the function keys.

#### Sending file to staging

The sender should send a file with no PII/PHI (only fake data) to the staging environment using the agreed upon 
authentication method. Details and examples can be found in the RS Programmer’s Guide 
(https://staging.reportstream.cdc.gov/resources/programmers-guide).

#### Validating file in staging

Ensure that there are no errors or warnings in the JSON response to the sender’s file. Example JSON response can be 
found in the RS Programmer’s Guide (https://staging.reportstream.cdc.gov/resources/programmers-guide). Ensure that the 
file from the sender would have successfully passed to a receiver. Iterate until there are no issues with the file 
submitted by the sender.

### Testing in Production

Testing in production should be done with real patient data and the sender in “testing” mode. Testing mode means that 
the Customer Status is set to “testing” if using one of the three master schemas or the schema is defaulting a 
processing mode code of “T”. This ensures that files sent to the ReportStream production endpoint will not move into a 
STLT production system. Working with the identified receiver send the production testing files to their preferred 
environment. Once the receiver has validated the testing files, the sender can be moved to full production mode.
