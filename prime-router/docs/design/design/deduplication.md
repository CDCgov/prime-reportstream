
# ReportStream Deduplication (Universal Pipeline) 

# Introduction 

#### Main Points 
- For a variety of reasons, it is possible that senders will send a report or item to ReportStream multiple times. This data would be recorded in error if added as unique data. In order to avoid issues for receivers ReportStream must implement a deduplication process. 
- This implementation of deduplication will consider something a duplicate if it is an item which has the exact same key fields (see Index 2 for a list of these fields) as another item sent by the same sender in the past year. At the report level, a report will be considered a duplicate if all items in the report are evaluated asdetermined to be duplicates. If an item is marked as a duplicate, then it will effectively be filtered out and not be sent to any receivers. A note of duplicate items/reports will be visible in the Submission History API.  
#### Technical Overview 
- Deduplication happens shall be performed at the item level (a FHIR Bundle) after conversion to FHIR happens. Each bundle will have key elements (see Index 2) converted to a string which is then hashed deterministically and stored for a year in the database (item_lineage.item_hash). The hash for the current item will be compared with all Valid hashes stored over the past year for that specific sender.  
#### Scope 
- Deduplication MVP will only be activated for ORU_R01 messages for FULL_ELR topic senders. Other message types or topics should be taken into account if their design affect the requirements. Other message types, such as test orders, shall be considered out of scope for this effort since deduplication requirements may be different. 
- Deduplication will only occur between items sent by the same sender within the past year.  
- Deduplication will only take into account certain key fields from the bundle (index 2).  

#### History of Deduplication on the Covid Pipeline  
- Refer to the [current epic](https://app.zenhub.com/workspaces/platform-6182b02547c1130010f459db/issues/gh/cdcgov/prime-reportstream/14103) for full background information. 
- Differences between deduplication in Covid Pipeline and Universal Pipeline: 
	- Deduplication will now happen during the Convert Step instead of the Receive Step 
	- Deduplication will only consider a hash of key fields instead of the entire submitted item. 
	- Deduplication will only consider hashes from the same sender.. 

# Requirements 

## Deduplication Workflow 

#### Considerations: 
- There are two instances where report data is altered in the Convert step. These instances must be taken into consideration because they have the ability to alter the data which forms the basis of the item hash. 
	- Condition stamping happens in this step. ([FHIRConverter.kt#L520](https://github.com/CDCgov/prime-reportstream/blob/fbede22be1805e34a7aaf28ba5ab259663dea38e/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L520)) 
	- Sender schema transformations application happen in this step. ([FHIRConverter.kt#L326](https://github.com/CDCgov/prime-reportstream/blob/fbede22be1805e34a7aaf28ba5ab259663dea38e/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L326)) 

#### Requirements: 
- Timing: The deduplication workflow will happen at the very end of the [convert step processing (FHIRConverter.kt#L541)](https://github.com/CDCgov/prime-reportstream/blob/c942a9a6f6be347d82196939e1cf677512af4a06/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L541). Reasoning: This is after items have been: converted, de-batched from report, parsed as bundles, validated, and stamped with condition codes. This is the end of formal convert step “processing”. After Convert Step “processing” the FHIRConverter will: Apply sender schema transform, make a report object with item lineage, create a route event, upload to blobstore, track the created report, add the report to the route destination filter step queue.  

### Deduplication Steps: 

- Item Hash generation. 
	- This will take in the item’s bundle and use only the specified fields (index 2) to generate a string which is then hashed. The current implementation uses [Java’s MessageDigest](https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html) to create a secure one-way hash of the string with SHA-256. TODO: Is this still the best choice for our purposes.  
	- Report.kt (see: getItemHashForRow()) is where this currently happens. TODO: Investigate best location for Item Hash generation. 
	- Important note: The ordering of fields within a bundle are not guaranteed. The method responsible for the Item Hash will have to force specific ordering when generating the hash. 
	- TODO: Need to verify that hash storage does not break db column constraints. 
- Item Hash comparison. 
	- Before comparison: Check sender settings. If sender has deduplication disabled, then skip comparison.  
		- TODO: Is this desired? This design would store item_hashes for senders even if they have deduplication disabled. This may be beneficial when this setting must be temporarily toggled. Otherwise, the entire deduplication workflow can be skipped.
- The generated Item Hash will first be compared with items in the same report.  
		- TODO: Consideration for items being processed in parallel. This may affect the best placement for deduplication workflow. 
	- The Item Hash will then be compared to existing hashes.  
		- This will happen with a SQL select query which will look for a match on item_lineage.item_hash within all results that are: 
			- Stored within the last year 
			- Were sent by the same sender 
- If item is a duplicate, 
	- Item should be “nulled” out so that FHIRConverter can treat it as an “empty” item. See [FHIRConverter.kt#L284-L322](https://github.com/CDCgov/prime-reportstream/blob/3f0b47d3b1526659b76049792a0412a3f01ed74e/prime-router/src/main/kotlin/fhirengine/engine/FHIRConverter.kt#L284-L322) 
		- This will cause the FHIRConverter to treat this item as an unprocessible report and will not create any routing or item lineage.  
		- TODO: Is it correct that no item_lineage row should be created for duplicate items?
- Create action log warning 
		- TODO: How to format/label item duplication in History Endpoint API? 
	- Set flag to check if entire report is duplicate (separate logging/treatment if this is the case).  
		- Note: This is out of scope for the MVP. This also may not be trivial. Would require searching parent lineage for related items. 
- If item is not a duplicate, 
	- Save Item Hash to item_linease.item_hash 
	- TODO: No clue where or how this happens at the moment. May need to add item_hash to several classes to allow for the hash to be tracked. 
		- Might be happening automatically as part of FHIREngine.kt or Report.kt hooks? 
	- Continue processing as normal. 
    
#### Receiver Step Requirements 
- Code to be removed: There is commented out code in the Receiver step referring to the theorized area where UP deduplication would be invoked. 
#### Convert Step Requirements 
- The Convert step is the entry point for the UP’s deduplication workflow.  
- As much of the processing in the convert step as possible should occur before the deduplication workflow happens.

#### Database Requirements 
- DatabaseAccess.kt will need a new query helper to check for duplicate hashes according to the new requirements (item hashes are unique to sender). (See existing db helper query for reference: [DatabaseAccess.kt#L171-L183](https://github.com/CDCgov/prime-reportstream/blob/3f0b47d3b1526659b76049792a0412a3f01ed74e/prime-router/src/main/kotlin/azure/DatabaseAccess.kt#L171-L183)) 
- SQL query currently operates against the item_lineage table which will need to be joined with a table that contains sender information.  
	- Note: It may be more efficient to save item sender directly to the item_lineage table. 
- No database tables or fields will need to be updated. 
#### Hash Generation Requirements 
- Must work regardless of key field ordering in bundle.  
- Hash created with key fields must be deterministic. 
- Only key fields which are present should be considered. If an item does not contain all key fields, then only those available fields should be hashed.  
	- Thus, if a sender submits an identical item but with a new key field, this will not be marked as duplicate. 
- Ordering of fields must remain static. When creating the string to be hashed from key fields, ordering of the elements must be enforced as FHIR element order is not guaranteed. 
- Covid Pipeline uses [Java’s MessageDigest](https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html) for hashing. 

#### Hash Comparison Requirements 
- Only compare to individual items from same sender within past year. 
- Must account for operations happening in parallel 
	- Scenario: Duplicate item submitted while original is still in pipeline. 
	- Scenario: Report sent with items which are duplicate of each other. 
- Existing SQL Query: [DatabaseAccess.kt#L166-L183](https://github.com/CDCgov/prime-reportstream/blob/9ec0a59c73d7dad9a319cd321baf9efd71ceab46/prime-router/src/main/kotlin/azure/DatabaseAccess.kt#L166-L183) 
	- Because this solution uses SQL to do filtering and matching, we’re able to pawn off questions of efficiency onto SQL’s performance, instead of loading everything into memory and creating our own algorithms. Unlikely that a faster solution could be found. 

#### Logging Requirements 
- Action logs generated when reports are marked as duplicated. 
	- Note: These logs are sucked up into the Submission History API. These logs should not be displayed as warnings in this API response.  
- Nice to have: If entire Message is a duplicate then this is logged with a different message. 
	- Outstanding Q: Should this scenario be an error?  
	- Outstanding Q: Should all duplication logs be in new info section, or warning?  

#### Sender Setting Requirements 
- Should have a binary setting for the sender. This will default to deduplication workflow is active 
	- TRUE – Deduplication workflow happens 
	- FALSE – Deduplication workflow does not happen

#### Database / Azure Requirements 
- Should require no updates to database. The existing item hash column is capable of storing the hash and all other requirements will be satisfied through Convert Step and related code 

#### History Endpoint Requirements 
- Note: The requirements to update the Submission History API response are not part of the MPV for Deduplication. 
- When items are marked as duplicates, these should not be listed as warnings (TODO: Verify with Jessica). If an entire report is marked as duplicate, then this would ideally appear as an error.




### Index 1. Code Areas NOT Receiving Changes (TODO: Remove Index 1, not necessary) 

- Submission Response 
- Covid Pipeline 

### Index 2. DEDUPLICATION KEY FIELDS FOR HASH 

Criteria for an item (ORU-0R1 message) to be considered a duplicate 

Combine all the following into a hash. Should the hashes match, remove the corresponding item(s) that correlate to the subsequent instance(s).  

|   |   |   |
|---|---|---|
|Data Element|HL7|FHIR|
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
|Date/Time of the Observation (this appears in multiple HL7 locations)|OBX.14, OBR.7, SPM.17|Observation.resource.issued  DiagnosticReport.effectiveDateTime DiagnosticReport.effectivePeriod.start|
|Observation Value/ Test result code|OBX.5|Observation.resource.valueCodeableConcept.coding.code|
|Observation Value/ Test result code system|OBX.3.3|Observation.resource.valueCodeableConcept.coding.system|