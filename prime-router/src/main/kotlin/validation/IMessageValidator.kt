package gov.cdc.prime.router.validation

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.validation.ValidationResult
import ca.uhn.hl7v2.model.Message
import gov.cdc.prime.router.fhirengine.engine.encodePreserveEncodingChars
import gov.nist.validation.report.Report
import hl7.v2.validation.SyncHL7Validator
import hl7.v2.validation.ValidationContextBuilder
import org.hl7.fhir.r4.model.Bundle

interface IMessageValidator {

    fun validate(message: Any): IMessageValidationResult
}

interface IMessageValidationResult {
    fun isValid(): Boolean
}

data class HL7ValidationResult(val rawReport: Report) : IMessageValidationResult {
    override fun isValid(): Boolean {
        val errors = rawReport.entries.values.flatten().filter { entry ->
            entry.classification == AbstractMessageValidator.ERROR_CLASSIFICATION
        }
        return errors.isEmpty()
    }
}

data class FHIRValidationResult(val rawValidationResult: ValidationResult) : IMessageValidationResult {
    override fun isValid(): Boolean {
        return rawValidationResult.isSuccessful
    }
}

abstract class AbstractMessageValidator : IMessageValidator {

    companion object {
        const val ERROR_CLASSIFICATION = "Error"
        private val hl7Validators = mutableMapOf<String, SyncHL7Validator>()

        private val classLoader = this::class.java.classLoader
        fun getHL7Validator(profileLocation: String): SyncHL7Validator {
            return hl7Validators.getOrElse(profileLocation) {
                (
                        classLoader.getResourceAsStream("$profileLocation/profile.xml")
                            ?: throw RuntimeException("profile.xml does not exist")
                        ).use { profile ->

                        val contextBuilder = ValidationContextBuilder(profile)
                        classLoader.getResourceAsStream("$profileLocation/constraints.xml").use { constraints ->
                            if (constraints != null) {
                                contextBuilder.useConformanceContext(listOf(constraints))
                            }
                        }
                        classLoader.getResourceAsStream("$profileLocation/coconstraints.xml").use { coconstraints ->
                            if (coconstraints != null) {
                                contextBuilder.useCoConstraintsContext(coconstraints)
                            }
                        }
                        classLoader.getResourceAsStream("$profileLocation/slicings.xml").use { slicings ->
                            if (slicings != null) {
                                contextBuilder.useSlicingContext(slicings)
                            }
                        }
                        classLoader.getResourceAsStream("$profileLocation/value-sets.xml").use { valueSets ->
                            if (valueSets != null) {
                                contextBuilder.useValueSetLibrary(valueSets)
                            }
                        }
                        classLoader.getResourceAsStream("$profileLocation/value-set-bindings.xml")
                            .use { valueSetBindings ->
                                if (valueSetBindings != null) {
                                    contextBuilder.useVsBindings(valueSetBindings)
                                }
                            }

                        val validator = SyncHL7Validator(contextBuilder.validationContext)
                        hl7Validators[profileLocation] = validator
                        validator
                    }
            }
        }
    }

    open val hl7ConformanceProfileLocation: String? = null

    override fun validate(message: Any): IMessageValidationResult {
        if (message is Message && hl7ConformanceProfileLocation != null) {
            val validator = getHL7Validator(hl7ConformanceProfileLocation!!)
            return validateHL7(message, validator)
        }

        if (message is Bundle) {
            return validateFHIR(message)
        }

        throw RuntimeException("Message must be an HL7 message or a FHIR bundle")
    }

    open fun validateHL7(message: Message, validator: SyncHL7Validator): IMessageValidationResult {
        val msgId = validator.profile().messages().keys().head()
        val report = validator.check(message.encodePreserveEncodingChars(), msgId)
        return HL7ValidationResult(report)
    }

    open fun validateFHIR(bundle: Bundle): IMessageValidationResult {
        val ctx = FhirContext.forR4()
        val validator = ctx.newValidator()
        val result = validator.validateWithResult(bundle)
        return FHIRValidationResult(result)
    }
}