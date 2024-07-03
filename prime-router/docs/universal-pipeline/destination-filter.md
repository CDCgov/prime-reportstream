# Universal Pipeline Destination Filter Step

## Context

The Destination Filter function’s purpose is to match FHIR bundles with receivers. Each receiver connected with 
ReportStream has unique interests in the data that flows through the pipeline. Destination filtering is designed to find
the data that meet those interests.

The function follows the [Convert](convert.md) function. At this point all data will be in FHIR format. These messages
are passed to the FHIR Destination Filter which first decodes a FHIR Bundle. It then evaluates jurisdiction filters for
all active receivers with a matching topic to find receivers that could accept the bundle. The message is then passed to
the [Receiver Filter](receiver-filter.md) function to evaluate remaining receiver-specific filters.

### Topic

A Topic must be set for all senders and receivers. The choice of topic determines which pipeline is used (Universal or
Legacy) and will affect how routing takes place. The routing step will start by limiting available receivers to only
those with a topic matching the sender topic. Topics include:


<table>
  <tr>
   <td><strong>Topic Name</strong>
   </td>
   <td><strong>Pipeline</strong>
   </td>
   <td><strong>Description</strong>
   </td>
  </tr>
  <tr>
   <td>full-elr
   </td>
   <td>Universal
   </td>
   <td>General use for test result reporting
   </td>
  </tr>
  <tr>
   <td>etor-ti
   </td>
   <td>Universal
   </td>
   <td>ETOR project with Flexion’s Trusted Intermediary
   </td>
  </tr>
  <tr>
   <td>elr-elims
   </td>
   <td>Universal
   </td>
   <td>ELR project with ELIMS
   </td>
  </tr>
</table>

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
groups*: Jurisdictional, Quality, Routing, Processing Mode Code, and Condition. Only the Jurisdictional group is
evaluated during the `Destination Filter Function`. The filter groups are used to organize the filters and make it
easier to report the filter results to a user, but the functionality is the same for all the filters. All filters take
an array of expressions where all expressions must evaluate to true for the filter group to evaluate to true.

_*Filter groups may have been referred to as filter types in the past._


### Configuring Filters

Filters are configured in the settings stored in the database. See the relevant [document](../standard-operating-procedures/configuring-filters.md) for more info.

### Jurisdictional Filter

Identifies data that falls within a receiver’s jurisdiction as most of our organizations are geographic entities (e.g.,
patient state is CO). Note that the non-matching result of the jurisdictional filter is not reported to users via the
submission history API as this filter is just to decide to which receiver data needs to go.


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
   <td><strong>Allow None</strong>: Allowing none is a safeguard as jurisdictional filtering keeps data from one jurisdiction being reported to another. This must be overwritten with a custom filter for each receiver
   </td>
  </tr>
</table>

## Storage

The `Destination Filter Function` retrieves messages from the `pdhprodstorageaccount` Azure Storage Account. Within that
storage account there is a blob container named `reports` containing folders for use by the Universal Pipeline.
The `Convert Function` places all messages into the `destination-filter` folder for retrieval by the
`Destination Filter Function`. Those messages that match an active receiver's topic and jurisdiction ill then be placed
in the `receiver-filter` folder for future retrieval by the `Receiver Filter Function`. Messages within the
`receiver-filter` folder are saved to sub-folders equaling the name of the sender.

## Logging

### Purpose

Filtering logic can be extensive and complex. Recording the outcome of the filters provides internal and external users
an important view of events. Logging is particularly important when reports do not pass filtering.

* **Jurisdictional Filter**
    * results of this filter are **not** logged. Given that most items are only meant for one or maybe a couple
      jurisdictions out of hundreds, there is little value in logging here.
* **Other Filters**(Quality, Routing, Processing Mode Code, and Condition)
    * Following Jurisdictional filtering, all other filter groups use `evaluateFilterAndLogResult()`. Upon failure of a
      filter in a filter group, the outcome is logged to the Action Log table. See Action Log table
      in [ReportStream Data Model](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/design/design/data-model.md#action_log-table)

```kotlin
if (!passes) {
    val filterToLog = "${
        if (isDefaultFilter(filterType, filters)) "(default filter) "
        else ""
    }${failingFilterName ?: "unknown"}"
    logFilterResults(filterToLog, bundle, report, receiver, filterType, focusResource)
}
```

* **Code Exceptions**
    * Results of code exceptions on all filters(including jurisdictional) are logged as warnings in the Action Log
      table:

```kotlin
catch(e: SchemaException) {
    actionLogger?.warn(EvaluateFilterConditionErrorMessage(e.message))
    exceptionFilters += filterElement
}
```

Various scenarios for filter logging are outlined in
the [filtering design](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/design/features/0001-universal-pipeline-filter-reporting.md#filter-log-scenarios).

## Events

## Retries

There is no custom retry strategy for this step. If an error occurs during this step, the message is re-queued up to
five times before being placed in the poison queue.
