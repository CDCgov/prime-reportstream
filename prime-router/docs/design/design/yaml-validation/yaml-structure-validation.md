# YAML Structure Validation

## Approach

To validate the structure of our YAML files, we will use JSON schema. This approach will allow us to use existing
libraries and IDE functionality to ensure we have consistent YAML structure and avoid errors at runtime.

## Json Schema details

- Version: 2020-12
- [Spec](https://json-schema.org/specification)

## Library

- The library we are using is networknt/json-schema-validator 
  - https://github.com/networknt/json-schema-validator
  - Most stared Java library supported by json-schema.org
  - Able to easily use in Kotlin

## Where to put schema files

- All JSON Schema JSON files will live in the following directory
  - prime-router/metadata/json_schemas

## How to write schema files

- [Follow the tutorial](https://json-schema.org/understanding-json-schema/basics)
- Below is an example schema for a `Documentation` object with a single field called `testField`
```json
{
  "$id": "https://reportstream.cdc.gov/schemas/documentation.schema.json", // ReportStream specific $id in this format. The link does not need to work but it must be unique. 
  "$schema": "https://json-schema.org/draft/2020-12/schema", // JSON Schema version
  "title": "Documentation", // object title (organization, mapping, etc)
  "type": "object", // top level type (will generally be object)
  "properties": { // list of all properties in the YAML file
    "testField": { // a property definition
      "type": "string", // the type of testField
      "description": "A testField for the documentation" // helpful tip on what the field is used for that will show up in autocomplete dialog boxes.
    }
  },
  "required": ["testField"], // Make the field required for errors to occur if the field is missing
  "additionalProperties": false // Throw errors if there are unknown properties in the file
}
```

## How to apply schema files to a new YAML file

- Add the following comment to the top of your yaml file to enable Intellij validation. Ensure the path is correct
```yaml
# $schema: ./documentation.json
```

You can see this in action [here](../../../../metadata/json_schema/testing/documentation.yml). 
Try to add a new field and see how Intellij reacts.

## How to run JSON validations at runtime
- See examples in [JsonSchemaValidationTest.kt](../../../../src/test/kotlin/validation/JsonSchemaValidationTest.kt)
