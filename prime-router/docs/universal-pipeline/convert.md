# Universal Pipeline Convert Step
The convert step is executed when the [Azure AQS message](README.md#aqs-usage) referencing a received [Report](README.md#report-and-item), created by the [Receive](receive.md) step, is scheduled by Azure. For each [Item](README.md#report-and-item) contained in the received Report, the convert step will:

  1. Transform the item to the FHIR format
  2. Apply the configured [sender transform](#sender-transforms), if one is configured
  3. Generate a new Report
  4. Schedule the Route step for the new Report

## Conversion
The first and most significant aspect of the convert step is the conversion of a message into the FHIR format. The process of this conversion depends on the incoming message's format:

- If the incoming message is HL7v2, ReportStream uses the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter) with custom configurations from the [fhir_mapping folder](https://github.com/CDCgov/prime-reportstream/tree/master/prime-router/metadata/fhir_mapping/hl7) to handle the conversion. See the [HL7v2-to-FHIR configurations](../design/design/transformations.md#HL7v2-to-FHIR-configurations) section for more details.
- If the incoming message is FHIR, then there's nothing to be done
- Apply Sender transforms specified by Sender configuration 

> See [HL7v2-to-FHIR Transformations](../design/design/transformations.md#hl7v2-to-fhir-transformations) for more information regarding configuration of HL7 to FHIR transformations.

> Currently, no other input formats besides HL7v2 and FHIR are supported in the Universal Pipeline. In order to support new formats, they would need to be transformed to FHIR by some new method.

## Sender Transforms
After the conversion of an Item to the FHIR format, an additional transform can run on the resulting FHIR bundle. This transform is called a "Sender Transform" because it is used to make sender-specific adjustments to the FHIR bundle.

Sender Transforms are completely optional, but ReportStream has created a default sender transform that can be extended or used directly (`prime-router/metadata/fhir_transforms/senders/default-sender-transform.yml`). This is a good place to start to understand what kind of transforms a Sender may want performed on their messages.

> See [FHIR-to-FHIR Transformations](../design/design/transformations.md#fhir-to-fhir-transformations) for more information regarding configuration of Sender Transforms.

> When creating a transform, reference [Changing/Updating Sender/Receiver Transforms](../getting-started/standard-operating-procedures/changing-transforms.md) for guidance.
 
## Retries

There is no custom retry strategy for this step.  If an error occurs during this step, the message is re-queued up to five
times before being placed in the poison queue.