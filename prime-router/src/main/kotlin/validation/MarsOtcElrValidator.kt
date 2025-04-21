package gov.cdc.prime.router.validation

import org.hl7.fhir.r4.model.Bundle

class MarsOtcElrValidator : AbstractItemValidator() {

    override val hl7ConformanceProfileLocation: String = "metadata/hl7_validation/v251/radxmars/production"

    override fun validateFHIR(bundle: Bundle): IItemValidationResult = NoopItemValidationResult()

    override val validatorProfileName: String = "RADx MARS"
}