# How to Onboard a new Organization to Send Data

## Welcome

Our goal is to allow any organization to send information to us via our RESTful API, that we can then
translate and route to different receivers based on the jurisdictional filters we have.

It is important to note that ReportStream is in production now, and if care is not taken when posting
information, it can end up being routed to receivers. Therefore, do ***NOT*** post any information into
production unless you are absolutely sure it should be going there.

As a simple running example, we will pretend we're creating a sender for Yoyodyne Propulsion, **YDP**.

## Steps

### Get Ready

Create a new branch to store your work on the new sender.

### Set up a New Organization

In your `organization.yml` file create a new organization. Senders typically are at the top of the
file.

It should look something like this:

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

- The name of the organization must be unique. It cannot share name with a state or other sender
- The jurisdiction should be FEDERAL since they are sending into the CDC (*NOTE*: this may change in the future)
- The organizationName under `senders` must match the name of the org above
- The format here is `CSV`, though it is possible it could another format, such as `HL7`
- The schema name does not need to start with the organization name. Most times we abbreviate the org name for the schema name

### Set up a New Schema

Once you've added the sender to the `organizations.yml` file you next need to create a schema file.

The schema describes the data coming in from the sender and maps back to the base schema, which for `covid-19`
is named `covid-19`. Schemas live in `metadata/schemas` so the one for Yoyodyne would be at
`metadata/schemas/YDP/ydp-covid-19.schema`

The header for a schema looks like this:

```yaml
name: ydp-covid-19
description: A COVID-19 schema for Yoyodyne Propulsion Lab
trackingElement: message_id
topic: covid-19
basedOn: covid-19
elements:
```

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
from NJ, then your test isn't really complete.

### Testing Locally
Assuming you have a receiver set up for your sender, the next step is to build your solution:

`mvn clean package`

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

At this point, once the container is loaded you can submit a file via curl:
```shell
curl -X POST -H 'client: yoyodyne' -H 'Content-Type: text/csv' --data-binary '@/Path/to/test/file.csv' 'http://localhost:7071/api/reports'
```
You will then see a JSON object reported back with the result of your post to the local container.

ReportStream will now begin the process of ingesting and routing your records. After a few minutes, you can sftp into the container and view the results of your file:

```shell
sftp foo@localhost
```

You then change into the `./upload` folder and can download and view the files you've created.
You absolutely should pull them down and review them to verify that the data you sent matches
what the receivers are getting.

If there are any exceptions, you will see them output in the console for Azure.

## Iterate with the Sender
Once you have passed testing locally, you can deploy the sender to staging and begin to iterate with the
sender. There are likely going to be several rounds of testing that are going to be required before the
schema is considered complete.
