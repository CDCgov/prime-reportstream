package gov.cdc.prime.router.fhirengine.translation.hl7.config

import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Receiver

/**
 * Top level configuration interface passed around with CustomContext
 */
sealed interface ContextConfig

/**
 * HL7 specific custom context configuration
 */
data class HL7TranslationConfig(
    val hl7Configuration: Hl7Configuration,
    val receiver: Receiver?
) : ContextConfig {
    val truncationConfig: TruncationConfig = hl7Configuration.truncationConfig
}