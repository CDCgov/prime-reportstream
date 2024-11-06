# Emitting Events to AppInsights

## Background
An event is something that an application accomplishes that is significant at the system level whereas a 
log is only locally significant. For Report Stream, we want to emit events where we successfully accomplish
an action that we want to observe through visualizations.

[Technical documentation/overview of ReportStream Azure Event logging](https://lucidgov.app/lucidchart/b2251f9d-ec5f-4186-9a87-cd2443ab0000/edit?viewport_loc=-1079%2C-2347%2C10754%2C4258%2C0_0&invitationId=inv_a428973e-8d90-429d-b61f-9305464f48f9)

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
### Items not routed to States that we are connected to in the UP
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
