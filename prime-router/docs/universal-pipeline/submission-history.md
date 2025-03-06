# Submission History API

## How to Use

As mentioned in the [Universal Pipeline Overview](README.md), a `Report` is submitted via an HTTP POST to the preferred `/api/waters/report` (in honor of [Dr. Michael Stephan Waters](https://reportstream.cdc.gov/developer-resources/api#about-our-api).) or the deprecated `/api/report` endpoints.  The subsequent status of that submission is determined via an HTTP GET request to `/api/waters/report{id}/history` or `/api/report/{id}/history` where id is either the “id” or “submissionId” returned from the original POST (or the "Report ID" from the Submission History page on the web site).

## Examples of JSON Responses

This section documents some typical JSON responses one might expect to receive as part of submission history requests.

### Checking for Overall Status and Destinations

Use the **overallStuatus** field to determine the current progress of the submission as it is being processed through the pipeline.  Use the **destinations** field to determine where the report has been assigned to be routed.

#### Waiting to Deliver

This response contains the "Waiting to Deliver" value within the **overallStatus** field. This means the report has been processed successfully and it's waiting to be delivered to the listed destination(s) – which, in this example, is the "FOR DEVELOPMENT PURPOSES ONLY" organization (normally this would be a public health organization).

```
{
    "id": "76c1f0ce-bae4-40ae-8297-a32d34816ff9",
    "submissionId": 2558,
    "overallStatus": "Waiting to Deliver",
    "timestamp": "2024-08-19T18:04:57.593Z",
    "plannedCompletionAt": null,
    "actualCompletionAt": null,
    "sender": "ignore.dev-simple-report",
    "reportItemCount": 1,
    "errorCount": 0,
    "warningCount": 0,
    "httpStatus": 201,
    "destinations": [
        {
            "organization": "FOR DEVELOPMENT PURPOSES ONLY",
            "organization_id": "development",
            "service": "DEV_FULL_ELR",
            "itemCount": 1,
            "itemCountBeforeQualityFiltering": 1,
            "filteredReportRows": [],
            "filteredReportItems": [],
            "sentReports": [],
            "downloadedReports": []
        }
    ],
    "actionName": "receive",
    "externalName": null,
    "reportId": "76c1f0ce-bae4-40ae-8297-a32d34816ff9",
    "topic": "full-elr",
    "bodyFormat": "",
    "errors": [],
    "warnings": [],
    "destinationCount": 1,
    "fileName": ""
}
```

#### Not Delivering

This response contains the "Not Delivering" value within the **overallStatus** field.  This response means ReportStream couldn't find any destination to route this report and/or the report was filtered out due to a receiver's filter settings.

```
{
    "id": "a86d4bb2-98d1-4c1a-9435-5bb2f139dcf7",
    "submissionId": 4277335,
    "overallStatus": "Not Delivering",
    "timestamp": "2024-08-28T20:08:02.647Z",
    "plannedCompletionAt": null,
    "actualCompletionAt": null,
    "sender": "simple_report.fullelr",
    "reportItemCount": 1,
    "errorCount": 0,
    "warningCount": 0,
    "httpStatus": 201,
    "destinations": [],
    "actionName": "receive",
    "externalName": null,
    "reportId": "a86d4bb2-98d1-4c1a-9435-5bb2f139dcf7",
    "topic": "full-elr",
    "bodyFormat": "",
    "errors": [],
    "warnings": [],
    "destinationCount": 0,
    "fileName": ""
}
```

#### Partially Delivered

This response contains the "Partially Delivered" value within the **overallStatus** field AND contains multiple **organizations** ("FOR CONNECTATHON PURPOSES ONLY" and "Data Ingestion CDC NBS") under **destinations**. This response means the report was assigned to be routed to multiple places, but it was only delivered successfully to the destination(s) that contains the **sentReports** set – in this case the "FOR CONNECTATHON PURPOSES ONLY" organization. Destinations that contain **filteredReportRows** mean that the report was filtered out for that specific destination – in this case the "Data Ingestion CDC NBS" organization – due to the matching arguments provided to the "PROCESSING_MODE_FILTER".

```
{
    "id": "ab774756-83e3-4add-bf91-765ddae1877f",
    "submissionId": 3680394,
    "overallStatus": "Partially Delivered",
    "timestamp": "2024-02-10T00:14:01.707Z",
    "plannedCompletionAt": "2024-02-10T00:15:00.000Z",
    "actualCompletionAt": null,
    "sender": "simple_report.default",
    "reportItemCount": 1,
    "errorCount": 0,
    "warningCount": 2,
    "httpStatus": 201,
    "destinations": [
        {
            "organization": "FOR CONNECTATHON PURPOSES ONLY",
            "organization_id": "connectathon",
            "service": "STLT-DASHBOARD-HL7-3",
            "itemCount": 1,
            "itemCountBeforeQualityFiltering": 1,
            "sending_at": "2024-02-10T00:15:00.000Z",
            "filteredReportRows": [],
            "filteredReportItems": [],
            "sentReports": [
                {
                    "reportId": "4d9a386f-a42e-41f4-8ccc-d9003b553ac7",
                    "externalName": "fl-covid-19-bdca04bc-8ea1-43db-8663-7384497a5d83-20240210001519.hl7",
                    "createdAt": "2024-02-10T00:15:34.567Z",
                    "itemCount": 1
                }
            ],
            "downloadedReports": []
        },
        {
            "organization": "Data Ingestion CDC NBS",
            "organization_id": "dataingestion",
            "service": "datateam-cdc-nbs",
            "itemCount": 0,
            "itemCountBeforeQualityFiltering": 1,
            "filteredReportRows": [
                "For dataingestion.datateam-cdc-nbs, filter matches[processing_mode_code, T, D] filtered out item 0568ddf9-5b0d-422f-be7c-8f2511ef5cdc"
            ],
            "filteredReportItems": [
                {
                    "filterType": "PROCESSING_MODE_FILTER",
                    "filterName": "matches",
                    "filteredTrackingElement": "0568ddf9-5b0d-422f-be7c-8f2511ef5cdc",
                    "filterArgs": [
                        "processing_mode_code",
                        "T",
                        "D"
                    ],
                    "message": "For dataingestion.datateam-cdc-nbs, filter matches[processing_mode_code, T, D] filtered out item 0568ddf9-5b0d-422f-be7c-8f2511ef5cdc"
                }
            ],
            "sentReports": [],
            "downloadedReports": []
        }
    ],
    "actionName": "receive",
    "externalName": null,
    "reportId": "ab774756-83e3-4add-bf91-765ddae1877f",
    "topic": "covid-19",
    "bodyFormat": "",
    "errors": [],
    "destinationCount": 2,
    "fileName": ""
}
```

#### Delivered

This response contains the "Delivered" value within the **overallStatus** field.  This response means the report was successfully delivered to all destination(s) – in this case to the "New York Public Health Department" organization.  Do note that this **destination** contains a **sentReprts** set.


```
{
    "id": "fc2b680b-702d-4106-adca-8297a3fea1ba",
    "submissionId": 4159914,
    "overallStatus": "Delivered",
    "timestamp": "2024-03-18T20:02:01.355Z",
    "plannedCompletionAt": "2024-03-18T20:03:00.000Z",
    "actualCompletionAt": "2024-03-18T20:03:39.044Z",
    "sender": "simple_report.fullelr",
    "reportItemCount": 1,
    "errorCount": 0,
    "warningCount": 0,
    "httpStatus": 201,
    "destinations": [
        {
            "organization": "New York Public Health Department",
            "organization_id": "ny-phd",
            "service": "full-elr-test",
            "itemCount": 1,
            "itemCountBeforeQualityFiltering": 1,
            "sending_at": "2024-03-18T20:03:00.000Z",
            "filteredReportRows": [],
            "filteredReportItems": [],
            "sentReports": [
                {
                    "reportId": "e659baa0-b958-496e-8250-748bdd64d1f2",
                    "externalName": "ny-phd-539aa93e-b3d3-473a-8251-40341e4e57e9-20240318200311.hl7",
                    "createdAt": "2024-03-18T20:03:39.044Z",
                    "itemCount": 1
                }
            ],
            "downloadedReports": []
        }
    ],
    "actionName": "receive",
    "externalName": null,
    "reportId": "fc2b680b-702d-4106-adca-8297a3fea1ba",
    "topic": "full-elr",
    "bodyFormat": "",
    "errors": [],
    "warnings": [],
    "destinationCount": 1,
    "fileName": ""
}
```

### Checking for Failures

Use the **errors** and **warnings** fields to identify any errors or warnings that may have been flagged during the processing of the report.


#### Failed to Parse Message

This is an example of a synchronous response when sending a report and the message is malformed. The **overallStatus** is "Received" – meaning the submission was received by ReportStream and awaits further processing in the pipeline – but **errors** show ReportStream "Failed to parse message".

```
{
    "id": null,
    "submissionId": 4277529,
    "overallStatus": "Received",
    "timestamp": "2024-08-29T22:06:27.715Z",
    "plannedCompletionAt": null,
    "actualCompletionAt": null,
    "sender": "",
    "reportItemCount": null,
    "errorCount": 2,
    "warningCount": 0,
    "httpStatus": 400,
    "destinations": [],
    "actionName": "receive",
    "externalName": "",
    "reportId": null,
    "topic": null,
    "bodyFormat": "",
    "errors": [
        {
            "scope": "report",
            "message": "Failed to parse message",
            "errorCode": "UNKNOWN"
        }
    ],
    "warnings": [],
    "destinationCount": 0,
    "fileName": ""
}
```

#### Profile Validation

This is an example of an asynchronous response, and because the message has failed a specified profile validation (as detailed in the **errors** set), the report will not be delivered (as stated in the **overallStatus**).


```
{
    "id": null,
    "submissionId": 4277530,
    "overallStatus": "Not Delivering",
    "timestamp": "2024-08-29T22:07:31.138Z",
    "plannedCompletionAt": null,
    "actualCompletionAt": null,
    "sender": "demo.mars-otc-elr-onboarding-sender",
    "reportItemCount": 1,
    "errorCount": 1,
    "warningCount": 0,
    "httpStatus": 201,
    "destinations": [],
    "actionName": "receive",
    "externalName": null,
    "reportId": null,
    "topic": "mars-otc-elr-onboarding",
    "bodyFormat": "",
    "errors": [
        {
            "scope": "item",
            "indices": [
                1
            ],
            "trackingIds": [
                "20240403205305_dba7572cc6334f1ea0744c5f235c823e"
            ],
            "message": "Item 1 in the report was not valid. Reason: HL7 was not valid at OBR[1]-16[1] for validator: RADx MARS Onboarding, HL7 was not valid at OBR[1]-3[1].1 for validator: RADx MARS Onboarding, HL7 was not valid at ORC[1]-23[1] for validator: RADx MARS Onboarding, HL7 was not valid at ORC[1]-12[1] for validator: RADx MARS Onboarding, HL7 was not valid at ORC[1]-3[1].1 for validator: RADx MARS Onboarding, HL7 was not valid at PID[1]-13[1].3 for validator: RADx MARS Onboarding, HL7 was not valid at ORU_R01[1] for validator: RADx MARS Onboarding, HL7 was not valid at PID[1] for validator: RADx MARS Onboarding, HL7 was not valid at OBX[1]-8[1] for validator: RADx MARS Onboarding, HL7 was not valid at OBX[1]-24[1] for validator: RADx MARS Onboarding, HL7 was not valid at SPM[1]-4[1].2 for validator: RADx MARS Onboarding",
            "errorCode": "INVALID_MSG_VALIDATION"
        }
    ],
    "warnings": [],
    "destinationCount": 0,
    "fileName": ""
}
```



## Fields Returned in JSON Response
The JSON data that is returned to the consumer of these API endpoints is identical in content.  The format of the JSON may vary as the order of the attributes is not guaranteed when it is serialized.

| Field | Possible Values | Description |
| ----- | --------------- | --------------- |
| actionName | e.g., "receive" | The type of action that created this report file. |
| actualCompletionAt | e.g., "2024-03-18T20:03:00.000Z" | Marks the actual time this submission finished sending. |
| bodyFormat | e.g., "fhir", "hl7" | File type.  Used for generating the filename. May be empty. |
| destinationCount | 1,2,3, ... | The integer number of destinations to which data will or have been sent. |
| destinations | See the examples in the **Destinations** section. | A list of destination(s) to which the Report is being sent.  This section doesn't get populated if it's too early in processing (received) or a destination cannot be found (not delivering).  See the breakdown in the **Destinations** section. |
| errorCount | 1,2,3, ... | The integer number of errors.  Note this is not the number of consolidated errors. |
| errors | <ul><li>**scope** - "report", "item"</li><li>**indices** - [1,3]</li><li>**trackingIds** - [" 20240403205305_dba7572cc6334f1ea0744c5f235c823e"]</li><li>**message** - "Failed to parse message."</li><li>**errorCode** - "UNKNOWN"</li></ul> | Errors are a list of zero or more objects composed of the following attributes:<ul><li>**scope** - the scope of the event.</li><li>**indices** - A list of integers specifying the index of the item related to the message.</li><li>**trackingIds** - A list of the ids for identifying the test that this log is related to - the ID of the item if applicable.</li><li>**message** - A readable version of the various other properties this object has.</li><li>**errorCode** - The error code used to translate the error in the user interface.</li></ul> |
| externalName |  | The actual filename of the file.  May be null. |
| fileName |  | The actual download path for the file. May be empty. |
| httpStatus | e.g., "201" | The [HTTP status](https://www.restapitutorial.com/httpstatuscodes) from when the Report was received. |
| id | e.g., "ab774756-83e3-4add-bf91-765ddae1877f" | Alias for the reportId ("Report ID" as seen in the web view of Submission History). |
| overallStatus | One of: <ol><li>"Valid"</li><li>"Error"</li><li> "Received"</li><li>"Not Delivering"</li><li>"Waiting to Deliver"</li><li>"Partially Delivered"</li><li>"Delivered"</li></ol> | Documents the current progress through the pipeline.<ol><li>Successfully validated but not sent.</li><li>Error on initial submission.</li><li>Passed the received step in the pipeline and awaits processing/routing.</li><li>Processed but has no intended receivers.</li><li>Processed but yet to be sent/downloaded.</li><li>Processed, successfully sent to/downloaded by at least one receiver.</li><li>Processed, successfully sent to/downloaded by all receivers.</li></ol> |
| plannedCompletionAt | e.g., "2024-08-19T18:04:57.593Z" | When this submission is expected to finish sending.  May be null if there is an error with submission. |
| reportId | e.g., "ab774756-83e3-4add-bf91-765ddae1877f" | [Unique identifier](https://en.wikipedia.org/wiki/Universally_unique_identifier) for this specific report file. |
| reportItemCount | 1,2,3, ... | The integer number of tests (data rows) contained in the report. |
| sender | e.g., "simple_report.default" | The sender's organization id. |
| submissionId | e.g., "2588" | A unique (to the system) integer associated with each action performed on a submitted Report. |
| timestamp | e.g., "2024-03-18T20:02:01.355Z" | The date/time of the query response. |
| topic | e.g., "full-elr", "covid-19" | The kind of data contained in the report. |
| warningCount | 1,2,3, ... | The number of warnings.  Note this is not the number of consolidated warnings. |
| warnings | See examples for errors. | Warnings utilize the same format and attributes as errors. |

### Destinations

| Field | Possible Values | Description |
| ----- | --------------- | --------------- |
| downloadedReports |  | Logs of reports for this submission downloaded for this destination. |
| filteredReportItems | <ol><li>**filterArgs** - [ "processing&#95;mode&#95;code", "T", "D" ]</li><li>**filteredTrackingElement** - "0568ddf9-5b0d-422f-be7c-8f2511ef5cdc"</li><li>**filterName** - "matches"</li><li>**filterType** - "PROCESSING_MODE_FILTER"</li><li>**message** - "For dataingestion.datateam-cdc-nbs, filter matches[processing&#95;mode&#95;code, T, D] filtered out item 0568ddf9-5b0d-422f-be7c-8f2511ef5cdc"</li></ol> | A more structured version of filteredReportRows.<ol><li>**filterArgs** - What specifically in the filter was triggered.</li><li>**filteredTrackingElement** - The name of the test that was filtered out.</li><li>**filterName** - The name of the actual filter that was triggered.</li><li>**filterType** - The kind of filter that was triggered.</li><li>**message** - Readable version of the various other properties this object has.</li></ol> |
| filteredReportRows | e.g., "For dataingestion.datateam-cdc-nbs, filter matches[processing_mode_code, T, D] filtered out item 0568ddf9-5b0d-422f-be7c-8f2511ef5cdc" | A list of filters that were triggered by the contents of the report. |
| itemCount | 1,2,3, ... | The final integer number of tests available in the report received by the destination. |
| itemCountBeforeQualityFiltering	 | 1,2,3, ... | The total integer number of tests that were in the submitted report before any filtering. |
| organization | e.g., "New York Public Health Department" | Name for the organization based on the id provided. |
| organization_id| e.g., "ny-phd" | The identifier for the organization that owns this destination. |
| sending_at | e.g., "2024-03-18T20:03:00.000Z" | The time that this destination is next expecting to receive a report. |
| sentReports | Examples:<ol><li>**createdAt** - "2024-03-18T20:03:39.044Z"</li><li>**externalName** - "ny-phd-539aa93e-b3d3-473a-8251-40341e4e57e9-20240318200311.hl7"</li><li>**itemCount** - 1,2,3, ...</li><li>**reportId** - "e659baa0-b958-496e-8250-748bdd64d1f2"</li></ol> | Logs of reports for this submission sent to this destination.<ol><li>**createdAt** - When the file was created.</li><li>**externalName** - Actual filename of the file.</li><li>**itemCount** - The final integer number of tests available in the report received by the destination.</li><li>**reportId** - The unique identifier for this specific report file.</li></ol> |
| service | e.g., "full-elr-test" | The service used by the organization (e.g. "elr"). |


## List of Potential Errors and Warnings

The following table lists a representation of the types of errors that you may be generated as a `Report` traverses the pipeline.
 

| ERR,WARNING,INFO | Code | Scope | Message Value |
| ---------------- | ---- | ----- | ------------- |
|ERR|UNKNOWN|Report|"Unable to find HL7 messages in provided data."
|ERR|UNKNOWN|Report|"Provided raw data is empty."
|ERR|UNKNOWN|Report|"$index: Unable to find FHIR Bundle in provided data."
|ERR|UNKNOWN|Report|"$index: Unable to parse FHIR data."
|WARN|UNKNOWN|Parameter|"Url Options Parameter, $optionsText has been deprecated. Valid options: ${Options.activeValues.joinToString()}"
|ERR|UNKNOWN|Report|"IllegalArgumentException.message OR ""Invalid Request."
|ERR|UNKNOWN|Report|"IllegalStateException.message OR ""Invalid Request."
|ERR|UNKNOWN|Report|"IllegalArgumentException.message OR ""Invalid Request."
|ERR|UNKNOWN|Report|"IllegalStateException.message OR ""Invalid Request."
|ERR|UNKNOWN|Report|"Options.InvalidOptionException.message OR ""Invalid Request."
|INFO|UNKNOWN|Translation|IllegalStateException.localizedMessage
|FILTER|UNKNOWN|Translation|"For $receiverName, filter $filterName$filterArgs filtered out item $filteredTrackingElement. Filter Type: $filterType Filter Args: $filterArgs Filtered Observation Details: $filteredObservationDetails"
|INFO|UNKNOWN|Report|"An unexpected error occurred requiring additional help. Contact the ReportStream team at reportstream@cdc.gov."
|WARN|INVALID_MSG_CONDITION_MAPPING|Item|"Observation missing code"
|WARN|INVALID_MSG_CONDITION_MAPPING|Item|"Missing mapping for code(s): "" + unmappableCodes.joinToString("","")"
|WARN|UNKNOWN|Item|"Item was not routed to $receiverOrg.$receiverName because it did not pass the $filterType. Item failed on: $filter"
|WARN|UNKNOWN|Item|"Item was not routed to $receiverOrg.$receiverName because it did not pass the $filterType. Item failed on: $filter"
|WARN|UNKNOWN|Item|"Item was not routed to $receiverOrg.$receiverName because it did not pass the $filterType. Item failed on: $filter"
|ERR|UNKNOWN|Report|"Unable to find HL7 messages in provided data."
|ERR|UNKNOWN|Parameter|"Invalid receiver name: $it"
|ERR|UNKNOWN|Parameter|InvalidParameterException.message
|ERR|UNKNOWN|Parameter|"'$CLIENT_PARAMETER:$clientName': unknown sender"
|ERR|UNKNOWN|Parameter|"Missing ${HttpHeaders.CONTENT_TYPE} header"
|ERR|UNKNOWN|Parameter|"Resubmit as '${sender.format.mimeType}'"
|ERR|UNKNOWN|Parameter|"Blank message(s) found within file. Blank messages cannot be processed."
|ERR|UNKNOWN|Parameter|"Expecting a post message with content"
|ERR|UNKNOWN|Parameter|"'$it' is not a valid default"
|ERR|UNKNOWN|Report|"Provided raw data is empty."
|ERR|UNKNOWN|Report|"Parse error while attempting to iterate over HL7 raw message"
|ERR|UNKNOWN|Report|"Received unsupported report format: $format"
|ERR|INVALID_MSG_PARSE|Item|"exception while parsing HL7: ${ExceptionUtils.getRootCause(HL7Exception).message}"
|ERR|INVALID_MSG_PARSE|Item|"exception while parsing FHIR: ${DataFormatException.message}"
|ERR|INVALID_MSG_VALIDATION|Item|"Includes ""HL7 was not valid at ${it.path} for validator: ${validator.validatorProfileName}"
|ERR|INVALID_MSG_VALIDATION|Item|SingleValidationMessage.message
|ERR|INVALID_MSG_CONVERSION|Item|"exception while converting HL7: ${Exception.message}"
|ERR|UNKNOWN|Report|"Exception.message OR ""
|ERR|UNKNOWN|Report|"Validation Failed: ${exception.message}"
|ERR|UNKNOWN|Report|"Required field missing: ${exception.message}"
|ERR|UNKNOWN|Report|"Data type error: ${exception.message}"
|ERR|UNKNOWN|Report|"Failed to parse message"
||500||"{""error"": ""Internal error at ${OffsetDateTime.now()}""}"
||404||IllegalStateException.message
||401||"Authentication Failed"
||404||"$id is not a valid report"
||401||"Unauthorized"%                                                                                                                                                                                                                               


