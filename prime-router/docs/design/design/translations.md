# Translations in the Universal Pipeline

The Universal Pipeline (UP) has the ability to **transform** messages from one format to another. Presently, the Universal Pipeline supports the following transforms:

- HL7v2 to FHIR
- FHIR to HL7v2

The UP can also **enrich** FHIR messages by applying "FHIR to FHIR transforms"

## HL7-to-FHIR Translations

HL7 is converted to FHIR during the [Convert](../../universal-pipeline/convert.md) step in the Universal Pipeline and is powered by the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter).

### HL7v2-to-FHIR configurations

> Prior to understanding ReportStream's configurations for the LinuxForHealth library and its use of it, it is important to understand how the library itself works and is architected. The root [README](https://github.com/LinuxForHealth/hl7v2-fhir-converter#readme) in the library's repo contains links that cover this subject in great detail; be sure to read all of it!

At its core, the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter)  uses the [HL7 HAPI library](https://hapifhir.github.io/hapi-hl7v2/) to translate HL7v2 to FHIR based on [HL7 Message Templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md#hl7-message-template-structure) which reference [FHIR Resource Templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md#structure-of-a-fhir-resource-template).

ReportStream's templates (often referred to as "mappings" or "schemas") for the LinuxForHealth HL7 to FHIR converter library can be found in the ReportStream repo under `prime-router/metadata/fhir_mapping/hl7`. This folder is broken up into various folders containing templates concerned with a specific part of a message. For example, the templates in the _message_ folder will reference templates in the _resource_ folder and so on. These folders and the templates contained therein were initially created by copying the [default templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/tree/master/src/main/resources/hl7) from the LinuxForHealth repo. This was necessary since the default templates do not adhere to the latest [HL7v2 to FHIR mapping inventory](https://docs.google.com/spreadsheets/d/1PaFYPSSq4oplTvw_4OgOn6h2Bs_CMvCAU9CqC4tPBgk/edit#gid=0), the source of truth for HL7v2 to FHIR mappings that ReportStream models all its templates on. Thus, when ReportStream needs to support a new message type or handle a sender's unique interpretation of HL7v2, it is just a matter of consulting the mapping inventory as needed and updating the templates.

> Want to learn how to read, update, and create templates? See the full documentation on [templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md) and [techniques](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TECHNIQUES.md) to understand the templates used in ReportStream.

#### HL7toFhirTranslator

The Convert step, in `FHIRConverter.kt`, uses `HL7Reader` to load the message as an HL7v2 HAPI object and then passes it to `HL7toFhirTranslator` which loads the mapping templates and uses the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter) to perform the translation to FHIR.

## FHIR-to-HL7 Translations

## FHIR-to-FHIR Translations

FHIR bundles are translated (enriched) to FHIR during the [Convert](../../universal-pipeline/convert.md) and [Translate](../../universal-pipeline/translate.md) steps in the Universal Pipeline. The class `FhirTransformer` is used to perform these FHIR to FHIR transforms.

### FHIR-to-FHIR configurations

FHIR to FHIR transform templates are configured as `.yml` files which are presently located in the repository under `prime-router/metadata/fhir_transforms`. Whereas HL7-to-FHIR and FHIR-to-HL7 templates use a library on top of the respective HAPI library to perform the translations, FHIR to FHIR transforms are performed using the HAPI FHIR library directly, with the logic for loading templates contained in `ConfigSchemaReader.kt` and the logic for using the loaded templates to perform the translation contained in `FhirTransformer.kt`.

#### Template Structure

For those familiar with the templates of [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter), the FHIR-to-FHIR templates will look like a simpler version. This is because, at their core, they are simply selecting a particular resource in a bundle and setting it to a particular value.

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

`ConfigSchemaReader`, a custom ReportStream class for loading FHIR to FHIR templates, has similar features to that of [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter), including the ability to "merge" schemas. This means templates can reference each other and the `ConfigSchemaReader` class will combine them into one object. `ConfigSchemaReader` has the ability to detect errors in the template structure, such as circular dependencies, and will throw an error if such an error is found.