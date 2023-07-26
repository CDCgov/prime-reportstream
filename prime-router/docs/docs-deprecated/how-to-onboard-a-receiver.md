# How to Onboard a new organization to receive data

## Welcome

Our goal is to onboard as many states and local jurisdictions as we can, to receive Hub data!
This is our internal documentation for how we _currently_ do that onboarding work.

As a simple running example, we'll pretend we're creating a new state, the State of Love and Trust, **LT**.

Assumptions:
- Knowledge of GitHub and ReportStream repositories
- A working local instance of the ReportStream application
- Has an active Okta admin account

## Steps

### Get ready

-  Create a new branch in git for your changes.

### Set up a new organization

Create a new organization for the State, (canonical style: `lt-phd`), in organizations.yml, which is used by your local 
commandline ./prime cli tool.  Follow the pattern of another existing organization.  Carefully set the initial 
jurisdiction-filter so that data is limited to that state.  (The jurisdiction: STATE and  stateCode: XY  fields should 
soon provide better enforcement of this)

The new organization must have at least one `receiver` defined, but the `receiver` does not need to have a transport 
defined - the download site can be used until an automated delivery mechanism is set up.

There are two fields that will be used as "keys" to further work, below.   The `-name` of the organization 
(eg, `lt-phd`) will be used as the Okta `group`, and the `translation.schemaName:` value (eg, `lt/lt-covid-19`) will be 
used as the schema name in the next step.

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

In the above example, the jurisdictional filter searches the `ordering_facility_state` field in the report for anything 
that matches the code LT. Filters can be applied to the organization or receiver. For more information on filters see: 
(https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/playbooks/how-to-use-filters.md)

In addition, there is the translation section, which specifies the output format that will be sent to the receiver. 
Currently, we have three formats available:

- HL7
- CSV

The quality filters verify that test results have the minimum fields required by most public health jurisdictions. 
Those fields are made adjustable to be customized per jurisdictional specifications. These filters are applied by 
default to the receiver and set in code. They do not need to be added to the receiver setting. If a custom rule is 
added to the quality filter for a receiver, all default quality filters will be ignored. The default filter includes 
these requirements:

- The following fields all have a value:
     - Patient First Name
     - Patient Last Name
     - Patient DOB
     - Test Result
     - Specimen Type
     - Specimen ID/Accession Number
     - Device Identifier
    
- At least one of the following fields has a value:
     - Patient Street Address
     - Patient Zip Code
     - Patient Phone
     - Patient Email
    
- At least one of the following date fields has a valid date:
     - Test Ordered Date
     - Specimen Collection Date
     - Test Result Date
    
- The following fields are 10 characters in length:
     - Testing Lab CLIA
     - Ordering Facility CLIA
    
- Processing Code does not equal T nor D

Only test results that pass all the above requirements will be transferred to the jurisdiction. There are two options 
for jurisdictions that want all results reported.

- Use AllowAll() to bypass all quality filters
- Create a secondary feed with the reverseQualityFilter() set to true. This will only allow results that fail the 
quality filters listed above.

The mechanism for how each record is translated is laid out in the schema, which is discussed below

### Set up a new schema
*NOTE - IF YOU ARE WORKING ON AN HL7 RECEIVER, YOU DO NOT NEED TO CREATE A NEW SCHEMA.*

By default, any HL7 receiver will use the COVID-19 schema and you do not need to create a schema
specific to your receiver. If they are going to receive a CSV file you *MUST* create a schema. In lieu
of a schema, we use the `TranslationConfig` to set default values and control HL7 processing. 
(https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/playbooks/how_to_use_translation_configuration_features.md)

For additional information on creating a schema see: 
(https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/how-to-onboard-a-sender.md)

### Generate test data

Generate fake, or better, synthesized test data. Prime has two ways to generate anonymous fake data:

- Fake data - Fake data uses a library to generate purely fake data for ALL data points except for the city, state, 
postal code, and county, which are tied to actual locations. The data that is generated is somewhat constrained to 
resemble reasonable defaults, but is designed to be very random, which allows us to test the limits of validation 
systems. *THERE IS NO PII OR PHI IN FAKE DATA*
- Synthetic data - Synthetic data takes a file of actual clinical results and does a combination of shuffling some 
PHI/PII and faking other data points, so the records cannot be traced back to the patient, but the actual portion of 
positive to negative tests, lab names & CLIA's, names of ordering providers, etc will be actual valid information. The 
goal is to provide a higher-quality, less-random, dataset that can then be used to validate the information being sent 
from PRIME to the receivers. *While great care has been taken to ensure we do not leak PII/PHI, this should not be used 
except with receivers we are in the process of onboarding.*

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
- Once you've got the kinks out of the organizations.yml, carefully update settings in the staging environment. 
  - `./prime multiple-settings set --help`
- Create a PR for the change, review, and push. The review is a good chance for someone to doublecheck the filters.
- It should deploy to staging automagically once the PR is approved and merged into master.
- Test again in Staging
- If you are ready, carefully update settings in the prod environment. Especially in production, check the batch 
timing. NOT every minute, eh?
- If needed, push to production following our procedures for doing that.

### Testing in your Docker container

Another important step to take when onboarding a receiver is to start the docker container and then submit a file to 
the container and make sure that it translates and routes correctly.

Ensure that the transport is set to the default sftp:
```
transport:
    type: SFTP
    host: sftp
    port: 22
    filePath: ./upload
    credentialName: DEFAULT-SFTP
```
First build the solution:

`./gradlew clean package`

Then start the docker container:

`docker-compose up`

Next, load the new organization and schema into the local DB using the below command:

`./gradlew reloadSettings`

NOTE: If developing on an Apple Mac with a Silicon chip please follow our guide in place of the above three commands:

- [Using Apple Silicon Macs for Development](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/getting-started/Using-an-apple-silicon-mac.md)


At this point, once the container is loaded you can submit a file via curl:
```shell
curl -X POST -H 'client: simple_report' -H 'Content-Type: text/csv' --data-binary '@/Path/to/test/file.csv' 'http://localhost:7071/api/reports'
```
You will then see a report of the result of your post to the local container.  After a few minutes, you can view the 
output here: `/prime-router/build/sftp`

### Create access to the Download site

- If the organization has elected for download access, set up an Okta account.
- If you are testing in Test, obviously you'll need to set up access to that download site.

### Validation in Prod

- Work with the customer to confirm their rules for validation in Prod.   PII vs no PII.  Synthesized data vs real data 
(yes, it appears many PHDs test using real data.)
- At this point you should be able to send data through to the customer, and they can validate.
- You may want to set the Processing_mode_code field to 'D' or 'T' to represent Debugging or Training data.
- Customer from **LT** should be able to go to the download site and pull down data.
