# Common Queries in KQL

## General tips
- Use the `Time range` selector in the Azure query dialog to specify what time range
you want your results from.
- The value of `null` for a receiver in the `ReportRouteEvent` means that report was not routed to any receiver

### Which senders have sent a report
```
customEvents
| where name == "ReportAcceptedEvent"
| extend sender = tostring(customDimensions.sender)
| distinct sender
```

### Which receivers have had a report routed to them
```
customEvents
| where name == "ReportRouteEvent"
| extend receiver = tostring(customDimensions.receiver)
| where receiver != "null"
| distinct receiver
```

### Get report count sent by sender
```
customEvents
| where name == "ReportAcceptedEvent"
| extend sender = tostring(customDimensions.sender)
| summarize count() by sender 
| order by count_
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

### Get reportable conditions count for all reports sent to Report Stream
```
customEvents
| where name == "ReportAcceptedEvent"
| extend conditions = parse_json(tostring(customDimensions.conditions))
| mv-expand conditions
| extend conditionDisplay = tostring(conditions.display)
| summarize count() by conditionDisplay
| order by count_
```

### Get reportable conditions count for all reports routed to receivers
```
customEvents
| where name == "ReportRouteEvent"
| extend conditions = parse_json(tostring(customDimensions.conditions))
| mv-expand conditions
| extend conditionDisplay = tostring(conditions.display)
| summarize count() by conditionDisplay
| order by count_
```