## Introduction

Sender transforms are used after validation of incoming sender data to manipulate and fix
issues with the data. Issues may include missing fields we can derive from other fields (e.g.
LIVD data), and detecting and fixing incorrect or badly formatted fields.

## COVID Pipeline Sender Transforms

The COVID pipeline uses the sender schemas to perform sender transforms. Transforms are
performed on the internal representation of a report which is analogous to a CSV formatted file.
The values in this internal report are referenced by the schema by their element names which
are the column names in the internal CSV report. Note that any HL7 spec references in the
COVID schemas are only used for the HL7 v2 to CSV conversion and are not used for the
sender transform process.
The COVID sender transforms are used to:

1. Set an element value to a literal value, replacing any existing value
2. Set an element value to the value of another element, replacing any existing value
3. Set an element value to the output value of a mapper (a Java-based function with
   complicated logic) that may take one or more element values, replacing any existing
   value including setting the value to empty

Note that the validation of data is done AFTER the sender transformation as some mappers are
used to perform validation of the data (e.g. LIVD lookup).

## Universal Pipeline Sender Transforms

### Requirements

For the UP, we receive data as HL7 v2 then convert the data to FHIR then send it for routing.
We would need to perform the sender transform either on the HL7 v2 data or the internal FHIR
data before it is sent for routing. Here are a few assumptions we will make:

1. The HL7 v2 data we want to do sender transformation is valid. Validation will be done
   before our transform step.
2. There is a possibility of some HL7 v2 data not conforming to the HL7 to FHIR conversion
   we have developed. This will mean some FHIR fields may be empty or have incorrect
   data. We may need to look at the HL7 v2 data directly to perform some transforms.
3. Some data we need may be missing from the HL7 v2. For example, equipment data
   may be missing that we will have to generate using something like the LIVD table
   lookup. This means we would have to generate NEW data that may require the creation
   of various levels of FHIR bundle properties (e.g. we may need to create an Address
   resource to then create a country property).
4. There is a potential that this transform process will also be the validation process. Right
   now mappers and cardinality are checked as part of the validation process in the COVID
   pipeline.

The possible use cases for sender transforms in the UP that we need to address are:

1. Set the value of an existing or new FHIR property with a literal constant
2. Set the value of an existing or new FHIR property with another FHIR value
3. Set the value of an existing or new FHIR property with a manipulated FHIR value (e.g.
   split a string)
4. Set the value of an existing or new FHIR property with the value from an HL7 field (this
   could happen with bad data that the HL7 to FHIR conversion could not handle)
5. Set the value of an existing or new FHIR property with the value from a complex
   operation (e.g. a mapper like LIVD lookup)

### Sender Transforms

It is proposed to do the sender transform AFTER the data has been validated and converted
into FHIR. We will then be able to perform the transforms using FHIR Path in a similar manner
as used in the FHIR to HL7 conversion library which will reduce the learning curve and make
the configuration for the UP more consistent. It is also proposed to give access to the raw HL7
v2 data to allow for transforms where the HL7 v2 data was not converted correctly to allow for
the correction of bad data.

### Schema Design

```
elements:
    - name: patient-country
      constants:
          msh11: HL7, ORC(%orderIndex)-11 -> from original HL7 to string
          something: JEXL, UUID.random() -> NOT DO TO COMPLEXITY
          provenance: FHIR, Bundle.entry... -> FHIR to string
          someString: String, blah
          anotherString: blah blah <- ONLY THIS FOR NOW
      resource: ‘Bundle.entry.resource.ofType(Patient)’
      condition: ‘%resource.address.country.exists().not()’
      bundleProperty: ‘%resource.address.country’
      value: [‘“USA”’]
      valueSet:
          key: value
          key2: value
      valueSetTable: <lookup table name>,<key column>,<value column>
    - name: patient-country
      resource: ‘Bundle.entry.resource.ofType(DiagnosticReport)’
      resourceIndex: orderIndex
      schema: order
```

Element properties (in order of execution):

- name - the name of an element
- constants - constants passed in to FHIR Path evaluations. They are resolved at the time
  an element uses it
- resource - the FHIR resource used as focus on all other FHIR Path expressions. Must
  be used with child schema to set the collection to iterate with
- condition - FHIR Path boolean expression that must evaluate to true for the element to
  be evaluated. Conditions can be used to check the value of a bundle property that
  another element may have populated, so it could be used to check the result of a
  previous element (elements must be kept in the correct order for this to work)
- target - a FHIR Path expression that denotes where to store the value
- value - a list of FHIR Path expressions that evaluates to the proper FHIR Type to be
  assigned to the property specified in the target element property. The first expression to
  have a value wins. This allows you to set defaults at the end of the list.
- valueSet - a list of key value pairs used to convert the value generated by the value
  property to another value that matches the key. Cannot be used with schema or
  valueSetTable
- valueSetTable - configuration to use a lookup table (in the database) for the value set.
  The configuration includes the name of the lookup table to use as well as the name of
  the columns to use as key and value. Cannot be used with schema or valueSet
- schema - the name of a child schema to process. Cannot be used with target and value
- resourceIndex - the name of a constant with the index of a resource collection. Useful to
  iterate over multiple resources. Can only be used with schema

### Complex Data Types

Some of the values that need to be set in the bundle will be complex data types such as a
CodeableConcept, Quantity, etc. For example, a CodeableConcept needs a Coding data type
which then needs other values such as code and system. We can potentially chain elements to
create this complex data type, but a simpler approach is to create extensions to the FHIR Path
functions to create it then use it in the value property to pass into the bundle. For example:
Value: [‘newCodeableConcept(“code”, “system”, “text”)’]

### Challenges

One of the main challenges with this design is the ability to set a value to a FHIR property that
does not exist. This problem arises from the fact that FHIR Path is meant to be an extraction
language for use in queries and not for data manipulation. We could potentially solve this
problem by parsing and dissecting FHIR Path expressions to detect what properties or
resources do not exist and create them on the fly. This could be done by using the Java FHIR
library which contains functions to add child properties with the correct data types. However,
due to the complexity of a FHIR bundle, it is expected that there will be limitations to what
resources we can add, especially when it comes to references or a property that can have
multiple types of resources. We may need to code solutions to these limitations as the use
cases arise.
```Bundle.entry.resource.ofType(Patient).address.country```

## Tickets

- Read and validate UP Sender Transform configuration
- UP Sender Transform detects schema circular dependencies in schema configuration
- Detect duplicate element names in UP Sender Transform schema
- UP Sender Transform can transform existing data in non-repeating FHIR resources
- UP Sender Transform can add new data to a FHIR bundle
- UP Sender Transform can transform data for repeating FHIR resources
- Support inline value sets in UP Sender Transform schema
- Support the use of lookup tables for value sets in UP Sender Transform schema