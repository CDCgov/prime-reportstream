# Managing Translation Schemas

## Context

Schemas are used at multiple points in the universal pipeline

- transforming items sent by senders
- transforming items right before they are delivered to a receiver

and cover different kinds of transforms:

- HL7 -> FHIR
    - **This is handled by another [library](https://github.com/LinuxForHealth/hl7v2-fhir-converter) with limited configuration option of where to read the mapping files from**
- FHIR -> FHIR
- FHIR -> HL7

These schemas are currently stored as files that are included in the deployed application and read from the file sytem
when they are applied to an item.

## Problem

The overarching problem is that since the schemas are part of the deployed application they can only be updated at the frequency at which the application itself is deployed.  This is quite limiting and has downstream impacts such as:

- onboarding new senders/receivers can take a while as iterative changes to the schema have to be tested out over several days or weeks
- bugs discovered in the schema cannot be immediately addressed without a hotfix
- the senders/receivers do not have any capability to self-serve changes to their own schemas

## Goal

Design a new implementation that enables iterating on the schemas outside of the deployments and unlocks the ability to implement a feature that enables sender/receivers to self-serve on changes.

## Storing and resolving schemas

### DB

This solution would involve a drastic re-write from the existing functionality as the current libraries are all driven by the file system and the solution would likely involve moving away from schemas that could be edited by hand.

#### Possible Implementations
- Store a fully resolved schema for each sender/receiver
    - Table for sender and receiver transforms
    - Store common elements
    - On updates resolve the schemas again and store new versions
    - Use the same versioning logic as `SETTING` table
- Update the schema definitions to reference database IDs and use those when resolving schemas
    - Rather than `schema: ../common/patient-contact` use `schema: {SOME-DB-ID}`
- Use a JSONB store
    - Do away with using YAML as the format and store all the schemas as JSON
    - Shared elements could either be duplicated across schemas (store the fully resolved schemas) or a sender/receiver schemas could be kept separate from the common elements

#### Pros
- Could potentially enable reporting on what is set in the schemas
- Common elements used across many senders/receivers might be cleaner to implement in a relational database
- There is an existing pattern that could be used from `SETTING`
- Reading values from the DB would perform well

#### Cons
- Would likely be a large re-write as the existing file model does not map well to a relational DB
- A custom solution would need to be implemented for the HL7->FHIR since the library only reads the mapping from disk
- Testing this change would be extremely complicated especially considering the current infrastructure around testing the DB
- Would make it much more difficult to extract libraries for other users to consume as the libraries would be more closely tied to the db

### Azure blob storage (Recommended)
This solution would maintain the majority of the existing functionality and just provide updates to use the azure blob storage as the underlying "file" storage rather than disk.  This solution would get versioning automatically by enabling [azure blob versioning](https://learn.microsoft.com/en-us/azure/storage/blobs/versioning-overview); this functionality enables automatically creating new versions when uploading a new file and then APIs for rolling back file updates in the event that there was a problem.

Additionally, the versioning has the added flexibility of referencing a specific version of a blob so schemas could reference version 3 while the next version is iterated on.
`https://<storage-account>.blob.core.windows.net/<container>/<blob-name>?versionid=<version-id>` . If the decision is made to continue to store some of the common schemas in source code this is an approach that could be adopted here as well by adding a version to the directory paths `file:///{BASE_DIR}/metadata/hl7_mapping/common/patient/v1/patient.yml``

#### FHIR -> FHIR, FHIR -> HL7
Steps for migrating:

1. Update the code to use the URI to determine if the schema should be read from disk or azure by looking at the URI scheme
    - Fallback to the existing behavior if it is not a valid URI (i.e. we're resolving an old schema with relative paths)
2. Update all the schema references (i.e. `extends`, `schemaRef`, `schemaName`, etc.) in the existing schemas and settings to reference absolute paths as URLs
    - `schema: ../common/patient` -> `schema: file:///{BASE_DIR}/metadata/hl7_mapping/common/patient.yml`
3. Update schemas to reference azure blobs (i.e `azure:///{AZURE_STORAGE_LOCATION}/metadata/hl7_mapping/common/patient.yml`) and upload schemas to azure blob service
4. Update all UP sender and receivers to reference the azure schema

During the migration process, it would make sense to all take another look at the directory structure and names to make sure they make sense moving forward.

#### HL7 -> FHIR
Long term, it would be great to add support to this library (it's open sourced) that would support reading files from different file-like storage solutions rather than just disk, but this would likely not be feasible to get done in a quick enough timeframe.  Instead, before instantiating the converter, the application will sync the azure storage to a spot on the disk the library will read from.

#### Cache
**This could potentially be deferred until it's been identified that the network requests to azure blob storage are the bottleneck**

Since the pipeline will now require reading several files out of the blob store at multiple spots, it will likely make sense to cache the resolved schemas since they will change relatively infrequently.  This could initially be done with a simple in-memory cache using the schema URI as the cache key.

#### Pros
- Maintains the current file based model for the schemas
- Schemas can be easily edited by hand via the Azure UI
- Supports reading schemas from azure as well as from disk
    - Enables leaving the common schemas on disk if that's preferred
- Enables easy rollback
    - Post MVP, a UI could be enabled to allow selecting what version to go back to
- Supports reading files from azure or from disk

#### Cons
- Need to support to different URI schemes
- Relies on pulling files out of azure which is slower than disk or the database
- Requires more complicated error handling

## Managing schemas
The schemas break down into two categories:

- schemas dedicated to a specific sender or receiver
  - with one specifically being a "leaf" schema or a schema that is consumed by name in the pipeline and associated to a sender or receiver setting
- schemas that are used across multiple senders or receivers across organizations
    - in some cases, it might be preferable to keep some of these in the source to prevent accidentally breaking things

and there will need to be proper permissions placed such that schemas that are used across multiple senders and receivers can only be edited by prime admins

### New APIs
- Transform types `{transformType}` parameter
    - HL7 -> FHIR: `hl7-fhir`
    - FHIR -> FHIR: `fhir-fhir`
    - FHIR -> HL7: `fhir-hl7`

#### Sender/Receiver
- Create a sender/receiver schema
    - POST
    - Auth: Prime admins, org admins
    - `/v1/senders|receivers/{sender|receiverName}/schemas/{transformType}`
    - Body
        - `schemaName` string
            - This is the path the schema should be placed at i.e. `metadata/hl7_mapping/common/patient.yml`
        - `leafSchema` boolean
            - If the field is set to true, set the `schemaName` property on the sender or receiver setting and indicates that this is a schema that will be consumed in the pipeline
        - `schema` string
        - `sampleInput` string
          - An input message that should have the schema applied against; if provided, `sampleOutput` must be provided as well
        - `sampleOutput` string
          - An output message that is checked against the value produced after applying the transform against the `sampleInput`
- Update a sender/receiver schema
    - PUT
    - Auth: Prime admins, org admins
    - `/v1/senders|receivers/{sender|receiverName}/schemas/{transformType}/{*schemaName}`
    - Body
        - `schema` string
        - `sampleInput` string
          - An input message that should have the schema applied against; if provided, `sampleOutput` must be provided as well
        - `sampleOutput` string
            - An output message that is checked against the value produced after applying the transform against the `sampleInput`
- Rollback a sender/receiver schema
    - Rolls back to the previous version of the provided schema name. If rolling back the first version, the file is deleted and the schemaName field is removed from the setting
    - Auth: Prime admins, org admins
    - DELETE
    - `/v1/senders|receivers/{sender|receiverName}/schemas/{transformType}/{*schemaName}`

#### Common schemas
- Create a common schema
    - POST
    - Auth: Prime admins
    - `/v1/schemas/{transformType}`
    - Body
        - `schemaName` string - optional
            - If the field is set also update the sender or receiver setting
        - `schema` string
- Update a common schema
    - PUT
    - Auth: Prime admins
    - `/v1/schemas/{transformType}/{*schemaName}`
    - Body
        - `schema` string
- Rollback a common schema
    - Rolls back to the previous version of that schema name, if the rolling back the first version, the file is deleted and the schemaName field is removed from the
    - DELETE
    - Auth: Prime admins
    - `/v1/senders|receivers/{sender|receiverName}/{transformType}/{*schemaName}`
- Invalidate cache
    - Invalidates all the cached values for the particular transform type; would potentially need to get invoked if someone edited the schema in the azure store
      - Future iterations could make this invalidation more fine-grained
    - GET
    - Auth: Prime admins
    - `/v1/schemas/{transformType}`

### Validation

As part of either creating or updating any schema, it will need to be validated before it can get persisted. Every schema that is uploaded will check that the YAML can be parsed and that resolving it does not throw a `SchemaException` that are currently thrown when parsing a schema by trying to resolve it.
Additionally, the APIs for changing schemas for a sender or receiver can accept a sample input and output message that will be used to verify that the schema works as expected.

Future iterations could improve upon this:

- Provide the specific line that is invalid

## Operations

### Local Development

Once the migration to store the schemas as data rather that source code is done, there will still need to be a solution in place that enables local developers to submit reports through the universal pipeline with sender/receiver transforms applied.  The easiest solution is to update the `reloadSettings` gradle task (or add a new `reloadSchemas`) that invokes the create schema APIs for some of the ignore senders and receivers.

### Testing

This is the area that is impacted the most by shifting from treating schemas as source code to data.  The current approach when working on a schema change is to make the change in the source code and then update the output file to reflect what the schema change should now do and these integration tests are run as part of CI builds to verify that they continue to work as expected.  This approach will no longer work effectively once the schemas are editable via the application; an example issue that could occur: an engineer is iterating on a schema change for a STLT. While going through these iterations, the schema test for that STLT will break and pull requests will be stuck.

Some potential solutions (several could be used):
- Increase the coverage of the underlying library for performing the transforms
- Keep the most frequently used schemas in source code and create integration tests with sample schemas to verify commonly used functionality
- Version all common schemas and test sender/receiver schemas with updates in staging before updating in production
- Build upon the API functionality for validating a schema as part of creating/updating against a sample input/output to create a test harness that can be run in staging or production

### Performance
- The main additional bottleneck will be the additional network requests to azure blob storage when resolving a schema, but this will be mitigated by implementing a caching solution

### Error handling
- Retries will need to be baked into the code that loads schemas out of azure

## Open Questions
- What is the long term goal for a UI to edit schemas?
- Do we need to change the underlying hl7v2-fhir-converter?
    - It is only configured to read files from disk and this will need to be worked around

## Followups

- Update the HL7 -> FHIR library to support reading files from more than just the file system

## Update log

