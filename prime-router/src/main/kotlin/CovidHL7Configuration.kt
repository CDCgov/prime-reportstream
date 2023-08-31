package gov.cdc.prime.router

import gov.cdc.prime.router.fhirengine.translation.hl7.utils.HL7TranslationConfig

data class CovidHL7Configuration(
    val hl7Configuration: Hl7Configuration,
    val receiver: Receiver?
) : HL7TranslationConfig {
    override val truncationConfig = hl7Configuration.truncationConfig
}