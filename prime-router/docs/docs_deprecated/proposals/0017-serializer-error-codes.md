# Serializer Error Codes

December 2022

This document came out of GitHub issue [#7453](https://github.com/CDCgov/prime-reportstream/issues/7453), which 
originally contained, and may still contain, some information found in this document.

The issue noted above was originally spawned from a 
[front-end error investigation](https://app.mural.co/t/primedatainput6410/m/primedatainput6410/1666190212832/25f971[…]d289837352dc406cb379ce362068c?sender=u027533c51148881a30036919).
It is highly recommended the reader review this document to fully understand the core problem, although this document 
will provide a summary in the following sections.

## Problem
When ReportStream processes a particular message, the library/libraries we use to parse the message can throw
errors that are NOT helpful to the end user. This can best be noticed in ReportStream's "Admin -> Validate" functionality
where a user can upload a file containing message(s) and the front-end will show all validation errors/warnings that
occurred so that, presumably, the end-user can discover exactly what changes are required for their file to
be "valid." Presently, these error messages are sometimes just long stack traces, passed by the back-end directly to
the front-end without any cleaning or helpful metadata. These types of errors are not helpful to the end-user 
because they are hard to read and often times don't give the user enough information to fix the issue.

## Solution
ReportStream should catch exceptions thrown by third-party libraries and wrap them in a specific error code
that the front-end can map to a nice, helpful message for the end-user. For example, ReportStream can have 
an error code for when an exception gets thrown when processing a date field, say `INVALID_HL7_PARSE_DATE`, and part of
the JSON that houses the error code would also have `Field=PID-29`.
With this information, front-end would then know to show the user a helpful message regarding what date formats are 
acceptable, instead of showing a stack-trace.

In addition to third-party library exceptions, ReportStream can also run into issues when trying to load parsed
values into Objects (Element). ReportStream should handle this case gracefully as well. This will be elaborated on down below and
the process of loading a parsed value into an Object will be referred to as "normalization," to match the language 
used in the code.

### Disclaimer

The benefit of this approach is that it is common and reusable. Since all that is at the heart of this proposal 
is the use of an "error code", different formats (FHIR, CSV, HL7, et. al) can all make use of this feature. However, 
Since the formats are different from each other, which error codes they support and the way they calculate those 
error codes MAY VERY. **This document discusses these specifics as they relate to HL7 in the old (covid) pipeline only**. 
When other pipelines or formats need to support error codes, work will need to be done to figure out how detailed the 
error codes can be and how they can be calculated.

# Design

Please note, the following design will be specific to HL7 but should be expandable enough for other formats as well.

## Action Messages

ReportStream defines various error/warning messages in `ActionMessages` which eventually get sent to the front-end
as JSON. These messages can be expanded to contain a new field, called `errorCode`, along with any other extra metadata
that would be helpful for the front-end, such as the specific field in question.
The front-end can then use the errorCode and other metadata in the messages to calculate and display user-friendly messages.

The following is the definition of the new action message that should be added. Its usage will be discussed in the
sections below. Please note, `FieldProcessingMessage` may not look exactly like what is shown below. For example, some of
its fields may be factored out and placed in a parent class, like `ItemActionLogDetail`. The other ActionMessages in
`ActionMessages.kt` will most likely end up inheriting from `FieldProcessingMessage`. Currently, the main purpose of the
would-be children of `FieldProcessingMessage` is to override the message parameter with a pre-built message specific
to a particular normalization error. See `MissingFieldMessage` for an example.

```kotlin
// This is a pseudocode class posted here just to show what kind of fields it would have. 
// During implementation, this class most likely will have some fields factored out and 
// integrated into ItemActionLogDetail. Other ActionMessages will probably get refactored as well
class FieldProcessingMessage(
    // Name of the HL7 field (example: PID-29) or FHIR path, or csv column name, etc
    field: String = "",
    // Back-end error message. front-end can choose to display 
    // this or a more friendly message using the other fields
    message: String = "",
    // error code
    errorCode: ErrorCode = ErrorCode.UNKNOWN
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
by HL7Serializer.kt and the sub-points outlined above can be directly mapped to specific error codes. 
See the errors tagged _GENERAL MSG_ in the error codes table below for more details.

### Field Type Errors

Field Type Errors can occur either from the HAPI library or when prime-router tries to normalize a field in a
HAPI-parsed message. In the latter case, HAPI was okay with it, but it is not "normalizable" to whatever 
data type (Element) a particular schema has associated with that field.

#### HAPI Parser Error

Since HL7Exceptions, the type of exception thrown by HAPI, contain the field name, via location property, 
we can create a `FieldProcessingMessage`, defined above, and set `field` to `e.location`,
`message` to `e.localizedMessage`, and `errorCode` to a response code that is specific for the particular error 
of the HL7 field.

Calculating the `errorCode` for HAPI exceptions is conceptually trivial. To get a detailed-enough error code that the 
front-end can map to, `e.location` can be cross-referenced against the schema that is being processed. Each schema
contains a collection of Elements that have a `field` parameter that can be compared against `e.location`. Once a 
matching element is found, then `Element.type` can be accessed to return the type of the field in question as
defined by the schema. Finally, with `Element.type` a data-specific ErrorCode can be calculated, like so: 
`INVALID_HL7_PARSE_$matchingElement.type`.

Note, this approach only works for HL7 in the old pipeline. A new approach will need to be invented for Universal Pipeline
and other potential pipelines or formats.

#### PrimeRouter Normalization Error

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

The full `Types` Enum is as follows:
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
  val value: String,
  val errorCode: ErrorType
) : Exception(message)
```

Where `errorCode` can be specific to the kind of error, since at this stage we would know more information as to why
processing failed, unlike at the HAPI stage. An example of an error code we could add here is
`INVALID_HL7_POSTAL_CODE_UNSUPPORTED` since part of the normalization code specifically checks whether the postal code
value matches a specific regex. We can also now provide the `fieldValue` since by this stage HAPI was able to parse
out the value, but still can't pass it to the `FieldPrecisionMessage` due to risk of exposing PII.

This exception can be thrown from inside `Element.toNormalized`, or anywhere else, and caught in the catch in
`convertMessageToMap`. The catch would then be simplified to just wrap the exception in the `FieldProcessingMessage`
mentioned above:

```kotlin
errors.add(
  FieldPrecisionMessage(
      field = fieldId,
      message = message,
      errorCode = errorCode
  )
)
```

### Error Codes

The following set of error codes should cover all the types of errors ReportStream currently checks for
(grouped by data type). More can be added on-demand as ReportStream adds more validation.

| ReportStream Element Type | Error Code                        | Error Description                                           |
|---------------------------|-----------------------------------|-------------------------------------------------------------|
| GENERAL MSG HL7           | INVALID_HL7_MSG_TYPE_MISSING      | Missing required HL7 message type field                     |
| GENERAL MSG HL7           | INVALID_HL7_MSG_TYPE_UNSUPPORTED  | Unsupported HL7 message type                                |
| GENERAL MSG HL7           | INVALID_HL7_MSG_FORMAT_INVALID    | Invalid HL7 message format                                  |
| GENERAL MSG HL7           | INVALID_HL7_MSG_VALIDATION        | General validation/parsing error                            |
| General                   | INVALID_MSG_MISSING_FIELD         | Required "field" is missing in message                      |
| General                   | INVALID_MSG_EQUIPMENT_MAPPING     | equipment was not found in the LIVD table for a given field |
| MISC                      | UNKNOWN                           | Error cannot be determined                                  |
| FIELD PARSE               | INVALID_MSG_PARSE_GENERAL         | Parse err for an unknown field type                         |
| FIELD PARSE               | INVALID_MSG_PARSE_TEXT            | Parse err for TEXT field type                               |
| FIELD PARSE               | INVALID_MSG_PARSE_TEXT_OR_BLANK   | Parse err for TEXT_OR_BLANK field type                      |
| FIELD PARSE               | INVALID_MSG_PARSE_NUMBER          | Parse err for PARSE_NUMBER field type                       |
| FIELD PARSE               | INVALID_MSG_PARSE_DATE            | Parse err for PARSE_DATE field type                         |
| FIELD PARSE               | INVALID_MSG_PARSE_DATETIME        | Parse err for PARSE_DATETIME field type                     |
| FIELD PARSE               | INVALID_MSG_PARSE_DURATION        | Parse err for DURATION field type                           |
| FIELD PARSE               | INVALID_MSG_PARSE_CODE            | Parse err for CODE field type                               |
| FIELD PARSE               | INVALID_MSG_PARSE_TABLE           | Parse err for TABLE field type                              |
| FIELD PARSE               | INVALID_MSG_PARSE_TABLE_OR_BLANK  | Parse err for TABLE_OR_BLANK field type                     |
| FIELD PARSE               | INVALID_MSG_PARSE_EI              | Parse err for EI field type                                 |
| FIELD PARSE               | INVALID_MSG_PARSE_HD              | Parse err for HD field type                                 |
| FIELD PARSE               | INVALID_MSG_PARSE_ID              | Parse err for ID field type                                 |
| FIELD PARSE               | INVALID_MSG_PARSE_ID_CLIA         | Parse err for ID_CLIA field type                            |
| FIELD PARSE               | INVALID_MSG_PARSE_ID_DLN          | Parse err for ID_DLN field type                             |
| FIELD PARSE               | INVALID_MSG_PARSE_ID_SSN          | Parse err for ID_SSN field type                             |
| FIELD PARSE               | INVALID_MSG_PARSE_ID_NPI          | Parse err for ID_NPI field type                             |
| FIELD PARSE               | INVALID_MSG_PARSE_STREET          | Parse err for STREET field type                             |
| FIELD PARSE               | INVALID_MSG_PARSE_STREET_OR_BLANK | Parse err for STREET_OR_BLANK field type                    |
| FIELD PARSE               | INVALID_MSG_PARSE_CITY            | Parse err for CITY field type                               |
| FIELD PARSE               | INVALID_MSG_PARSE_POSTAL_CODE     | Parse err for POSTAL_CODE field type                        |
| FIELD PARSE               | INVALID_MSG_PARSE_PERSON_NAME     | Parse err for PERSON_NAME field type                        |
| FIELD PARSE               | INVALID_MSG_PARSE_TELEPHONE       | Parse err for TELEPHONE field type                          |
| FIELD PARSE               | INVALID_MSG_PARSE_EMAIL           | Parse err for EMAIL field type                              |
| FIELD PARSE               | INVALID_MSG_PARSE_BLANK           | Parse err for BLANK field type                              |
