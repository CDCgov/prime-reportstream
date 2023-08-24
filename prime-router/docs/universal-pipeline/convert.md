# Universal Pipeline Convert Step
The convert step comes after the [receive step](receive.md). This step first converts an incoming message into the FHIR format. Once the message has been converted to FHIR, then it will apply any sender transforms to the resulting FHIR bundle.

## Conversion
The first and most significant aspect of the convert step is the conversion of a message into the FHIR format. The process of this conversion depends on the incoming message's format:

- If the incoming message is HL7v2, we use the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter) with our custom configurations from the [fhir_mapping folder](https://github.com/CDCgov/prime-reportstream/tree/master/prime-router/metadata/fhir_mapping/hl7) to handle the conversion. See the [HL7v2-to-FHIR configurations](#HL7v2-to-FHIR-configurations) section for more details.
- If the incoming message is FHIR, then there's nothing to be done, so we can continue onto sender transforms.
- Currently, no other input formats are supported in the Universal Pipeline, but to support new input types, they would need to be translated to FHIR by some other method.

### HL7v2-to-FHIR configurations
[section all about the mapping configurations in `fhir_mapping`, link to separate page if necessary]

## Sender Transforms
After the conversion to FHIR format is complete, we can now apply sender transforms. We use these transforms to make sender-specific adjustments to the resulting FHIR bundle, so we can think of this as a FHIR-to-FHIR transform. The class `FhirTransformer` is used to perform these kinds of transforms.

Note: When creating a transform, please reference [Changing/Updating Sender/Receiver Transforms](../getting-started/standard-operating-procedures/changing-transforms.md) for guidance.

### FhirTransformer configurations
[section all about configuration files and the different settings that are possible, link to separate page if necessary]
