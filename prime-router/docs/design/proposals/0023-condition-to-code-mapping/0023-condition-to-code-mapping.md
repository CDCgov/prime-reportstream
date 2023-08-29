# Proposal for Code-to-Condition Mapping

## Background

Public Health reporting in the United States is not uniform in nature with each public health agency (PHA) setting their
own criteria for what conditions need to be reported to that jurisdiction. In the context of electronic lab reporting (ELR), the onus is typically on the reporting entity (lab, hospital, clinic etc.) to determine which result messages qualify for each PHA the entity reports to.

Since ReportStream takes on the burden of identifying the appropriate destination for sender’s messages, it will be necessary to implement a feature to ensure only messages for the appropriate conditions/diseases are allowed to route to each connected receiver. This feature will be similar to the COVID-19 LIVD table in that it will be a table of LOINC codes and their associated conditions that is used to reference received LOINC/SNOMED codes and return their associated condition.

## Assumptions
1.) The CSTE ValueSets that make up the Reportable Conditions Trigger Codes (RCTC) will be sufficient to capture a majority of codes sent to ReportStream. If this turns out to be false, we can modify the design to include additional data sources.

## Criteria

1.) Must be able to map LOINC and SNOMED codes to condition.<br>
2.) Tables must have the ability to be updated without a PR.<br>
3.) Mapping must account for all 160 conditions available in Report Content Knowledge Management System that relate to ELR (https://www.rckms.org/conditions-available-in-rckms/) <br>
4.) Must be able to add ad-hoc mappings as needed for local codes/LDTs or other non-standard codes

## Out of Scope
The below items are not necessary to accomplish the main task of mapping code-to-condition but are instead enhancements to the core functionality that will have their own work effort.

1.) Utility to check sender test compendiums against current mapping table<br>
2.) Utility to automatically update tables from Value Set Authority Center API (https://www.nlm.nih.gov/vsac/support/usingvsac/vsacfhirapi.html) <br>
3.) Reporting mechanism to inform RS/sender when unmapped value was found.<br>
4.) Mechanism to automate which conditions each state considers reportable.

## Design


### Condition Mapping Table

The condition mapping table will be made up of CSTE ValueSets and contain the following columns:

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
| Condition Code                | SONMED value associated with condition        | 240589008                                                                      | 
| Condition Code System         | System used for condition code                | SNOMEDCT                                                                       |
| Condition Code System Version | SNOMED version associated with condition code | 2023-03                                                                        |

The column names are taken directly from the RCTC spreadsheet.Both LOINC and SNOMED codes are combined in this table and can be identified by column "Code System".

### Supplemental Condition Mapping Table

The RCTC does a fairly good job of keeping up to date with LOINC and SNOMED codes and is regularly updated. It is anticipated that despite this there will be a requirement to map codes that are not present in the condition mapping table. This will be accomplished by a supplemental condition table in a similar manner to the LIVD supplemental table with the exception that the two tables will not be merged into a single table in the database. Instead the supplemental table will only be queried if there is a failure to find a match on the main table. The supplemental table will contain the same columns as the main table. If a column is not applicable it can be left blank unless it is the "Code", "Condition Name" or "Condition Code" columns.

Example:

| Column Name                   | Description                                   | Example                                        |
|-------------------------------|-----------------------------------------------|------------------------------------------------|
| Member OID                    | ValueSet Identifier                           | 2.16.840.1.114222.4.5.288                      |
| Name                          | ValueSet Name                                 | Lab Test (PHLIP)                               |
| Code                          | LOINC or SNOMED coded value                   | PLT1483                                        |
| Descriptor                    | LOINC or SNOMED term descriptio               | Cabapenemase [Presence] in isolate by Carba NP |
| Code System                   | Indicates whether code is LOINC or SNOMED     | PLT                                            |
| Version                       | LOINC or SNOMED release version               | 2022-09-22                                     |
| Status                        | Indicates if code is active or depracated     | Active                                         |
| Condition Name                | Name of associated reportable condition       | Carbapenem resistant Enterobacteriaceae        |
| Condition Code                | SONMED value associated with condition        | 712662001                                      | 
| Condition Code System         | System used for condition code                | SNOMEDCT                                       |
| Condition Code System Version | SNOMED version associated with condition code | 2023-03                                        |

### Appending condition information to FHIR Bundle

The condition information will be most useful and easy to work with if it exists in the FHIR bundle itself. Making use of the fact that FHIR allows for metadata about a resource to include tags that include a code and display value we can provide a tag to each observation resource in a FHIR bundle that describes the condition associated with that observation

Example:
```json
        {
            "fullUrl": "Observation/d683b42a-bf50-45e8-9fce-6c0531994f09",
            "resource": {
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
                },
                "subject": {
                    "reference": "Patient/9473889b-b2b9-45ac-a8d8-191f27132912"
                },
                "performer": [
                    {
                        "reference": "Organization/1a0139b9-fc23-450b-9b6c-cd081e5cea9d"
                    }
                ],
                "valueCodeableConcept": {
                    "coding": [
                        {
                            "system": "http://snomed.info/sct",
                            "code": "260373001",
                            "display": "Detected"
                        }
                    ]
                },
                "meta": {
                    "tag": [
                          {
                              "system": "http://snomed.info/sct",
                              "code": "541131000124102",
                              "display": "Infection caused by novel Influenza A virus variant (disorder)"
                         }
                    ]
                }
            }
        }
```

In order to account for both HL7 and FHIR input from senders, the lookup should occur after conversion of the incoming HL7 V2 message into a FHIR bundle. Since the sender transforms are used after conversion from HL7 to FHIR, adding the below elements to the default sender transform will add the condition tag to the observation resource.

```yaml
  - name: test-condition
    resource: 'Bundle.entry.resource.ofType(Observation)'
    condition: '%resource.code.coding.exists()'
    bundleProperty: '%resource.meta.tag.code'
    value: ['%resource.code.coding.code']
    valueSet:
      lookupTable:
        tableName: Condition-Mapping
        keyColumn: code
        valueColumn: condition_code
        
  - name: test-condition-supplemental
    resource: 'Bundle.entry.resource.ofType(Observation)'
    condition: '%resource.code.coding.exists() and %resource.meta.tag.code.not.exists()'
    bundleProperty: '%resource.meta.tag.code'
    value: ['%resource.code.coding.code']
    valueSet:
      lookupTable:
        tableName: Supplemental-Condition
        keyColumn: code
        valueColumn: condition_code      
        
        
```
The first element will check against the RCTC values loaded into the Condition-Mapping tables, while the second will check against the supplemental table if a match was not found in the Condition_Mapping table.

Not all reportable conditions can be identified from order and result LOINC codes. In the case of bacterial cultures. The condition is identified by the found organism which is populated in the result field/resource (OBX-5 or Bundle.entry.resource.ofType(Observation).value.valueCodeableConcept). In order to correctly tag these messages, an additional element will need to be added to the transform like below to check the result code.

```yaml
- name: result-condition
  resource: 'Bundle.entry.resource.ofType(Observation)'
  condition: '%resource.code.coding.exists() and %resource.meta.tag.code.not.exists()'
  bundleProperty: '%resource.meta.tag.code'
  value: ['%resource.valueCodeableConcept.coding.code']
  valueSet:
    lookupTable:
      tableName: Condition-Mapping
      keyColumn: code
      valueColumn: condition_code
```
This element should be placed after the above elements as we only want to check the mapping for a result value if we have failed to find a match in the test ordered and test performed locations. 

### Filtering for condition

Due to the presence of the tag on the fhir bundle we can utilize any of the existing filters to manage the list of conditions for each receiver. There are several potential options for filtering.

1.) Jurisdictional filter - This option would remove visibility into which messages qualified for the STLT but were filtered out due to condition it is not recommended to use the jurisdictional filter for this purpose.<br>
2.) Condition filter - This option allows visibility into which messages were filtered out due to condition, and visually separates the condition logic from the other filters. This is the proposed method.

There are multiple ways to write the logic in the condition filter. The first way will remove any observations that do not pass the condition filter.<br>

### Condition filter logic #1

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

This will result in a message with potentially missing results in the event that there is a missing mapping, and also presents difficulties in dealing with culture results as it would require all the LOINCS for all possible micro cultures to be mapped.

### Condition filter logic #2

The second way to write the condition logic will pass the entire bundle if a match is found for one observation.

Example input: [prime-reportstream/prime-router/docs/design/proposals/0023-condition-to-code-mapping/exampleinput.fhir]()

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
          - "bundle.entry.ofType(Observation).where(meta.tag.code in (398102009' |'13906002' |'409498004' |'40610006' |'21061004' |'75702008' |'359761005' |'414015000' |'840539006' |'416925005' |'83436008')).exists()"
      timing:
        operation: MERGE
        numberPerDay: 1440 # Every minute
        initialTime: 00:00
```
Example output: [prime-reportstream/prime-router/docs/design/proposals/0023-condition-to-code-mapping/exampleoutput2.hl7]()

This will potentially pass along results for conditions that are not desired by the receiver in the event the message/bundle represents a multiplex test where the receiver only desires some of the test results (e.g. if NY wanted to receive FLU A and FLU B results but not covid results and ReportStream received a messages where all three were represented, all three would be sent). This is likely an edge case that will not be commonly encountered and the risk of missing results by going with option #1 is much higher.

## Monitoring Mapping

A key component for long term success of mapping code-to-condition will be the ability to capture codes that are unmapped and subsequently map them and re-send any affected messages. This will require that processes are in place to review and map any codes that do not have a mapping at time of receipt. This is called out in the "out-of-scope" section but reiterated here for greater clarity as it is the single most important factor to long term viability of this method.
