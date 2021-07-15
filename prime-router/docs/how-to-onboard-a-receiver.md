# How to Onboard a new State or local jurisdiction to receive Hub data

## Welcome

Our goal is to onboard as many states and local jurisdictions as we can, to receive Hub data!
This is our internal documentation for how we _currently_ do that onboarding work.

As a simple running example, we'll pretend we're creating a new state, the State of Love and Trust, **LT**.

## Steps

### Get ready

-  Create a new branch in git for your changes.

### Set up a new organization

Create a new organization for the State, (canonical style: `lt-phd`), in organizations.yml, which is used by your local commandline ./prime cli tool.  Follow the pattern of another existing organization.  Carefully set the initial jurisdiction-filter so that data is limited to that state.  (The jurisdiction: STATE and  stateCode: XY  fields should soon provide better enforcement of this)

The new organization must have at least one `receiver` defined, but the `receiver` does not need to have a transport defined - the download site can be used until an automated delivery mechanism is set up.

There are two fields that will be used as "keys" to further work, below.   The `-name` of the organization (eg, `lt-phd`) will be used as the Okta `group`, and the `translation.schemaName:` value (eg, `lt/lt-covid-19`) will be used as the schema name in the next step.

Below is an example of the organization file

```yaml
- name: lt-pdh
  description: LT Department of Health
  jurisdiction: STATE
  stateCode: LT
  receivers:
    - name: elr
      topic: covid-19
      jurisdictionalFilter: [ "matches(ordering_facility_state, LT)" ]
      translation:
        type: HL7
        useBatchHeaders: true
        receivingApplicationName: LT-PDH
        receivingApplicationOID:
        receivingFacilityName: LT-PDH
        receivingFacilityOID:
```

In the above example, the jurisdictional filter searches the `ordering_facility_state` field in the report for anything that matches the code LT.

In addition, there is the translation section, which specifies the output format that will be sent to the receiver. Currently, we have three formats available:

- HL7 - Each record is converted to HL7 before being sent to the receiver
- CSV - The records are converted to CSV before being sent to the receiver

The mechanism for how each record is translated is laid out in the schema, which is discussed below

### Set up a new schema
*NOTE - IF YOU ARE WORKING ON AN HL7 RECEIVER, YOU DO NOT NEED TO CREATE A NEW SCHEMA.*

By default, any HL7 receiver will use the COVID-19 schema and you do not need to create a schema
specific to your receiver. If they are going to receive a CSV file you *MUST* create a schema. In lieu
of a schema, we use the `TranslationConfig` to set default values and control HL7 processing.

You should, however, still read the next section about schemas so you know how they work.

- Canonical location style:   `metadata/schemas/LT/lt-covid-19.schema`
- Canonical name style: `name: lt-covid-19`
- Typically, you can copy a basic/standard schema based on a previous state, and modify.

Let's look at a schema header, and then a schema element so you can see an example of each.

#### Schema Header
```yaml
---
name: lt-covid-19
description: LT COVID-19 HL7
topic: covid-19
trackingElement: message_id
basedOn: covid-19
elements:
  # a list of elements follows here
```

In the example above, the name is what the schema is known by in the system. If you execute the following command in the `prime-router` folder you will see a list of the schemas, clients, and receivers that we currently have loaded in PRIME.

```shell
./prime list
```
`description` is used to give a more descriptive name to the schema.

`topic` ties the schema to a specific topic for routing information. If you refer back to the organization example above, you will see that a topic is provided. These must match. If the topic of the schema does not match the schema being used to translate information for a receiver, you will get an error.

`trackingElement` is the name of the unique key for each row being processed

`basedOn` is one of two ways we can inherit from another schema. The other potential option is `extends`. The difference between the two is subtle but important.

#### Schema Element

Here is a more complex element
```yaml
- name: patient_ethnicity
  type: CODE
  default: U
  referenceUrl: https://phinvads.cdc.gov/vads/ViewValueSet.action?oid=2.16.840.1.114222.4.11.6066
  cardinality: ZERO_OR_ONE
  valueSet: hl70189
  natFlatFileField: Patient_ethnicity
  hhsGuidanceField: Patient ethnicity
  hl7Field: PID-22
  hl7OutputFields: [ PID-22 ]
  csvFields: [ { name: Patient_ethnicity } ]
  documentation: |
    The patient's ethnicity. There is a valueset defined based on the values in PID-22, but downstream
    consumers are free to define their own values. Please refer to the consumer-specific schema if you have questions.
```
And here is a simpler element:
```yaml
- name: receiving_application
  type: TEXT
  default: LT-DPH-ELR
```

There is a lot to go over here, so we'll take this a piece at a time. Note, not all fields are required for every element, especially once you're inheriting from another schema, such as the base `covid-19` schema where this came from.

`name` is the name of the field

`type` is the data type of the field. We have any available types for elements. Refer back to the code in Element for a complete list. In this case, `CODE` means it refers to a valueSet we have defined in the application elsewhere.

`default` is a default value is one is not provided

`cardinality` allows us to specify if a field is required, or can accept only one, or multiple values

`valueSet` is the name of the value set the element ties back to

`hl7Field` and `hl7OutputFields` can be used by themselves or in conjunction with each other. In some cases, a data point might be needed in multiple different locations, for example, the specimen ID might show up in multiple segments so they would be put into the list for `hl7OutputFields`, but if it's only used in a single location, then you can omit it and just use `hl7Field`.

`csvFields` is similar to `hl7OutputFields` in that it takes a list of the column header name the report will write out for this data point, and you can have multiple in case you need to duplicate the data. CSV fields also let you output different formats, so for example, with a value set you might want to output both the code and the description, which you could do by providing multiple CSV fields and different format specifiers.

`documentation` lets you add documentation to the element, which is then carried over into the documentation generator we have built into prime. This can be used in conjunction with the `referenceUrl` which points to a website with additional information about the element.

### Generate test data

Generate fake, or better, synthesized test data. Prime has two ways to generate anonymous fake data:

- Fake data - Fake data uses a library to generate purely fake data for ALL data points except for the city, state, postal code, and county, which are tied to actual locations. The data that is generated is somewhat constrained to resemble reasonable defaults, but is designed to be very random, which allows us to test the limits of validation systems. *THERE IS NO PII OR PHI IN FAKE DATA*
- Synthetic data - Synthetic data takes a file of actual clinical results and does a combination of shuffling some PHI/PII and faking other data points, so the records cannot be traced back to the patient, but the actual portion of positive to negative tests, lab names & CLIA's, names of ordering providers, etc will be actual valid information. The goal is to provide a higher-quality, less-random, dataset that can then be used to validate the information being sent from PRIME to the receivers. *While great care has been taken to ensure we do not leak PII/PHI, this should not be used except with receivers we are in the process of onboarding.*

#### Generate some fake data for testing
```
./prime data --input-fake 100 --input-schema lt/lt-covid-19 --output myfile.csv --target-states LT
```

#### Synthesized data generation:
If you want to generate a CSV file, here is how you do it:
```
./prime data --input file-with-real-test-results.internal --input-schema primedatainput/pdi-covid-19 --synthesize --output path/to/your/output/folder/synthesized_data.csv --output-format CSV --target-states LT
```

If you want to create the data as HL7, that is easy to do as well:
```
./prime data --input file-with-real-test-results.internal --input-schema primedatainput/pdi-covid-19 --synthesize --output path/to/your/output/folder/synthesized_data.csv --output-format HL7_BATCH --target-states LT
```

### Test and commit, and deploy to Test and maybe Prod

- Test locally using the above fake data.
- Once you've got the kinks out of the organizations.yml, carefully copy the info into organizations-test.yml, and make adjustments as needed.
- If you are ready, also carefully copy it into organizations-prod.yml.  Especially in production, check the batch timing.  NOT every minute, eh?
- Create a PR for the change, review, and push to Test.  The review is a good chance for someone to doublecheck the filters.
- It should deploy to Test automagically once the PR is approved and merged into master.
- Test again in Test
- If needed, push to production following our procedures for doing that.

#### Testing locally
The best way to test locally is to use the `quick-test.sh` shell script that we have in the `prime-router` folder in the project.

```shell
    # add your state here
    for arg in "$@"
    do
      case "$arg" in
        az | AZ) RUN_AZ=1;;
        lt | LT) RUN_LT=1;;
        all | ALL) RUN_ALL=1;;
        merge | MERGE) RUN_MERGE=1;;
      esac
    done

    # and add the state here as well
    if [ $RUN_ALL -ne 0 ]
    then
      RUN_AZ=1
      RUN_LT=1
      RUN_STANDARD=1
      RUN_MERGE=1
    fi

    # and then at a minimum run your state like this
    # run LT
    if [ $RUN_LT -ne 0 ]
    then
      LT_FILE_SEARCH_STR="/lt.*\.hl7"
      echo Generate fake LT data
      text=$(./prime data --input-fake 50 --input-schema lt/lt-covid-19 --output-dir $outputdir --target-states LT --output-format HL7_BATCH)
      parse_prime_output_for_filename "$text" $LT_FILE_SEARCH_STR
    fi
```

If you are generating CSV data, then you can test a roundtrip in the code:
```shell
    if [ $RUN_LT -ne 0 ]
    then
      LT_FILE_SEARCH_STR="/lt.*\.hl7"
      echo Generate fake LT data
      actual_lt=$(./prime data --input-fake 50 --input-schema lt/lt-covid-19 --output-dir $outputdir --target-states LT --output-format HL7_BATCH)
      parse_prime_output_for_filename "$text" $LT_FILE_SEARCH_STR

      # Now read the data back in to their own schema and export again.
      # LT again
      echo Test sending LT data into its own Schema:
      text=$(./prime data --input-schema lt/lt-covid-19 --input $actual_lt --output-dir $outputdir)
      parse_prime_output_for_filename "$text" $LT_FILE_SEARCH_STR
      actual_lt2=$filename
      compare_files "LT->LT" $actual_lt $actual_lt2
    fi
```

Testing a round trip like this is good practice because it ensures your schema is valid in both directions and values are translating as you expect.

### Testing in your Docker container

Another important step to take when onboarding a receiver is to start the docker container and then submit a file to the container and make sure that it translates and routes correctly.

First build the solution:

`mvn clean package`

Then build the docker image:

`docker-compose build`

Then start the docker container:

`docker-compose up`

At this point, once the container is loaded you can submit a file via curl:
```shell
curl -X POST -H 'client: simple_report' -H 'Content-Type: text/csv' --data-binary '@/Path/to/test/file.csv' 'http://localhost:7071/api/reports'
```
You will then see a report of the result of your post to the local container.  After a few minutes, you can sftp into the container and view the results of your file:

```shell
ftp foo@localhost
```

You then change into the `./upload` folder and can download and view the files you've created.

### Create access to the Download site

- Set up an Okta account for **LT**.  Be sure to include an internal Hub staffperson as a user, so they can test connectivity.
- If you are testing in Test, obviously you'll need to set up access to that download site.

### Validation in Prod

- Work with the customer to confirm their rules for validation in Prod.   PII vs no PII.  Synthesized data vs real data (yes, it appears many PHDs test using real data.)
- At this point you should be able to send data through to the customer, and they can validate.
- You may want to set the Processing_mode_code field to 'D' or 'T' to represent Debugging or Training data.
- Customer from **LT** should be able to go to the download site and pull down data.

### Iterate on the schema with the customer

- If you change organization data, think about which of our 4 org files it needs to go into (sigh)
