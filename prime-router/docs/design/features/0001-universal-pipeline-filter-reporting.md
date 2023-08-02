# Summary

When a report satisfies a receiver’s jurisdictional filter requirements, the report still needs to satisfy other filters
before it actually gets sent to that receiver. If any of those filters return negative results, logs summarizing the
filtered report are saved to the submission history with a description of which filters resulted in the filtered items.
Because there are a few different situations in which a report would be filtered out at this step, this document intends
to summarize what information is included in the filtered report logs, and why that information is included.

# Details

## Filter Types

The basic structure of a filter is a list of strings which should evaluate to true or false. In order to “pass” a
filter, each of those predicates in the list must evaluate to true.

### Jurisdictional Filter

This filter is typically used to restrict routing by jurisdiction, for instance by the patient's home state or county.
Since the vast majority of reports are expected to be filtered out by jurisdictional filters, these are not recorded in
the filtered report logs at all. The default behavior for jurisdictional filters is to allow none, so if a receiver does
not have jurisdictional filters set, then the default response is false, and the reports are filtered out without being
logged.

### Quality Filter

Quality filters tend to be used to require the data to meet some set of criteria for usefulness before it is received.
When a quality filter filters out items, it is recorded in the filtered report logs. There is a default quality filter,
so if a receiver does not have quality filters set, the following filters will be used by default:

```kotlin
/**
 * Default Rules:
 *   Must have message ID, patient last name, patient first name, DOB, specimen type
 *   At least one of patient street, patient zip code, patient phone number, patient email
 *   At least one of order test date, specimen collection date/time, test result date
 */
val qualityFilterDefault: ReportStreamFilter = listOf(
        "%messageId.exists()",
        "%patient.name.family.exists()",
        "%patient.name.given.count() > 0",
        "%patient.birthDate.exists()",
        "%specimen.type.exists()",
        "(%patient.address.line.exists() or " +
            "%patient.address.postalCode.exists() or " +
            "%patient.telecom.exists())",
        "(" +
            "(%specimen.collection.collectedPeriod.exists() or " +
            "%specimen.collection.collected.exists()" +
            ") or " +
            "%serviceRequest.occurrence.exists() or " +
            "%observation.effective.exists())"
    )
```

Additionally, quality filters can be reversed using the setting `reverseTheQualityFilter` on a receiver. When that
property is set to `true`, anything that would normally pass the quality filter will be filtered out, and anything that
would normally be filtered by the quality filter will be allowed through. In practice, this is usually used when an
organization has two receivers for the jurisdiction; they can have one receiver use a quality filter to receive their
"good" data, and they can set up a secondary receiver with the same quality filter reversed to receive everything else.

### Routing Filter

Routing filters are used for any kind of routing filtering that doesn't necessarily fit well into the other categories.
When a routing filter filters out items, it is recorded in the filtered report logs. The default behavior for the
routing filter is to allow all, so if a receiver does not have routing filters set, then all reports at this stage will
move on to the next filter without being logged.

### Processing Mode Filter

Processing mode filters typically restrict received data to be either test or production data. When a processing mode
filter filters out items, it is recorded in the filtered report logs. There is a default processing mode filter, so if a
receiver does not have processing mode filters set, the following filters will be used by default:

```kotlin
   /**
 * Default Rule:
 *  Must have a processing mode id of 'P'
 */
val processingModeFilterDefault: ReportStreamFilter = listOf(
        "%processingId = 'P'"
    )
```

### Condition Filter

Condition filters are used to allow receivers to only receive results for particular medical conditions. When a
condition filter filters out items, it is recorded in the filtered report logs. The default behavior for the condition
filter is to allow all, so if a receiver does not have condition filters set, then all reports at this stage will be
allowed through without being logged. Additionally, if there are no observations in a report, no condition filters will
be applied to that report, and it will be allowed through, so if the receiver does not want to receive reports without
observations, those should be filtered by one of the other filters above.

### Review

| Filter Type     | Negative Results Logged? | Has Default Filter? | Default Response | Reversible? |
|-----------------|--------------------------|---------------------|------------------|-------------|
| Jurisdictional  | No                       | No                  | allowNone        | No          |
| Quality         | Yes                      | Yes                 | N/A              | Yes         |
| Routing         | Yes                      | No                  | allowAll         | No          |
| Processing Mode | Yes                      | Yes                 | N/A              | No          |
| Condition       | Yes                      | No                  | allowAll         | No          |

## Filter Log Scenarios

As mentioned, only “failing” filters are logged, and only for non-jurisdictional filters. However, there are a few ways
that filters can fail, and the way they are logged varies slightly.

### Simply Filtered Out

In the most basic case, one or more of the predicates within a filter evaluates to false and the report is filtered out.
If the filter is [A, B, C, D, E], which can be thought of as `A ∧ B ∧ C ∧ D ∧ E`, any predicate of the filter
evaluating to false would result in an overall negative result, so all predicates evaluating to false are logged, but
those evaluating to true are less relevant/actionable in the logs. So in that case of the filter [A, B, C, D, E], if B,
D, and E all evaluate to false, the logged message might look something like this:

`For someOrg.someReceiver, filter [B, D, E][] filtered out item someItemId`

### Filtered Out w/ Default Filter

If a report is filtered out by application of a default filter, the logged message will include the text “(default
filter)”. So for instance if the default filter was [A, B, C, D, E], and B, D, and E all evaluate to false, the message
might look like this:

`For someOrg.someReceiver, filter (default filter) [B, D, E][] filtered out item someItemId`

The extra tag is intended to give some indication of where the filter came from if none existed on the receiver. Note
that in cases where a receiver has specified filters that happen to be equivalent to the default filter, it will not be
marked as the default filter in the message.

### Schema Exception

If the evaluation of a filter leads to an exception, the exception message will be added to the action log so that it
can be resolved, but the filter result will still also be logged. The logged message will include the text “(exception
found)”. So for instance, if the filter was [A, B, C, D, E] and A and C result in exceptions, the message might look
like this:

`For someOrg.someReceiver, filter (exception found) [A, C][] filtered out item someItemId`

We would never expect to have exceptions in evaluation of default filters, but if we did, the resulting message would
look like this:

`For someOrg.someReceiver, filter (default filter) (exception found) [A, C][] filtered out item someItemId`

### Filtered Out w/ Reversed Filter

If a report is filtered out due to a quality filter along with a setting of `reverseTheQualityFilter: true`, the logged
message will include the text “(reversed)”. Imagine a reversed filter of [A, B, C, D, E], which can be thought of as or
equivalently . The only way for this to yield a negative result is if each and every predicate A, B, C, D, and E
evaluate to true; therefore each of those predicates is relevant in logging why the filter yielded a negative result. So
while in non-reversed cases, we only include the individual predicates that evaluated to false, in reversed cases, we
include all predicates, and the resulting message might look like this:

`For someOrg.someReceiver, filter (reversed) [A, B, C, D, E][] filtered out item someItemId`

As it is today, the only filters that can be reversed are quality filters. If the default quality filter is reversed,
the resulting message might look like this:

`For someOrg.someReceiver, filter (default filter) (reversed) [A, B, C, D, E][] filtered out item someItemId`

### Filtered Out w/ Default Response

Currently only jurisdictional filters have default results that would filter out reports, and jurisdictional filter
results are not logged. If that were to change, we would have to log that we filtered out an item, but without any
filter to reference, so this case might have an entirely different format, or the message might look like:

`For someOrg.someReceiver, filter default response[] filtered out item someItemId`

## Future Considerations

Ticket [#8837](https://github.com/CDCgov/prime-reportstream/issues/8837)
involves changing the way that condition filters are evaluated so that instead of evaluating condition
filters with the usual `A ∧ B ∧ C ∧ D ∧ E`, we would instead use `A ∨ B ∨ C ∨ D ∨ E`. We’ll want to make sure during
that process that the logging behavior for condition filters still makes sense and that the behavior for the other
filter types is maintained.

## Relevant Tickets

[#8915](https://github.com/CDCgov/prime-reportstream/issues/8915)
