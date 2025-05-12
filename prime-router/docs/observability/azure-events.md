# Emitting Events to AppInsights

## Background
An event is something that an application accomplishes that is significant at the system level whereas a 
log is only locally significant. For Report Stream, we want to emit events where we successfully accomplish
an action that we want to observe through visualizations.

## How to emit an event

An event is defined as a data class that extends from `AzureCustomEvent`.

The first thing to figure out is what data you would like to be preserved, queried, and visualized about this event.
Any piece of data that is important for those goals should be at the top level of your data class! Any additional 
pieces of data which may or may not be used in future can be levels deeper. This nested data can potentially be queried 
in the future, but it will be much slower so plan carefully!

```kotlin
data class MyEvent(
    val importantString: String,
    val veryImportantNumber: Int,
    val bucketOfAdditionalData: NestedComplexObject
) : AzureCusomEvent

data class NestedComplexObject(
    val fieldWhichCouldHelpInTheFuture: String,
    val anotherField: Double
)
```

To actually emit your event, you will have to inject `AzureEventService` into your class and call `trackEvent` with
your defined event.

```kotlin
class MyService(
    private val azureEventService: AzureEventService = AzureEventService()
) {
    
    fun doWork() {
        // ...
        val event = MyEvent(/*...*/)
        azureEventService.trackEvent(event)
        // ...
    }
    
}
```

## Event properties
The following properties contain the metadata that can be queried for observability purposes.
+ submittedReportIds
+ parentReportId
+ childReportId
+ pipelineStepName
+ error
+ submittedReportIds
+ parentReportId
+ childReportId
+ topic
+ blobUrl
+ pipelineStepName
+ timestamp
+ submittedItemIndex
+ parentItemIndex
+ childItemIndex
+ sender
+ trackingId
+ queueMessage

Additional Parameters
+ params.itemFormat
+ params.fileLength
+ params.requestParameters
+ params.senderIp
+ params.senderName
+ params.fileName
+ params.receiverName
+ params.transportType
+ params.processingError
+ params.validationProfile
+ params.enrichments[]
+ params.originalFormat
+ params.targetFormat
+ params.failingFilters
+ params.filterType
+ params.retryCount
+ params.nextRetryTime
+ params.queueMessage

`BundleDigestLabResult` (for test results)
+ observationSummaries []
+ patientState []
+ performerSummaries []
+ orderingFacilitySummaries []

`BundleDigestTestOrder` (for test orders [ETOR use case])
+ LabTestOrderedSummary


## Current custom events
| **Pipeline location** | **Event name** |
|---|---|
| Receive | REPORT_RECEIVED |
| Convert | REPORT_NOT_RECEIVABLE |
| Convert | REPORT_NOT_PROCESSABLE |
| Convert | ITEM_FAILED_VALIDATION |
| Convert | ITEM_ACCEPTED |
| Destination Filter | ITEM_NOT_ROUTED |
| Destination Filter | ITEM_ROUTED |
| Receiver Enrichment | ITEM_TRANSFORMED |
| Receiver Filter | ITEM_FILTER_FAILED |
| Translate | ITEM_TRANSFORMED |
| Batch | - |
| Send | ITEM_SENT |
| Send | REPORT_SENT |
| Send | ITEM_SEND_ATTEMPT_FAIL |
| Send | ITEM_LAST_MILE_FAILURE |
| Send | REPORT_LAST_MILE_FAILURE |

The PIPELINE_EXCEPTION event can be logged if any step within the pipeline fails or encounters a processing error.

This information is also captured in [LucidChart](https://lucidgov.app/lucidchart/89edc4c3-695b-4324-be8e-6556dd9fec95/edit?viewport_[…]I_OT3aNK&invitationId=inv_a44a9145-fc99-49ca-9053-a873937c7b23) (requires login), which also denotes what properties are being logged for each custom event.
To see where in the pipeline these events sequentially occur, please refer to another [LucidChart diagram](https://lucidgov.app/lucidchart/3820834e-a916-43d5-9575-0482c8e3f299/edit?viewport_loc=-608%2C-997%2C2973%2C1621%2CKYWo9OtDiKnx&invitationId=inv_db7796a6-a5ef-476b-b199-329581794844).

## How to query for events

Events that are pushed to Azure can be found in the `customEvents` table in the log explorer. The properties defined in
your event can be found under the `customDimensions` column. You can query for events using the 
[Kusto Query Language (KQL)](https://learn.microsoft.com/en-us/azure/data-explorer/kusto/query/).

```
// name comes from your event classname 
customEvents
| where name == "MyEvent"
| where customDimensions.importantString == "important"
```

## Common Queries in KQL

### General tips
- Use the `Time range` selector in the Azure query dialog to specify what time range
  you want your results from.

### Distinct senders
```
customEvents
| where name == "REPORT_RECEIVED"
| extend senderName = tostring(parse_json(tostring(customDimensions.params)).senderName)
| distinct senderName
```

### Get report count sent by sender
```
customEvents
| where name == "REPORT_RECEIVED"
| extend senderName = tostring(parse_json(tostring(customDimensions.params)).senderName)
| summarize count() by senderName 
| order by count_
```

### Get report count sent by topic
```
customEvents
| where name == "REPORT_RECEIVED"
| extend topic = tostring(customDimensions.topic)
| summarize count() by topic 
| order by count_
```

### Get reportable conditions count for all reports sent to Report Stream
```
customEvents
| where name == "ITEM_ACCEPTED"
| extend conditionDisplay = tostring(parse_json(tostring(parse_json(tostring(parse_json(tostring(parse_json(tostring(parse_json(tostring(customDimensions.params)).bundleDigest)).observationSummaries))[0].testSummary))[0].conditions))[0].display)
| summarize count() by conditionDisplay
| order by count_
```

### Distinct receivers
```
customEvents
| where name == "ITEM_ROUTED"
| extend receiverName = tostring(parse_json(tostring(customDimensions.params)).receiverName)
| distinct receiverName
```

### Get report count routed to a receiver
```
customEvents
| where name == "ITEM_ROUTED"
| extend receiverName = tostring(parse_json(tostring(customDimensions.params)).receiverName)
| summarize count() by receiverName 
| order by count_
```

### Get report count routed by topic
```
customEvents
| where name == "ITEM_ROUTED"
| extend topic = tostring(customDimensions.topic)
| summarize count() by topic 
| order by count_
```

### Get reportable conditions count for all reports routed to receivers
```
customEvents
| where name == "ITEM_ROUTED"
| extend conditionDisplay = tostring(parse_json(tostring(parse_json(tostring(parse_json(tostring(parse_json(tostring(parse_json(tostring(customDimensions.params)).bundleDigest)).observationSummaries))[0].testSummary))[0].conditions))[0].display)
| summarize count() by conditionDisplay
| order by count_
```

### List of items that were not routed where patient lived in state X
```kql
customEvents
| where name == "ITEM_NOT_ROUTED"
| extend params = parse_json(tostring(customDimensions.params))
| where params.bundleDigest.patientState contains "X"
```

### List of items that failed a filter where patient lived in state X
```kql
customEvents
| where name == "ITEM_FILTER_FAILED"
| extend params = parse_json(tostring(customDimensions.params))
| where params.bundleDigest.patientState contains "X"
```

### Find the original report ID for a report that failed a filter where patient lived in state X
```kql
customEvents
| where name == "REPORT_RECEIVED"
| extend reportId = tostring(customDimensions.childReportId)
| join ( 
    customEvents
    | where name == "ITEM_FILTER_FAILED"
    | extend params = parse_json(tostring(customDimensions.params))
    | where params.bundleDigest.patientState contains "X"
    | extend submittedReportIds = parse_json(tostring(customDimensions.submittedReportIds))
    | mv-expand submittedReportIds
    | project childReportId=tostring(submittedReportIds)) on $left.reportId == $right.childReportId
```
### Items not routed to States that we are connected to in the UP (but we think they should be)
```kql
let States = dynamic(["DC","CT", "NE","VA","MI","KY","WV","NC","GA","PA", "NJ","DE","OH","MS","NH","VT", "WA","IN","NM","PR","MH","TN","UT","WY","WI"]);
customEvents
| where name == "ITEM_NOT_ROUTED"
| extend params = parse_json(tostring(customDimensions.params))
| extend Ordering_Facility_State = parse_json(params.bundleDigest.orderingFacilityState)[0]
| extend Performer_State = parse_json(params.bundleDigest.performerState)[0]
| extend Patient_State = parse_json(params.bundleDigest.patientState)[0]
| where not(Ordering_Facility_State in (States) or Performer_State in (States) or Patient_State in (States))
| project timestamp, Sender = customDimensions.sender, Ordering_Facility_State, Patient_State, Performer_State, Topic = customDimensions.topic, TrackingID = customDimensions.trackingId, customDimensions
```
### Items that were routed by filtered out
```kql
customEvents
| where name == "ITEM_FILTER_FAILED"
| extend Receiver_Name = tostring(parse_json(tostring(customDimensions.params)).receiverName)
| extend Filter_Type = tostring(parse_json(tostring(customDimensions.params)).filterType)
| project timestamp, Receiver_Name, Filter_Type, customDimensions
```
### Errors that did not trigger an exception
```kql
customEvents
| where name == "REPORT_NOT_RECEIVABLE" or name == "REPORT_NOT_PROCESSABLE" or name == "REPORT_NOT_PROCESSABLE" or name == "ITEM_FAILED_VALIDATION" or name == "REPORT_LAST_MILE_FAILURE"
```
### Routed but not Sent, and did not trigger a Filter event (under construction, but functional)
```kql
customEvents
| where name == "ITEM_ROUTED"
| extend trackingId_ = tostring(customDimensions.trackingId)
| join kind=leftanti (
  customEvents 
  | where name == "ITEM_SENT" or name == "ITEM_FILTER_FAILED"
  | extend trackingId_ = tostring(customDimensions.trackingId)
  )
  on trackingId_
| extend Receiver_Name = tostring(parse_json(tostring(customDimensions.params)).receiverName)
| project timestamp, Receiver_Name, customDimensions
```
The four queries above were made into modules within the [UP Message Monitoring dashboard](https://portal.azure.com/#@cdc.onmicrosoft.com/dashboard/arm/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourcegroups/prime-data-hub-test/providers/microsoft.portal/dashboards/9a35cfea-cebd-4c9e-9a63-32c5d510d528) in Azure (requires CDC login, see DevOps for additional permissions).
