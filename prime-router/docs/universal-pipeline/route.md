# Universal Pipeline Route Step

## Context

The Route function’s purpose is to match FHIR bundles with receivers. Each receiver connected with ReportStream has unique interests in the data that flows through the pipeline. Routing is designed to find the data that meet those interests.

The Route function follows the [Convert](convert.md) function. At this point all data will be in FHIR format. These messages are passed to the FHIR Router which first decodes a FHIR Bundle. `FHIRRouter.applyFilters` does the work to find receivers that accept the bundle. With the list of acceptable receivers, FHIR Endpoints are added to the Provenance resource identifying those receivers.  An Endpoint resource describes the details of a receiver including which test results to include. With that information, the message is passed to the [Translate](translate.md) function where receiver specific work is done.

### Topic

A Topic must be set for all senders and receivers. The choice of topic determines which pipeline is used (Universal or Legacy) and will affect how routing takes place. The routing step will start by limiting available receivers to only those with a topic matching the sender topic. Topics include:


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

FHIRPath is used to build filter expressions. See FHIRPath documentation in [fhir-functions.md](https://github.com/CDCgov/prime-reportstream/blob/d43ab6297a44a4ef2a0fef8d467e79cfcc154f33/prime-router/docs/getting-started/fhir-functions.md)

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
   <td><code>%messageId.exists().not()</code>
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
%patientState = "CO"</code>
   </td>
  </tr>
</table>



## Filter Types

### Purpose

Routing configuration is a part of the settings for a specific organization and/or receiver. There are five main filter groups*: Jurisdictional, Quality, Routing, Processing Mode Code, and Condition. These filter groups are used to organize the filters and make it easier to report the filter results to a user, but the functionality is the same for all the filters. All filters can take an array of expressions where all expressions must evaluate to true (an AND operation) or at least one must evaluate to true (an OR operation) for the filter group to evaluate to true. All filters can be set by defaults (set in code).

_*Filter groups may have been referred to as filter types in the past._


### **Jurisdictional Filter**

Identifies data that falls within a receiver’s jurisdiction as most of our organizations are geographic entities (e.g., patient state is CO).  Note that the non-matching result of the jurisdictional filter is not reported to users via the submission history API as this filter is just to decide to which receiver data needs to go.


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

The processing mode of the data indicates the sender’s intended context for the data. Options for this field are found here [CodeSystem: processingId](https://terminology.hl7.org/5.2.0/CodeSystem-v2-0103.html). The intention is to ensure the sender and receiver operate with the same data content context. Test data should only be accepted by test receivers. Production data should only be accepted by production receivers.


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
    "%processingId = 'P'"
)
```

### Condition Filter

Filter data based on the test identifiers. A receiver expecting flu results should only accept tests for flu. If the message contains multiple observations, some that pass the condition filter and others that do not, the condition filter will be used to identify the desired observations. Identifiers for the needed observations are added to the Endpoint which is then added to the Provenance resource.

*The Translate step will review the Endpoints and remove any observations that are not identified.


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

The `Route Function` retrieves messages from the `pdhprodstorageaccount` Azure Storage Account. Within that storage account there is a blob container named `reports` containing folders for use by the Universal Pipeline. The `Convert Function` places all messages into the route folder for retrieval by the `Route Function`. Those messages that match a receiver's filtering will then be placed in the `translate` folder for future retrieval by the `Translate Function`. Messages within the `route` folder are saved to sub-folders equaling the name of the sender.


## Filter Reversal

This is a NOT operation on the result of the filters set in the Quality Filter. The primary use is to set the filters for a secondary receiver to ingest all data not accepted by a primary receiver. This may be helpful to keep the qualityFilter setting the same for both the primary and secondary and make it easier to read the configuration.


## Logging


### Purpose

Filtering logic can be extensive and complex. Recording the outcome of the filters provides internal and external users an important view of events. Logging is particularly important when reports do not pass filtering.

* **Jurisdictional Filter**
    * results of this filter are **not** logged. Given that most items are only meant for one or maybe a couple jurisdictions out of hundreds, there is little value in logging here.
* **Other Filters**(Quality, Routing, Processing Mode Code, and Condition)
    * Following Jurisdictional filtering, all other filter groups use `evaluateFilterAndLogResult()`. Upon failure of a filter in a filter group, the outcome is logged to the Action Log table. See Action Log table in [ReportStream Data Model](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/design/design/data-model.md#action_log-table)
  
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
    * Results of code exceptions on all filters(including jurisdictional) are logged as warnings in the Action Log table:

```kotlin
catch (e: SchemaException) {
    actionLogger?.warn(EvaluateFilterConditionErrorMessage(e.message))
    exceptionFilters += filterElement
}
```

Various scenarios for filter logging are outlined in the [filtering design](https://github.com/CDCgov/prime-reportstream/blob/master/prime-router/docs/design/features/0001-universal-pipeline-filter-reporting.md#filter-log-scenarios).

## Configuring Filters


### Frontend User Interface

The admin user interface at[ https://reportstream.cdc.gov/](https://reportstream.cdc.gov/) allows a PRIME admin to manage the settings of an organization, sender and/or receiver.  Filters are configured as free text and the input text must conform to the expected syntax.

### Command Line Interface

All filters for receivers and organizations can be created/updated/deleted via the command line.



1. create a .yml file containing the updated FHIRPath expressions. Ensure the file begins with “---”. Example:


```yaml
---
- name: yoyodyne
  description: Yoyodyne Propulsion Laboratories, the Future Starts Tomorrow!
  jurisdiction: FEDERAL
  receivers:
    - name: ELR
      externalName: yoyodyne ELR
      organizationName: yoyodyne
      topic: full-elr
      customerStatus: active
      jurisdictionalFilter: [ "(%performerState.exists() and %performerState = 'CA')]
```

2. Use the following commands to load the information from the .yml files into the staging database. First obtain a login token for staging

`./prime login –env staging`

Next update the staging DB

`./prime multiple-settings set –env staging –input <file-location>`