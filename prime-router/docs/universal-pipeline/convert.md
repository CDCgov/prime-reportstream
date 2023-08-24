# Universal Pipeline Convert Step
The convert step is executed when the [Azure AQS message](README.md#aqs-usage) referencing a received Report, created by the [Receive](receive.md) step, is scheduled by Azure. For each [Item](README.md#report-and-item) contained in the received [Report](README.md#report-and-item), the convert step will:

  1. Translate the item to the FHIR format
  2. Apply the configured [sender transform](#sender-transforms), if one is configured
  3. Generate a new Report
  4. Schedule the Route step for the new Report

## Conversion
The first and most significant aspect of the convert step is the conversion of a message into the FHIR format. The process of this conversion depends on the incoming message's format:

- If the incoming message is HL7v2, ReportStream uses the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter) with custom configurations from the [fhir_mapping folder](https://github.com/CDCgov/prime-reportstream/tree/master/prime-router/metadata/fhir_mapping/hl7) to handle the conversion. See the [HL7v2-to-FHIR configurations](#HL7v2-to-FHIR-configurations) section for more details.
- If the incoming message is FHIR, then there's nothing to be done, so we can continue onto sender transforms.
- Currently, no other input formats are supported in the Universal Pipeline, but to support new input types, they would need to be translated to FHIR by some other method.

### HL7v2-to-FHIR configurations

> Prior to understanding ReportStream's configurations for the LinuxForHealth library and its use of it, it is important to understand how the library itself works and is architected. The root [README](https://github.com/LinuxForHealth/hl7v2-fhir-converter#readme) in the library's repo contains links that cover this subject in great detail; be sure to read all of it!

At its core, the converter library uses the [HL7 HAPI library](https://hapifhir.github.io/hapi-hl7v2/) to translate HL7v2 to FHIR based on [HL7 Message Templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md#hl7-message-template-structure) which reference [FHIR Resource Templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md#structure-of-a-fhir-resource-template). 

ReportStream's templates (often referred to as "mappings") for the LinuxForHealth HL7 to FHIR converter library can be found in the repo under `prime-router/metadata/fhir_mapping/hl7`. This folder is broken up into various folders containing templates concerned with a specific part of a message. For example, the templates in the message folder will reference templates in the resource folder and so on. These folders and the templates contained therein were initially created by copying the [default templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/tree/master/src/main/resources/hl7) from the LinuxForHealth repo. This was necessary since the default templates do not adhere to the latest [HL7v2 to FHIR mapping inventory](https://docs.google.com/spreadsheets/d/1PaFYPSSq4oplTvw_4OgOn6h2Bs_CMvCAU9CqC4tPBgk/edit#gid=0), the source of truth for HL7v2 to FHIR mappings that ReportStream models all its templates on. Thus, when ReportStream needs to support a new message type or handle a sender's unique interpretation of HL7v2, it is just a matter of consulting the mapping inventory as needed and updating the templates. 

> See the full documentation on [templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md) and [techniques](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TECHNIQUES.md) to understand the templates used in ReportStream.

#### HL7toFhirTranslator

The Convert step, in FHIRConverter.kt, uses `HL7Reader` to load the message as an HL7v2 HAPI object and then passes it to `HL7toFhirTranslator` which loads the mapping templates and uses the LinuxForHealth library to perform the translation to FHIR. 

## Sender Transforms
After the conversion to FHIR format is complete, we can now apply sender transforms. We use these transforms to make sender-specific adjustments to the resulting FHIR bundle, so we can think of this as a FHIR-to-FHIR transform. The class `FhirTransformer` is used to perform these kinds of transforms.

Note: When creating a transform, please reference [Changing/Updating Sender/Receiver Transforms](../getting-started/standard-operating-procedures/changing-transforms.md) for guidance.

### FhirTransformer configurations
[section all about configuration files and the different settings that are possible, link to separate page if necessary]
