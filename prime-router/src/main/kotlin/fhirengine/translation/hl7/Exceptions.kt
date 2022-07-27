package gov.cdc.prime.router.fhirengine.translation.hl7

import gov.cdc.prime.router.fhirengine.translation.hl7.schema.ConfigSchemaElement

/**
 * Exception thrown when there are schema errors.
 */
class SchemaException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, e: Throwable) : super(message, e)
}

/**
 * Exception thrown when a required [element] has no value.
 */
class RequiredElementException(val element: ConfigSchemaElement) :
    Exception("Required element ${element.name} conditional was false or value was empty.")

/**
 * Exception thrown when there are conversion issues when writing values to an HL7 message.
 */
class HL7ConversionException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, e: Throwable) : super(message, e)
}