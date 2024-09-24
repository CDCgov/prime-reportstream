package gov.cdc.prime.reportstream.shared

import ca.uhn.hl7v2.model.Message

/**
 * Configuration class that contains details on how to parse an HL7 message and then how
 * to convert it to FHIR
 *
 * @param messageModelClass a class that inherits from [Message]
 * @param hl7toFHIRMappingLocation the location of the mappings files to convert the message to FHIR
 */
data class HL7MessageParseAndConvertConfiguration(
    val messageModelClass: Class<out Message>,
    val hl7toFHIRMappingLocation: String,
)