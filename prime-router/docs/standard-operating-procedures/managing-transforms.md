# Managing Transforms in ReportStream

This document describes guidelines to follow when working through adding transforms to senders and receivers.

### Background

Currently, there are a few places where transforms can happen in the universal Pipeline.

* FHIR - FHIR sender transforms
* FHIR - FHIR receiver transforms
* FHIR - HL7 receiver transforms

### Sender Transform Guidelines

In general Sender Transforms should be used when we need to:
* Add ReportStream Metadata. 
  * An example is the [original-pipeline-sender-transforms.yml](../../src/main/resources/metadata/fhir_transforms/senders/original-pipeline-transforms.yml)
  * The original-pipeline-sender-transform.yml contains transforms that were supported by our legacy covid pipeline
* Sender is missing data to generate HL7 v2 complaint messages
  * An examples is the [simple-report-sender-transform.yml](../../src/main/resources/metadata/fhir_transforms/senders/SimpleReport/simple-report-sender-transform.yml)
    * This file mostly contains FHIR extensions that are needed so that RS can generate an HL7 v2 message

Whenever adding sender transforms keep in mind that every receiver getting data from this sender will be affected.

### Receiver Transforms

Receiver transforms should be used to meet specific receiver needs and follow their Implementation Guides

Transforms that are very common to receivers are:
* Suppressing fields
* Converting AOE Observations into Notes
  * Most STLTs can't process AOE questions as observations and require them to be suppressed or turned into NTE segments
* Supporting Receiver specific value sets for Race and Ethnicity

Examples for receiver transforms can be found here * An example of this can be found inside the [NV-receiver-transform.yml](../../src/main/resources/metadata/hl7_mapping/receivers/STLTs/NV/NV-receiver-transform.yml)

When adding receiver transforms keep in mind that the all messages being routed to this receiver will be affected regardless of who the sender is.