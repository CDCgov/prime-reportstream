package gov.cdc.prime.router.fhirengine.config

import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.fhirengine.translation.hl7.config.ContextConfig
import gov.cdc.prime.router.fhirengine.translation.hl7.config.TruncationConfig

/**
 * HL7 specific custom context configuration
 */
data class HL7TranslationConfig(
    val hl7Configuration: Hl7Configuration,
    val receiver: Receiver?
) : ContextConfig {
    val truncationConfig: TruncationConfig = hl7Configuration.truncationConfig
}