# Serializer Error Codes

December 2022

This document came out of GitHub issue [#7453](https://github.com/CDCgov/prime-reportstream/issues/7453), which 
originally contained, and may still contain, some information found in this document.

## Problem
When ReportStream processes a particular message, the library/libraries we use to parse the message can throw
errors that are NOT helpful to the end user. This can best be noticed in ReportStream's "Validate" functionality
where a user can upload a file containing message(s) and the front-end will show all validation errors that
occurred so that, presumably, the end-user can discover exactly what changes are required for their file to
be "valid." Presently, these error messages are sometimes just long stack traces, passed by the back-end directly to
the front-end without any cleaning. These types of errors are not helpful to the end-user because they are hard to 
read and often times don't give the user enough information to fix the issue.

## Solution
ReportStream should catch exceptions thrown by third-party libraries and wrap them in a specific error code
that the front-end can map to a nice, helpful message for the end-user. For example, ReportStream can have 
an error code for when an exception gets thrown when processing a date field, say `INVALID_HL7_MESSAGE_DATE_VALIDATION`.
The front-end would then know to show the user a helpful message regarding what date formats are acceptable, instead of
showing a stack-trace.

In addition to third-party library exceptions, ReportStream can also run into issues when trying to load parsed
values into Objects. ReportStream should handle this case gracefully as well. This will be elaborated on down below and
the process of loading a parsed value into an Object will be referred to as "normalization," to match the language 
used in the code.

# Design

Please note, the following design will be specific to HL7 but should be expandable enough for other formats as well.

## Action Messages

ReportStream defines various error/warning messages in `ActionMessages` which eventually get sent to the front-end
as JSON. These messages can be expanded to contain a new field, called `errorCode`, along with any other extra metadata
that would be helpful for the front-end. The front-end can then use the errorCode and other metadata in the messages to
calculate and display user-friendly messages.

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

### Field Type Errors

Field Type Errors can occur either from the HAPI library or when prime-router tries to normalize a field in a
HAPI-parsed message. In the latter case, HAPI was okay with it, but it is not "normalizable" to whatever data type 
ReportStream has associated with that field.

#### HAPI Parser Error

Since HL7Exceptions, the type of exception thrown by HAPI, contain the field type, via location property, 
we can create a `FieldPrecisionMessage` error and set `fieldMapping` to `e.location`, `message` to `e.localizedMessage`, 
and `errorCode` to a response code that is specific for the HL7 field's data type, like "date" or "phone-number".

Current Catch Logic (Works only for PID-29):
```
  if (e.location?.toString() == "PID-29(0)") {
      errors.add(
          FieldPrecisionMessage(
              e.location.toString(),
              "Error parsing HL7 message: ${e.localizedMessage}",
              ErrorType.INVALID_HL7_MESSAGE_DATE_VALIDATION.type
          )
      )
  }
```

Proposed Catch Logic (works for all fields):
```
errors.add(
    FieldPrecisionMessage(
        e.location.toString(),
        "Error parsing HL7 message: ${e.localizedMessage}",
        ErrorType.INVALID_HL7_MESSAGE_<TYPE>_VALIDATION.type
    )
)
```

#### PrimeRouter Normalization Error (429 - 448)

When extracting a value from an HL7 field, in order to put it in the schema map, ReportStream can run into various 
exceptions. For example, a phone number may be formatted incorrectly and the third-party phone number processing
library ReportStream uses, google, cannot load the value.

`Element.toNormalized` is what ultimately throws the exception if ReportStream cannot "normalize" a certain value based 
on its supposed type. Currently, there is special logic for normalizing the following types:

- BLANK
- DATE
- DATETIME
- CODE
- TELEPHONE
- POSTAL_CODE
- HD
- EI

Instead of throwing a simple `IllegalStateException` like it does now, ReportStream should create a new 
exception: `ElementNormalizeException`:

```
data class ElementNormalizeException(
override val message: String,
  val field: String,
  val errorCode: ErrorType
) : Exception(message)
```

Where `errorCode` can be specific to the type, like `ErrorType.INVALID_HL7_PHONE_NUMBER` and `field` would be the 
HL7 field in question, like ORC-23

This exception can be thrown from inside `Element.toNormalized` and caught in the catch in `convertMessageToMap`. 
The catch would then be simplified to just wrap the exception in a `FieldPrecisionMessage`:

```
errors.add(
  FieldPrecisionMessage(
    e.field,
    e.message,
    e.errorCode.type
  )
)
```
