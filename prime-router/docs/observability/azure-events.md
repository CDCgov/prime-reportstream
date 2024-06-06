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

Under the hood, it will serialize your event class and push the event
to the configured Microsoft AppInsights instance.

## Event Glossery

### ReportAcceptedEvent
This event is emitted during the route step _before_ running any receiver specific filtering
- reportId
  - The ID assigned to the report
- topic
  - The Topic of the report
- sender
  - The full sender name
- observations
  - A list of observations each containing a list of its mapped conditions
- bundleSize
  - Length of the bundle JSON string
- messageId
  - From the bundle.identifier value and system. If ingested as HL7 this comes from MSH-10

### ReportRouteEvent
This event is emitted during the route step _after_ running any receiver specific filtering. 
Many `ReportRouteEvent` can correspond to a `ReportAcceptedEvent` and can be "joined" on 
`ReportAcceptedEvent.reportId == ReportRouteEvent.parentReportId`.

- parentReportId
  - The ID assigned to parent report. This ID can be used to find the corresponding `ReportAcceptedEvent`.
- reportId
  - The ID assigned to the report
- topic
  - The Topic of the report
- sender
  - The full sender name
- receiver
  - The full receiver name. When a report does not get routed to a receiver this value will be `"null"`.
- observations
  - A list of observations each containing a list of its mapped conditions 
- bundleSize
  - Length of the bundle JSON string
- messageId
    - From the bundle.identifier value and system. If ingested as HL7 this comes from MSH-10

### ReportCreatedEvent
- reportId
    - The ID assigned to the report
- topic
    - The Topic of the report

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
| where name == "ReportAcceptedEvent"
| extend sender = tostring(customDimensions.sender)
| distinct sender
```

### Get report count sent by sender
```
customEvents
| where name == "ReportAcceptedEvent"
| extend sender = tostring(customDimensions.sender)
| summarize count() by sender 
| order by count_
```

### Get report count sent by topic
```
customEvents
| where name == "ReportAcceptedEvent"
| extend topic = tostring(customDimensions.topic)
| summarize count() by topic 
| order by count_
```

### Get reportable conditions count for all reports sent to Report Stream
```
customEvents
| where name == "ReportAcceptedEvent"
| extend observations = parse_json(tostring(customDimensions.observations))
| mv-expand observations
| extend conditions = parse_json(tostring(observations.conditions))
| mv-expand conditions
| extend conditionDisplay = tostring(conditions.display)
| summarize count() by conditionDisplay
| order by count_
```

### Distinct receivers
```
customEvents
| where name == "ReportRouteEvent"
| extend receiver = tostring(customDimensions.receiver)
| where receiver != "null"
| distinct receiver
```

### Get report count routed to a receiver
```
customEvents
| where name == "ReportRouteEvent"
| extend receiver = tostring(customDimensions.receiver)
| where receiver != "null"
| summarize count() by receiver 
| order by count_
```

### Get report count routed by topic
```
customEvents
| where name == "ReportRouteEvent"
| extend topic = tostring(customDimensions.topic), receiver = tostring(customDimensions.receiver)
| where receiver != "null"
| summarize count() by topic 
| order by count_
```

### Get reportable conditions count for all reports routed to receivers
```
customEvents
| where name == "ReportRouteEvent"
| extend observations = parse_json(tostring(customDimensions.observations)), receiver = tostring(customDimensions.receiver)
| where receiver != "null"
| mv-expand observations
| extend conditions = parse_json(tostring(observations.conditions))
| mv-expand conditions
| extend conditionDisplay = tostring(conditions.display)
| summarize count() by conditionDisplay
| order by count_
```
