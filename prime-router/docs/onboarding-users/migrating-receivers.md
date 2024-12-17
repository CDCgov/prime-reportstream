# How to Migrate an existing receiver to the UP

## Welcome

The goal of this documentation is to provide a guide on how to migrate existing covid receivers to the Universal pipeline.

The main goal of receiver onboarding is to establish a connection to send data from ReportStream to the STLT.  
In order to do so there are multiple receiver configurations that need to be configured.

## Pre-Migration
* Identify if the STLTs transport is set-up in staging [How to check if STLTs transport is working](#how-to-check-if-stlts-transport-is-working)
    *  Determine if receiver's SFTP server needs to be whitelisted by the CDC [How to check if STLT needs to be whitelisted](#how-to-check-if-stlt-needs-to-be-whitelisted)
* During kick-off call -Determine if we need to send to the existing endpoint and if new credentials are needed for existing endpoint

## Table of Contents
1. [Fetch current receiver settings](#1-fetch-current-receiver-settings)
2. [Update receiver settings to route messages through UP](#2-update-receiver-settings-to-route-messages-through-up)
3. [Send test message from SimpleReport to STLT](#3-send-test-message-from-simplereport-to-stlt)
4. [Set-up Receiver Transforms](#4-set-up-receiver-transforms)
5. [Compare Covid and UP messages](#5-compare-covid-and-up-messages)
6. [Testing in Staging](#6-testing-in-staging)
7. [Move setting to production](#7-move-settings-to-production)

### 1. Fetch current receiver settings
To start migrating the receiver to the UP their most current settings need to be retrieved from production. 
After retrieving the settings they will be updated to be able to route a message through the UP.

* To begin create a new branch in git for your changes.
* Login to prod: `./prime login --env prod`
* Fetch the STLTs production settings, append the receiver settings to your local organizations.yml, put them in a local file, and also load them into the local database: `./prime multiple-settings get -f mt-doh --output mt-doh.yml --env prod -l -a`
* Change the `numberPerDay` setting to `1440` (one per minute) so that you are not stuck waiting on the message to send. 
  * Change the transport to go to your local machine:      
`transport:
    host: "sftp"
    port: "22"
    filePath: "./upload"
    credentialName: "DEFAULT-SFTP"
    type: "SFTP"`
* Then `./gradlew reloadSettings`.


### 2. Update receiver settings to route messages through UP

Below is a Covid pipeline receiver setting sample yml file with some settings removed for readability purposes:

```yaml
- name: "tx-doh"
  description: "Texas Department of Health"
  jurisdiction: "STATE"
  receivers:
      - name: "elr"
        organizationName: "tx-doh"
        topic: "covid-19"
        customerStatus: "active"
        translation: !<HL7>
            schemaName: "covid-19"
        jurisdictionalFilter:
            - "orEquals(ordering_facility_state, TX, patient_state, TX)"
        qualityFilter: []
        routingFilter:
            - "DoesNotMatch(test_performed_code, 85478-6,76080-1,92141-1,80383-3,82170-2,85477-8,76078-5,77026-3,77028-9,77027-1,92142-9,80382-5,82166-0,82167-8,82169-4,82168-6,92809-3,92808-5,95423-0)"
```

In order to migrate existing covid pipeline settings to the UP a few settings need to be updated. 
* `topic:` The topic needs to be updated to "full-elr". 
* `name:` The naming convention we've been following for the name is "full-elr" as well.
* `customerStatus:` Customer status needs to be updated to "testing" once the STLT has been fully migrated and live in production it should be updated back to "active"
* `schemaName:` Schema name specifies how the RS FHIR bundle should be translated to HL7 if the receiver's format is HL7. 
If they're receiving HL7 v2 ORU_R01. The schema name can be updated to `azure:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml`. 
If the receiver has any specific receiver transforms the schema name should be updated to point to the schema location.
* `convertDateTimesToReceiverLocalTime` This setting is used to convert datetimes to a specified time zone. If this setting is used an enrichment schema should be added under the `enrichmentSchemaNames` list to ensure compatibility with datetime conversions for all receivers. See also: [Translate#extending-schemas](https://github.com/CDCgov/prime-reportstream/blob/main/prime-router/docs/universal-pipeline/translate.md#extending-schemas)
* `jurisdictionalFilter:` The jurisdictional filter needs to be updated to use FHIR path. 
The most common way to route messages to a STLT is based on the patient's or performer's state. 
The FHIR path for that looks like this: `"(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'MT') or (Bundle.entry.resource.ofType(Patient).address.state = 'MT')"`
* `routingFilter:` Most STLTs in the covid pipeline are using a filter that specifies Flu results should not be routed to them. This filter can be removed from STLTs in the UP. a `conditionFilter` shoudl be added though to limit them just to Covid tests `# Accept COVID only 
"(%resource.code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code  in ('840539006')).exists())"`
* `qualityFilter:` The covid pipeline has functionality to add default quality filters if the filter is empty. 
The UP doesn't have default quality filters, so if a STLT that is being migrated to the UP doesn't have qualityFilters the default qualityFilters will have to be added manually.
```yaml
        # Message id is not empty (MSH-10)
        - "Bundle.identifier.value.exists()"
        - "Bundle.entry.resource.ofType(Patient).name.family.exists()"
        - "Bundle.entry.resource.ofType(Patient).name.given.count() > 0"
        - "Bundle.entry.resource.ofType(Patient).birthDate.exists()"
        #  Specimen type (SPM-4) is not empty
        - "Bundle.entry.resource.ofType(Specimen).where(type.empty()).count() = 0"
        # Patient has at least a street address(PID-11-1), postal code (PID-11-5),phone(PID-13) or email(PID-13-4)
        - "Bundle.entry.resource.ofType(Patient).where(address.line[0].empty() and address.postalCode.empty() and telecom.where(system = 'phone').value.empty() and telecom.where(system = 'email').value.empty()).count() = 0"
        - "((Bundle.entry.resource.ofType(Specimen).collection.collectedPeriod.exists() or Bundle.entry.resource.ofType(Specimen).collection.collected.exists())
           or (Bundle.entry.resource.ofType(ServiceRequest).occurrence.exists() or Bundle.entry.resource.ofType(Observation).effective.exists()))"
```
* `Filters:` Keep in mind that all these filters need to pass to route the message to the STLT. In general all filters in the UP use FHIR path

After updating the receiver to route messages to the UP it should look like this:
```yaml
- name: "tx-doh"
  description: "Texas Department of Health"
  jurisdiction: "STATE"
  receivers:
      - name: "full-elr"
        organizationName: "tx-doh"
        topic: "full-elr"
        customerStatus: "testing"
        translation: !<HL7>
            schemaName: "azure:/metadata/hl7_mapping/ORU_R01/ORU_R01-base.yml"
            convertDateTimesToReceiverLocalTime: true
        # These two settings are used with convertDateTimesToReceiverLocalTime translation
        timeZone: "MOUNTAIN"
        dateTimeFormat: "OFFSET"
        jurisdictionalFilter:
            - "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'TX') or (Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'TX')"
        qualityFilter:
            # Message id is not empty (MSH-10)
            - "Bundle.identifier.value.exists()"
            - "Bundle.entry.resource.ofType(Patient).name.family.exists()"
            - "Bundle.entry.resource.ofType(Patient).name.given.count() > 0"
            - "Bundle.entry.resource.ofType(Patient).birthDate.exists()"
            #  Specimen type (SPM-4) is not empty
            - "Bundle.entry.resource.ofType(Specimen).where(type.empty()).count() = 0"
            # Patient has at least a street address(PID-11-1), postal code (PID-11-5),phone(PID-13) or email(PID-13-4)
            - "Bundle.entry.resource.ofType(Patient).where(address.line[0].empty() and address.postalCode.empty() and telecom.where(system = 'phone').value.empty() and telecom.where(system = 'email').value.empty()).count() = 0"
            - "((Bundle.entry.resource.ofType(Specimen).collection.collectedPeriod.exists() or Bundle.entry.resource.ofType(Specimen).collection.collected.exists())
           or (Bundle.entry.resource.ofType(ServiceRequest).occurrence.exists() or Bundle.entry.resource.ofType(Observation).effective.exists()))"
        routingFilter: []
        conditionFilter:
            # Accept COVID only
            - "(%resource.code.coding.extension('https://reportstream.cdc.gov/fhir/StructureDefinition/condition-code').value.where(code in ('840539006')).exists())"
        enrichmentSchemaNames:
            # Example enrichment schema for US Mountain time zone
            - "azure:/fhir_transforms/common/datetime-to-local/datetime-to-local-us-mtz.yml"
```

### 3. Send test message from SimpleReport to STLT

At the time this documentation was written the only sender onboarded to the UP is SimpleReport. 
Therefore, to test sending reports to STLTs we can only use reports generated from SimpleReport. 
The most up to date messages can be found here:
* [UP SR FHIR bundle](./samples/SimpleReport/SR_UP_02262024.fhir)
* [CP SR CSV message](./samples/SimpleReport/SR_CP_02262024.csv)  
However, they continue making updates to their messages so a more up-to-date message can be retrieved by following these instructions: [Simple Report Test data](#simple-report-test-data)   
Make sure to update the patient state or facility state to match the jurisdictional filter of the state being migrated.

After deciding what message to use the following postman collections can be used to send reports using ReportStream's API as a Simple Report Covid and UP sender.
* [SimpleReport covid postman collection](./samples/SimpleReport/Simple%20Report%20Covid.postman_collection.json)
* [SimpleReport UP postman collection](./samples/SimpleReport/Simple%20Report%20UP.postman_collection.json)   
Make sure to replace the Body request with a most up-to-date message if needed.

The API will return a Submission Id and report Id that can be used to find the status of the submission by calling this API:
```shell
curl GET 'http://localhost:7071/api/waters/report/{submissionId}/history'
```
If the receivers were set-up correctly in step 2, the status of the API should say the message got routed to the STLT being migrated. If not make sure the settings have been  updated in the `settings/organization/yml` and loaded to the local database by running `./gradlew reloadSettings`
If the message is still not being routed look through the logs to see if there are any exceptions.
The receiver filters can be tested by running the `./prime fhirpath -i {path-to-file}` with the sample FHIR bundle and making sure the FHIR path filters return true.

### 4. Set up receiver transforms
To set up the receiver transforms we need to look closely at the STLT's current Covid Pipeline receiver settings. 
If the receiver has any of the following settings enabled they will need a receiver transform schema:
- receivingApplicationName
- receivingApplicationOID
- receivingFacilityName
- receivingFacilityOID
- messageProfileId
- replaceValue
- replaceValueAwithB
- reportingFacilityName
- reportingFacilityId
- reportingFacilityIdType
- suppressQstForAoe: true
- suppressHl7Fields
- suppressAoe: true
- defaultAoeToUnknown
- replaceUnicodeWithAscii
- convertDateTimesToReceiverLocalTime: true
- useBlankInsteadOfUnknown
- usePid14ForPatientEmail: true
- suppressNonNPI
- replaceDiiWithOid
- useOrderingFacilityName not STANDARD
- receivingOrganization
- stripInvalidCharsRegex

Examples on how to create and set-up a receiver schema can be found under `src/main/resources/hl7_mapping/receivers/STLTs`
More information on how transforms in the UP work can be found in the [transform design doc](../design/design/transformations.md)

### 5. Compare Covid and UP messages
After setting up the UP receiver transforms if necessary. Send a new message through both pipelines following the steps in step [3. Send test message from SimpleReport to STLT](#3-send-test-message-from-simplereport-to-stlt)
Compare the two messages using a diff tool of your choice. If the message doesn't contain any sensitive data you may use an online tool like https://hl7inspector.com to compare the messages.
Document any differences you see in the message and share them with the team to find what's the best way to address them.  Depending on the difference there are a few ways it can be resolved:
* Add a receiver transform
* Add a sender transform
* Ask the sender to make the change
* If it's not a major difference and the receiver is OK with this change, no action is needed.

An example on how to document the differences can be found here: https://docs.google.com/spreadsheets/d/197AeFMvozqUGRE1BuvOSMiUL_r2EEkyQv4l8D_OhhZk/edit#gid=492389121

When deciding if a transform should go on the receiver or sender. These are a couple of guidelines to keep in mind [Managing Transforms](../standard-operating-procedure/managing-transforms.md)


### 6. Testing in Staging
Once the settings and transforms have been migrated and tested successfully through the UP locally, we can start testing in staging. 
First have somebody review your changes by either creating your PR or by sharing your changes. 
Once they have been reviewed push the settings to staging
One of the ways to push the settings to staging is by running the PRIME command:
`./prime multiple-settings set --input {path-to-schema} --env staging`

Follow the steps here [Simple Report Test data](#simple-report-test-data) to generate a combination of Single Entry and CSV upload test messages through SimpleReport.
The message should get routed to the STLT if their transport has been set-up correctly.  
Messages routed to the STLT can be viewed by:
* Logging in as an Admin to ReportStream
* Search for the STLT organization under Organizations and Click on Set
* Navigate to https://staging.reportstream.com/daily-data
* You can view and download all messages routed to the STLT
* If for some reason you can't view the message routed to the STLT 
  * Ensure the filters are set-up correctly and that the message you sent has the right patient state
  * Wait a couple of minutes, it can take a few mins for SimpleReport to release the message

### 7. Move settings to Production
The process to move setting to production is the same as in staging. 
However, we won't be able to send test messages to the STLT. 
We will have to monitor their channel for a couple of weeks and wait until we see data for the STLT being routed through the UP. 
Once we have data we can notify the STLT to start reviewing the messages.

### Simple Report Test data

A quick and easy way to get test data to send to a STLT is by going into SimpleReport's test environment https://test.simplereport.gov.   
* Access can be requested on the [shared-simple-report-universal-pipeline](https://nava.slack.com/archives/C0411VC78DN) thread.   
* Instructions on how to send a test message can be found on this youtube playlist https://www.youtube.com/playlist?list=PL3U3nqqPGhab0sys3ombZmwOplRYlBOBF.     
* The file [SR upload](./samples/SimpleReport/SR-UPLOAD.csv) can be used test sending reports through SimpleReport's CSV upload.    
* To route the report to a specific STLT either the patient or facility state needs to updated to the STLT's jurisdiction. Keep in mind that if they are not updated the message might get routed to the incorrect STLT.
* The report sent by SimpleReport can be found in the Azure BlobStorage. The UP message will be stored in the `receive/simple_report.fullelr` and the covid pipeline message will be stored in `receive/simple_report.default`. This message can be used locally to test any new sender or receiver transforms.
* To access the blob storage. Microsoft Storage Explorer needs to be installed and login with your CDC SU credentials.

## How to check if STLTs transport is working
A STLTs transport needs to be working in Staging in order to migrate them to the UP and send test messages to their test environment.
Sometimes STLTs use the same SFTP server or REST endpoint for test and production with different paths to differentiate between environments.

* To start testing sending data to STLTs through the UP we can check if their existing transport is working in the covid pipeline.
* Most of the receivers transport were turned off in staging to prevent from accidentally sending unwanted data.
* If they have been turned off, receiver transport settings can be found by looking through their history in the UI, in the repo, or in the production receiver settings.
* A spreadsheet that keeps track of where to find a receiver's transport and their status can be found here: https://cdc.sharepoint.com/:x:/r/teams/ReportStream/_layouts/15/doc2.aspx?sourcedoc=%7B28EDE785-0FC9-4921-BCD0-B423F3C5E92A%7D&file=Receiver%20Staging%20Connection%20Test%20-%20Jan%202024.xlsx&action=default&mobileredirect=true
* If the transport can't be found we'll need to get that information from the STLT during the kick-off call and request new credentials if necessary.

Once the STLT's transport has been found. It can be tested by following these instructions [How to Check Transport](./transport/how-to-check-transport.md)
* Status returns "Network error": This probably means their SFTP server needs to be whitelisted.
* Status returns anything related to credentials: This probably means the credentials were not created correctly or are not present.
    * Instructions on how to create credentials can be found here:
        * [REST](./transport/rest.md)
        * [SFTP](./transport/sftp.md)


## How to check if STLT needs to be whitelisted
When transmitting data through SFTP the CDC needs to whitelist the STLTs SFTP server.
This may have been previously done when onboarding them to the Covid pipeline. However, some of them have been removed from the whitelist and will need to be re-added again.
If while performing a check through the UI the error returns "network error", they probably need to be whitelisted.
An additional check can be done by:
1. logging-in into Azure with a super-user account
2. going into the pdhstaging-functionapp
3. Using the SSH development tools
4. Once the console opens up, run sftp <sftp-server> command
5. If the command times-out or gives any errors, that means the server needs to be whitelisted.

To request the SFTP server be whitelisted. Submit a devops ticket and contact them via the [prime-devops channel](https://nava.slack.com/archives/C01SFFVH31Q)








