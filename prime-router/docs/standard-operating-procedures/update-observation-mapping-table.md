# Updating Observation-Mapping table

## Context

Individual FHIR observations are "stamped" with a SNOMED value that represents the reportable condition indicated by the test represented by a LOINC or SNOMED code
on that observation. In addition, observations that do not represent a reportable condition can be 
stamped with a value that assists in identifying their administrative purpose such as Ask-at-Order-Entry questions (AOEs).

The observation mapping table is used to map LOINC/SNOMED codes to their relevant reportable conditions. The initial 
table is made up of values that are contained in valuesets maintained by the Council of State and Territorial 
Epidemiologists (CSTE) on the National Library of Medicines' [Value Set Authority Center (VSAC)]
(https://vsac.nlm.nih.gov/). The valuesets are compiled and mapped to conditions by CSTE and published in the Reportable
Conditions Trigger Codes (RCTC). The RCTC is available to be downloaded from AIMS at 
https://ersd.aimsplatform.org/#/home. A login is required (registration is free).

The Observation Mapping table is made up of CSTE ValueSets and contains the following columns:

| Column Name                   | Description                                   | Example                                                                        |
|-------------------------------|-----------------------------------------------|--------------------------------------------------------------------------------|
| Member OID                    | ValueSet Identifier                           | 2.16.840.1.113762.1.4.1146.239                                                 |
| Name                          | ValueSet Name                                 | Chlamydia trachomatis Infection (Tests for Chlamydia trachomatis Nucleic Acid) |
| Code                          | LOINC or SNOMED coded value                   | 16601-7                                                                        |
| Descriptor                    | LOINC or SNOMED term descriptio               | Chlamydia trachomatis rRNA [Presence] in Urine by Probe                        |
| Code System                   | Indicates whether code is LOINC or SNOMED     | LOINC                                                                          |
| Version                       | LOINC or SNOMED release version               | 2.74                                                                           |
| Status                        | Indicates if code is active or deprecated     | Active                                                                         |
| Condition Name                | Name of associated reportable condition       | Chlamydia trachomatis infection (disorder)                                     |
| Condition Code                | SNOMED value associated with condition        | 240589008                                                                      | 
| Condition Code System         | System used for condition code                | SNOMEDCT                                                                       |
| Condition Code System Version | SNOMED version associated with condition code | 2023-03                                                                        |
| Value Source                  | Source of value (e.g. RCTC vs manual mapping) | RCTC                                                                           |   
| Created At                    | Date that table entry updated                 | 20231020                                                                       |

The column names are taken directly from the [RCTC spreadsheet](https://docs.google.com/spreadsheets/d/1rO_p145xXO1AD76vx8vBqXgoQUnLqfc8/edit#gid=739612351) with the exception of "Value Source" and "Created At" which are additional columns added for administrative purposes that will be used when updating the table. Both LOINC and SNOMED codes are combined in this table and can be identified by column "Code System".
The RCTC does a fairly good job of keeping up to date with LOINC and SNOMED codes and is regularly updated. It is anticipated that despite this there will be a requirement to map codes that are not present in the condition mapping table. These will have to be mapped manually after review by RS personnel in order to ensure that the proper condition code is mapped to the LOINC or SNOMED code. These codes can be submitted to CSTE valueset reviewers to be included in future releases. If a column is not applicable it can be left blank unless it is the "Code", "Code System", "Condition Name", "Condition Code"  "Value Source" columns.
It is possible that an observation may not contain information intended to convey the results of a diagnostic test but instead represent additional important information regarding the testing sample of patient. Information of this type is referred to as "Ask at Order Entry" (AOE) questions commonly referred to by the abbreviation AOE only. The specific information being asked is identified in the observation with a LOINC code in OBX-3 (HL7-V2) or Observation.Code.Coding.Code (FHIR). We need to be able to identify these observations as AOEs in the same manner as we would identify which reportable condition the observation represents so that they can be filtered out if requested by STLTs and also to eliminate them as being unmapped observations. The LOINC codes for the base table come from two lists on LOINC.org.
<br>1.) [LOINC terms for SARS-CoV-2 AOE questions](https://loinc.org/sars-cov-2-and-covid-19/)<br>
2.) [Public health laboratory ask at order entry panel](https://loinc.org/81959-9)<br>

## Updating the table 
There are two ways to update the table.

1.) Automatically using the Value Set Authority Center (VSAC) API <br>
2.) Manually by downloading and manipulated the table locally

In the event that an update to the RCTC introduces a new valueset, or we need to map a code that does not exist in the RCTC such as a local code or AOE, we will need to update the table manually for the time being. This is accomplished by downloading the table locally to a CSV
file and then add or remove rows as needed. The Condition Code System for any mappings added ad-hoc (included the AOEs) should be labled as "ReportStream" since they do not come from any standardized ValueSet.

*********Important, ensure that when editing the file locally, entries containing longer codes do not get converted to a different format (If editing in excel, it will attempt to use scientific notation for longer numeric values)*********

All tables should be updated in staging first using any of the below methods and smoke tests run against staging prior to pushing changes to production. In order to run the smoke tests use the ./prime test CLI command. Steps to run this tests against the staging environment can be found in the "Deployment Process" document pinned to the top to #prime-reportstream-deployment channel or in the [running-tests document](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/getting-started/running-tests.md). In order to run any commands against the remote environments, you will first need to run the ./prime login --env <prod-or-staging> CLI command to obtain an access token.

### Updating RCTC valuesets

The RCTC valuesets are updated regularly. These valuesets are stored in the Value Set Authority Center (VSAC) and can be accessed via an [API](https://www.nlm.nih.gov/vsac/support/usingvsac/vsacsvsapiv2.html).
In order to update the table automatically using the VSAC API you will need a UMLS license (free). To obtain a license follow these steps: <br>

1.) Visit the [VSAC Support Center](https://www.nlm.nih.gov/vsac/support/index.html)
2.) Click on "Request a UMLS License" under "How to Use VSAC"
3.) Login with an identity provider
4.) Get apikey from profile to populate parameter -k in commands below
    a.) Navigate to https://uts.nlm.nih.gov/uts/
    b.) Select "My Profile" in top right
    c.) Copy value from "API KEY"

### Updating all RCTC valuesets from the Value Set Authority Center (VSAC)
Use ./prime lookuptables update-mapping CLI command to update all RCTC value sets from the VSAC. Be sure to select yes when prompted to write the table or output a new local csv. In order to run the commands against a remote environment (Prod or Staging) you will first need to run the ./prime login --env <prod-or-staging>  CLI command to obtain a token to access the remote environment.

Example:
```
./prime lookuptables update-mapping -a -n observation-mapping -k YOUR_API_KEY -e prod
```
options:
```
-e, --env <name>          Connect to <name> environment. Choose between [local|test|staging|prod]
-s, --silent              Do not generate diff or ask for confirmation
-a, --activate            Activate the table upon creation
-n, --name TEXT           The name of the table to perform the operation on
-o, --output-file=<path>  Specify a file to save the table's data as CSV  
-d, --OID                 Provide a comma-separated list of OIDs to update         
```

### Update one of more specific valueset(S) from the Value Set Authority Center(VSAC)
To update only specific valuesets in the table include the -d parameter and provide a comma separated list of OIDs. This will add new values from only the specified valuesets. 

Example:
```
./prime lookuptables update-mapping -a -n observation-mapping -k YOUR_API_KEY -d comma_separated_list_of_OIDS -e prod
```
options:
```
-e, --env <name>          Connect to <name> environment. Choose between [local|test|staging|prod]
-s, --silent              Do not generate diff or ask for confirmation
-a, --activate            Activate the table upon creation
-n, --name TEXT           The name of the table to perform the operation on
-o, --output-file=<path>  Specify a file to save the table's data as CSV  
-d, --OID                 Provide a comma-separated list of OIDs to update   
```

### Downloading the active table manually

When the table needs to be updated manually you should first download the active table locally before updating using the ./prime lookuptables CLI command. 

Example
```
./prime lookuptables get -n observation-mapping -v "table version to get" -o "file-to-output-to"
```
options:
```
  -e, --env=<name>          Connect to <name> environment. Choose between
                            [local|test|staging|prod]
  -o, --output-file=<path>  Specify a file to save the table's data as CSV
  -n, --name=<text>         The name of the table to perform the operation on
  -v, --version=<int>       The version of the table to get
  -h, --help                Show this message and exit
```

## Finding the active version of a table

Table versions can be found by looking in the "Lookup Table Version" table in staging or prod Metabase. The active version of a table will have the value "true" in the "Is Active" column of the table.  

### Uploading new table manually

Uploading the table to remote environments can utilize the lookuptables CLI command (./prime lookuptables create). Creating a new table with the same name will automatically create a new version of that table with that name and activate it if the -a parameter is used.

Example:

```
./prime lookuptables create -i "file-path-location" -s -a -n observation-mapping -e "prod or staging"
```
options:
```
-e, --env <name>       Connect to <name> environment. Choose between [local|test|staging|prod]
-i, --input-file PATH  Input CSV file with the table data
-s, --silent           Do not generate diff or ask for confirmation
-a, --activate         Activate the table upon creation
-n, --name TEXT        The name of the table to perform the operation on
--show-table           Always show the table to be created
-f, --force            Force the creation of new table(s) even if it is already exist
-h, --help             Show this message and exit
```

## Checking for mapping failures
Mapping failues are logged in the action log and can be viewed using the below query:

```postgresql
SELECT action_log.created_at,
       detail ->> 'message'      as message,
       detail ->> 'fieldMapping' as field,
       action_log.report_id,
       report_file.body_url
FROM action_log
         INNER JOIN report_file ON report_file.report_id = action_log.report_id
WHERE action_log.detail ->> 'errorCode' = 'INVALID_MSG_CONDITION_MAPPING'
ORDER BY action_log.created_at DESC
LIMIT 100;
```

Output will include the missing code, its origin, and the URL of the source data. Use the azure storage explorer
or the azure portal to download the file being careful to observe PII precautions.

### Example output

| message | field | report\_id | body\_url |
| :--- | :--- | :--- | :--- |
| Missing mapping for code\(s\): N | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |
| Missing mapping for code\(s\): Y | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |
| Missing mapping for code\(s\): N | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |
| Missing mapping for code\(s\): N | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |
| Missing mapping for code\(s\): 260415000 | observation.valueCodeableConcept.coding.code | 3a947e0f-0832-403d-a9d8-92f9b88557a8 | http://localhost:10000/devstoreaccount1/reports/receive%2Fdevelopment.dev-elims%2FNone-3a947e0f-0832-403d-a9d8-92f9b88557a8-20240102233706.fhir |


## Creating a new observation-mapping table from scratch

Note: This should not ever be necessary for production use as the table has already been created and should be iterated on. The instructions here are to document how the base table was created.

### Columns
Refer to the latest [observation-mapping.csv](/prime-router/metadata/tables/local/observation-mappings.csv). The column names below
are accurate as of the publish date of this document.

- Member OID
- Name
- Code
- Descriptor
- Code System
- Version
- Status
- condition_name
- condition_code
- Condition
- Code System
- Condition Code System Version
- Value Source
- Created At

The `Value Source` column needs to be manually entered based on data source below. Current possible values:
- LOINC.org
- RCTC

### Data sources
- RCTC: [AIMS](https://ersd.aimsplatform.org/#/home) *Account required*
- AOE Questions *Account required for download (can copy/paste w/o)*
    - [LOINC General](https://loinc.org/81959-9)
    - [LOINC COVID](https://loinc.org/sars-cov-2-and-covid-19/#aoe)

#### Reportable Conditions Trigger Codes (RCTC)
We need to source data from the following spreadsheets:
- Organism-Substance
- Lab Order Test Name
- Lab Obs Test Name

Scroll down in the spreadsheet and you will find an `Expansion List` table. Each row in this table represents a test or
some resource associated with an OID. Each resource/test needs to be mapped to a condition at the top of the
spreadsheet using its code/OID. The resulting row is then mapped to the appropriate columns in the lookup table.

#### AOE Questions
Either copy the values from the website and map them into the appropriate columns  
-OR-  
Sign up for an account, download the CSV, and map the data from it

## Sender Onboarding

As part of sender onboarding we will need to check the list of codes that the sender will be sending against the list of codes that are mapped in the observation-mapping table. In order to accomplish this we will need the list of LOINC/SNOMED order and result codes the sender will be sending (also known as a "compendium") in a CSV file. Information on this process can be found in [senders.md](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/senders.md)

### How to map condition to LOINC/SNOMED code not found in RCTC and not an AOE

The RCTC is frequently updated and should contain the majority of codes that we receive. In the event we need to map a Lab Developed Test (LDT) or a LOINC/SNOMED code that is not represented in the RCTC
we will need to determine what standardized SNOMED condition code it should be mapped to. In some cases this will be obvious and we can use our best judgment (i.e. a test for Flu should be mapped to Influenza, a test for chlamydia should be mapped to chlmydia etc.).
In cases where the condition is not obvious we should check with the sender (this will usually be accomplished in the sender onboarding as described in [sender.md](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/onboarding-users/sender-onboarding/senders.md)). The sender's assertion of the appropriate condition the code is mapped to should be validated with a receiving STLT.
