# Universal Pipeline Convert Step
The convert step is executed when the [Azure AQS message](README.md#aqs-usage) referencing a received [Report](README.md#report-and-item), created by the [Receive](receive.md) step, is scheduled by Azure. For each [Item](README.md#report-and-item) contained in the received Report, the convert step will:

  1. Translate the item to the FHIR format
  2. Apply the configured [sender transform](#sender-transforms), if one is configured
  3. Generate a new Report
  4. Schedule the Route step for the new Report

## Conversion
The first and most significant aspect of the convert step is the conversion of a message into the FHIR format. The process of this conversion depends on the incoming message's format:

- If the incoming message is HL7v2, ReportStream uses the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter) with custom configurations from the [fhir_mapping folder](https://github.com/CDCgov/prime-reportstream/tree/master/prime-router/metadata/fhir_mapping/hl7) to handle the conversion. See the [HL7v2-to-FHIR configurations](#HL7v2-to-FHIR-configurations) section for more details.
- If the incoming message is FHIR, then there's nothing to be done
- Apply Sender transforms specified by Sender configuration 

> Currently, no other input formats besides HL7v2 and FHIR are supported in the Universal Pipeline. In order to support new formats, they would need to be translated to FHIR by some new method.

### HL7v2-to-FHIR configurations

> Prior to understanding ReportStream's configurations for the LinuxForHealth library and its use of it, it is important to understand how the library itself works and is architected. The root [README](https://github.com/LinuxForHealth/hl7v2-fhir-converter#readme) in the library's repo contains links that cover this subject in great detail; be sure to read all of it!

At its core, the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter)  uses the [HL7 HAPI library](https://hapifhir.github.io/hapi-hl7v2/) to translate HL7v2 to FHIR based on [HL7 Message Templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md#hl7-message-template-structure) which reference [FHIR Resource Templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md#structure-of-a-fhir-resource-template). 

ReportStream's templates (often referred to as "mappings") for the LinuxForHealth HL7 to FHIR converter library can be found in the ReportStream repo under `prime-router/metadata/fhir_mapping/hl7`. This folder is broken up into various folders containing templates concerned with a specific part of a message. For example, the templates in the _message_ folder will reference templates in the _resource_ folder and so on. These folders and the templates contained therein were initially created by copying the [default templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/tree/master/src/main/resources/hl7) from the LinuxForHealth repo. This was necessary since the default templates do not adhere to the latest [HL7v2 to FHIR mapping inventory](https://docs.google.com/spreadsheets/d/1PaFYPSSq4oplTvw_4OgOn6h2Bs_CMvCAU9CqC4tPBgk/edit#gid=0), the source of truth for HL7v2 to FHIR mappings that ReportStream models all its templates on. Thus, when ReportStream needs to support a new message type or handle a sender's unique interpretation of HL7v2, it is just a matter of consulting the mapping inventory as needed and updating the templates. 

> Want to learn how to read, update, and create templates? See the full documentation on [templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md) and [techniques](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TECHNIQUES.md) to understand the templates used in ReportStream.

#### HL7toFhirTranslator

The Convert step, in `FHIRConverter.kt`, uses `HL7Reader` to load the message as an HL7v2 HAPI object and then passes it to `HL7toFhirTranslator` which loads the mapping templates and uses the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter) to perform the translation to FHIR. 

## Sender Transforms
After the conversion of an Item to the FHIR format, an additional transform can run on the resulting FHIR bundle. This transform is called a "sender transform" because it is used to make sender-specific adjustments to the FHIR bundle. These transforms can also run in the [translate](translate.md) step, in the case a receiver wants to receive FHIR, where they would be called "receiver transforms" so the preferred term for these transforms is "FHIR to FHIR transforms." The class `FhirTransformer` is used to perform these FHIR to FHIR transforms.

Sender Transforms are completely optional, but ReportStream has created a default sender transform that can be extended or used directly (`prime-router/metadata/fhir_transforms/senders/default-sender-transform.yml`). This is a good place to start to understand what kind of transforms a Sender may want performed on their messages.

> When creating a transform, reference [Changing/Updating Sender/Receiver Transforms](../getting-started/standard-operating-procedures/changing-transforms.md) for guidance.

### Fhir-to-Fhir configurations

FHIR to FHIR transforms (templates) are configured as `.yml` files which are presently located in the repository under `prime-router/metadata/fhir_transforms`. Whereas HL7-to-FHIR and FHIR-to-HL7 templates use a library on top of the respective HAPI library to perform the translations, FHIR to FHIR transforms are performed using the HAPI FHIR library directly, with the logic for loading templates contained in `ConfigSchemaReader.kt` and the logic for using the loaded templates to perform the translation contained in `FhirTransformer.kt`.

#### Template Structure

For those familiar with the templates of [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter), the Fhir-to-Fhir templates will look like a simpler version. This is because, at their core, they are simply selecting a particular resource in a bundle and setting it to a particular value. 

##### Template Elements

An "element" is the basic building block of a template and contains a set of parameters that describe what bundle property to set, under what conditions to set it to, and what value to set it to. The `FhirTransformer` class traverses each element in the template and applies the transform following the rules specified by a particular element. The basic fields that make up an element are as follows:

- **condition**: an expression evaluating to true or false. When true, the transform will be applied
- **resource**: the FHIR path of the resource in the bundle that is to have its property/properties modified
- **bundleProperty**: The FHIR path to the property of the resource returned by the resource parameter above
- **value**: The value to set the bundleProperty to

Example Element:
```yml
  - name: processing-id
    condition: 'Bundle.entry.resource.ofType(MessageHeader).meta.tag.code.empty()'
    resource: 'Bundle.entry.resource.ofType(MessageHeader)'
    bundleProperty: '%resource.meta.tag.code'
    value: [ '"P"' ]
```

As seen in the example above, each element can have a name, which is used purely for the purposes of extending or overriding values from [extended templates](#extending-templates).

##### Template Constants

Each template can define a set of constants to use throughout its elements in order to make the template easier to read. Constants are defined by populating the `constants` property. Like so:

```yml
constants:
  rsext: '"https://reportstream.cdc.gov/fhir/StructureDefinition/"'
```

The constants can be used in any FHIR expression belonging to any element in the template or any child template. When a constant appears in a special string enclosed in %\`\`, the code will look for strings defined as constants and replace them with their values. Example: %\`rsext-software-vendor-org\`

##### Extending Templates

One template can extend another by using the keyword `extends` at the very top of the template. Example `extends: ../default-sender-transform`

`ConfigSchemaReader`, a custom ReportStream class for loading Fhir to Fhir templates, has similar features to that of [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter), including the ability to "merge" schemas. This means templates can reference each other and the `ConfigSchemaReader` class will combine them into one object. `ConfigSchemaReader` has the ability to detect errors in the template structure, such as circular dependencies, and will throw an error if such an error is found.