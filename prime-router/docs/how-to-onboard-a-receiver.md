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

- Canonical location style:   `metadata/schemas/LT/lt-covid-19.schema`
- Canonical name style: `name: lt-covid-19`
- Typically, you can copy a basic/standard schema based on a previous state, and modify.

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

### Create access to the Download site

- Set up an Okta account for **LT**.  Be sure to include an internal Hub staffperson as a user, so they can test connectivity.  
- If you are testing in Test, obvioulys you'll need to set up access to that download site.

### Validation in Prod

- Work with the customer to confirm their rules for validation in Prod.   PII vs no PII.  Synthesized data vs real data (yes, it appears many PHDs test using real data.)
- At this point you should be able to send data through to the customer, and they can validate.
- You may want to set the Processing_mode_code field to 'D' or 'T' to represent Debugging or Training data.
- Customer from **LT** should be able to go to the download site and pull down data.

### Iterate on the schema with the customer

- If you change organization data, think about which of our 4 org files it needs to go into (sigh)
