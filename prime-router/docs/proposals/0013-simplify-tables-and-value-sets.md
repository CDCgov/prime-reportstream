# Simplification of Value Sets and Tables Proposal

## Context and Motivation
New data translation features have been added over time to ReportStream to support onboarding senders and receivers, but some of these features have evolved to have similar functionality that may be confusing to those onboarding new entities. For this proposal, we are examining the value sets and tables translation features and their configuration.  

## Goals
Simplify the use of value sets and tables in schemas to make them simpler to configure and understand.

## Background
Value sets and tables are used to translate data values from different sources. The following subsections provide some background on the current functionality of value sets and tables.

### Value Sets
Value sets are defined in a value set file (see metadata/valuesets folder), and are identified in a schema field by the CODE field type and a valueSet parameter that specifies the name of the value set to use. A simple way to see value sets is as a key/value pair. Value sets are defined in YAML and each file can have one or more value sets defined as a YAML array. Values are comprised of the following fields:
 - code
 - display
 - system (Optional)

Example value set:
```yaml
- name: hl70136
  system: HL7
  values:
    - code: Y
      display: Yes
    - code: N
      display: No
    - code: UNK
      display: Unknown
      system: NULLFL
```

Example schema configuration:
```yaml
- name: employed_in_healthcare
  type: CODE
  valueSet: hl70078
```

There are a few variations on how value sets are used in the schema and in code:

|Variation|Serialization (data from a sender)|Deserialization (data to a receiver)
|---|---|---|
|Code ($code) (default)|Data is translated from a display to a coded value based on the value set|Data is translated from a display to a coded value based on the value set|
|Display ($display)|Data is translated from a display to a coded value based on the value set|Data is translated from a coded value to a display value based on the value set|
|Caret  ($code^$display^$system) - for HL7 related value, e.g. SNOMED|NOT SUPPORTED|Supported in a schema, but only used by REDOX serializer code - A combination of normalizedValue^display^systemCode|
|System ($system)|NOT SUPPORTED|UNUSED - Translates a code to a system name that supports the code (e.g. NULLFL, hl70136)|

Note that variations can only be configured in a schema for CSV data fields, so non-default variations can only be configured to work on CSV and internal CSV data. Here is an example of a schema configuration using the display token variation:
```yaml
- name: specimen_source_site_code
  type: CODE
  valueSet: covid-19/specimen_source_site
  csvFields: [{name: Specimen_source_site, format: $display}]
```
  
###Alternate Value Sets
Alternate display values work the same way as value sets of the display variation, but are a custom set of values defined in a schema instead of a value set file as a means to have a unique value set for a specific receiver or sender.

|Variation|Serialization (data from a sender)|Deserialization (data to a receiver)|
|---|---|---|
|Alternate Values|Data is translated from a display to a coded value based on the alternate values specified in the schema|Data is translated from a coded value to a display value|

Example alternate values:
```yaml
name: patient_ethnicity
altValues:
- code: H
  display: 2135-2
- code: N
  display: 2186-5
- code: U
  display: UNK
  csvFields: [{ name: patientEthnicity, format: $alt}]
```

### Tables
Tables are used to lookup values defined in a CSV file containing columns and rows of data.  The table lookup feature uses one or two fields and indices to try to match a row of data from the table and return the value from the specified column if found. Table fields are defined in a schema using the TABLE field type, table name parameter and tableColumn parameter. Table fields have different meanings depending on its configuration as folllows.  Of note, is that the fake data generator grabs data from a table for each of the table fields.
A table field that uses NO mapper is simply a definition of a field that could be looked up by the specified tableColumn name. This field name is then used in a table lookup for a different field with the table mapper using the specified tableColumn as an index.
A table field with a lookup mapper with one or two arguments will lookup a row in the table using the specified field values (indices) passed as arguments to the mapper.  When using two indices, both field values must be found in the same table row (an AND logical operator) for a match to occur.  If the row is found then the value from the column specified in the tableColumn parameter is returned.

Example table index field:
```yaml
- name: patient_county
  type: TABLE_OR_BLANK
  table: fips-county
  tableColumn: County
```

Example table lookup:
```yaml
- name: ordering_provider_county_code
  type: TABLE
  pii: false
  table: fips-county
  tableColumn: FIPS
  mapper: lookup(ordering_provider_state, ordering_provider_county)
```

Example table data:
```csv
FIPS,County,State
01001,Autauga,AL
01003,Baldwin,AL
```

### LIVD Lookup
The LIVD lookup works similarly to the table lookup, but the LIVD lookup mapper matching logic is more complicated.  The LIV lookup looks for a row in a table that matches ANY (a logical OR operation) of device ID, equipment model ID, test kit name ID or equipment model name (in that specific order). Furthermore, there are some specific rules on how to handle received compounded device IDs to match the values in the table.

Example LIVD lookup mapper use:
```yaml
- name: equipment_model_id_type
  type: TABLE
  table: LIVD-SARS-CoV-2-2021-04-28
  tableColumn: Equipment UID Type
  mapper: livdLookup()
```

## Detailed proposal
### Remove the Use of the Table and Code Field Types
It is proposed to remove the TABLE and CODE field types and use the more primitive field types like text, number, email, etc. for fields to be clearer as to what type a field really is. Table fields already require the table and tableColumn parameters, and code fields require the valueSet parameter, so those fields can be identified by the presence of those required parameters.

Example table lookup with primitive field type:
```yaml
- name: ordering_provider_county_code
  type: NUMBER
  table: fips-county
  tableColumn: FIPS
  mapper: lookup(ordering_provider_state, ordering_provider_county)
```

### Remove the Use of Fields to Define Index Columns
It is proposed to remove the use of fields to define the name of columns to lookup in a table for a different field. These fields that define column names have the table and tableColumn parameters, but do not use a lookup mapper. This type of field will then be changed to use a more primitive data type and will not need to specify a table or tableColumn field. This will remove the confusion as to if those index fields are looked up in a table or not, and why are they labeled as table fields if not.  The column names are to be specified in a new tableIndex parameter of the field that is doing the lookup, so the mapper no longer needs to be passed in arguments. This way, the table parameter will truly mean the table a field uses for its data, and tableColumn will truly mean the column to use for the data of the field. Furthermore, this change allows for using any column name for a given field without having to overwrite a field definition.

Example of specifying lookup fields
```yaml
- name: ordering_provider_county_code
  type: NUMBER
  table: fips-county
  tableColumn: FIPS
  tableIndex:
- ordering_provider_state, FIPS
- ordering_provider_county, fips-county
  mapper: lookup()
```

Making this change, however, results in those index fields not being populated with fake data taken from a table when generating test data.  It is proposed to change the fake data generator to look at the indices listed in each new table field and populate them with the proper data, taking care not to let the fake data generator overwrite those values as it walks through all the fields in a schema.

### Support More than Two Lookup Indices
It is proposed to support table lookups with more than two table indices.  The code is currently generic enough to support this functionality and, although we currently do not use such a feature, it will simplify the schema validation when adding the above proposed change to define the indices as a parameter.

### Remove the Use of a Mapper for Table Lookups
It is proposed to remove the use of the lookup mapper when looking up data in a table. The mapper will no longer be needed once the above proposals are implemented.

Example proposed table lookup:
```yaml
- name: ordering_provider_county_code
  type: NUMBER
  pii: false
  table: fips-county
  tableColumn: FIPS
  tableIndex:
- ordering_provider_state, FIPS
- ordering_provider_county, fips-county
```

### Use Tables for Value Sets
It is proposed to change value sets to be defined as tables. Some value sets will be simple tables with just two columns, but others, like hl70136, will have a third row for a system value. Using tables allows us to remove the need to have variations like $code and $display that define the direction of translation and replace it with a clear definition of which column to match and which to return.

Example of a field that used to be a CODE field with the default $code variation:
```yaml
-name:  abnormal_flag
type: TEXT
table: hl70078
tableColumn: code
tableIndex:
- display
```

Example of a table replacing a value set:
```csv
code,display,system
Y,Yes,hl70136
N,No,hl70136
UNK,Unknown,NULLFL
```

### Add Alternate Value Sets to Existing Values
It is proposed to add alternate values to an existing table that has the value sets. This will allow for easier reuse of those values in multiple schemas.

Example value set with added alternate values:
```csv
Code,display
Y,Yes
Y,Si
Y,Positivo
N,No
N,Negativo
UNK,Unknown
UNK,No se
```

The example above makes it simple to translate the display value to a code, but care must be taken when translating a code to a display value as the first match on the table will be returned. For readability, it is recommended to create a new table with a simplified translation, so as to not rely on the ordering of the table.

Example simplified table used to translate code to display:
```csv
Code,display
Y,Yes
N,No
UNK,Unknown
```

### Specify Table Lookup Logical Operator on Multiple Indices
It is proposed to add a parameter to specify the logical operator when specifying more than one table index. There is currently no table lookup that needs an OR logical operation, so this can be done as a later enhancement. This will allow us to search a table with an AND or OR operator. The default operator can be set to AND, so it does not have to be specified for all tables.

Example table lookup specifying logical operator:
```yaml
- name: ordering_provider_county_code
  type: TABLE
  pii: false
  table: fips-county
  tableColumn: FIPS
  tableIndex:
- ordering_provider_state, FIPS
- ordering_provider_county, fips-county
  tableIndexOperator: AND
```

### Change Livd Lookup to Be Just a Mapper
It is proposed to change the LIVD lookup to be just a mapper and not use the table parameters as configuration to the mapper.  This will keep the schema configuration simple and straightforward.  Arguments passed into the mapper will specify the table and table column to use for the LIVD lookup. Note that the LIVD lookup has some very specific logic when looking up device IDs which makes it difficult to include that logic as part of the table lookup and keep the table configuration simple.  The preference in this proposal is for simplicity and it is proposed to keep the LIVD lookup as a mapper.

Example table lookup using the LIVD table:
```yaml
- name: equipment_model_id_type
  type: TEXT
  mapper: livdLookup(LIVD-SARS-CoV-2-2021-04-28, Equipment UID Type)
``` 

### Proposal Summary
 - Remove the TABLE and CODE field types
 - Remove the use of fields as table indices
 - Support more than two table indices for lookups
 - Remove the lookup mapper used for table lookups
 - Replace value sets with tables and table lookups
 - Remove alternate value sets from schemas and add values to a value set table
 - Add parameter to specify logical operator for matching table indices
 - Change LIVD lookup to pass table information as mapper arguments and not use tables
