# History API Proposal

## Context and Motivation
As the product feature set continues to grow along with the sending and receiving organizations being onboarded, more robust tracking and reporting is needed to help provide insight into how reports were processed, routed and delivered. This proposal will define end points to provide information to developers and organizations for testing, monitoring and reporting.      

## Goals
The goals of this propsal:
* Define end points for report submission results including errors and warnings
* Define end points for report item routing details 
* Define end points for report delivery status (sent, pending, failed)

## Prerequisites
* Issue [#1437](https://github.com/CDCgov/prime-reportstream/issues/1437) which adds a new JSONB column to the action table to capture the full verbose response to an /api/reports POST. This will contain error/warning messages, destinations, item specific routing which can be reported on via reportId and potentially itemId.
* Add a HttpStatus column to the action table specifically for `receive` actions to make it easier to determine pass/fail submissions. The new JSONB field will likely need to have the 'CREATED' and or 'BAD_REQUEST' prefix removed.
* PR [#1231](https://github.com/CDCgov/prime-reportstream/pull/1231) which adds report routing by trackingId further broken down by jurisdiction (federal, state and county). This information will be used for data in the end points.
* Not a prerequisite, but useful to implement is proposal [#1285](https://github.com/CDCgov/prime-reportstream/issues/1285) which attempts to normalize messaging and summarize/aggregate to reduce the payload size. This may help fit the full response into the 2048 char `action_result` column with the full verbose response in the `action_result_json` column which will be used for end point querying.

## Proposed End Points
All endpoints will use the same security model as the `/settings/*` end points. Consideration should be given to check the user's role / Okta claims to ensure that they do have access to the organization to which the report is linked. Admins will be able to see all reports. Organization admins can see only their reports.

### Report Submissions for a Given Date Range by Organization
`GET /reports/{organization}?start=YYYY-MM-DD&end=YYYY-MM-DD&limit=20&offset=0`

Parameters:
* start - start date (inclusive) to query against the created_at date
* end - end date (exclusive) to queery against the created_at date
* limit - max number of submissions to pull back
* offset - where to start the offset for pagination

This end point will query the action and report_file tables to generate a list of all successful and failed `POST` submissions to the /api/reports endpoint. The intent is to show report submission frequency as well as capturing failed submissions. Pagination is supported with the limit and offset parameters. The resultSet provided in the response will allow for the UI to create pagination navigation elements.

Example Json:
```json
{
    "result_set": {
        "count": 20,
        "offset": 0,
        "limit": 20,
        "total": 3
    },
    "submissions": [
        {
            "actionId": 143,
            "reportId" : "18a8e461-b3f8-436d-b94e-4065c7b9d186",
            "createdAt": "2021-06-30 13:03:00.305657-04",
            "reportItemCount" : 23,
            "destinationCount" : 5,
            "warningCount" : 8,
            "errorCount" : 0,
            "status": "CREATED"
        },
        {
            "actionId": 158,
            "reportId" : null,
            "createdAt": "2021-06-30 13:03:00.305657-04",
            "reportItemCount" : 0,
            "destinationCount" : 0,
            "warningCount" : 5,
            "errorCount" : 3,
            "status": "BAD_REQUEST"
        },
        {
            "actionId": 177,
            "reportId" : null,
            "createdAt": "2021-06-30 13:03:00.305657-04",
            "reportItemCount" : 0,
            "destinationCount" : 0,
            "warningCount" : 0,
            "errorCount" : 0,
            "status": "EXCEPTION"
        }
    ]
}
```
To simplify, the status could be made binary (pass/fail) with more detail added on a separate API call. Also, adding a new HttpStatus column to the action table for the receive results would simplify having to parse action_result to determine if the POST was successful. Alternatively, we could assume that if no report_file record was created for the action then the POST failed. However, I think the HttpStatus would be more useful long term.

### Action Details for a Given Id
`GET /reports/{organization}/action/{id}`

As a followup request to the above endpoint, this provides more details on the specific action referenced in the results above. The response on this endpoint will show the metadata stored for the specific action. This shold be limited to receive actions only and check the action_params to ensure the organizations match.

Example Json:
```json
{
    "actionId": 233,
    "actionName": "receive",
    "createdAt": "2021-06-30 13:03:00.305657-04",
    "status": "BAD_REQUEST",
    "actionParams": "{\r\n   \"method\" : \"POST\",\r\n   \"Headers\" : {\r\n     \"connection\" : \"keep-alive\",\r\n     \"content-type\" : \"text\/csv\",\r\n     \"accept\" : \"text\/html, image\/gif, image\/jpeg, *; q=.2, *\/*; q=.2\",\r\n     \"host\" : \"localhost:7071\",\r\n     \"user-agent\" : \"Java\/11.0.10\",\r\n     \"content-length\" : \"0\",\r\n     \"client\" : \"ignore.ignore-simple-report\"\r\n   },\r\n   \"QueryParameters\" : { }\r\n }",
    "actionResult": "{\r\n   \"id\" : null,\r\n   \"warningCount\" : 0,\r\n   \"errorCount\" : 1,\r\n   \"errors\" : [ {\r\n     \"scope\" : \"PARAMETER\",\r\n     \"id\" : \"Content\",\r\n     \"details\" : \"expecting a post message with content\"\r\n   } ],\r\n   \"warnings\" : [ ]\r\n}"
}
```
The actionParams and actionResult elements are escaped Json/mixed text. Both parameters should be well formatted Json once the prerequisites are implemented. This response is basically serializing a record in the action table to a Json response.

### Report Items
`GET /report/{reportId}/items?limit=20&offset=0`

This end point will report the status of all items in the given reportId after verifying that the authenticated user has access to the organization that sent the report. This endpoint will also use the same pagination pattern as the endpoint above. 

Exmaple Json:
```json
{
    "result_set": {
        "count": 20,
        "offset": 0,
        "limit": 20,
        "total": 2
    },
    "reportId" : "18a8e461-b3f8-436d-b94e-4065c7b9d186",
    "createdAt": "2021-06-30 13:03:00.305657-04",
    "items": [
        {
            "trackingId": "reportIndex-0",
            "destinationCount": 2,
            "destinations": {
                "COUNTY": [],
                "STATE": [
                    {
                        "reportId": "33a8e478-b3d2-498d-b94e-4065f7b9e134",
                        "receiverOrg": "md-doh",
                        "recieverName": "elr",
                        "format": "HL7_BATCH",
                        "sentOn": "2021-06-29 11:00:00.086375-04"
                    }
                ],
                "FEDERAL": [
                    {
                        "reportId": "44a8e478-c3f2-498d-b94d-5068f7b9e134",
                        "receiverOrg": "hhsprotect",
                        "recieverName": "elr",
                        "format": "CSV",
                        "sentOn": "2021-06-29 11:00:00.086375-04"
                    }
                ]
            },
            "trackingId": "reportIndex-2",
            "destinationCount": 0,
            "destinations": {
                "COUNTY": [],
                "STATE": [],
                "FEDERAL": []
            }
        }
    ]
}
```
Alternatively, the jurisdiction breakdown can be removed and added as an element to each individual report. Items routing no where would have a destination count of zero and empty jurisdiction arrays (unless the structure is changed).

Alternative Json Response:
```json
{
    "result_set": {
        "count": 20,
        "offset": 0,
        "limit": 20,
        "total": 2
    },
    "reportId" : "18a8e461-b3f8-436d-b94e-4065c7b9d186",
    "createdAt": "2021-06-30 13:03:00.305657-04",
    "items": [
        {
            "trackingId": "reportIndex-0",
            "destinationCount": 1,
            "destinations": [
                {
                    "reportId": "33a8e478-b3d2-498d-b94e-4065f7b9e134",
                    "receiverOrg": "md-doh",
                    "recieverName": "elr",
                    "jurisdiction": "STATE",
                    "format": "HL7_BATCH",
                    "sentOn": "2021-06-29 11:00:00.086375-04"
                },
                {
                    "reportId": "44a8e478-c3f2-498d-b94d-5068f7b9e134",
                    "receiverOrg": "hhsprotect",
                    "recieverName": "elr",
                    "jurisdiction": "FEDERAL",
                    "format": "CSV",
                    "sentOn": "2021-06-29 11:00:00.086375-04"
                }
            ]
        },
        {
            "trackingId": "reportIndex-1",
            "destinationCount": 0,
            "destinations": []
        }
    ]
}
```
The above Json only represents the sentOn status for the report. What are the different states/status of the destination reports that we want to represent? 
* batch?
* pending?
* sent
* error/failed

### Report Items
`GET /report/{reportId}/item/{trackingId}`

This endpoint will return the individual response for a report item. The Json response is structured the same as for all items just an array of one so consumers can use that same Json parsing.

Exmaple Json:
```json
{
    "reportId" : "18a8e461-b3f8-436d-b94e-4065c7b9d186",
    "createdAt": "2021-06-30 13:03:00.305657-04",
    "items": [
        {
            "trackingId": "reportIndex-0",
            "destinationCount": 1,
            "destinations": [
                {
                    "reportId": "33a8e478-b3d2-498d-b94e-4065f7b9e134",
                    "receiverOrg": "md-doh",
                    "recieverName": "elr",
                    "jurisdiction": "STATE",
                    "format": "HL7_BATCH",
                    "sentOn": "2021-06-29 11:00:00.086375-04"
                },
                {
                    "reportId": "44a8e478-c3f2-498d-b94d-5068f7b9e134",
                    "receiverOrg": "hhsprotect",
                    "recieverName": "elr",
                    "jurisdiction": "FEDERAL",
                    "format": "CSV",
                    "sentOn": "2021-06-29 11:00:00.086375-04"
                }
            ]
        }
    ]
}
```
