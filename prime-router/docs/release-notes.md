#  ReportStream Release Notes

## General useful links:

- All Schemas are documented here: [Link to detailed schema dictionaries](./schema_documentation)
- The ReportStream API is documented here: [Hub OpenApi Spec](./api)
- More detailed changelog for individual releases: [Recent releases](https://github.com/CDCgov/prime-reportstream/releases)

## January 31, 2023

### Make validation endpoint public

This release removes authentication/authorization from the `/api/validate` endpoint.

Previously, the `client` parameter had to be set in the header. This is still an option, but now, the client
can pass `schema` and `format` as query parameters and a schema matching those values will be used to 
validate the message. Additional information can be found in the API documentation.

### Update Message API documentation

Renamed `/api/messages/search` to `/api/messages` and updated API documentation for accuracy.

## November 29, 2022

### Change to the api/token endpoint

This release makes a change to how parameters should be POSTed to the api/token endpoint, when requesting bearer tokens.

Previously, parameters were sent in the URL.   Now, parameters should be sent in the body of the POST.  Note that the older way of POSTing is still currently accepted, however this should be considered deprecated and will not be accepted in a future release.

Details are documented in the [Programmer's Guide](./ReportStream-Programmers-Guide-v2.3-updated.docx) and in the [openapi documentation for the token endpoint](./api/token.yml)

## September 6, 2022

### Additional Lookuptable API Endpoint

This release adds a new content download endpoint, and other improvements, to the ReportStream _LookupTable API_.

#### What is the LookupTable API?

In ReportStream, LookupTables are used to store standardized SNOMED and LOINC valuesets, providing 
simple mappings between coded values and human readable names.
LookupTables are also used to store many other kinds of such simple mappings, for example, we use LookupTables to 
 expose Covid-19 "LIVD" data in a way that we can map between different columns in that spreadsheet.
We also use LookupTables to store mappings between non-standard values and standard values, in cases where
a customer is unable do that mapping themselves.

All LookupTable data is available read-only in the LookupTable API, to any authenticated user of ReportStream.

#### New Features added to the LookupTable API 

This release contains the following improvements to the ReportStream LookupTable API:

1. Both human users and servers can now access the LookupTable API.  Humans use Okta authentication, and Servers use server-to-server authentication.
2. An `authentication-type: okta` header parameter is no longer required to use Okta authentication.  If you were using it, you can remove it.
3. A new LookupTable API endpoint `/api/lookuptables/{tableName}/content` has been added.  This will return the full contents of the _**active**_ version of `tableName`, or an error if there is no active version.  This complements the existing `/api/lookuptables/{tableName}/{tableVersion}/content` endpoint, which required you to know the version number prior to querying.

For detailed information on the LookupTable API, consult [the LookupTable OpenApi Spec](./api/lookup-tables-openapi.yaml).


## April 28, 2022

### Added Server-to-Server Authentication Option the ReportStream History API

This release contains further enhancements to the `api/waters/org/ORGNAME/submissions` and `api/waters/report/REPORTID/history` API endpoints.   The enhancement is that these endpoints can now be accessed both via a human-entered authentication (via username/password entry using our Okta interface), and now also by using our server-to-server _two-leggged_ authentication protocol.

This means that automated senders can access those two APIs programmatically, with automated tools, rather than having to depend on a person to login and authenticate.   There are several steps involved in using the automated server-to-server authentication:
1. Submit your public key to ReportStream ahead of time.
2. When you want to use the APIs, create a signed token using your private key.
3. ReportStream will confirm the signed token with the previously submitted public key, and respond with a 5-minute access token.
4. Repeat steps 2 and 3 as often as needed.

#### For further information on ReportStream's Server-to-Server Authentication

- See the [Token-based authentication section of the Programmer's Guilde](./ReportStream-Programmers-Guide-v2.1.docx)
- See the [Token authentication Playbook](./playbooks/how-to-use-token-auth.md)

## April 12, 2022

This release contains further enhancements to the json response to Report History GETs and report submission POSTs.

The new fields are `overallStatus`, `plannedCompletionAt`, `actualCompletionAt`, and `itemCountBeforeQualityFiltering`

### 1. overallStatus

The new `overallStatus` field tells whether all the data in that submission have made their way to all the intended recipients. Values are:

- `Error` - error on initial submission; not successfully received.
- `Received` - submission successfully received but not yet processed (routing determination and filtering have not yet completed).
- `Not Delivering` - submission has been processed, but has no intended recipients.  This may happen if the data did not meet the recipient's quality criteria. This is a final status.
- `Waiting to Deliver` - submission has been processed, has intended recipients, but not yet delivered.
- `Partially Delivered` -- submission has gone to some recipients, but not all.
- `Delivered`- submission has gone to all intended recipients.  This is the final status.

### 2. plannedCompletionAt and actualCompletionAt

- `plannedCompletionAt` is the timestamp when ReportStream intends to finish sending all data to all intended recipients, based on their chosen timing.  Note: `plannedCompletionAt` will be null if there is no data to deliver, or has not been processed yet.

- `actualCompletionAt` is the timestamp when ReportStream actually finished sending all data to all intended recipients.  Note: this value will be null if there is no delivery, or if not complete yet.

The `actualCompletionAt` might be later than the `plannedCompletionAt` if there was a delivery delay.  For example, a down sftp site might prevent ReportStream from delivering.

### 3.  itemCountBeforeQualityFiltering

The Report History was already displaying `itemCount`, the number of items (aka Covid-19 Tests) that were being sent to each destination.   Now, the Report History also displays `itemCountBeforeQualityFiltering`, the number of items available to be sent to that destination, prior to quality filtering.   Quality filtering is a step that ReportStream takes to ensure that the submitted data meets the minimum standards of the STLT (State/Local/Tribal/Territorial) jurisdiction destined to receive that item.  Each STLT can set their own minimum standards for each data feed they get from ReportStream.

For example, if Covid-19 data was submitted to ReportStream containing 7 patients with addresses in Maryland, but 4 of those patients were missing information required by the Maryland primary Covid-19 data feed, then that data feed would have

```
itemCount: 3
itemCountBeforeQualityFiltering: 7
```

To find detailed information on _why_ items were filtered, look at the `filteredReportItems` section for that destination.

Note: in this release the filteringReportItems field `originalCount` has been removed, because it is redundant with the new `itemCountBeforeQualityFiltering` field.

### Examples

Updated Examples including the new field can be found here:

- [Example **asychronous** submission response](../examples/submission/example1-async-response.json). 
- [Example of the **synchronous** submission response](../examples/submission/example2-sync-response.json)
- [Example of a **complete History API response**, after data has flowed to the states](../examples/submission/example3-complete-response.json).


## March 29, 2022

This release contains a much-enhanced **_Submission Response_** json, a new ***Submission History Details AP*I**,
and changes to the **_Submission History List API_**.

### Changes and Additions to the _Submission Response_ json

Upon submission, ReportStream returns a json object with useful information about the submission.   That json object has been greatly expanded.

#### Changes to the _Submission Response_

Full detailed _before_ and _after_ examples of changes are in the links below.

1.  In error and warning objects, the `itemNums` field has been removed and replaced with `indices`

Example:

Old `itemNums` looked like this: 

`"itemNums": "6 to 7, 9"`

New `indices` looks like this:
```
      "indices": [
            6,
            7,
            9
        ],
 ```

Same information, but the new `indices` are designed to be easily  machine-readable.


#### Additions to the Submission Response

Full detailed before and after examples of all these changes are in the links below.

1.  `destinations` array objects now include detailed information about each item that was filtered, in the `filteredReportItems` object.  The information in `filteredReportItems` is identical to the older `filteredReportRows` array.
2.  `destinations` array objects now include detailed information about when data was sent to receiving Health Departments, in the `sentReports`.  Manual downloads by Health Departments are reported in the `downloadedReports` object.
3.  If all items for a receiving Health Department are filtered out, the `itemCount` will be `0` for that `destination`.
4.  `error` and `warning` objects are now broken down into fine-grained subfields, in addition to the human-readable text string `message`.  The subfields are `scope`, `indices`, `trackingIds`, and `field`.

### Announcing the new Submission History Details API

The Submission History Details API endpoint is:

`https://prime.reportstream.gov/api/waters/report/{id}/history`

The Submission History Details API returns detailed information about a single Report submitted to ReportStream.   As the Report's data flows through the system, later queries to this endpoint will add more information as it becomes available.   For example, most states only take deliver of data one time or a few times a day.   When your submitted data has been successfully delivered, that information will be added, so the data in the Submission History Details API response will grow over time.

For convenience, the Submission History Details API can be queried with either an `id` or a `submissionId`, both of which can be found in the Submission History List API response and in the Submission Response.

#### Example Submission History Detail GET calls:

**GET based on an _id_ value**

`https://prime.reportstream.gov/api/waters/report/5f1ba919-f1ed-40d7-abc5-924a9c20b7fe/history`

**GET based on a _submissionId_ value**

`https://prime.reportstream.gov/api/waters/report/123456/history`

Both requests use the same endpoint and return the same data in the same object structure.  

Then why have both? 
The reason is that only successfully submitted reports (httpStatus: 201) have an `id` value.
Therefore while `submissionId` queries succeed in gathering history on both 
successful and failed submissions, `id` queries can only be used for successful submissions.
This anomaly may be cleaned up in a future release.

Note that the Submission History Detail API response is identical to that returned by the initial submission, but will include further information as the data progresses through ReportStream and is sent to states.   

### Links to Before and After Examples

**Before**

- [Example of the old submission response](../examples/submission/old-submission-response.json)

**After**

- [Example of the **new** asychronous submission response](../examples/submission/example1-async-response.json).  High throughput senders to ReportStream use the _asynchronous_ endpoint to submit data to ReportStream.   This handles high volumes, but, as you can see from the example, initially returns less data.
- [Example of the **new** synchronous submission response](../examples/submission/example2-sync-response.json)
- [Example of a complete **new** History API response, after data has flowed to the states](../examples/submission/example3-complete-response.json).   The History Details API can be used subsequent to _any_ submission, to get all the details about the processing of that submission.  Further data is added, as it flows to State, Local, and Federal health departments.


### Modifications to the Submission History List API

There are two changes to the ReportStream API that returns lists of submissions done by a Sending organization.

#### 1) New URL Path for the Submission History List

The old path 

`https://prime.reportstream.gov/api/history/{organizationName}/submissions`

has been changed to

`https://prime.reportstream.gov/api/waters/org/{organizationName}/submissions`

You must provide your organization name.  Please contact the ReportStream team if you do not know the name you are using.

#### 2) Changes to several field names in the Submission History List

Three field names have changed.

`taskId` has been changed to `submissionId`
`createdAt` has been changed to `timestamp`
`sendingOrg` has been changed to `sender`

These changes are to ensure that the Submission History List fieldnames are identical to those in the Submission History Details.

Here are before and after examples of Submission History List API response

##### Old Submission History List API response

```
    {
        "taskId": 29,
        "createdAt": "2022-03-23T21:29:14.860Z",
        "sendingOrg": "ignore",
        "httpStatus": 201,
        "id": "88785ed2-0375-4286-97f6-b649f107384b",
        "topic": "covid-19",
        "reportItemCount": 20,
        "warningCount": 0,
        "errorCount": 0
    },
```


##### New Submission History List API response

```
   {
        "submissionId": 29,
        "timestamp": "2022-03-23T21:29:14.860Z",
        "sender": "ignore",
        "httpStatus": 201,
        "id": "88785ed2-0375-4286-97f6-b649f107384b",
        "topic": "covid-19",
        "reportItemCount": 20,
        "warningCount": 0,
        "errorCount": 0
    }
```
-----



## March 8, 2022
### Consistent formatting of timestamps in API responses
Updated the timestamp formats to have consistent precision and time zone in our API JSON responses.  Our API responses 
still use the ISO 8601 standard, but we have updated all timestamps be in the UTC time zone as well as to always show
a precision down to milliseconds.  This follows the pattern of `yyyy-MM-dd'T'HH:mm:ss.SSSXX` per 
[Java's DateTimeFormatter](https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html).

Example updated timestamp: 2022-03-02T17:53:17.981Z

## March 3, 2022
### Added duplicate detection

ReportStream will now reject duplicate submissions

This feature is turned on by setting the allowDuplicates flag to false in the Sender's settings configuration.   By default the flag is true.  A duplicate submission will return a 400 error.

A duplicate is considered identical if is byte-for-byte identical to an earlier successfully submitted payload from the same organization and sender.

## February 15, 2022
### Added new senders for our CSV Sending Pilot
ReportStream continues to welcome new facilities to our CSV senders group. 

### Improved testing tools
ReportStream has added a series of tools that enhance our testing capabilities. This includes the ability to generate 
and compare files on a per-partner basis and ensure that any new changes will not affect a downstream receiver unexpectedly.

### Admin Settings UI
ReportStream has exposed an admin settings UI available to administrators in the app, making it easier to make changes 
for senders and receivers.

### Code Cleaning and Refactoring
We've cleaned up our code and removed some older types of transports and translations that are no longer needed, 
reducing the complexity of our code.

## January 28, 2022
### Support for new date and date/time formats for CSV reports

ReportStream now supports the following two new formats for date and date/time fields in CSV reports:
- `M/d/yyyy[ HH:mm[:ss[.S[S][S]]]]`
- `yyyy/M/d[ HH:mm[:ss[.S[S][S]]]]`

The current list of supported date and date/time formats is:
- `yyyyMMdd`
- `yyyyMMddHHmmZZZ`
- `yyyyMMddHHmmZ`
- `yyyyMMddHHmmss`
- `yyyy-MM-dd HH:mm:ss.ZZZ`
- `yyyy-MM-dd[ HH:mm:ss[.S[S][S]]]`
- `yyyyMMdd[ HH:mm:ss[.S[S][S]]]`
- `M/d/yyyy[ HH:mm[:ss[.S[S][S]]]]`
- `yyyy/M/d[ HH:mm[:ss[.S[S][S]]]]`
- `MMddyyyy`


## January 10, 2022
### Addition of warnings and errors when submitting invalid test devices
When uploading to the ReportStream REST endpoint, a warning is returned if the test device information
provided in a report cannot be validated against the list provided by the 
[Department of Health and Human Services’ (HHS) LOINC Mapping spreadsheet](https://www.cdc.gov/csels/dls/sars-cov-2-livd-codes.html).
If a given field with the test device information is required and cannot be validated then an error is returned instead and the 
report is rejected.


## December 12, 2021
### Addition of ability to attach a filename to your URL submission
When uploading to the ReportStream REST endpoint, the actual data payload is sent as part of the HTTP body.

This means that ReportStream does not have or track a filename associated with the file; in fact for automated systems, the data might not come from a file at all.

However, many senders to ReportStream will be sending files, and it would be valuable to track the filename for later reference by the sender.

The new payloadName parameter is designed to meet this need.

Here is an example, using curl:

```
curl -X POST -H "client:healthytests"  -H "content-type:text/csv"  --data-binary "@mytestfile12345.csv" "https://prime.cdc.gov/api/reports?payloadName=mytestfile12345.csv"
```

ReportStream does not use the payloadName, however, ReportStream will associated the payloadName with the submission as part of its history/lineage tracking, so that users can map submissions back to original filenames.

The payloadName does not have to be a filename - it could be any string that is meaningful to the data sender.   The name will appear in the json response as the 'externalName'.  Max 1000 chars.

### Additions to the json response
In addition to the externalName field, the httpStatus and sender names have been added to the standard json response.


## November 18, 202
### Results from the receiver quality filters in the response.
A new `filteredReportRows` entry has now been added to `destination` in the json response.

Each jurisdiction that receives data from ReportStream can configure 'quality filters' that filter out data that does not meet that receiver's needs.   With this release we are now returning information back to submitters on why certain rows were filtered from going to a particular jurisdiction.  
 
Each entry lists the filter applied and the rows that were filtered by it.  `filteredReportRows` is present only if at least one row has been filtered.

Many receivers have a 'secondary' feed, often designed to catch data that was filtered from that receiver's primary feed.   In those cases, if the submitted data shows as filtered from the primary feed, it may still have been sent to that jurisdiction's secondary feed.

**Examples**

All Items filtered out:

ReportStream applies a number of quality filters on behalf of the California Dept of Public Health.  One of these filters requires a number of fields to have valid values.  (See `hasValidDataFor` and the list of values, in the example below).   In this example, it turns out that all three of the data items destined for CA failed this test, so all are being filtered.

However, as you can see in the destinations list, all those data items are being sent to CA's secondary feed.
```
{
    "id": "8049440b-f95a-43e9-bdb7-a7cdfbf261af",
    "timestamp": "2021-11-05T15:37:47.804670Z",
    "topic": "covid-19",
    "reportItemCount": 3,
    "destinations": [
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr",
            "filteredReportRows": [
                "For ca-dph.elr, qualityFilter hasValidDataFor, [message_id, equipment_model_name, specimen_type, test_result, patient_last_name, patient_first_name, patient_dob] filtered out Rows 1,2,3 reducing the Item count from 3 to 0."
            ],
            "sending_at": "never - all items filtered out",
            "itemCount": 0
        },
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr-secondary",
            "sending_at": "2021-11-05T11:38-04:00",
            "itemCount": 3
        }
    ],
    "destinationCount": 2,
    "warningCount": 0,
    "errorCount": 0,
    "errors": [],
    "warnings": [],
    "consolidatedErrors": [],
    "consolidatedWarnings": []
}
```

Some but not all items filtered out:

In this example one of the items passed CA DPH's filter criteria, but two items failed.

A note on the item numbering:  ReportStream can accept data in CSV and HL7 batch formats.

For a CSV submission, an 'item' is just a row of data in the CSV, and item 1 is the first row of actual data _after_ the header row, item 2 is the second row of actual data, and so on.

For an HL7 batch submission an 'item' is a single HL7 message in the batch, and item 1 is the first message in the batch, item 2 is the second message, and so on.


```
{
    "id": "5fd89dcc-4767-40be-b72b-9ebfbe0db8e5",
    "timestamp": "2021-11-05T15:57:38.582762Z",
    "topic": "covid-19",
    "reportItemCount": 3,
    "destinations": [
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr",
            "filteredReportRows": [
                "For ca-dph.elr, qualityFilter hasValidDataFor, [message_id, equipment_model_name, specimen_type, test_result, patient_last_name, patient_first_name, patient_dob] filtered out Rows 1,3 reducing the Item count from 3 to 1."
            ],
            "sending_at": "2021-11-05T11:58-04:00",
            "itemCount": 1
        },
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr-secondary",
            "sending_at": "2021-11-05T11:58-04:00",
            "itemCount": 2
        }
    ],
    "destinationCount": 2,
    "warningCount": 0,
    "errorCount": 0,
    "errors": [],
    "warnings": [],
    "consolidatedErrors": [],
    "consolidatedWarnings": []
}
```

No items filtered out:
```
{
    "id": "e6f4b7ea-7a74-4beb-ae82-bfecfedbe6c6",
    "timestamp": "2021-11-05T15:58:04.009910Z",
    "topic": "covid-19",
    "reportItemCount": 3,
    "destinations": [
        {
            "organization": "California Department of Public Health",
            "organization_id": "ca-dph",
            "service": "elr",
            "sending_at": "2021-11-05T11:59-04:00",
            "itemCount": 3
        }
    ],
    "destinationCount": 1,
    "warningCount": 0,
    "errorCount": 0,
    "errors": [],
    "warnings": [],
    "consolidatedErrors": [],
    "consolidatedWarnings": []
}
```


## November 4, 2021
### Updates to the Warnings and Errors JSON response for `/api/waters`
Building upon the error and warning message updates from the 2021-10-19 release, a few changes have been made to
the server's error and warning responses.

1. `consolidatedWarnings` and `consolidatedErrors` have been renamed to `warnings` and `errors`.

2. The `scope` property has been added to every warning and error.

3. The previously-named `rows` property has been renamed to `itemNums` and is only
displayed for `ITEM` scope warnings and errors.

4. When running in `verbose` mode, the new `itemDetails` array displays an array of objects that
contain the `itemNum` and `groupId` properties per-item.

#### Standard example:
```json
{
  "id" : null,
  "warningCount" : 13,
  "errorCount" : 8,
  "errors" : [ ],
  "warnings" : [ {
    "scope" : "ITEM",
    "message" : "Invalid date: '11/01/2021' for element 'Date_result_released' ('date_result_released'). Reformat to yyyyMMdd.",
    "itemNums" : "Rows: 15, 19 to 20"
  } ]
}
```

#### Verbose example:
```json
{
    "id" : null,
    "warningCount" : 13,
    "errorCount" : 8,
    "errors" : [ ],
    "warnings" : [ {
        "scope" : "ITEM",
        "message" : "Invalid date: '11/01/2021' for element 'Date_result_released' ('date_result_released'). Reformat to yyyyMMdd.",
        "itemNums" : "Rows: 15, 19 to 20",
        "itemDetails" : [ {
            "itemNum" : "15",
            "groupingId" : "'Date_result_released' ('date_result_released')11/01/2021"
        }, {
            "itemNum" : "19",
            "groupingId" : "'Date_result_released' ('date_result_released')11/01/2021"
        }, {
            "itemNum" : "20",
            "groupingId" : "'Date_result_released' ('date_result_released')11/01/2021"
        } ]

    } ]
}
```


## October 18, 2021
### More instructive error and warning messages in API response
Previously, when there were problems with health data the error or warning message would simply state where the problem occurs. 
These changes update the messages so also include instructions for correcting the problems. Example new messages:

Invalid phone number:
> Invalid phone number 555-5555 for phoneNumber. Reformat to a 10-digit phone number (e.g. (555) - 555-5555).

Invalid date:
> Invalid date: 2021/09-02 for element testOrderedDate. Reformat to YYYYMMDD.

Invalid code:
> Invalid code: 'R' is not a display value in altValues set for patientSex.

Invalid postal code:
> Invalid postal code 1234 for patZip. Reformat to 01234.

Invalid column count:
> Expecting 26 columns, but found 28 in row 8.

Too many rows:
> Your file's row size of 20000 exceeds the maximum of 10000 rows per file. Reduce the amount of rows in this file.

Too many columns:
> Number of columns in your report exceeds the maximum of 2000 allowed. Adjust the excess columnar data in your report.

General CSV parsing error:
> There's an issue parsing your file. Contact the ReportStream team at reportstream@cdc.gov.

Missing column header:
> Your file is missing patientAge header. Add a header.

Unexpected header:
> Unexpected column header found, 'starSign' will be ignored.

Too many errors:
> Report file failed: Number of errors exceeded threshold. Contact the ReportStream team at reportstream@cdc.gov for assistance.


## September 8, 2021
### HL7 option to add NCES ID to ordering facility name
`useNCESFacilityName` flag added to HL7 receiver settings. If set and `site_of_care` is `k12`, ReportStream will lookup
the facility name in the NCES ID table and set the ORC-21 according to the APHL guidance. 


## August 24, 2021
### Changes for Specific Senders
- Data from Reddy Health is now flowing to HHSProtect
- Patient_tribal_citizenship has been added to the Simple Report schema


## August 17, 2021
### PEM file can now be used for authentication with SFTP servers
We previously only supported Putty key files for authentication with SFTP servers,
but we now also support the use of PEM files, as well.

### Web Content v3 Release
We've updated the flow and content on our static site to add additional information and
improve navigation. We also now have a map that shows where ReportStream is currently live!

### Dependency Updates
The following dependencies were updated:
- azure-storage-core
- azure-storage-queue


## August 10, 2021
### Improved Security
Added scanning for secrets into our CI/CD pipeline via gitleaks.

### Enhanced Documentation
Documentation enhancements.

### HL7 serialization improvements
Improvements for generation of HL7 files for AK, NH, and CA.

HL7 serializer improvements including verification of CODE fields on HL7 ingest.

### Improved Unit Tests
Converted unit tests to use assertK.

### New `correctedTestId` column
A new column has been added to allow for test results to amend/update previous results.   This feature has been added for those who were first working with Waters, and are now sending CSV data directly to ReportStream.  It works like this:

First, for all rows of data you submit to ReportStream, you’ll need to make sure that every row of data you send is tagged with completely new/unique/never-previously-used ID.  You can use the `specimenId` column for this purpose, or if your `specimenId` value is re-used on an update, then you’ll need to add a new column called `testId` to your data, that meets this uniqueness rule.

Then, also add a new column called `correctedTestId`.  This column will almost always be blank, except when you are submitting a result that amends a previous result.  In those cases, the `correctedTestId` column will be filled in with the older/previous `testId` (or `specimenId`, if yours meets the uniqueness criteria above) row that is getting amended.  In this way, corrections can be “chained” and a history can be maintained, each `correctedTestId` pointing to the `testId` of the most recent previous version.

### Name updated in SFT segment
We recently updated our project's name, and we've updated it in the `SFT` segment of the HL7 we generate now too to reflect it.

### Dependency Updates
The following dependencies were updated:
- eslint
- flyway
- libphonenumber
- azurefunctions
- sendgrid-java
- fontawesome
- netty
- azure-core
- azure-identity


## August 3, 2021
This release adds improvements and enhancements for the website including the ability to view the downloadable files in
distinct feeds as configured for each PHD.#  ReportStream Release Notes

## July 22, 2021
### Changes to api/reports response
This release adds a new `routing` element to the Json response if the query string contains a `verbose=true` parameter. This routing shows the destination(s) for each item in the report along with the report index and trackingId. The openapi.yml has been updated to reflect these changes with the structure of the routing element. Example curl command with query string parameters:
```
curl -X POST -H 'client: sample_client' -H 'Content-Type: text/csv' \
    --data-binary '@./examples/upload-fake-data/sample_report.csv' \
    'http://localhost:7071/api/reports?verbose=true'
```

An example of this routing element:

```
"routing" : [ {
    "reportIndex" : 0,
    "trackingId" : "123456",
    "destinations" : [ "st-phd.elr", "county-st-phd.elr" ]
  }, {
    "reportIndex" : 1,
    "trackingId" : "987654",
    "destinations" : [ "st-phd.elr" ]
  } ]  
```

### Data Model / Schema changes
- Added a new field, patAgeUnits, to payloads delivered to HHSProtect, per the HHS standard.
- Waters data now all flowing to HHSProtect
- Added site_of_care field as optional on all Covid data
- Added support for HL7 date/times with no offset


## July 7, 2021
### Corrected phone numbers to follow HL7 2.5.1 expectations
The first component of a Telephone number has been changed to `(###)###-####`
which follows of the HL7 2.3 specification for the US. It was `(###)#######`. 
In addition, if an extension number was present, it is now added to the number. #  ReportStream Release Notes


## June 25, 2021
### Submit Results in HL7 Format
Added the ability to ingest results as one HL7 message, or an HL7 batch message. The content 
type of `application/hl7-v2` must be provided in the headers when sending the results in HL7 format.  

### Documentation Updates
Added additional features to the documentation generation, including links for HL7 fields to the
documentation at Caristix.

### COVID-19 Test Result Metadata Capture
On ingestion of files from senders, ReportStream will now capture non-PII/non-PHI data allowing us to
build metrics and visualizations off of the data and provide trend analysis to our partners.

### HHSProtect
Fixed data formats to be a simple yyyyMMdd

### Dependency Updates
Updates done to:
- Postgresql libraries
- Flyway libraries
- Azure libraries


## June 14, 2021
### Abnormal_flag
As part of the jurisdiction filters feature, we are now defining a positive and negative results flag that 
can be used to create separate positive and negative feeds. The abnormal_flag was previously
only available on output as an HL7 OBX-8 value. 

### AS2 Transport
Added the ability to send results over an HTTPS/AS2 transport. Applicability Statement 2 (AS2) is
common in EDI systems and is used by HIEs.


## June 8, 2021
### File name changes
We are preparing the code to provide better filename formats.   More to come.

### Quality Updates
#### Default Quality Filter now excludes Training/Test and Debug data
The current default quality filter is:
```
To ensure basic Covid test quality, these fields MUST be supplied:
            message_id
            equipment_model_name
            specimen_type
            test_result
            patient_last_name
            patient_first_name
            patient_dob

To ensure minimal valid location or other contact info (for contact tracing), at least ONE of these must be supplied:
            patient_street
            patient_zip_code
            patient_phone_number
            patient_email
To ensure valid date/time (for relevance/urgency), at least ONE of these date fields must be supplied:
            order_test_date
            specimen_collection_date_time
            test_result_date

To ensure valid CLIA numbers, at least ONE of the following fields must be supplied and must be a valid CLIA (10 alphanumeric characters)
           testing_lab_clia
           reporting_facility_clia

The processing_mode_code must not be T (Training/Test) or D (Debug).
```

#### Secondary feed
States can now receive a secondary feed of data that does NOT meet the above filter criteria.  Other criteria can be added on a per-jurisdiction basis, or the quality filter itself can be overridden on a per-jurisdiction basis.


## June 1, 2021
### Addition of Quality Check
A basic Quality check on certain fields is now applied to Covid-19 data before sending it to downstream public health departments.
```
To ensure basic Covid test quality, these fields MUST be supplied:
            message_id
            equipment_model_name
            specimen_type
            test_result
            patient_last_name
            patient_first_name
            patient_dob

To ensure minimal valid location or other contact info (for contact tracing), at least ONE of these must be supplied:
            patient_street
            patient_zip_code
            patient_phone_number
            patient_email
To ensure valid date/time (for relevance/urgency), at least ONE of these date fields must be supplied:
            order_test_date
            specimen_collection_date_time
            test_result_date

To ensure valid CLIA numbers, at least ONE of the following fields must be supplied and must be a valid CLIA (10 alphanumeric characters)
           testing_lab_clia
           reporting_facility_clia

Important Notes
- The field names above are our ReportStream names - each jurisdiction often uses different names for these fields.
- It is easy to override the above default quality filter on a per-jurisdiction basis, as needed, to make it stricter or more lenient.
- It is also possible to turn off the default filter completely, by using the allowAll() filter.
- "supplied" means: The column name must be present (for CSV data), and a valid value must be present.

```

### Error and Warning Message Improvements
If there are problems with a data submission to ReportStream's reports endpoint, it returns detailed warning and error messages.   The submission continues even if there are warnings, but the entire submission will fail if there are any errors.

We have fixed a problem where ReportStream was failing to give warnings when a non-required coded field had an improper value.  Coded fields are those with an enumerated set of possible values, as found in a SNOMED, LOINC, HL7, or other valueset.    If a coded field is not required, and an illegal or unknown value is passed, ReportStream will continue to process that record to completion, but will replace the erroneous value with an empty string - this is all as designed.  However, we were not providing a warning back to the sender in these situations.  This is now fixed, so you may see more warnings in your submissions than before.

Example:
```
{
    "scope" : "ITEM",
    "id" : "2021042920-dsr^2021042920-f6ebbc3b133243e482cfdb4537c55ba4",
    "details" : "Invalid phone number 'n/a' for 'orderingProviderPhone' ('ordering_provider_phone_number') - setting value to ''"
}
```

#### Error and Warning Limits
Note that currently there is no limit on the number of warnings you may get.  Depending on the type of error, there is a limit of about 100 errors, after which ReportStream will stop processing the report.


## May 27, 2021
### Addition of Quality Check
A basic Quality check on certain fields is now applied to Covid-19 data before sending it to downstream public health departments.
```
For basic Covid test quality, these fields MUST be supplied:
            message_id
            equipment_model_name
            specimen_type
            test_result
            patient_last_name
            patient_first_name
            patient_dob

For minimal valid location or other contact info (for contact tracing), at least ONE of these must be supplied:
            patient_street
            patient_zip_code
            patient_phone_number
            patient_email
For valid date/time (for relevance/urgency), at least ONE of these date fields must be supplied:
            order_test_date
            specimen_collection_date_time
            test_result_date

For valid CLIA numbers, at least ONE of the following fields must be supplied:
           testing_lab_clia
           reporting_facility_clia

Important Notes
- The field names above are our ReportStream names - each jurisdiction often uses different names for these fields.
- It is easy to override the above default quality filter on a per-jurisdiction basis, as needed, to make it stricter or more lenient.
- It is also possible to turn off the default filter completely, by using the allowAll() filter.
- This is new, so we may tweak it.   
- "supplied" means: The column name must be present (for CSV data), and a valid value must be present.

```

### Error and Warning Message Improvements
If there are problems with a data submission to ReportStream's reports endpoint, it returns detailed warning and error messages.   The submission continues even if there are warnings, but the entire submission will fail if there are any errors.

In this release we have improved the messages to give better detail about the field with problems, giving both the column-name in the csv, and the internal ReportStream name for that field as well, for reference.

### Unicode
ReportStream CSV submissions now support the UTF-8 character set.

### Updated LIVD Table
ReportStream is updated to use the latest Covid LIVD table from HHS


## May 12, 2021
### LVID table update
The LVID table has been updated to the April 28, 2021 version. 
This update should add support multiple new SARS COV-2 tests. 
A review of the update shows that current tests that 
current ReportStream senders use are not affected. 

### Changes to api/reports response
This release adds the configured topic for the organization sender to the Json response along with an ISO-8601 timestamp. The openapi.yml was updated to reflect the changes as well.

```
{
  "id" : "abcd1234-abcd-1234-abcd-abcd1234abcd",
  "timestamp" : "2021-05-11T20:05:02.571867Z",
  "topic" : "covid-19",
  "reportItemCount" : 25,
  "destinations" : [ {
```

### Addition of Quality Check
A basic Quality check on certain fields is now applied to Covid-19 data before sending it to downstream public health departments.
```
       // valid human and valid test
        "hasValidDataFor(" +
            "message_id," +
            "equipment_model_name," +
            "specimen_type," +
            "test_result," +
            "patient_last_name," +
            "patient_first_name," +
            "patient_dob" +
        ")",
        // has valid location (for contact tracing)
        "hasAtLeastOneOf(patient_street,patient_zip_code)",
        // has valid date (for relevance/urgency)
        "hasAtLeastOneOf(order_test_date,specimen_collection_date_time,test_result_date)",
        // able to conduct contact tracing
        "hasAtLeastOneOf(patient_phone_number,patient_email)"
```

### Adds Reporting Facility Name to PID.3.4.1
The PID.3.4.1 field should contain the name of the reporting facility that has assigned the patient their ID. This has been added.

The field will also properly truncate if the state requires truncation of HD fields.


## April 26, 2021
### The `/api/reports` end-point handles malformed CSVs differently
A POST to the end-point (i.e. a submission of a report) will error if the CSV contains a malformed row. 
Examples of malformed rows include:
 - Empty rows
 - Rows with different number of columns

Previously, the row would be skipped and warning would be given for the row. 
The new behavior is to error on these malformed CSV files. #  Report Stream March 15, 2021

### New `/api/reports` options to support back filling of results
Two changes to the `/api/reports` end-point to assist in sending reports to a particular receiver. This is type action will typically be done when a new receiver is defined after a results are received by the hub. 

- `SendImmediately` value for the `option` query parameter to bypass any timing found on the receiver.
- `routeTo` query parameter restricts the list of receivers which a report can be routed to. 
  
The `routeTo` parameter **does not** bypass the jurisdictional filters of the receiver, so results meant for one receiver still cannot be sent to another receiver.#  Hub Release Notes

## Feb 12, 2021
### Changes to api/reports response
This release changes the format of destinations in our return Json from informal sentences

```
"destinations" : [ "Sending 58 items to Jones County Public Health Dept (prod-elr) at 2021-02-08T12:00-07:00", etc ]
```

to more formal json:
```
  "destinations" : [ {
    "organization" : "Jones County Public Health Dept",
    "organization_id" : "prod-phd",
    "service" : "prod-elr",
    "sending_at" : "2021-02-08T12:00-07:00",
    "itemCount" : 58
  }, {
```

Also adds a 
`destinationCount`

If the data is valid, but is going nowhere, you'll see:
```
  "destinations" : [ ],
  "destinationCount" : 0,
```


##Feb 4, 2021
### Changes to the Schemas, including for SimpleReport:
Added optional field `Corrected_result_ID`, a pointer/link to the unique Result_ID of a previously submitted result.  Usually blank. Or, if an item modifies/corrects a prior item, this field holds the ResultID of the prior item.

### LVID table updated
Added support for the January 20, 2021 LVID table. New tests supported includes the Binax NOW Home test. 

### Download site
- Download site now operational.
- Download files now have improved filenames, with the timestamp embedded in the filename.
- Filenames used by sftp send are now identical to the download site filesnames.
- If a disallowed or garbled report_id is access, delivers a basic 'not found' message (rather than no message at all).  Should never occur under normal site link usage.


## Tuesday January 12, 2021
## Changes to the Schemas, including for SimpleReport:
Added this value to `Specimen_type_code`
`445297001`

## Changes for AZ
One additional facility is now sending to `PROD/IN`.


##Thursday January 7, 2021
### Changes to the response json
Added these fields to the response to a POST to `reports/`
- `reportItemCount`
- `errorCount`
- `warningCount`
For details, see the [Hub OpenApi Spec](./openapi.yml)

### SimpleReport Schema changes
Hub is now ready to accept these fields, currently listed as optional:
- Processing_mode_code
- Test_result_status

Note: the data dictionary for SimpleReport csv data sent to the hub is here: [Simple Report Schema](./schema_documentation/primedatainput-pdi-covid-19.md)

### Incoming Data Formats and Customers
Now accepting data from Strac.   See [Link to detailed schema dictionaries](./schema_documentation)

### Outgoing Data Formats and Customers
Hub now sends HL7 data to AZ TEST site (only).   This data will be identical to the .csv data sent to TEST, but in HL7 instead of CSV format, so they can be compared.   Note:  currently this data is not 'batched', so there is no delay - it will be sent as soon as its sent by simplereport.

New outgoing data formats are now available for PA, FL, CO.  See [Link to detailed schema dictionaries](./schema_documentation)

### Timing for AZ
- In anticipation of SimpleReport sending us one report at a time, we've switching the timing of sends to AZ DHS to 6 times a day, every 4 hours.
- In test, files are sent to our mock sftp site every 10 minutes.
- Production Pima is now set to send files once a day, at 8am AZ time.  However, currently there is no send to Pima, its still manual.


## December 29, 2020
### No options specified
`curl -X POST -H "client:simple_report" -H "Content-Type: text/csv" -H "x-functions-key:<secret>" --data-binary "@./src/test/csv_test_files/input/simplereport.csv" "https://prime-data-hub-prod.azurefd.net/api/reports"`

#### Example Results
```
{
  "id" : "0336462c-7002-4e08-a4de-9606752f32fa",
  "destinations" : [ "Sending 50 items to Arizona PHD (elr-test) at 2020-12-29T16:23-07:00", "Sending 25 items to Pima County, Arizona PHD at 2020-12-29T16:23-07:00" ],
  "errors" : [ ],
  "warnings" : [ {
    "scope" : "REPORT",
    "id" : "",
    "details" : "Missing 'Test_result_status' header"
  } ]
}
```

### With `option=CheckConnections`
Always safe to call in Production

`curl -X POST -H "client:simple_report" -H "Content-Type: text/csv" -H "x-functions-key:<secret>" --data-binary "@./src/test/csv_test_files/input/simplereport.csv" "https://prime-data-hub-prod.azurefd.net/api/reports?option=CheckConnections"`

#### Everything looks good, so you see this:
```
{
  "id" : null,
  "destinations" : [ ],
  "errors" : [ ],
  "warnings" : [ ]
}
```

### With	`option=ValidatePayload`:  Parse the data for correctness
Always safe to call in Production

`curl -X POST -H "client:simple_report" -H "Content-Type: text/csv" -H "x-functions-key:<secret>" --data-binary "@./src/test/csv_test_files/input/simplereport.csv" "https://prime-data-hub-prod.azurefd.net/api/reports?option=ValidatePayload"`

#### Note the destinations is now empty.
```
{
  "id" : "42ec8a8f-f7bd-42e5-85ab-c0d69364f6f3",
  "destinations" : [ ],
  "errors" : [ ],
  "warnings" : [ {
    "scope" : "REPORT",
    "id" : "",
    "details" : "Missing 'Test_result_status' header"
  } ]
}
```

### With `option=SkipSend`: Run data thru the gauntlet, but safely skip the final step
Always safe to call in Production

`curl -X POST -H "client:simple_report" -H "Content-Type: text/csv" -H "x-functions-key:<secret>" --data-binary "@./src/test/csv_test_files/input/simplereport.csv" "https://prime-data-hub-prod.azurefd.net/api/reports?option=SkipSend"`

#### You can't tell any difference here, but in theory the Hub team can watch for internal errors.  We plan to build a query api that lets you track data custody

```
{
  "id" : "69bb28ee-5dd0-4f80-91de-b094a45518b7",
  "destinations" : [ ],
  "errors" : [ ],
  "warnings" : [ {
    "scope" : "REPORT",
    "id" : "",
    "details" : "Missing 'Test_result_status' header"
  } ]
}
```
