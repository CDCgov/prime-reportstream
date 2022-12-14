# Serializer Error Codes

December 2022

This document came out of GitHub issue [#7453](https://github.com/CDCgov/prime-reportstream/issues/7453), which 
originally contained, and may still contain, some information found in this document.

## Problem
When ReportStream processes a particular message, the library/libraries we use to parse the message can throw
errors that are NOT helpful to the end user. This can best be noticed in ReportStream's "Admin -> Validate" functionality
where a user can upload a file containing message(s) and the front-end will show all validation errors that
occurred so that, presumably, the end-user can discover exactly what changes are required for their file to
be "valid." Presently, these error messages are sometimes just long stack traces, passed by the back-end directly to
the front-end without any cleaning or helpful metadata. These types of errors are not helpful to the end-user 
because they are hard to read and often times don't give the user enough information to fix the issue.

## Solution
ReportStream should catch exceptions thrown by third-party libraries and wrap them in a specific error code, 
combined with helpful metadata,
that the front-end can map to a nice, helpful message for the end-user. For example, ReportStream can have 
an error code for when an exception gets thrown when processing a date field, say `INVALID_HL7_GENERAL_PARSE`, and part of
the JSON that houses the error code would also have `Type=DATE` and `Field=PID-29`.
With this information, front-end would then know to show the user a helpful message regarding what date formats are 
acceptable, instead of showing a stack-trace.

In addition to third-party library exceptions, ReportStream can also run into issues when trying to load parsed
values into Objects (Element). ReportStream should handle this case gracefully as well. This will be elaborated on down below and
the process of loading a parsed value into an Object will be referred to as "normalization," to match the language 
used in the code. One important thing to note here right away is that when ReportStream tries to load the parsed value as an element,
it has additional information that it doesn't in the external library case, and that is "what exactly is wrong?", 
along with the actual parsed value. This allows for the possibility of a more detailed error code, like `INVALID_HL7_DATE_SCHEMA_TYPE`
instead of `INVALID_HL7_GENERAL_PARSE`.

# Design

Please note, the following design will be specific to HL7 but should be expandable enough for other formats as well.

## Action Messages

ReportStream defines various error/warning messages in `ActionMessages` which eventually get sent to the front-end
as JSON. These messages can be expanded to contain a new field, called `errorCode`, along with any other extra metadata
that would be helpful for the front-end, such as the specific field in question, its type, or its parsed value.
The front-end can then use the errorCode and other metadata in the messages to calculate and display user-friendly messages.

The following is the definition of the new action message that should be added. Its usage will be discussed in the
sections below.

```kotlin
class FieldProcessingMessage(
    // Name of the HL7 field
    fieldMapping: String = "",
    // the ReportStream Element type associated with fieldMapping 
    // for the particular schema associated
    fieldType: String = "",
    // the raw value that was parsed out by an external library (HAPI)
    fieldValue: String = "",
    // Back-end error message. front-end can choose to display 
    // this or a more friendly message using the other fields
    override val message: String,
    // error code
    override val errorCode: String = ""
    
) : ItemActionLogDetail(fieldMapping)
```

## Types of Errors

There are TWO main types of errors that can occur during serialization:

1. **Message Format Errors**
- Empty Message
- Missing HL7 Type field
- Not a supported message type, we only support ORU_R01
2. **Field Type Errors**
- HAPI Parser error (the library used to parse HL7 messages)
- PrimeRouter normalization error

### Message Format Errors

These are straightforward and no further discussion is required. These errors are always caught manually 
by HL7Serializer.kt and the sub-points outlined above can be directly mapped to specific error codes. See error codes
table below for more details.

### Field Type Errors

Field Type Errors can occur either from the HAPI library or when prime-router tries to normalize a field in a
HAPI-parsed message. In the latter case, HAPI was okay with it, but it is not "normalizable" to whatever 
data type (Element) a particular schema has associated with that field.

#### HAPI Parser Error

Since HL7Exceptions, the type of exception thrown by HAPI, contain the field name, via location property, 
we can create a `FieldProcessingMessage`, defined above, and set `fieldMapping` to `e.location`,
`message` to `e.localizedMessage`, and `errorCode` to a response code that is specific for the particular error 
of the HL7 field. Finally, we can set `FieldType` to the Element associated with the field name in the schema 
that is being processed. 

Unfortunately, in this case, `fieldValue` will need to be left empty since HAPI was not able to parse it out.

For `errorCode` it is important to note that we will most likely not be able to determine exactly why the processing failed
without processing `e.localizedMessage` and mapping that value to a response code. I propose we default to a generic
response code like `INVALID_HL7_FIELD_PARSE`.

Example proposed catch logic (works for all fields):
```kotlin
errors.add(
    FieldProcessingMessage(
        fieldMapping = e.location.toString(),
        fieldType = schema.elements.find { it.hl7Field == e.location.toString() },
        fieldValue = "",
        message = "Error parsing HL7 field: ${e.localizedMessage}",
        errorCode = INVALID_HL7_FIELD_PARSE.type
    )
)
```

#### PrimeRouter Normalization Error (429 - 448)

When extracting a value from an HL7 field, in order to put it in the schema map, ReportStream can run into various 
exceptions. For example, a phone number may be formatted incorrectly and the third-party phone number processing
library ReportStream uses, google, cannot load the value.

`Element.toNormalized` is one of the main methods that throws the exception if ReportStream cannot "normalize" a 
certain value based on its supposed type. Currently, there is special logic for normalizing the following types:

- DATE
- DATETIME
- CODE
- TELEPHONE
- POSTAL_CODE
- HD
- EI

The full types Enum is as follows:
```kotlin
enum class Type {
    TEXT,
    TEXT_OR_BLANK, // Blank values are valid (not null)
    NUMBER,
    DATE,
    DATETIME,
    DURATION,
    CODE, // CODED with a HL7, SNOMED-CT, LONIC valueSet
    TABLE, // A table column value
    TABLE_OR_BLANK,
    EI, // A HL7 Entity Identifier (4 parts)
    HD, // ISO Hierarchic Designator
    ID, // Generic ID
    ID_CLIA, // CMS CLIA number (must follow CLIA format rules)
    ID_DLN,
    ID_SSN,
    ID_NPI,
    STREET,
    STREET_OR_BLANK,
    CITY,
    POSTAL_CODE,
    PERSON_NAME,
    TELEPHONE,
    EMAIL,
    BLANK,
}
```

Instead of throwing a simple `IllegalStateException` like it does now, ReportStream should create a new 
exception: `ElementNormalizeException`:

```kotlin
data class ElementNormalizeException(
  override val message: String,
  val field: String,
  val type: String,
  val value: String,
  val errorCode: ErrorType
) : Exception(message)
```

Where `errorCode` can be specific to the kind of error, since at this stage we would know more information as to why
processing failed, unlike at the HAPI stage. An example of an error code we could add here is
`INVALID_HL7_POSTAL_CODE_UNSUPPORTED` since part of the normalization code specifically checks whether the postal code
value matches a specific regex. We can also now provide the `fieldValue` since by this stage HAPI was able to parse
out the value.

This exception can be thrown from inside `Element.toNormalized`, or anywhere else, and caught in the catch in
`convertMessageToMap`. The catch would then be simplified to just wrap the exception in the `FieldProcessingMessage`
mentioned above:

```kotlin
errors.add(
  FieldPrecisionMessage(
      fieldMapping = field,
      fieldType = type,
      fieldValue = value,
      message = message,
      errorCode = errorCode
  )
)
```

### Error Codes

The following set of error codes should cover all the types of errors ReportStream currently checks for
(grouped by data type). More can be added on-demand as ReportStream adds more validation. Also, please
note that the exact type of the Field is actually defined/specified by the schema being mapped to, and it cannot
be internally inferred from the official HL7 field type.

Initial Response Code Ideas (See the second table for where I landed):

| ResportStream Element Type | Error Code                               | Error Description                                            |
|----------------------------|------------------------------------------|--------------------------------------------------------------|
| DATE or DATETIME           | INVALID_HL7_DATE_PARSE                   | HAPI or DateUtilities lib can't parse value                  |
| DATE or DATETIME           | INVALID_HL7_DATE_PRECISION               | date is not HL7 v2.4 TS or ISO 8601 standard format          |
| DATE or DATETIME           | INVALID_HL7_DATE_SCHEMA_TYPE             | the Schema specifies the date is not either DATE or DATETIME |
| TELEPHONE                  | INVALID_HL7_PHONE_NUMBER_PARSE           | HAPI or Google Telecom lib can't parse phone number          |
| EMAIL                      | INVALID_HL7_EMAIL_PARSE                  | HAPI can't parse email                                       |
| CODE                       | INVALID_HL7_CODE_TOKEN_PARSE             | code is not a display value in valueSet for given field      |
| CODE                       | INVALID_HL7_CODE_ALT_TOKEN_PARSE         | code is not a display value in altValues set for given field |
| CODE                       | INVALID_HL7_CODE_ALT_DISPLAY_TOKEN_PARSE | code is not a display value for given field                  |
| POSTAL_CODE                | INVALID_HL7_POSTAL_CODE_UNSUPPORTED      | postal code not in DHL list of supported format              |
| POSTAL_CODE                | INVALID_HL7_POSTAL_CODE_PARSE            | HAPI lib can't parse value                                   |
| EI (Entity Identifier)     | INVALID_HL7_EI_FORMAT                    | EI format is not eiCompleteFormat or eiNameToken             |
| EI (Entity Identifier)     | INVALID_HL7_EI_PARSE                     | HAPI lib can't parse value                                   |
| HD (Hierarchic Designator) | INVALID_HL7_HD_FORMAT                    | HD format is not hdCompleteFormat or hdNameToken             |
| HD (Hierarchic Designator) | INVALID_HL7_HD_PARSE                     | HAPI lib can't parse value                                   |
| GENERAL                    | INVALID_HL7_MSG_TYPE_MISSING             | Missing required HL7 message type field                      |
| GENERAL                    | INVALID_HL7_MSG_TYPE_UNSUPPORTED         | Unsupported HL7 message type                                 |
| GENERAL                    | INVALID_HL7_MSG_FORMAT_INVALID           | Invalid HL7 message format                                   |

Version 2 (My preferred table)

| ResportStream Element Type | Error Code                               | Error Description                                            |
|----------------------------|------------------------------------------|--------------------------------------------------------------|
| DATE or DATETIME           | INVALID_HL7_DATE_PRECISION               | date is not HL7 v2.4 TS or ISO 8601 standard format          |
| DATE or DATETIME           | INVALID_HL7_DATE_SCHEMA_TYPE             | the Schema specifies the date is not either DATE or DATETIME |
| CODE                       | INVALID_HL7_CODE_TOKEN_PARSE             | code is not a display value in valueSet for given field      |
| CODE                       | INVALID_HL7_CODE_ALT_TOKEN_PARSE         | code is not a display value in altValues set for given field |
| CODE                       | INVALID_HL7_CODE_ALT_DISPLAY_TOKEN_PARSE | code is not a display value for given field                  |
| POSTAL_CODE                | INVALID_HL7_POSTAL_CODE_UNSUPPORTED      | postal code not in DHL list of supported format              |
| EI (Entity Identifier)     | INVALID_HL7_EI_FORMAT                    | EI format is not eiCompleteFormat or eiNameToken             |
| HD (Hierarchic Designator) | INVALID_HL7_HD_FORMAT                    | HD format is not hdCompleteFormat or hdNameToken             |
| GENERAL FIELD              | INVALID_HL7_GENERAL_PARSE                | field was unable to be parsed by HAPI or ReportStream        |
| GENERAL MSG                | INVALID_HL7_MSG_TYPE_MISSING             | Missing required HL7 message type field                      |
| GENERAL MSG                | INVALID_HL7_MSG_TYPE_UNSUPPORTED         | Unsupported HL7 message type                                 |
| GENERAL MSG                | INVALID_HL7_MSG_FORMAT_INVALID           | Invalid HL7 message format                                   |