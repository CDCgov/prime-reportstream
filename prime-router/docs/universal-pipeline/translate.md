# Universal Pipeline Translate Step
After the [route step](route.md) has determined the receivers for a message, the translate step will determine based on each receiver’s translation settings what format the message should be sent in. If the receiver wants HL7v2, the UP can translate the message using the default receiver transforms or use a custom receiver transform. If the receiver wants FHIR, since the message at this point is already in the FHIR format, we can either send it to them as it is, or we can optionally apply a `FhirTransformer`, similarly to what is used for sender-specific changes in the [convert step](convert.md), but this time as a receiver transform.

## Configuration

The configuration for this step is done on an individual receiver level and there are a variety of settings that can be
applied to change the resulting output report that is passed along to the batch step

### Receiver Settings

- `format`: what format the receiver wants, either HL7 or FHIR
- `schemaName`: the name of the schema to apply to the input FHIR bundle, the schema will either map FHIR -> FHIR or FHIR -> HL7
- `customerStatus`: only applies to HL7 receivers; this will value `MSH-11-1` with `T` when present
- `translation` only applies when converting from FHIR to HL7
  - `useTestProcessingMode`: this will value `MSH-11-1` with `T` when present
  - `convertPositiveDateTimeOffsetToNegative`: when true and a date contains `+0000` as the offset covert it to `-0000` 
  - `useHighPrecisionHeaderDateTimeFormat`: covert all dates to `yyyyMMddHHmmss.SSSSxx`
  - `truncationConfig`: configures how truncation will be performed for HL7 fields
    - `truncateHDNamespaceIds`: special handler for specifically truncating HD datatypes
    - `truncateHl7Fields`: HL7 fields that should be truncated to the maximum length in the spec
    - `customLengthHl7Fields`: HL7 fields that should get truncated to a length specified by the receiver

### Transforms

The `schemaName` setting specifies transforms that will be applied to the FHIR bundle before being sent to the batch
step and either transform a FHIR bundle or convert a FHIR bundle into HL7; this is dependent on the format the receiver
has set.

The two kinds of transforms work the same at a high level. The schema enumerates a series of element that
- contains a FHIR path to the resource that needs to be transformed
- a condition specifying whether the resource should be transformed
- how the resource should get transformed

The primary difference between the FHIR and HL7 schemas is that the HL7 converter has special handling for converting
a FHIR resource into an HL7 segment or component.

For more details, consult the [transform design doc](../design/design/transformations.md).

#### Custom FHIR functions

In order to support custom business logic, the transform code is extensible and allows for creating custom functions
that can then be used while defining schema elements.  One example of this is converting a date timestamp to a numerical age.

See [CustomFHIRFunctions.kt]((https://github.com/CDCgov/prime-reportstream/blob/acbaddc2d6a3f7da06ee99ead34c6ee4f05e9572/prime-router/src/main/kotlin/fhirengine/translation/hl7/utils/CustomFHIRFunctions.kt#L22)) for more examples

#### Extending schemas

In order to support receivers that would like small customizations to the bundles or HL7 messages they receive, schemas
can be extended and overrides applied without having to duplicate a whole schema.  A current example of this is that 
California specifies receiving HL7 ORU_R01 messages that mostly match the conversion specified by the base spec, but has the
datetimes changed to be PST.

#### De-identification

There is limited support for de-identification in the universal pipeline by applying the RADx_MARS-base-deidentified.yml
schema for a receiver, which performs de-identification according to the RADx_MARS spec.

## How it works

This step is triggered when a message is placed on the `translate` message queue.  The FHIR message is downloaded and then
all of the `Endpoint` resources in the `Provenance` resource are parsed to determine which receivers the message is intended for.  For each receiver,
the bundle is pruned and then converted into a FHIR bundle ready for the receiver or HL7, using the configured `schemaName`.
That report is then uploaded to azure and a batch task is inserted into the database

### Condition Pruning

As part of generating either FHIR or HL7 per receiver, the bundle is pruned so that only the observations that a receiver
is interested in is included.  This is accomplished via the `Endpoint` resource for that receiver which indicates which `Observations`
that receiver should receive.  See the example below for more details.

### Retries

There is no custom retry strategy for this step.  If an error occurs during this step, the message is re-queued up to five
times before being placed in the poison queue.

## Code entry points

- [FHIRTranslator](https://github.com/CDCgov/prime-reportstream/blob/acbaddc2d6a3f7da06ee99ead34c6ee4f05e9572/prime-router/src/main/kotlin/fhirengine/engine/FHIRTranslator.kt#L46)
- [Translate Azure Function](https://github.com/CDCgov/prime-reportstream/blob/acbaddc2d6a3f7da06ee99ead34c6ee4f05e9572/prime-router/src/main/kotlin/fhirengine/azure/FHIRFunctions.kt#L109)
- [Custom Translation Functions](https://github.com/CDCgov/prime-reportstream/blob/acbaddc2d6a3f7da06ee99ead34c6ee4f05e9572/prime-router/src/main/kotlin/fhirengine/engine/CustomTranslationFunctions.kt#L14)
- [Custom FHIR Functions](https://github.com/CDCgov/prime-reportstream/blob/acbaddc2d6a3f7da06ee99ead34c6ee4f05e9572/prime-router/src/main/kotlin/fhirengine/translation/hl7/utils/CustomFHIRFunctions.kt#L22)
- [Adding receivers in the route step](https://github.com/CDCgov/prime-reportstream/blob/acbaddc2d6a3f7da06ee99ead34c6ee4f05e9572/prime-router/src/main/kotlin/fhirengine/utils/FHIRBundleHelpers.kt#L35)

## Examples

### A report is intended for two receivers who will each receive different observations

If a sample FHIR bundle being processed contained the following, with one observation being for flu and one observation
being for covid and one receiver that is interested in flu results and one interested in covid results:

```fhir
{
  "fullUrl": "Endpoint/1666038441953190000.245100f2-cee6-4c1b-9f7c-9635ab87fe74",
  "resource": {
    "resourceType": "Endpoint",
    "id": "1666038441953190000.245100f2-cee6-4c1b-9f7c-9635ab87fe74",
    "identifier": [
      {
        "system": "https://reportstream.cdc.gov/prime-router",
        "value": "co-phd.flu"
      }
    ],
    "status": "active",
    "name": "elr",
    "extension": [
      {
        "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/reportable-condition",
        "valueReference": {
          "reference": "Observation/1671741861219479500.1e349936-127c-4edc-8d77-39fb231f4391"
        }
      }
    ]
  }
},
{
  "fullUrl": "Endpoint/1666038441953190000.245100f2-cee6-4c1b-9f7c-9635ab87fe74",
  "resource": {
    "resourceType": "Endpoint",
    "id": "1666038441953190000.245100f2-cee6-4c1b-9f7c-9635ab87fe74",
    "identifier": [
      {
        "system": "https://reportstream.cdc.gov/prime-router",
        "value": "co-phd.covid"
      }
    ],
    "status": "active",
    "name": "elr-secondary",
    "extension": [
      {
        "url": "https://reportstream.cdc.gov/fhir/StructureDefinition/reportable-condition",
        "valueReference": {
          "reference": "Observation/1667861767830636000.7db38d22-b713-49fc-abfa-2edba9c12347"
        }
      }
    ]
  }
},
```

In this example, the bundle was routed to both receivers as it contained a observation that they were configured to receive,
so it passed the condition filter.  At the end of the route step, an `Endpoint` resource is added to the bundle with references
to the observations that should be kept when the translation step happens.

### A bundle is meant to go to multiple receivers, but one receiver is misconfigured

The bundle contains two different endpoints, but the second receiver is misconfigured and when the translate step attempts
to generate an HL7 message an exception is thrown.  In this case, neither receiver will have a batch task inserted 
and the message will be re-queued before ultimately being placed in the poison queue.  At that point, the receiver can be
fixed and the message can be moved back into the translate message queue for processing.
