package gov.cdc.prime.router.validation

import org.hl7.fhir.instance.model.api.IBaseResource

class MarsOtcElrOnboardingValidator : AbstractItemValidator() {

    override val hl7ConformanceProfileLocation: String = "metadata/hl7_validation/v251/radxmars/onboarding"

    override fun validateFHIR(resource: IBaseResource): IItemValidationResult = NoopItemValidationResult()

    override val validatorProfileName: String = "RADx MARS Onboarding"
}