# How to Onboard a New Organization to Receive Data
Add subsections that mimic the linked resources: ([Brandon’s version](https://docs.google.com/document/d/1noB3lK2Nc_vbD4s5ZHgdTjgIjhCii63x_2bjBz7GM1I/edit#heading=h.be9yxi8thtdw), [Github version](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/how-to-onboard-a-sender.md))

## Welcome

Our goal is to onboard as many states and local jurisdictions as we can, to receive Hub data!
This is our internal documentation for how we _currently_ do that onboarding work.


The main goal of receiver onboarding is to establish a connection to send data from ReportStream to the STLT.  
In order to do so there are multiple receiver configurations that need to be configured.  
During pre-onboarding ideally we will be able to know/collect/obtain all the unknown variables in order to set up the receiver configurations.

## Pre-Onboarding
* Identify how they want to receive data
* Determine if we need to be whitelisted prior to connecting
* Identify STLT Specific (HL7) values (e.g. MSH-5, MSH-6, etc)
* Determine if they need/want specific data quality or condition filters
* Determine if receivers need any specific transforms
* Create Okta Accounts (probably once they are set up fully).

## Table of Contents
1. Set up new organization
2. Set up receiver schema
3. Test and commit, and deploy to Test and maybe Prod
4. Testing in your Docker container
5. Create access to the Download site
6. Validation in Prod
7. Set up transport 

### 1. Set up new organization
* Create a new branch in git for your changes.
* Create a new organization for the State, (canonical style: `lt-phd`), in organizations.yml, which is used by your local 
commandline ./prime cli tool.
* Follow the pattern of another existing organization.  Carefully set the initial 
jurisdiction-filter so that data is limited to that state.  (The jurisdiction: STATE and  stateCode: XY  fields should 
soon provide better enforcement of this)
* The new organization must have at least one `receiver` defined, but the `receiver` does not need to have a transport 
defined - the download site can be used until an automated delivery mechanism is set up.
* There are two fields that will be used as "keys" to further work, below.   The `-name` of the organization 
(eg, `lt-phd`) will be used as the Okta `group`, and the `translation.schemaName:` value (eg, `metadata/hl7_mapping/ORU_R01/ORU_R01-base`) will be 
used as the schema name in the next step.
* Below is an example of the organization file
  
```yaml
- name: lt-pdh
  description: LT Department of Health
  jurisdiction: STATE
  stateCode: LT
  receivers:
    - name: full-elr
      topic: full-elr
      jurisdictionalFilter: [ "(%performerState.exists() and %performerState = 'LT') or (%patientState.exists() and %patientState = 'LT')" ]
      translation: !<HL7>
        schemaName: "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
        type: HL7
        useBatchHeaders: true
        receivingApplicationName: LT-PDH
        receivingApplicationOID:
        receivingFacilityName: LT-PDH
        receivingFacilityOID:
```
* In the above example, the jurisdictional filter uses FHIR path to check if the patient or Test performer 
are in the state of LT. `%performerState` and `%patientState` are shorthand FHIR paths defined in `metadata/tables/local/fhirpath_filter_shorthand.csv`
* Filters can be applied to the organization or receiver. For more information on filters see: 
(https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/universal-pipeline/route.md)
* In addition, there is the translation section, which specifies the output format that will be sent to the receiver. 
Currently, we have three formats available:
    - HL7
    - FHIR
    - CSV (Not yet implemented)
    
* The quality filters verify that test results have the minimum fields required by most public health jurisdictions. 
* Those fields are made adjustable to be customized per jurisdictional specifications.
* These filters are applied by default to the receiver and set in code.
* They do not need to be added to the receiver setting.
* If a custom rule is added to the quality filter for a receiver, all default quality filters will be ignored. The default filter includes 
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

- Only test results that pass all the above requirements will be transferred to the jurisdiction. There is only one option 
for jurisdictions that want all results reported.
- Create a secondary feed with the reverseQualityFilter() set to true. This will only allow results that fail the 
quality filters listed above.

The mechanism for how each record is translated is laid out in the schema, which is discussed in the next section.

### 2. Set up receiver schema

* By default, any HL7 receiver will use the universal or covid schema and you do not need to create a schema
specific to your receiver.
* In the UP for HL7 v2 we have to check what HL7 message type they want to receive data in. We support ADT_A01, OML_O21 and ORU_R01. Depending on the message type we can set `translationSchema` to the respective message type schema.
* If the receiver wants specific receiver transforms that are not supported by the translation settings a schema can be created for them. More information on how to manage translation schemas can be found here (https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/universal-pipeline/translate.md)


### 3. Test and commit, and deploy to Test and maybe Prod

* Test locally using sample messages.
* Once you've got the kinks out of the organizations.yml, carefully update settings in the staging environment. 
* `./prime multiple-settings set --help`
* Create a PR for the change, review, and push. The review is a good chance for someone to doublecheck the filters.
* It should deploy to staging automagically once the PR is approved and merged into master.
* Test again in Staging
* If you are ready, carefully update settings in the prod environment. Especially in production, check the batch 
timing. NOT every minute, eh?
* If needed, push to production following our procedures for doing that.

### 4. Testing in your Docker container

* Another important step to take when onboarding a receiver is to start the docker container and then submit a file to 
the container and make sure that it translates and routes correctly.

* Ensure that the transport is set to the default sftp:
```
transport:
    type: SFTP
    host: sftp
    port: 22
    filePath: ./upload
    credentialName: DEFAULT-SFTP
```
* First build the solution:

`./gradlew clean package`

* Then start the docker container:

`docker-compose up`

* Next, load the new organization and schema into the local DB using the below command:

`./gradlew reloadSettings`

* NOTE: If developing on an Apple Mac with a Silicon chip please follow our guide in place of the above three commands:

- [Using Apple Silicon Macs for Development](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/getting-started/Using-an-apple-silicon-mac.md)


* At this point, once the container is loaded you can submit a file via curl:
```shell
curl -X POST -H 'client: simple_report' -H 'Content-Type: application/hl7-v2' --data-binary '@/Path/to/test/file.hl7' 'http://localhost:7071/api/reports'
```
* You will then see a report of the result of your post to the local container.  After a few minutes, you can view the 
output here: `/prime-router/build/sftp`

### 5. Create access to the Download site

* If the organization has elected for download access, set up an Okta account.
* If you are testing in Test, obviously you'll need to set up access to that download site.

### 6. Validation in Prod

* Work with the customer to confirm their rules for validation in Prod.   PII vs no PII.  Synthesized data vs real data 
(yes, it appears many PHDs test using real data.)
* At this point you should be able to send data through to the customer, and they can validate.
* You may want to set the Processing_mode_code field to 'D' or 'T' to represent Debugging or Training data.
* Customer from **LT** should be able to go to the download site and pull down data.

### 8. Set up transport

ReportStream supports the below forms of transport for receivers and can be configured through the `transport` receiver setting.
- [SFTP](./transport/sftp.md)
- [SOAP](./transport/soap.md)
- [REST](./transport/rest.md)
- [Azure Blob](./transport/blob.md)










