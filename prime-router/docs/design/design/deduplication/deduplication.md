
# Universal Pipeline Deduplication

## Introduction

Refer to the [UP Deduplication epic](https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/14103) for full background information.

#### Technical Overview
Deduplication shall be performed at the item level (a FHIR Bundle) after conversion to FHIR happens. Each bundle will have key elements (see [Key FHIR Elements table](#key-fhir-elements)) converted to a string which is then hashed deterministically and stored in the database (item_lineage.item_hash). If the sender has deduplication enabled, that hash will only be compared against other hashes from the same sender that were created within the last year.

#### Scope for Initial Implementation
Deduplication will only be implemented for ORU_R01 messages from FULL_ELR topic senders. Covid Pipeline deduplication functionality shall remain unchanged.

#### Deduplication Differences between Covid and Universal Pipeline
 There are three key differences between the deduplication designs. In the Universal Pipeline,
- Deduplication will now happen during the Convert Step instead of the Receive Step.
- Deduplication will only consider a hash of key fields instead of the entire submitted item.
- Deduplication will only consider hashes from the same sender in the last year.

## Deduplication Workflow Design
The Convert Step is where the UP’s deduplication workflow will live. See [Deduplication Workflow Placement](#deduplication-workflow-placement) for it's context within the Convert Step.

### Convert Step

##### Item Hash Generation
The first part of the Deduplication Workflow will take in the item’s Bundle and use the specified fields ([Key FHIR Elements table](#key-fhir-elements)) to generate a string which is then hashed. The current hashing implementation ([Report.getItemHashForRow()](https://github.com/CDCgov/prime-reportstream/blob/15648395efc2b60322d931bf88e0c2c5b6cc0371/prime-router/src/main/kotlin/Report.kt#L1287-L1289)) uses Java’s [MessageDigest](https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html) to create a secure one-way hash of the string with SHA-256. This class also satisfies the requirements for UP Deduplication.

Technical Considerations:
- This should be flexible enough that different senders, message types, and topics can implement different key fields.
- The ordering of fields within a FHIR Bundle are **not** guaranteed. Before converting to a string to be hashed, the [populated key fields](#key-fhir-elements) should be extracted from the bundle and put in a static order (alphabetical etc.).
- Sender id/name shall be incorporated into the pre-hashed string. (Dependent upon sql efficiency investigation noted below).
- Investigation TODO (_To be removed and appropriate sections updated before merge_): Investigate the time efficiency of the following options:
  - Adding a sender id column to item_lineage table (and using this to narrow the SQL query).
  - Adding the sender id (or name) into the string to be hashed and allowing the column index to be the main SQL query parameter.
  - Performing a SQL join to a table with the sender id to narrow the query. 
  - Notes:
    - (Date range would also be applied to the SQL query in all scenarios.) 
    - (Comment from Arnej: "Evaluate the performance, quality, and security trade-off of including the sender name in the hash vs querying it.")
    - (Comment from Michael: "For this to be performative you would need to be sure that this index can fit in memory of postgresql instead of having to go into disk.")

##### Item Hash Comparison
Hash comparison will be skipped if `sender.allowDuplicates` is set to true. Otherwise, the generated item hash shall first be compared with items in the same report. (Note: At this part of the Convert Step, parallelization is not a blocker as the original items are [still within scope](https://github.com/CDCgov/prime-reportstream/blob/15648395efc2b60322d931bf88e0c2c5b6cc0371/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L510)). The item hash will then be compared to existing hashes from the item_lineage table. There is an [existing SQL Query](https://github.com/CDCgov/prime-reportstream/blob/9ec0a59c73d7dad9a319cd321baf9efd71ceab46/prime-router/src/main/kotlin/azure/DatabaseAccess.kt#L166-L183) which performs the hash comparison through a database query. This will need to be enhanced or recreated as the original query only searches within the last 7 days and does not take sender into account. (_This last part will be unnecessary if the sender is incorporated into the hash._) 
- If item is not a duplicate OR sender has deduplication disabled,
    - Save the item hash to the ItemLineage object [when the Report object is created](https://github.com/CDCgov/prime-reportstream/blob/4a2231af2031bc3b2d5d7949d2b21d33c525c44d/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L329).
        - [Report.ParentItemLineageData()](https://github.com/CDCgov/prime-reportstream/blob/cadc9fae10ff5f83e9cbf0b0c0fbda384889901d/prime-router/src/main/kotlin/Report.kt#L373) will need to be enhanced so that it can accept the generated hash string. The contents will then take the place of [the random UUID which is currently stored](https://github.com/CDCgov/prime-reportstream/blob/cadc9fae10ff5f83e9cbf0b0c0fbda384889901d/prime-router/src/main/kotlin/Report.kt#L412) in the column for UP items.
    - Continue processing as normal.
- If item is a duplicate,
  - Item should be handled so that FHIRConverter treats it as “empty” and not route the item.
  - Save a null result for the item hash on the ItemLineage object when the [Report object is created](https://github.com/CDCgov/prime-reportstream/blob/4a2231af2031bc3b2d5d7949d2b21d33c525c44d/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L285).
  - Create action log warning
    - Duplicates will be logged using the existing action logger pattern ([Ex 1](https://github.com/CDCgov/prime-reportstream/blob/0c5e0b058e35e09786942f2c8b41c1d67a5b1d16/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L526-L533), [Ex 2](https://github.com/CDCgov/prime-reportstream/blob/cadc9fae10ff5f83e9cbf0b0c0fbda384889901d/prime-router/src/main/kotlin/fhirengine/engine/FHIRReceiverFilter.kt#L307-L315)). These will be visible in the Submission History API. See [example warning](#example-submission-history-api-some-items-in-batched-report-are-duplicates-) below.
  - Set flag or enter a workflow to check if entire report is duplicate.
      - If the entire report is found to be duplicate, this will be logged as an error. See [example error](#example-submission-history-api-entire-report-is-duplicate) below.

##### Example: Submission History API, Some Item(s) in Batched Report are Duplicates 

```json
{
    "id": "849b3151-25f7-41f3-b19b-fd8ad47bae18",
    "submissionId": 63,
    "overallStatus": "Delivered",
    "timestamp": "2025-01-22T00:42:10.404Z",
    "sender": "development.dev-elims",
    "reportItemCount": 2,
    "errorCount": 0,
    "warningCount": 1,
    "httpStatus": 201,
    "destinations": [
        {
            "organization": "FOR DEVELOPMENT PURPOSES ONLY",
            "organization_id": "development",
            "service": "DEV_ENRICHMENT_FHIR",
            "itemCount": 1,
            "itemCountBeforeQualityFiltering": 0,
            "sending_at": "2025-01-22T00:43:00.000Z",
            "filteredReportRows": [],
            "filteredReportItems": [],
            "sentReports": [
                {
                    "reportId": "dbc9ac68-77a0-4e3e-8d8b-2d665b8fd92c",
                    "externalName": "none-c7c7e0fe-1951-4031-9d17-2895fc03c974-20250121164303.fhir",
                    "createdAt": "2025-01-22T00:43:04.252Z",
                    "itemCount": 1
                }
            ],
            "downloadedReports": []
        }
    ],
    "reportId": "849b3151-25f7-41f3-b19b-fd8ad47bae18",
    "topic": "full-elr",
    "errors": [],
    "warnings": [
        {
            "scope": "item",
            "indices": [
                1
            ],
            "trackingIds": [
                "849b3151-25f7-41f3-b19b-fd8ad47bae18"
            ],
            "message": "Duplicate message was detected and removed.",
            "errorCode": "DUPLICATION_DETECTION"
        }
    ],
    "destinationCount": 1,
}
```

##### Example: Submission History API, Entire Report is Duplicate

```json
{
    "id": "849b3151-25f7-41f3-b19b-fd8ad47bae18",
    "overallStatus": "Not Delivered",
    "timestamp": "2025-01-22T00:42:10.404Z",
    "sender": "development.dev-elims",
    "reportItemCount": 1,
    "errorCount": 1,
    "warningCount": 0,
    "httpStatus": 201,
    "destinations": [],
    "reportId": "849b3151-25f7-41f3-b19b-fd8ad47bae18",
    "topic": "full-elr",
    "errors": [
        {
            "scope": "item",
            "indices": [
                1
            ],
            "trackingIds": [
                "849b3151-25f7-41f3-b19b-fd8ad47bae18"
            ],
            "message": "Duplicate report was detected and removed.",
            "errorCode": "DUPLICATION_DETECTION"
        }
    ],
    "warnings": [],
    "destinationCount": 0,
}
```

### Deduplication Workflow Placement

<BR><img alt="Dedupe_Design.jpg" height="310" src="Dedupe_Design.jpg" width="450"/>

- The deduplication workflow will happen in the middle of the Convert Step. After conversion related processing, but before any of the queue related processing. ([FHIRConverter.kt#L541](https://github.com/CDCgov/prime-reportstream/blob/c942a9a6f6be347d82196939e1cf677512af4a06/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L541)).
  - Before the Deduplication Workflow, the Convert Step will:
    - Validate the items
    - Convert to FHIR, if necessary
    - De-batch items from the report
    - Convert these items into FHIR bundles
    - Stamp Observations with condition codes
  - During the Deduplication Workflow, the Convert Step will:
    - Generate a hash for the item
    - Compare this hash against those batched in the same submitted report
    - Compare this hash with others stored in the database
    - Create appropriate action logs if any duplicates were detected
  - After the Deduplication Workflow occurs, the Convert Step will perform necessary pipeline functions:
    - Apply sender schema transform
    - Make a report object with item lineage
      - **This will now include the newly generated (or null) hash**
    - Create a route event
    - Upload to blobstore
    - Update the database
    - Add the report to the route destination filter step queue

### Other Updates
- Sender Settings: The Universal Pipeline will use the `allowDuplicates` sender setting. 
  - By default, the deduplication workflow is enabled for UP senders. (`allowDuplicates` = false)
  - The abstract Sender class [should not initialize](https://github.com/CDCgov/prime-reportstream/blob/f48d719b876859169deb0360487f63965d8be5a0/prime-router/src/main/kotlin/Sender.kt#L60) `allowDuplicates`.
  - The UniversalPipelineSender class [should default to false](https://github.com/CDCgov/prime-reportstream/blob/f48d719b876859169deb0360487f63965d8be5a0/prime-router/src/main/kotlin/Sender.kt#L196).
- Receiver Step: There is stubbed code to be removed here: The SumissionReceiver refers to the once theorized area where UP deduplication would be invoked. ([SubmissionReceiver.kt#L284-L292](https://github.com/CDCgov/prime-reportstream/blob/0c5e0b058e35e09786942f2c8b41c1d67a5b1d16/prime-router/src/main/kotlin/SubmissionReceiver.kt#L284-L292))
- Database: No updates will be made to the schema of the database itself.
- Database: Investigation will need to happen on how to best purge out of date records on the item_lineage table. item_lineage.created_at will be used for this. This is outside the scope for this design/implementation.
- FHIRDestinationFilter and FHIRReceiverFilter both currently use Report.getItemHashForRow. They are incorrectly adding non-null data to item_lineage.item_hash. These steps should be only adding null (or `"0"`, Ex: [Report.generateReportAndUploadBlob](https://github.com/CDCgov/prime-reportstream/blob/4a2231af2031bc3b2d5d7949d2b21d33c525c44d/prime-router/src/main/kotlin/Report.kt#L1654)) values for item_hash. 
  - TODO: Investigate other potential incorrect use of this column. Other possible entry points may create ItemLineage objects directly or use other functions which call it such as [Report.createItemLineageForRow](https://github.com/CDCgov/prime-reportstream/blob/4a2231af2031bc3b2d5d7949d2b21d33c525c44d/prime-router/src/main/kotlin/Report.kt#L1404).

### Proposed Universal Pipeline SRD Additions
Under `Convert Step Business Logic Requirements`:
```
10. The Convert step shall apply the following enrichments ... [No change]
11. The Convert step shall generate a one-way deterministic hash from key fields from the FHIR bundle. (Link to Key Fields section of deduplication.md)
    11.1. For all senders with deduplication enabled, the Convert step shall check for duplicates by first matching hashes against items in the same report, and then in the database against items from same sender in past year.
        11.1.1. If an item is found to be a duplicate, the Convert step shall generate an ActionLog warning. 
        11.1.2. If all item(s) in a report are found to be duplicates, the Convert step shall generate an ActionLog error. 
        11.1.3. For all items identified this way as duplicates, the Convert step shall properly update the lineage to indicate the next_action is None and will not process the report any further.
        11.1.4. For all items that are not duplicates, the Convert step shall save the generated hash to item_lineage.item_hash.
12. If a conversion or schema application error occurs on ANY item ... [No change]
```

## Key FHIR Elements

These fields represent the criteria for an item (ORU-0R1 message) to be considered a duplicate.

|Data Element|HL7|FHIR|
|---|---|---|
|Specimen ID|SPM.2|Specimen.Identifier|
|Accession number|SPM.30 (not in v2.5.1)|Specimen.accessionIdentifier|
|Specimen Collection Date/Time (if different from other date/time)|SPM.17|Specimen.collection.collectedDateTime|
|Patient ID|PID.3|Patient.identifier|
|Patient name|PID.5|Patient.name|
|Patient DOB|PID.7|Patient.birthDate birthDate.extension[1].valueDateTime|
|Results Rpt/Status Chng – Date/Time|OBR.22|DiagnosticReport.issued|
|Result Status|OBR.25|DiagnosticReport.status|
|Performing Organization/ Testing facility CLIA|OBX.23|Observation.Performer -> Organization.Identifier.value|
|Performing Organization/ Testing facility name|OBX.23|Observation.Performer -> Organization.name|
|Test performed code|OBX.3.1|Observation.resource.code.system|
|Test performed code system|OBX.3.3|Observation.resource.code.coding|
|Date/Time of the Observation (this appears in multiple HL7 locations)|OBX.14, OBR.7, SPM.17|Observation.resource.issued DiagnosticReport.effectiveDateTime DiagnosticReport.effectivePeriod.start|
|Observation Value/ Test result code|OBX.5|Observation.resource.valueCodeableConcept.coding.code|
|Observation Value/ Test result code system|OBX.3.3|Observation.resource.valueCodeableConcept.coding.system|