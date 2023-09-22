# Transformations in the Universal Pipeline

The Universal Pipeline (UP) has the ability to **transform** messages from one format to another. Presently, the Universal Pipeline supports the following transforms:

- HL7v2 to FHIR
- FHIR to HL7v2

The UP can also **enrich** or **modify** FHIR messages by applying [FHIR to FHIR transforms](#fhir-to-fhir-transformations).

## HL7v2-to-FHIR Transformations

HL7v2 is converted to FHIR during the [Convert](../../universal-pipeline/convert.md) step in the Universal Pipeline and is powered by the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter).

### HL7v2-to-FHIR configurations

> Prior to understanding ReportStream's configurations for the LinuxForHealth library and its use of it, it is important to understand how the library itself works and is architected. The root [README](https://github.com/LinuxForHealth/hl7v2-fhir-converter#readme) in the library's repo contains links that cover this subject in great detail; be sure to read all of it!

The [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter)  uses the [HL7 HAPI library](https://hapifhir.github.io/hapi-hl7v2/) to transform HL7v2 to FHIR based on [HL7 Message Templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md#hl7-message-template-structure) which reference [FHIR Resource Templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md#structure-of-a-fhir-resource-template).

ReportStream's templates (often referred to as "mappings" or "schemas") for the LinuxForHealth HL7v2 to FHIR converter library can be found in the ReportStream repo under `prime-router/metadata/fhir_mapping/hl7`. This folder is broken up into various folders containing templates concerned with a specific part of a message. For example, the templates in the _message_ folder will reference templates in the _resource_ folder and so on. These folders and the templates contained therein were initially created by copying the [default templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/tree/master/src/main/resources/hl7) from the LinuxForHealth repo. This was necessary since the default templates do not adhere to the latest [HL7v2 to FHIR mapping inventory](https://docs.google.com/spreadsheets/d/1PaFYPSSq4oplTvw_4OgOn6h2Bs_CMvCAU9CqC4tPBgk/edit#gid=0), the source of truth for HL7v2 to FHIR mappings that ReportStream models all its templates on. Thus, when ReportStream needs to support a new message type or handle a sender's unique interpretation of HL7v2, it is just a matter of consulting the mapping inventory as needed and updating the templates.

> Want to learn how to read, update, and create templates? See the full documentation on [templates](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TEMPLATING.md) and [techniques](https://github.com/LinuxForHealth/hl7v2-fhir-converter/blob/master/TECHNIQUES.md) to understand the templates used in ReportStream.

#### HL7toFhirTranslator

The Convert step, in `FHIRConverter.kt`, uses `HL7Reader` to load the message as an HL7v2 HAPI object and then passes it to `HL7toFhirTranslator` which loads the mapping templates and uses the [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter) to perform the transformation to FHIR.

## FHIR-to-HL7v2 Transformations

FHIR-to-HL7v2 transforms operate similarly to FHIR-to-FHIR transforms described below but schema elements have an added field, `hl7Spec`, which is a list of HL7 fields to which the data in the element map to. This field is required for all elements not operating in schema mode. Converter schemas are defined and processed in `ConverterSchema.kt`.

TBD: Content to be added in a future story

## FHIR-to-FHIR Transformations

FHIR bundles are transformed (enriched) to FHIR during the [Convert](../../universal-pipeline/convert.md) and [Translate](../../universal-pipeline/translate.md) steps in the Universal Pipeline. The class `FhirTransformer` is used to perform these FHIR to FHIR transforms.

## Transform Schemas

RS Transform schemas are shared between FHIR-to-FHIR and FHIR-to-HL7v2 transformations. They are configured as `.yml` files which are presently located in the repository under `prime-router/metadata/fhir_transforms` and `prime-router/metadata/hl7_mappings`. Whereas HL7v2-to-FHIR and FHIR-to-HL7v2 schemas use a library on top of the respective HAPI library to perform the transformations, FHIR-to-FHIR transforms are performed using the HAPI FHIR library directly, with the logic for loading schemas contained in `ConfigSchemaReader.kt` and the logic for using the loaded schemas to perform the transformation contained in `FhirTransformer.kt`.

#### Schema Structure

For those familiar with the templates of [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter), the RS transform schemas will look like a simpler version. This is because, at their core, they are simply selecting a particular resource in a bundle and setting it to a particular value.

The basic fields that make up a schema are defined as properties in `FhirTransformSchema` class. See `FhirTransformSchema` for more details of what makes up a schema (name, constants, elements, and extends).

Example schemas can be found in the repo under `prime-router/metadata/fhir_transforms`.

##### Schema Elements

An "element" is the basic building block of a schema and contains a set of parameters that describe what bundle property to set, under what conditions to set it to, and what value to set it to. The `FhirTransformer` class traverses each element in the schema and applies the transform following the rules specified by a particular element. 

Elements can be configured for one of two "modes": _value mode_ or _schema mode_. Elements are made up of different properties, which properties exactly depends on the mode. Regardless of mode, however, all elements may have the following properties ( see `FhirTransformSchemaElement` for more details):

- name
- condition
- required
- resource
- constants
- valueSet
- debug

> Note that specifying resource is optional. If resource is not specified, then resource will essentially default to the value of `'%resource'`, meaning the resource will be whatever FHIR resource is being processed. See `FHIRTransformer.transformBasedOnElement` for more detail.

###### Elements using value mode
Elements configured with value mode consist of the following additional properties:

- bundleProperty
- value
- valueSet
- hl7Spec (FHIR-to-HL7v2 only)

When configuring an element with value mode, the **value** and **bundleProperty** properties must be set. This is the more straightforward of the two modes, where each resource matching the **resource** expression will have each of its properties matching the **bundleProperty** expression set to **value**.

**Example** 

Example Element that sets a resource's value to `"P"`:
```yml
  - name: processing-id
    condition: 'Bundle.entry.resource.ofType(MessageHeader).meta.tag.code.empty()'
    resource: 'Bundle.entry.resource.ofType(MessageHeader)'
    bundleProperty: '%resource.meta.tag.code'
    value: [ '"P"' ]
```

As seen in the example above, each element can have a name, which is used for the purposes of extending or overriding values from [extended schemas](#extending-schemas).

###### Elements using schema mode
Elements configured with schema mode consist of the following additional properties:

- schema
- resourceIndex

When configuring an element with schema mode, the **schema** property must be set and the **resourceIndex** property is optional.

When schema mode is selected, the `FhirTransformer` will evaluate the resource expression and will recursively call `transformWithSchema` on each resource that matches the resource expression. Thus, the referenced schema should *eventually* contain element(s) configured with value mode, which serves as the base case of the recursion. In order to prevent a stack overflow, there is a circular dependency check performed when the schema(s) are first loaded.

**Example**

Example Element that evaluates a bundle a resource's value to `"P"`. Say this element belongs to "schemaA":
```yml
- name: sending-application-universal-id-parent
  condition: '%resource.count() > 0'
  resource: 'Bundle.entry.resource.ofType(MessageHeader)'
  schema: 'schemaB'
  resourceIndex: messageHeaderIndex
```

Example Element that sets a resource's value to `"P"`. Say this element belongs to "schemaB":
```yml
- name: sending-application-universal-id-child
  condition: true
  resource: '%resource'
  bundleProperty: '%resource.source.endpoint'
  value: [ '%messageHeaderIndex']
```

If the input JSON has two MessageHeaders, then the output FHIR bundle will set the `source.endpoint` of the first one to the string "0" and the `source.endpoint` of the second one to the string "1".

Additional example schemas, containing elements, can be found in the repo under `prime-router/metadata/fhir_transforms`.

##### Schema Constants

Each schema can define a set of constants to use throughout its elements in order to make the schema easier to read. Constants are defined by populating the `constants` property. Like so:

```yml
constants:
  rsext: '"https://reportstream.cdc.gov/fhir/StructureDefinition/"'
```

The constants can be used in any FHIR expression belonging to any element in the schema or any child schema. When a constant appears in a special string enclosed in %\`\`, the code will look for strings defined as constants and replace them with their values. Example: %\`rsext-software-vendor-org\`

##### Extending Schemas

One schema can extend another by using the keyword `extends` at the very top of the schema. Example `extends: ../default-sender-transform`. When a schema extends another schema, the elements and constants from the referenced schema will be added to the referencing schema. An error will be thrown if there are name collisions for element names.

`ConfigSchemaReader`, a custom ReportStream class for loading FHIR to FHIR schemas, has similar features to that of [LinuxForHealth HL7 to FHIR Converter library](https://github.com/LinuxForHealth/hl7v2-fhir-converter#linuxforhealth-hl7-to-fhir-converter), including the ability to override schemas. This means schemas can reference each other and the `ConfigSchemaReader` class will combine them into one object. `ConfigSchemaReader` has the ability to detect errors in the schema structure, such as circular dependencies, and will throw an error if such an error is found.

###### Implementation details related to extending schemas

- Schemas can only be extended at the root level, i.e. ORU_R01_extended can extend ORU_R01, but a nested schema element cannot use extends
- When an element overrides an existing element it is evaluated in the same context and has access to all the same constants
- When an overriding schema adds a new element, it is evaluated in the context where it is added

For more detailed examples, the [tests](https://github.com/CDCgov/prime-reportstream/blob/438535937071eaf5a76643cffa652dc77415422b/prime-router/src/test/kotlin/fhirengine/translation/hl7/FhirToHl7ConverterTests.kt#L571)
and schemas used can be referenced for how to override schemas.