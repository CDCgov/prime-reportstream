package gov.cdc.prime.router.validation

import org.hl7.fhir.r4.model.Bundle

class MarsOtcElrValidator : AbstractMessageValidator() {

    override val hl7ConformanceProfileLocation: String = "metadata/hl7_validation/v251/radxmars"

    override fun validateFHIR(bundle: Bundle): IMessageValidationResult {
        return NoopMessageValidationResult()
    }
}