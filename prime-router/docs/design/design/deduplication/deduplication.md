
# Universal Pipeline Deduplication

## Introduction

Refer to the [UP Deduplication epic](https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/14103) for full background information.

#### Technical Overview
Deduplication shall be performed at the item level (a FHIR Bundle) after conversion to FHIR happens. Each bundle will have key elements (see [Key FHIR Elements table](#key-fhir-elements)) converted to a string which is then hashed deterministically and stored in the database (item_lineage.item_hash). That hash will only be compared against other hashes from the same sender that were created within the last year.

#### Scope for Initial Implementation
Deduplication will only be implemented for ORU_R01 messages from FULL_ELR topic senders. Covid Pipeline deduplication functionality should remain unchanged.

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
- Sender id/name needs to be incorporated into pre-hashed string. (Dependent upon sql efficiency investigation noted below).
- Investigation TODO (_To be removed and appropriate sections updated before merge_): Investigate the time efficiency of the following options:
  - Adding a sender id column to item_lineage table (and using this to narrow the SQL query).
  - Adding the sender id (or name) into the string to be hashed and allowing the column index to be the main SQL query parameter.
  - Performing a SQL join to a table with the sender id to narrow the query. 
  - Notes:
    - (Date range would also be applied to the SQL query in all scenarios.) 
    - (Comment from Arnej: "Evaluate the performance, quality, and security trade-off of including the sender name in the hash vs querying it.")
    - (Comment from Michael: "For this to be performative you would need to be sure that this index can fit in memory of postgresql instead of having to go into disk.")

##### Item Hash Comparison
Hash comparison will be skipped if deduplication is disabled on that sender's settings. The generated item hash shall first be compared with items in the same report. (Note: At this part of the Convert Step, parallelization is not a blocker as the original items are [still within scope](https://github.com/CDCgov/prime-reportstream/blob/15648395efc2b60322d931bf88e0c2c5b6cc0371/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L510)). The item hash will then be compared to existing hashes from the item_lineage table. There is an [existing SQL Query](https://github.com/CDCgov/prime-reportstream/blob/9ec0a59c73d7dad9a319cd321baf9efd71ceab46/prime-router/src/main/kotlin/azure/DatabaseAccess.kt#L166-L183) which performs the hash comparison through a database query. This will need to be enhanced or recreated as the original query only searches within the last 7 days and does not take sender into account. (_This last part will be unnecessary if the sender is incorporated into the hash._) 
- If item is not a duplicate OR sender has deduplication disabled,
    - Save the item hash to the ItemLineage object [when the Report object is created](https://github.com/CDCgov/prime-reportstream/blob/4a2231af2031bc3b2d5d7949d2b21d33c525c44d/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L329).
        - [Report.ParentItemLineageData()](https://github.com/CDCgov/prime-reportstream/blob/cadc9fae10ff5f83e9cbf0b0c0fbda384889901d/prime-router/src/main/kotlin/Report.kt#L373) will need to be enhanced so that it can accept the generated hash string. The contents will then take the place of [the random UUID which is currently stored](https://github.com/CDCgov/prime-reportstream/blob/cadc9fae10ff5f83e9cbf0b0c0fbda384889901d/prime-router/src/main/kotlin/Report.kt#L412) in the column for UP items.
    - Continue processing as normal.
- If item is a duplicate,
  - Item should be handled so that FHIRConverter treats it as “empty” and not route the item.
  - Save a null result for the item hash on the ItemLineage object when the [Report object is created](https://github.com/CDCgov/prime-reportstream/blob/4a2231af2031bc3b2d5d7949d2b21d33c525c44d/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L285).
  - Create action log warning
    - Duplicates will be logged using the existing action logger pattern ([Ex 1](https://github.com/CDCgov/prime-reportstream/blob/0c5e0b058e35e09786942f2c8b41c1d67a5b1d16/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L526-L533), [Ex 2](https://github.com/CDCgov/prime-reportstream/blob/cadc9fae10ff5f83e9cbf0b0c0fbda384889901d/prime-router/src/main/kotlin/fhirengine/engine/FHIRReceiverFilter.kt#L307-L315)). These will be visible in the Submission History API.
    - TODO: How to format/label item duplication in History Endpoint API?
      - Feedback Notes:  "Think of the data that would be helpful to see: item's message ID, index in the submitted report, the date and report ID of the report that contains the duplicate item and its index in that report?" 
  - Set flag or enter a workflow to check if entire report is duplicate.
    - If the entire report is found to be duplicate, this will be logged as an error.
    - TODO: "Duplicate report was detected and removed." +  metadata (the date and report ID)

### Deduplication Workflow Placement
- The deduplication workflow will happen at the very end of the convert step processing ([FHIRConverter.kt#L541](https://github.com/CDCgov/prime-reportstream/blob/c942a9a6f6be347d82196939e1cf677512af4a06/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L541)).
  - Before the Deduplication Workflow, the Convert Step will:
    - Convert to FHIR, if necessary
    - De-batched items from the report
    - Parse these items into bundles
    - Validate the items
    - Stamp Observations with condition codes
  - During the Deduplication Workflow, the Convert Step will:
    - Generate a hash for the item
    - Compare this hash against those batched in the same submitted report
    - Compare this hash with others stored in the database
    - Create appropriate action logs if any duplicates were detected
  - After the Deduplication Workflow occurs, the Convert Step will perform necessary pipeline functions:
    - Apply sender schema transform
    - Make a report object with item lineage
      - **This will now include the newly generated hash**
    - Create a route event
    - Upload to blobstore
    - Update the database
    - Add the report to the route destination filter step queue

<BR><img alt="Dedupe_Design.jpg" height="310" src="Dedupe_Design.jpg" width="450"/>

### Other Updates
- Sender Settings: Should have a binary setting for the sender. **By default, the deduplication workflow is enabled.**
  - TRUE – Deduplication detection is active.
  - FALSE – Deduplication detection is not active. (Item hashes will still be stored in the item_lineage table.)
- Receiver Step: There is stubbed code to be removed here: The SumissionReceiver refers to the once theorized area where UP deduplication would be invoked. ([SubmissionReceiver.kt#L284-L292](https://github.com/CDCgov/prime-reportstream/blob/0c5e0b058e35e09786942f2c8b41c1d67a5b1d16/prime-router/src/main/kotlin/SubmissionReceiver.kt#L284-L292))
- Database: No updates will be made to the schema of the database itself.
- Database: Investigation will need to happen on how to best purge out of date records on the item_lineage table. item_lineage.created_at will be used for this. This is outside the scope for this design/implementation.
- FHIRDestinationFilter and FHIRReceiverFilter both currently use Report.getItemHashForRow. They are incorrectly adding non-null data to item_lineage.item_hash. These steps should be only adding null (or `"0"`, Ex: [Report.generateReportAndUploadBlob](https://github.com/CDCgov/prime-reportstream/blob/4a2231af2031bc3b2d5d7949d2b21d33c525c44d/prime-router/src/main/kotlin/Report.kt#L1654)) values for item_hash. 
  - TODO: Investigate other potential incorrect use of this column. Other possible entry points may create ItemLineage objects directly or use other functions which call it such as [Report.createItemLineageForRow](https://github.com/CDCgov/prime-reportstream/blob/4a2231af2031bc3b2d5d7949d2b21d33c525c44d/prime-router/src/main/kotlin/Report.kt#L1404).

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