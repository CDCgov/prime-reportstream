# Managing Translation Schemas

## Context

Translation schemas are used at multiple points in the universal pipeline

- transforming items sent by senders from their chosen format to ReportStream's internal FHIR format
- transforming items from ReportStream's internal FHIR format to the chosen format of the receiver right before sending

and cover different kinds of transforms:

- HL7 -> FHIR
    - **This is handled by another [library](https://github.com/LinuxForHealth/hl7v2-fhir-converter) with limited configuration option of where to read the mapping files from**
- FHIR -> FHIR
- FHIR -> HL7

These translation schemas are currently stored as files that are included in the deployed application and read from the file sytem
when they are applied to an item.

## Problem

The overarching problem is that since the translation schemas are part of the deployed application they can only be updated at the frequency at which the application itself is deployed.  This is quite limiting and has downstream impacts such as:

- onboarding new senders/receivers can take a while as iterative changes to the translation schema have to be tested out over several days or weeks
- bugs discovered in the translation schema cannot be immediately addressed without a hotfix

## Goal

Design a new implementation that enables iterating on the translation schemas outside of the deployments

## Storing and resolving translation schemas

### DB

This solution would involve a drastic re-write from the existing functionality as the current libraries are all driven by the file system and the solution would likely involve moving away from translation schemas that could be edited by hand.

#### Possible Implementations
- Store a fully resolved translation schema for each sender/receiver
    - Table for sender and receiver transforms
    - Store common elements
    - On updates resolve the translation schemas again and store new versions
    - Use the same versioning logic as `SETTING` table
- Update the translation schema definitions to reference database IDs and use those when resolving translation schemas
    - Rather than `schema: ../common/patient-contact` use `schema: {SOME-DB-ID}`
- Use a JSONB store
    - Do away with using YAML as the format and store all the translation schemas as JSON
    - Shared elements could either be duplicated across translation schemas (store the fully resolved translation schemas) or a sender/receiver translation schemas could be kept separate from the common elements

#### Pros
- Could potentially enable reporting on what is set in the translation schemas
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

Additionally, the versioning has the added flexibility of referencing a specific version of a blob so translation schemas could reference version 3 while the next version is iterated on.
`https://<storage-account>.blob.core.windows.net/<container>/<blob-name>?versionid=<version-id>` . If the decision is made to continue to store some of the common translation schemas in source code this is an approach that could be adopted here as well by adding a version to the directory paths `file:///{BASE_DIR}/metadata/hl7_mapping/common/patient/v1/patient.yml``

#### FHIR -> FHIR, FHIR -> HL7
Steps for migrating:

1. Update the code to use the URI to determine if the translation schema should be read from disk or azure by looking at the URI scheme
    - Fallback to the existing behavior if it is not a valid URI (i.e. we're resolving an old translation schema with relative paths)
2. Update all the translation schema references (i.e. `extends`, `schemaRef`, `schemaName`, etc.) in the existing translation schemas and settings to reference absolute paths as URLs
    - `schema: ../common/patient` -> `schema: file:///{BASE_DIR}/metadata/hl7_mapping/common/patient.yml`
3. Update all receiver/sender translation schemas to reference azure blobs (i.e `azure:///{AZURE_STORAGE_LOCATION}/metadata/hl7_mapping/common/patient.yml`) and upload translation schemas to azure blob service
4. Update all UP sender and receivers to reference the azure translation schema

During the migration process, it would make sense to all take another look at the directory structure and names to make sure they make sense moving forward.

#### HL7 -> FHIR
Long term, it would be great to add support to this library (it's open sourced) that would support reading files from different file-like storage solutions rather than just disk, but this would likely not be feasible to get done in a quick enough timeframe.  Instead, before instantiating the converter, the application will sync the azure storage to a spot on the disk the library will read from.

#### Cache
**This could potentially be deferred until it's been identified that the network requests to azure blob storage are the bottleneck**

Since the pipeline will now require reading several files out of the blob store at multiple spots, it will likely make sense to cache the resolved translation schemas since they will change relatively infrequently.  This could initially be done with a simple in-memory cache using the translation schema URI as the cache key.

#### Pros
- Maintains the current file based model for the translation schemas
- Translation schemas can be easily edited by hand via the Azure UI
- Supports reading translation schemas from azure as well as from disk
    - Enables leaving the common translation schemas on disk if that's preferred
- Enables easy rollback
    - Post MVP, a UI could be enabled to allow selecting what version to go back to
- Supports reading files from azure or from disk

#### Cons
- Need to support different URI schemes
- Relies on pulling files out of azure which is slower than disk or the database
- Requires more complicated error handling

## Managing translation schemas
The translation schemas break down into two categories:

- translation schemas dedicated to a specific sender or receiver
  - one of them will typically be set in the `setting` for either the sender or receiver
- translation schemas that are used across multiple senders or receivers across organizations
    - for now, these will continue to be stored in the source code since changes here should be carefully rolled out and they are not subject to frequent changes
    - an example of this would be `ADT-01` directory

### Directory Structure

Below is the directory structure that we would implement inside the azure blob store.  It includes the location of
where common translation schemas used across organizations would go, but the current design states these should remain in the repository.
The largest change from the existing
directory structure is that a sample input and output file will be included in order to verify the translation schema works as expected.

**Note: the `fhir_mapping` directory will be lifted exactly as is to azure since the directory structure is decided by the underlying library**

```
/azure-blob-container
    /fhir_mapping
        valid-2023-03-29T23:53:00.488Z.txt
        previous-valid-2023-03-25T23:53:00.488Z.txt
    /fhir_transforms
        valid-2023-03-29T23:53:00.488Z.txt
        previous-valid-2023-03-25T23:53:00.488Z.txt
        /common
        /receivers
        /senders
    /hl7_mapping
        valid-2023-03-29T23:53:00.488Z.txt
        previous-valid-2023-03-25T23:53:00.488Z.txt
        /receivers
            /STLTS
                /CA
                    CA.yml
                    /resources
                        ca-aoe-note.yml
                        ...
                    input.fhir
                    output.hl7
            /flexion
                ...
```

### Creating/updating sender/receiver translation schemas

The current process for creating or updating translation schemas provides a good guarantee that changes will work as expected
and will not have any downstream impact since they exist in source code and integration tests guarantee the translation schemas are 
correct before merging is allowed.  An example of a down stream impact is a translation schema that is used in a receiver's setting, but
also used in an `extend` property. See [default-sender-transform.yml](https://github.com/CDCgov/prime-reportstream/blob/3e69f874c78d49bd69b8303a38ba3e0a747ef3f9/prime-router/metadata/fhir_transforms/senders/default-sender-transform.yml)
and [simple-report-sender-transform.yml](https://github.com/CDCgov/prime-reportstream/blob/3e69f874c78d49bd69b8303a38ba3e0a747ef3f9/prime-router/metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml)
as an example.

A side effect of this is that translation schemas cannot be atomically edited and validated.  Using the above example, changing `default-sender-transform`
would necessitate at the minimum changing the sample out for `simple-report-sender-transform` and possibly require making a change
to the translation schema itself.  This issue can end up involving many translation schemas as there are no limits on the use of the `extend` property.

The workflow that will be employed to create or update translation schemas will be the following.

1. Developer syncs all the translation schemas in azure to their local environment
2. Developer makes changes to the translation schemas and their samples
3. Developer verifies these changes by running the validation that asserts that running the translation schema with its sample input matches the sample output
4. Developer syncs translation schemas to the staging azure container and a `validating.txt` file is created
5. An azure function with a BlobTrigger on the existence of `validating.txt` runs the same validation in staging
    - If the validation fails a page goes off and all files are rolled back to a previous version
6. The `validating.txt` file is replaced with a `valid-{timestamp}.txt`
7. The developer runs the GitHub action that syncs the staging and production azure containers performing the same validation

New Prime CLI command:

- `syncSchemas`
  -  This command will sync translation schemas between the various environments and will be used by developers locally to grab up-to-date
  versions of the translation schemas used in production as well as pushing updates to staging and production.  Before syncing the command will perform a 
  few checks
      - it will check for the presence of a `validating.txt` file in the `from` location and error if it's found; this would mean the translation schemas
    have just been updated in the environment and are being checked for validity; more changes should not be pushed until that process is complete
      - it will check that the timestamp in `valid-{timestamp}.txt` for the `from` location is the same or after the timestamp in the `to` location;
    this prevents a developer from overwriting any updates that were synced after they had copied translation schemas down locally
  - after those checks are completed, the following steps will run
      - the `previous-valid-{timestamp}.txt` is stored temporarily as `previous-previous-valid-{timestamp}.txt` in case of the need to rollback
      - the current `valid-{timestamp}.txt` is renamed `previous-valid-{timestamp}.txt` 
      - invoke the [`azcopy`](https://learn.microsoft.com/en-us/azure/storage/common/storage-ref-azcopy-copy)
    command
          - `azcopy cp '{from}' '{to}' --recursive`
      - create a `validating.txt` file
  - The special case for this command will be copying translation schemas from staging/production to the localhost which will not perform any validation
  - Arguments
      - `transformType`
          - Options: `hl7-fhir`, `fhir-fhir`, `fhir-hl7`
      - `from`
          - Options: `local`, `staging`, `production`
      - `to`
         - Options: `local`, `staging`, `production`
- `validateSchemas`
  - This command will validate that all the translation schemas work as expected by iterating over each of them and executing them against the sample input and verifying
  that it matches the sample output and will be used as part of the local development flow.
  - Arguments
     - `transformType`
        - Options: `hl7-fhir`, `fhir-fhir`, `fhir-hl7`

New Azure Functions:

- `Validate{TransformType}Schemas`
  - **Azure blob triggers do not support wild cards in the path so there will need to be a function per transform type**
  - This will be triggered upon the creation of a `validating.txt`
  - The function will perform the following actions
    - Run all the translation schemas against their sample inputs and validate that it matches the sample output
      - If this validation fails, a page will be generated and all the files will be rolled back to their previous version
    - Replace the `validating.txt` with a `valid-{timestamp}.txt`
      - If the validation fails, the `previous-valid-{timestamp}.txt` will simply be renamed to `valid-{timestamp}.txt`
      - If the validation passes, a new `valid-{timestamp}.txt` is created

New GitHub action:

- Sync Staging Schemas to Production
  - In order to avoid having developers keep azure blob credentials locally as well as to have a better access log of when
  translation schema updates are applied to production, this new GitHub action will be used to sync translation schemas between staging and production
  - It will use the same workflow of invoking the `syncSchemas` command and will then poll for an updated `valid-{timestamp}.txt`


Example of the flow with a sample directories that shows adding a new translation schema:

1. Developer picks up ticket to create a new translation schema for Trusted Intermediary 
    <details>
      <summary>Local</summary>
   
        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-02-25T23:53:00.488Z.txt
                    previous-valid-2023-02-23T23:53:00.488Z.txt
                    /default
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
        ```
    </details>
3. Developer creates the new translation schema and make sure it works with `./prime validateSchemas`
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
        ```
    </details>
4. Developer syncs the translation schemas to staging `./prime syncSchemas --from local --to staging`
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
        ```
    </details>
6. The new translation schema is copied to staging
   <details>
         <summary>Local</summary>

           ```
               /local-azure-container
                   /fhir_transforms
                       valid-2023-03-29T23:53:00.488Z.txt
                       previous-valid-2023-02-25T23:53:00.488Z.txt
                       /default
                           input.fhir
                           output.fhir
                       /Flexion
                           flexion-sender-transform.yml
                           input.fhir
                           output.fhir
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
                           input.fhir
                           output.fhir
                           default-sender-transform.yml
                       /Flexion
                           flexion-sender-transform.yml
                           input.fhir
                           output.fhir
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
                          input.fhir
                          output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
        ```
    </details>
8. The azure function is triggered and successfully validates all the translation schemas
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
        ```
    </details>
10. Translation schemas are synced between staging and production and the `validating.txt` is created
    <details>
      <summary>Local</summary>

        ```
            /local-azure-container
                /fhir_transforms
                    valid-2023-03-29T23:53:00.488Z.txt
                    previous-valid-2023-02-25T23:53:00.488Z.txt
                    /default
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
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
                        input.fhir
                        output.fhir
                        default-sender-transform.yml
                    /Flexion
                        flexion-sender-transform.yml
                        input.fhir
                        output.fhir
        ```
    </details>

#### Validation

The initial validation will mimic how the current integration tests work.  Each translation schema will:

- be checked to make sure it can be successfully
- ran with its sample input and checked against its sample output failing translation schemas will be indicated in the output

Some follow-up improvements could include indicating which lines did not match between the sample output and produced value
or what line was invalid in the schema.

### Creating/updating translation schemas used across organizations

These will continue to live in the source code and will only get updated as part of the application getting deployed.  Changes to these translation
schemas will need to be handled very carefully and there will be two primary update paths.

Non-breaking changes:

These can be made by directly editing the file and opening a PR to merge update.

Breaking changes:

A breaking change will need to be handled differently as there is no longer a guarantee that an update to a translation schema in the source
code will be updated synchronously with files that live in azure.  In order to support this use case a versioning system will be introduced

```
/hl7_mapping
    /ADT_A01
      ADT-A01-base.yml
      /v1
        ADT-A01-base.yml
```

In the example above, the `/v1` directory contains a breaking change in the translation schema.  A developer would merge and deploy a PR
that introduces the new version and then separately update all receivers schemas that consume it using the normal azure translation update and validation process.

#### Validation

To make sure that a breaking change does not accidentally leak into the system, the CI build will be updated to sync the azure schemas from production
and then run the validation CLI command, failing the build if validation does not pass.

## Operations

### Rollout

1. Implement the code update to support using the URI scheme to determine how to retrieve the file
2. Update all current translation schemas in source code to use the `file:///` scheme
3. Create the workflow for creating/updating a translation schema used directly in the convert or translate step
    - This would be a translation schema that is referenced in a `setting`
4. Migrate all translation schemas referenced in a `setting` to azure
5. Evaluate whether schemas used across organizations should be migrated to azure
6. Evaluate if the `fhir_mapping` files for HL7->FHIR library should move to azure

### Performance
- The main additional bottleneck will be the additional network requests to azure blob storage when resolving a schema, but this will be mitigated by implementing a caching solution

### Error handling
- Retries will need to be baked into the code that loads translation schemas out of azure

## Open Questions
- What is the long term goal for a UI to edit translation schemas?
  - No
- Do we need to change the underlying hl7v2-fhir-converter?
    - It is only configured to read files from disk and this will need to be worked around

## Followups

- Update the HL7 -> FHIR library to support reading files from more than just the file system
- Improve validation output
- Optimize the validation flows to detect schema usage and only run validation against impacted on

## Update log

