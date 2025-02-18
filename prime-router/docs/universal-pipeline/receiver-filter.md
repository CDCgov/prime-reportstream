# Universal Pipeline Receiver Filter Step

## Context

The Destination Filter function’s evaluates a receiver's filters on a bundle and determines if it should be sent. This
may include pruning of observations in the bundle. Each receiver connected with ReportStream has unique interests in 
the data that flows through the pipeline. This step is designed to find the data that meet those interests.

The function follows the [Receiver Enrichment](receiver-enrichment.md) function. These messages are passed to the FHIR Receiver Filter which first decodes a FHIR Bundle. Then, quality, processing, routing, and condition filters are 
evaluated to determine if the bundle should be sent and prune it of unneeded data. If the message passes, it is sent
to the [Translate](translate.md) function where receiver specific work is done to prepare for batching and sending.

### Pruning

During evaluation of the condition filters in the `Receiver Filter Function`, bundle observations that are not of
interest to the receiver are pruned, or removed. If there are no observations of interest to the receiver, the bundle
is not sent.

### FHIRPath for Routing

FHIRPath is used to build filter expressions. See FHIRPath documentation
in [fhir-functions.md](https://github.com/CDCgov/prime-reportstream/blob/d43ab6297a44a4ef2a0fef8d467e79cfcc154f33/prime-router/docs/getting-started/fhir-functions.md)

The table below demonstrates a few filter functions and their FHIRPath equivalent.


<table>
  <tr>
   <td><strong>Legacy Pipeline: filter functions</strong>
   </td>
   <td><strong>Universal Pipeline: FHIRPath Expressions</strong>
   </td>
  </tr>
  <tr>
   <td><code>hasValidDataFor(message_id)</code>
   </td>
   <td><code>Bundle.entry.resource.ofType(MessageHeader).id.exists().not()</code>
   </td>
  </tr>
  <tr>
   <td><code>hasValidDataFor(patient_last_name, patient_first_name)</code>
   </td>
   <td><code>%patientLastname.exists() and %patientFistname.exists()</code>
   </td>
  </tr>
  <tr>
   <td><code>hasAtLeastOneOf(patient_street,patient_zip_code,patient_phone_number,patient_email)</code>
   </td>
   <td><code>%patientStreet.exists() or %patientZipcode.exists() or %patientPhoneNumber.exists() or %patientEmail.exists()</code>
   </td>
  </tr>
  <tr>
   <td><code>isValidCLIA(testing_lab_clia,reporting_facility_clia)</code>
   </td>
   <td><code>%testingLabId.getIdType() = "CLIA" or %reportingFacilityId.getIdType() = 'CLIA'</code>
   </td>
  </tr>
  <tr>
   <td><code>allowAll()</code>
   </td>
   <td><code>true</code>
   </td>
  </tr>
  <tr>
   <td><code>allowNone()</code>
   </td>
   <td><code>false</code>
   </td>
  </tr>
  <tr>
   <td><code>orEquals(ordering_facility_state, CO, patient_state, CO)</code>
   </td>
   <td><code>%orderingFacilityState = "CO" or \
Bundle.entry.resource.ofType(Patient).address.state = "CO"</code>
   </td>
  </tr>
</table>

## Filtering

### Purpose

Filter configuration is a part of the settings for a specific organization and/or receiver. There are five main filter
groups*: Jurisdictional, Quality, Routing, Processing Mode Code, and Condition. The Quality, Routing, Processing Mode
Code, and Condition groups are evaluated during the `Receiver Filter Function`. These filter groups are used to organize
the filters and make it easier to report the filter results to a user, but the functionality is the same for all the
filters. All filters take an array of expressions where all must evaluate to true for the group to be true.

_*Filter groups may have been referred to as filter types in the past._

### Configuring Filters

Filters are configured in the settings stored in the database. See the relevant [document](../standard-operating-procedures/configuring-filters.md) for more info.

### Quality Filter

Filter out any data that does not meet the specified minimum requirements (e.g. must have patient last name)


<table>
  <tr>
   <td><strong>Topic</strong>
   </td>
   <td>Applies to following topics: <em>full-elr, etor-ti, elr-elims</em>
   </td>
  </tr>
  <tr>
   <td><strong>Operation</strong>
   </td>
   <td>Expressions are evaluated as an AND operation
   </td>
  </tr>
  <tr>
   <td><strong>Default</strong>
   </td>
   <td>See code block below
   </td>
  </tr>
</table>

```kotlin
/**
 * Default Rules:
 *   Must have message ID, patient last name, patient first name, DOB, specimen type
 *   At least one of patient street, patient zip code, patient phone number, patient email
 *   At least one of order test date, specimen collection date/time, test result date
 */
val qualityFilterDefault: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).id.exists()",
        "Bundle.entry.resource.ofType(Patient).name.family.exists()",
        "Bundle.entry.resource.ofType(Patient).name.given.count() > 0",
        "Bundle.entry.resource.ofType(Patient).birthDate.exists()",
        "Bundle.entry.resource.ofType(Specimen).type.exists()",
        "(Bundle.entry.resource.ofType(Patient).address.line.exists() or " +
            "Bundle.entry.resource.ofType(Patient).address.postalCode.exists() or " +
            "Bundle.entry.resource.ofType(Patient).telecom.exists())",
        "(" +
            "(Bundle.entry.resource.ofType(Specimen).collection.collectedPeriod.exists() or " +
            "Bundle.entry.resource.ofType(Specimen).collection.collected.exists()" +
            ") or " +
            "Bundle.entry.resource.ofType(ServiceRequest).occurrence.exists() or " +
            "Bundle.entry.resource.ofType(Observation).effective.exists())"
    )
```

### Routing Filter

Generic filtering that does not concern data quality or condition (e.g. test result is positive)


<table>
  <tr>
   <td><strong>Topic</strong>
   </td>
   <td>Applies to following topics: <em>full-elr, etor-ti, elr-elims</em>
   </td>
  </tr>
  <tr>
   <td><strong>Operation</strong>
   </td>
   <td>Expressions are evaluated with AND operation
   </td>
  </tr>
  <tr>
   <td><strong>Default</strong>
   </td>
   <td><strong>Allow All: </strong>The door is open. No filter is in place
   </td>
  </tr>
</table>

### Processing Mode Code Filter

The processing mode of the data indicates the sender’s intended context for the data. Options for this field are found
here [CodeSystem: processingId](https://terminology.hl7.org/5.2.0/CodeSystem-v2-0103.html). The intention is to ensure
the sender and receiver operate with the same data content context. Test data should only be accepted by test receivers.
Production data should only be accepted by production receivers.


<table>
  <tr>
   <td><strong>Topic</strong>
   </td>
   <td>Applies to following topics: <em>full-elr, etor-ti</em>
   </td>
  </tr>
  <tr>
   <td><strong>Operation</strong>
   </td>
   <td>Expressions are evaluated with AND operation
   </td>
  </tr>
  <tr>
   <td><strong>Default</strong>
   </td>
   <td>See code block below
   </td>
  </tr>
</table>

```kotlin
/**
 * Default Rule:
 *  Must have a processing mode id of 'P'
 */
val processingModeFilterDefault: ReportStreamFilter = listOf(
        "Bundle.entry.resource.ofType(MessageHeader).meta.tag.where(system = 'http://terminology.hl7.org/CodeSystem/v2-0103').code = 'P'"
    )
```

### Condition Filter

Filter data based on the test identifiers using FHIR expressions. A receiver expecting flu results should only accept
tests for flu. If the message contains multiple observations, some that pass the condition filter and others that do
not, the condition filter will pass if any bundle observations are of interest to the receiver.

<table>
  <tr>
   <td><strong>Topic</strong>
   </td>
   <td>Applies to following topics: <em>full-elr, etor-ti, elr-elims</em>
   </td>
  </tr>
  <tr>
   <td><strong>Operation</strong>
   </td>
   <td>Expressions are evaluated with OR operation
   </td>
  </tr>
  <tr>
   <td><strong>Default</strong>
   </td>
   <td><strong>Allow All:</strong> The door is open. No filter is in place
   </td>
  </tr>
</table>

### Mapped Condition Filter

Filter data based on the condition stamp added by ReportStream. A receiver expecting flu results should only accept
tests for flu. If the message contains multiple observations, some that pass others that do not, the mapped condition
filter will pass if the bundle contains any mapped conditions of interest to the receiver.

<table>
  <tr>
   <td><strong>Topic</strong>
   </td>
   <td>Applies to following topics: <em>full-elr, etor-ti, elr-elims</em>
   </td>
  </tr>
  <tr>
   <td><strong>Operation</strong>
   </td>
   <td>Expressions are evaluated with OR operation
   </td>
  </tr>
  <tr>
   <td><strong>Default</strong>
   </td>
   <td><strong>Allow All:</strong> The door is open. No filter is in place
   </td>
  </tr>
</table>

## Storage

The `Receiver Filter Function` retrieves messages from the `pdhprodstorageaccount` Azure Storage Account. Within that
storage account there is a blob container named `reports` containing folders for use by the Universal Pipeline. The
`Destination Filter Function` places all messages into the `receiver-filter` folder for retrieval by the
`Receiver Filter Function`. Those messages that match a receiver's filtering will then be placed in the `translate`
folder for future retrieval by the `Translate Function`. Messages within the `receiver-filter` folder are saved to
sub-folders equaling the name of the sender.

## Logging

### Action Logs

This filter will log actions when:
- Filtering observations due to any filter (incl: filter text, filter type, org details, index)

### Console Logging

This filter will log messages to the console when:
- First initialized
- Downloading the bundle (includes duration)
- Observations have been filtered from a bundle
- Receiver filters passed and a pruned bundle is ready
- Receiver filters failed and lineage will be terminated 

## Events

This step emits one of the below events _per receiver_ each time it runs.

| Event                                                                                   | Trigger                                                    |
|-----------------------------------------------------------------------------------------|------------------------------------------------------------|
| ITEM_FILTER_FAILED | When a report fails receiver filters                       |

## Retries

There is no custom retry strategy for this step. If an error occurs during this step, the message is re-queued up to
five times before being placed in the poison queue.