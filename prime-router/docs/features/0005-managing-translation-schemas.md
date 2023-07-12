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
  - one of them will typically be set in the `setting` for either the sender or receiver
- schemas that are used across multiple senders or receivers across organizations
    - for now, these will continue to be stored in the source code since changes here should be carefully rolled out

### Directory Structure

Below is the directory structure that we would implement inside the azure blob store.  It includes the location of
where common schemas used across organizations would go, but the current thought is whether (and which ones) they should exist in the blob
store will be decided down the road since this design supports the file living in either location.  The largest change from the existing
directory structure is that a sample input and output file will be included in order to verify the schema works as expected.

**Note: the `fhir_mapping` directory will be lifted exactly as is to azure since the directory structure is decided by the underlying library**

```
/azure-blob-container
    /fhir_mapping
        valid-2023-03-29T23:53:00.488Z.txt
    /fhir_transforms
        valid-2023-03-29T23:53:00.488Z.txt
        /common
        /receivers
        /senders
    /hl7_mapping
        valid-2023-03-29T23:53:00.488Z.txt
        /receivers
            /STLTS
                /CA
                    CA.yml
                    /resources
                        ca-aoe-note.yml
                        ...
                    /sample-input
                        sample.fhir
                    /sample-output
                        sample.hl7
            /flexion
                ...
```

### Creating/updating translation schemas

The current process for creating or updating translation schemas provides a high degree that changes will work as expected
and will not have any downstream impact since they exist in source code and integration tests guarantee the schemas are 
correct before merging is allowed.  An example of a down stream impact is a schema that is used in a receiver's setting, but
also used in an `extend` property. See [default-sender-transform.yml](https://github.com/CDCgov/prime-reportstream/blob/3e69f874c78d49bd69b8303a38ba3e0a747ef3f9/prime-router/metadata/fhir_transforms/senders/default-sender-transform.yml)
and [simple-report-sender-transform.yml](https://github.com/CDCgov/prime-reportstream/blob/3e69f874c78d49bd69b8303a38ba3e0a747ef3f9/prime-router/metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml)
as an example.

A side effect of this is that schemas cannot be atomically edited and validated.  Using the above example, changing `default-sender-transform`
would necessitate at the minimum changing the sample out for `simple-report-sender-transform` and possibly require making a change
to the schema itself.  This issue can end up involving many schemas as there are no limits on the use of the `extend` property.

The workflow that will be employed to create or update schemas will be the following.

1. Developer syncs all the schemas in azure to their local environment
2. Developer makes changes to the schemas and their samples
3. Developer verifies these changes by running the validation that asserts that running the schema with its sample input matches the sample output
4. Developer syncs schemas to the staging azure container and a `validating.txt` file is created
5. An azure function with a BlobTrigger on the existence of `validating.txt` and runs the same validation in staging
    - If the validation fails a page goes off and all files are rolled back to a previous version
6. The `validating.txt` file is replaced with a `valid-{timestamp}.txt`
7. The developer runs the GitHub action that syncs the staging and production azure containers performing the same validation

New Prime CLI command:

- `syncSchemas`
  -  This command will sync schemas between the various environments and will be used by developers locally to grab up-to-date
  versions of the schemas used in production as well as pushing updates to staging and production.  Before syncing the command will perform a 
  few checks
    - it will check for the presence of a `validating.txt` file in the `from` location and error if it's found; this would mean the schemas
    have just been updated in the environment and are being checked fo validity; more changes should not be pushed until that process is complete
    - it will check that the timestamp in `valid-{timestamp}.txt` for the `from` location is greater than or equal to the timestamp in the `to` location;
    this prevents a developer from overwriting any updates that were synced after they had copied schemas down locally
  - after those checks are completed, the following steps will run
    - the current `valid-{timestamp}.txt` is renamed `previous-valid-{timestamp}.txt` 
    - invoke the [`azcopy`](https://learn.microsoft.com/en-us/azure/storage/common/storage-ref-azcopy-copy)
    command
      - `azcopy cp '{from}' '{to}' --recursive`
    - create a `validating.txt` file
  - The special case for this command will be copying schemas from staging/production to the localhost which will not perform any validation
  - Arguments
  - `transformType`
    - Options: `hl7-fhir`, `fhir-fhir`, `fhir-hl7`
  - `from`
    - Options: `local`, `staging`, `production`
  - `to`
    - Options: `local`, `staging`, `production`
- `validateSchemas`
  - This command will validate that all the schemas work as expected by iterating over each of them and executing them against the sample input and verifying
  that it matches the sample output and will be used as part of the local development flow.
  - Arguments
  - `transformType`
      - Options: `hl7-fhir`, `fhir-fhir`, `fhir-hl7`
- `validateSchemas`
  - This is a local development tool to quickly verify that any schema changes are working as intended without having to go through the entire sync process

New Azure Functions:

- `Validate{TransformType}Schemas`
  - **Azure blob triggers do not support wild cards in the path so there will need to be a function per transform type**
  - This will be triggered upon the creation of a `validating.txt`
  - The function will perform the following actions
    - Run all the schemas against their sample inputs and validate that it matches the sample output
      - If this validation fails, a page will be generated and all the files will be rolled back to their previous version
    - Replace the `validating.txt` with a `valid-{timestamp}.txt`
      - If the validation fails, the `previous-valid-{timestamp}.txt` will simply be renamed to `valid-{timestamp}.txt`
      - If the validation passes, a new `valid-{timestamp}.txt` is created

New GitHub action:

- Sync Staging Schemas to Production
  - In order to avoid having developers keep azure blob credentials locally as well as to have a better access log of when
  schema updates are applied to production, this new GitHub action will be used to sync schemas between staging and production
  - It will use the same workflow of invoking the `syncSchemas` command and will then poll for an updated `valid-{timestamp}.txt`


Example of the flow with a sample directories that shows adding a new schema:

1. Developer picks up ticket to create a new schema for Flexion
    <details>
      <summary>Local</summary>
   
        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-02-25T23:53:00.488Z.txt
                    previous-valid-2023-02-23T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
    <details>
      <summary>Staging</summary>
   
        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
    <details>
      <summary>Production</summary>
   
        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
2. Developer runs `./prime syncSchemas --from staging --to local`
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
    <details>
      <summary>Staging</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
    <details>
      <summary>Production</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
3. Developer creates the new schema and make sure it works with `./prime validateSchemas`
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Staging</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
    <details>
      <summary>Production</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
4. Developer syncs the schemas to staging `./prime syncSchemas --from local --to staging`
5. The command renames the valid files
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    default-sender-transform.yml
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Staging</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    previous-previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
    <details>
      <summary>Production</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
6. The new schema is copied to staging
   <details>
         <summary>Local</summary>

           ```
               /local-azure-container
                   /fhir_transforms
                       valid-2023-03-29T23:53:00.488Z.txt
                       previous-valid-2023-02-25T23:53:00.488Z.txt
                       /default
                           sample-input.fhir
                           sample-output.fhir
                       /Flexion
                           flexion-sender-transform.yml
           ```
   </details>
   <details>
         <summary>Staging</summary>

           ```
               /local-azure-container
                   /fhir_transforms
                       previous-valid-2023-03-29T23:53:00.488Z.txt
                       previous-previous-valid-2023-02-25T23:53:00.488Z.txt
                       /default
                           sample-input.fhir
                           sample-output.fhir
                           default-sender-transform.yml
                       /Flexion
                           flexion-sender-transform.yml
                           sample-input.fhir
                           sample-output.fhir
           ```
   </details>
   <details>
         <summary>Production</summary>

           ```
               /local-azure-container
                   /fhir_transforms
                       valid-2023-03-29T23:53:00.488Z.txt
                       previous-valid-2023-02-25T23:53:00.488Z.txt
                       /default
                          sample-input.fhir
                          sample-output.fhir
                          default-sender-transform.yml
           ```
   </details>
7. The command creates the validating file
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Staging</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    validating.txt
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    previous-previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Production</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
8. The azure function is triggered and successfully validates all the schemas
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Staging</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-04-02T23:53:00.488Z.txt
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Production</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
9. The developer triggers the GitHub action and invokes the sync command
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Staging</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-04-02T23:53:00.488Z.txt
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Production</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    previous-previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
        ```
    </details>
10. Schemas are synced between staging and production and the `validating.txt` is created
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Staging</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-04-02T23:53:00.488Z.txt
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Production</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    validating.txt
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    previous-previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
11. The azure function for validation runs successfully
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Staging</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-04-02T23:53:00.488Z.txt
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>
    <details>
      <summary>Production</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-04-03T23:53:00.488Z.txt
                    previous-valid-2023-03-29T23:53:00.488Z.txt
                    /default
                        sample-input.fhir
                        sample-output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        sample-input.fhir
                        sample-output.fhir
        ```
    </details>

### Validation

The initial validation will mimic how the current integration tests work.  Each schema will:

- be checked to make sure it can be successfully
- ran with its sample input and checked against its sample output
  - failing schemas will be indicated in the output

Some follow-up improvements could include indicating which lines did not match between the sample output and produced value
or what line was invalid in the schema.

## Operations

### Rollout

1. Implement the code update to support using the URI scheme to determine how to retrieve the file
2. Update all current schemas in source code to use the `file:///` scheme
3. Create the workflow for creating/updating a translation schema used directly in the convert or translate step
    - This would be a translation schema that is referenced in a `setting`
4. Migrate all translation schemas referenced in a `setting` to azure
5. Evaluate whether schemas used across organizations should be migrated to azure
6. Evaluate if the `fhir_mapping` files for HL7->FHIR library should move to azure

### Local Development

Once the migration to store the schemas as data rather that source code is done, there will still need to be a solution in place that enables local developers to submit reports through the universal pipeline with sender/receiver transforms applied.  The easiest solution is to update the `reloadSettings` gradle task (or add a new `reloadSchemas`) that invokes the create schema APIs for some of the ignore senders and receivers.

### Testing

This is the area that is impacted the most by shifting from treating schemas as source code to data.  The current approach when working on a schema change is to make the change in the source code and then update the output file to reflect what the schema change should now do and these integration tests are run as part of CI builds to verify that they continue to work as expected.  This approach will no longer work effectively once the schemas are editable via the application; an example issue that could occur: an engineer is iterating on a schema change for a STLT. While going through these iterations, the schema test for that STLT will break and pull requests will be stuck.



### Performance
- The main additional bottleneck will be the additional network requests to azure blob storage when resolving a schema, but this will be mitigated by implementing a caching solution

### Error handling
- Retries will need to be baked into the code that loads schemas out of azure

## Open Questions
- What is the long term goal for a UI to edit schemas?
  - No
- Do we need to change the underlying hl7v2-fhir-converter?
    - It is only configured to read files from disk and this will need to be worked around

## Followups

- Update the HL7 -> FHIR library to support reading files from more than just the file system

## Update log

