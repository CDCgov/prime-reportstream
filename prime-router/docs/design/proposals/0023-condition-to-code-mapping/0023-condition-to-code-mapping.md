# Proposal for Code-to-Condition Mapping

## Background

Public Health reporting in the United States is not uniform in nature with each public health agency (PHA) setting their
own criteria for what conditions need to be reported to that jurisdiction. In the context of electronic lab reporting (ELR), the onus is typically on the reporting entity (lab, hospital, clinic etc.) to determine which result messages qualify for each PHA the entity reports to.

Since ReportStream takes on the burden of identifying the appropriate destination for sender’s messages, it will be necessary to implement a feature to ensure only messages for the appropriate conditions/diseases are allowed to route to each connected receiver. This feature will be similar to the COVID-19 LIVD table in that it will be a table of LOINC codes and their associated conditions that is used to reference received LOINC/SNOMED codes and return their associated condition.

## Assumptions
1.) The Council of State and Territorial Epidemiologist (CSTE) ValueSets that make up the Reportable Conditions Trigger Codes (RCTC) will be sufficient to capture a majority of codes sent to ReportStream. If this turns out to be false, we can modify the design to include additional data sources.

## Out of Scope
The below items are not covered in this proposal.

1.) Mechanism to automate which conditions each state considers reportable.

## Design

Overview Diagram

![img.png](code-to-condition diagram.png)

### Creating Observation Mapping Table

Criteria:

1.) Must be able to map LOINC and SNOMED codes from observations to a SNOMED code representing a reportable condition.<br>
2.) Table must have the ability to be updated without a PR <br>
3.) Mapping must account for all 160 conditions available in the CSTE [Report Content Knowledge Management System](https://www.rckms.org/conditions-available-in-rckms/) that relate to ELR.<br>
4.) Must be able to add ad-hoc mappings as needed for local codes/LDTs or other non-standard codes including LOINC codes for AOE questions

The observation mapping table will be used to map LOINC/SNOMED codes to their relevant reportable conditions in the UP. The initial table is made up of values that are contained in valuesets maintained by the Council of State and Territorial Epidemiologists (CSTE) on the National Library of Medicines' [Value Set Authority Center (VSAC)](https://vsac.nlm.nih.gov/). The valuesets are compiled and mapped to conditions by CSTE and published in the Reportable Conditions Trigger Codes (RCTC) documents used by the CSTE/AIMS product Reportable Condition Knowledge Management System (RCKMS) project.
The RCTC is available to be downloaded from AIMS at https://ersd.aimsplatform.org/#/home. A login is required (registration is free).

The Observation Mapping table will be made up of CSTE ValueSets and contain the following columns:

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
1.) [LOINC terms for SARS-CoV-2 AOE questions](https://loinc.org/sars-cov-2-and-covid-19/)<br>
2.) [Public health laboratory ask at order entry panel](https://loinc.org/81959-9)<br>

The Condition Code System for any mappings added ad-hoc (included the AOEs) should be labled as "ReportStream" since they do not come from any standardized ValueSet.

Example Table:

| Member OID                     | Name                                                                             | Code    | Descriptor                                              | Code System| Version  | Status | Condition Name                            | Condition Code | Condition Code System | Condition Code System Version | Value Source | Created At |    
|--------------------------------|----------------------------------------------------------------------------------|---------|---------------------------------------------------------|------------|----------|--------|-------------------------------------------|----------------|-----------------------|-------------------------------|--------------|------------|
| 2.16.840.1.113762.1.4.1146.239 | Chlamydia trachomatis Infection (Tests for Chlamydia trachomatis Nucleic Acid)   | 16601-7 | Chlamydia trachomatis rRNA [Presence] in Urine by Probe | LOINC      | 2.74     | Active | Chlamydia trachomatis infection (disorder)| 240589008      | SNOMEDCT              | 2023-03                       | RCTC         | 20231020   | 
|                                | SARS-CoV-2 AOE questions                                                         | 97155-6 | SARS coronavirus 2 (COVID-19) immunization status       | LOINC      | 2.70     | Active | Ask at order entry question               | AOE            | ReportStream          | 1.0                           | LOINC.org    | 20231020   |


Uploading the table to remote environments can utilize the lookuptables CLI command (./prime lookuptables create). Creating a new table with the same name will automatically create a new version of that table with that name and activate it if the -a parameter is used.

Example

```
./prime lookuptables create -i 'file-path-location' -s -a -n observation-mapping -e staging
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

### Sender Compendium Comparison Utility

In order to greatly reduce the number of unmapped errors that we will have to deal with. It is recommended that we build a utility where senders can submit their test compendiums during the onboarding process to allow engagement to pre-emptively map any missing codes. This will be a utility that takes in a csv file prepared by the sender and compares the codes against the observation mapping table. The utility should then return which codes are not found in the table so that the engagement team can map them prior to the sender moving to production.

Criteria:

1.) Utility must be able to be invoked via a CLI</br>
2.) Utility must be able to use a CSV file as input<br>
3.) Utility must be able to output a CSV file with an additional column indicating whether or not a row containing the same code and code system exists on both the input CSV file and the observation mapping table.


Example parameters: <br>
```
env - environment to run command against <br>

tableName - name of table to compare input to <br>

input -file location of input CSV <br>

output -location to output post comparison csv
```
Example CLI command:

./prime mapping-table-comparison -tableName observation-mapping -input <input-file-location> output <output-file-location> -env staging


Example input compendium CSV:
```csv
test code,test description,coding system
97099-6,Influenza virus A and B and SARS-CoV-2 (COVID-19) Ag panel - Upper respiratory specimen by Rapid immunoassay, LOINC
47457-7,Influenza virus A H8 Ab [Titer] in Serum, LOINC
123456, LDT Flu Test, ABC TESTING LABS
```
Example Observation Mapping Table:

| Member OID                     | Name                                                 | Code    | Descriptor                                                                          | Code System      | Version | Status | Condition Name                                                 | Condition Code  |
|--------------------------------|------------------------------------------------------|---------|-------------------------------------------------------------------------------------|------------------|---------|--------|----------------------------------------------------------------|-----------------|
| 2.16.840.1.113762.1.4.1146.798 | Influenza (Tests for influenza A virus Nucleic Acid) | 80382-5 | Influenza virus A Ag [Presence] in Upper respiratory specimen by Rapid immunoassay  | LOINC            | 2.74    | Active | Infection caused by novel Influenza A virus variant (disorder) | 541000000000000 |
| 2.16.840.1.113762.1.4.1146.799 | Influenza (Tests for influenza A virus Antigen)      | 80382-5 | Influenza virus A Ag [Presence] in Upper respiratory specimen by Rapid immunoassay  | LOINC            | 2.74    | Active | Influenza (disorder)                                           | 6142004         |
| ABC TESTING LABS               | Influenza - (ABC TESTING LABS)                       | 123456  | Influenza virus A                                                                   | ABC TESTING LABS |         | Active | Infection caused by novel Influenza A virus variant (disorder) | 541000000000000 |

Example output CSV:

```csv
test code,test description,coding system, mapped?
97099-6,Influenza virus A and B and SARS-CoV-2 (COVID-19) Ag panel - Upper respiratory specimen by Rapid immunoassay, LOINC., Y
47457-7,Influenza virus A H8 Ab [Titer] in Serum, LOINC, Y
123456, LDT Flu Test, ACME LABS, N
```

### Manually Updating Observation Tables
There may occur situations where we have to update the table manually such as when adding ad-hoc values that do not belong to a published value set. This can be accomplished with the same lookuptables CLI command used to create the tables above. (./prime lookuptables get). This will allow Engagement to extract the current table and add values as needed.

Example:

```
./prime lookuptables get -o 'file-path-location' -n observation-mapping -v 2 -e staging
```
options:
```
-e, --env <name>        Connect to <name> environment. Choose between [local|test|staging|prod]
-o, --output-file PATH  Output CSV file with the table data to specified file location
-n, --name TEXT         The name of the table to perform the operation on
-v, --version           The version of the table to get
-h, --help              Show this message and exit
```

The output CSV file can then have the necessary values added and the new table uploaded following the steps from Creating Observation Mapping Table section above. 

### Mapping LOINC/SNOMED codes in received message/bundle to condition code

Criteria

1.) Mapping solution must work whether the input data is an HL7 V2 message or a FHIR bundle. <br>
2.) Must be able to compare LOINC or SNOMED codes from both OBX-3-1/Observation.code.coding.code (LOINC) and OBX-5-1/Observation.ValueCodeableConcept.coding.code (SNOMED) to values in "code" column of the Observation Mapping table and return value(s) from "condition code" column.<br>
3.) Values from "Condition Code" column must be appended to FHIR bundle as an element so they can be used in FHIRpath condition filter logic. <br>
4.) Must be able to return multiple values from observation-mapping table if LOINC/SNOMED code maps to multiple condition codes <br>

Information regarding which reportable condition(s) an HL7 V2 message or FHIR Diagnostic Report is representing is not stored in a single element but instead can be extrapolated from either a LOINC code identifying what test was performed (OBX-3 HL7 V2 or Observation.Code.Coding.Code) or in the case of microbacterial cultures a SNOMED code identifying what organism was found (OBX-5 HL7 V2 or Observation.ValueCodeableConcept.Coding.Code).
![HK7](HL7-condition-information.png)![FHIR](fhir-condition-information.png)

Reportable condition information is needed to determine whether a particular diagnostic report will qualify to route to a particular receiver. This can be accomplished using the condition filter if condition information is stored in a defined element in a FHIR resource that can be accessed via a FHIRPath expression. The FHIR US Core Implementation guide does not currently define an element on the observation resource that stores reportable condition information so ReportStream will have to create our own extension for the Observation resource that can store the coded value from the Observation-Mapping table representing the condition information. The extension will need to repeat for every matching value found in the table for the LOINC/SNOMED code in the observation resource.  

The condition information will need to be appended to the FHIR bundle prior to the Universal Pipeline's Route step as the condition information will be used to determine whether a particular diagnostic report will qualify to route to a particular receiver. In the Convert step of the pipeline we currently utilize functions called FHIR Bundle Helpers to modify existing FHIR bundles. In order to map the code to condition  







### Filtering for condition

Due to the presence of the tag on the fhir bundle we can utilize any of the existing filters to manage the list of conditions for each receiver. There are several potential options for filtering.

1.) Jurisdictional filter - This option would remove visibility into which messages qualified for the STLT but were filtered out due to condition it is not recommended to use the jurisdictional filter for this purpose.<br>
2.) Condition filter - This option allows visibility into which messages were filtered out due to condition, and visually separates the condition logic from the other filters. This is the proposed method.

There are multiple ways to write the logic in the condition filter. The first way will remove any observations that do not pass the condition filter.<br>

### Condition filter logic #1 (Recommended)

Example input: [prime-reportstream/prime-router/docs/design/proposals/0023-condition-to-code-mapping/exampleinput.fhir]()

Example condition logic:
```yaml
    - name: TEST-RECEIVER
      externalName: TEST
      organizationName: TEST
      topic: full-elr
      customerStatus: active
      jurisdictionalFilter:
        - "(%performerState.exists() and %performerState = 'TEST') or (%patientState.exists() and %patientState = 'TEST')"
      qualityFilter: []
      conditionFilter:
          - "%resource.where(meta.tag.code in (398102009' |'13906002' |'409498004' |'40610006' |'21061004' |'75702008' |'359761005' |'414015000' |'840539006' |'416925005' |'83436008')).exists()"
      timing:
        operation: MERGE
        numberPerDay: 1440 # Every minute
        initialTime: 00:00
```
Example output: [prime-reportstream/prime-router/docs/design/proposals/0023-condition-to-code-mapping/exampleoutput1.hl7]()

This approach will prune any observations that do not match the listed conditions in the filter during the translation step (FHIRTranslator.kt).


## Monitoring Mapping

A key component for long term success of mapping code-to-condition will be the ability to capture codes that are unmapped and subsequently map them and re-send any affected messages. This will require that processes are in place to review and map any codes that do not have a mapping at time of receipt.
In order to monitor for unmapped codes we need to log in the Action Log when an unmapped value is sent from a sender and have a process to add that mapping to the table. Below are two proposed strategies to add an entry to the Action Log when an unmapped value is encountered. For both strategies it is recommended to add a new ActionLogLevel in ActionLog.kt of "mapping" to differentiate missing mappings from other logged issues. Errors logged in the action log will contain a "tracking Id" and creation time to allow for follow-up on messages containing mapping errors.

Example:
```kotlin
enum class ActionLogLevel {
    info,
    warning,
    error,
    filter,
    mapping
}
```
### Logging Missing Mapping Strategy 

The proposed way of monitoring mapping is to add functionality to LookupTableValueSet.kt to log a mapping error to the Action Log if the LOINC/SNOMED from the targeted observation resource used as the Key does not return a Value.

Example:

For the below resource:

```json
  {
    "resourceType": "Observation",
    "id": "d683b42a-bf50-45e8-9fce-6c0531994f09",
    "status": "final",
    "code": {
      "coding": [
       {
          "system": "http://loinc.org",
          "code": "80382-5"
        }
      ],
      "text": "Flu A"
    }
  }
```
the below element in the default transformation utilizes LookupTableValueSet to use key "80382-5" to search Observation-Mapping table stored in the database to return a value from the condition_code column

```yaml
- name: test-condition
  resource: 'Bundle.entry.resource.ofType(Observation)'
  condition: '%resource.code.coding.exists()'
  bundleProperty: '%resource.meta.tag.code'
  value: ['%resource.code.coding.code']
  valueSet:
    lookupTable:
      tableName: Observation-Mapping
      keyColumn: code
      valueColumn: condition_code
```

In the example table below we can see that there is no match found for code "80382-5"

Example Observation Mapping Table:

| Member OID                     | Name                                                 | Code    | Descriptor                                                                          | Code System | Version | Status | Condition Name                                                 | Condition Code   | Condition Code System | Condition Code System Version | Value Source |
|--------------------------------|------------------------------------------------------|---------|-------------------------------------------------------------------------------------|-------------|---------|--------|----------------------------------------------------------------|------------------|-----------------------|-------------------------------|--------------|
| 2.16.840.1.113762.1.4.1146.798 | Influenza (Tests for influenza A virus Nucleic Acid) | 80588-7 | Influenza virus A M gene [Presence] in Nasopharynx by NAA with probe detection      | LOINC       | 2.74    | Active | Infection caused by novel Influenza A virus variant (disorder) | 541000000000000  | SNOMEDCT              | 2023-03                       | RCTC         |
| 2.16.840.1.113762.1.4.1146.799 | Influenza (Tests for influenza A virus Antigen)      | 88904-8 | Influenza virus A Ag [Presence] in Lower respiratory specimen by Immunofluorescence | LOINC       | 2.74    | Active | Infection caused by novel Influenza A virus variant (disorder) | 541000000000000  | SNOMEDCT              | 2023-03                       | RCTC         |
|                                | Influenza - (ABC TESTING LABS)                       | 123456  | Influenza virus A                                                                   | LOCAL       |         | Active | Infection caused by novel Influenza A virus variant (disorder) | 541000000000000  | SNOMEDCT              | 2023-03                       | LOCAL        |   

Example Action Log Error Message: "Missing mapping for: 123456 for table: Observation-Mapping"

In this instance we would add an entry to the Action Log if a null value or empty string was returned. The content of the error message should include both the keyValue and the tableName in the format: "Missing mapping for: " + keyValue + " for table:" + tableName".
By following this strategy we could also log and monitor missing mappings for any lookupTable which is used in a translation schema, not just condition mappings.

Example Action Log Detail:

```json
 {
    "class": "gov.cdc.prime.router.ReportStreamMapping",
    "scope": "item",
    "message": "Missing mapping for: 123456 for table: Observation-Mapping",
    "filteredTrackingElement": "<tracking id>"
}
```

## Monitoring Logged Errors

Mapping errors logged by the action log can be monitored through metabase queries by the engagement/customer service team. The following is an example query that would be monitored on a regular cadence (dependent on error volume).

Example query
```yaml
select * from action_log
where type = 'mapping'
and created_at >= current date -1
```
## Unmapped receiver queue

Creation of a receiver with the correct filters will allow us to catch all messages with unmapped observations in a queue for Engagement to work. Depending on how we write the condition filter for this queue we can either capture the entire message intact even if there is only one of many observations that are  Combined with Action Log monitoring engagement will be able to update all mappings and resend affected messages as needed to STLTs. In order to create this receiver the following settings would be needed:

```yaml
- name: "unmapped-message-queue"
  description: "All messages that contain unmapped observations"
  jurisdiction: "FEDERAL"
  filters:
      - topic: full-elr
        jurisdictionalFilter: ["%patientState.exists()"]
        conditionFilter: ["Bundle.entry.resource.ofType(Observation).where.meta.tag.code.exists().not()"]
  featureFlags: null
  keys: null
  senders: []
  receivers:
      - name: "default"
        organizationName: unmapped-message-queue
        topic: "full-elr"
        customerStatus: "active"
        translation:
            schemaName: "metadata/hl7_mapping/ORU_R01/ORU_R01-base"
            type: HL7
        jurisdictionalFilter: []
        qualityFilter: []
        routingFilter: []
        reverseTheQualityFilter: false
        conditionFilter: []
        deidentify: false
        deidentifiedValue: ""
        timing:
            operation: MERGE
            numberPerDay: 1440 # Every minute
            initialTime: 00:00
        transport:
            type: SFTP
            host: BLANK
            port: BLANK
            filePath: BLANK
            credentialName: BLANK
        externalName: null
        timeZone: null
        dateTimeFormat: "OFFSET"
```
This will gather all unmapped messages into a single queue and also fire a "last mile failure" alert that will automatically ping the engagement slack channel when a message is added to the queue.

## Automated Mapping updates

Since the bulk of the mapping table will be made up of values taken from CSTE codesets that are stored in the Value Set Authority Center (VSAC). We can make use of the VSAC's [FHIR Terminology Service](https://www.nlm.nih.gov/vsac/support/usingvsac/vsacfhirapi.html) to query and update the table. Using the "Member OID" and "Version" columns from the table we can create a utility to query each Member OID for which the "Value Source" column is "RCTC" to see if we have the latest version and if not to update to the latest version. This should be a CLI utility similar to update-livd-table command that can be run on-demand to update the table.

The FHIR Terminology Service for VSAC Resources is a RESTful API service for accessing the VSAC value sets and supported code systems.

Example parameters: </br>
environment - name of table to compare input to </br>
silent - do not generate diff or ask for confirmation
activate - activate the table upon creation
key - apikey to access VSAC FHIR terminology service
OID - OID of valueset to update (default to all OID in table if blank)

Example CLI command for update tool:

./prime condition-mapping-update --activate --silent --environment staging --key <apikey>
