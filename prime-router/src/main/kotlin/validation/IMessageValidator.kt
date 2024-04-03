package gov.cdc.prime.router.validation

import ca.uhn.hl7v2.model.Message
import gov.cdc.prime.router.fhirengine.engine.encodePreserveEncodingChars
import hl7.v2.validation.SyncHL7Validator
import hl7.v2.validation.ValidationContextBuilder
import org.hl7.fhir.r4.model.Bundle

interface IMessageValidator {

    fun validate(message: Any): Boolean
}

abstract class AbstractMessageValidator : IMessageValidator {

    companion object {
        const val ERROR_CLASSIFICATION = "Error"
        private val hl7Validators = mutableMapOf<String, SyncHL7Validator>()

        private val classLoader = Companion::class.java
        fun getHL7Validator(profileLocation: String): SyncHL7Validator {
            return hl7Validators.getOrElse(profileLocation) {
                (
                    classLoader.getResourceAsStream("$profileLocation/profile.xml")
                    ?: throw RuntimeException("profile.xml does not exist")
                ).use { profile ->

                    val contextBuilder = ValidationContextBuilder(profile)
                    classLoader.getResourceAsStream("$profileLocation/constraints.xml").use { constraints ->
                        contextBuilder.useConformanceContext(listOf(constraints))
                    }
                    classLoader.getResourceAsStream("$profileLocation/coconstraints.xml").use { coconstraints ->
                        contextBuilder.useCoConstraintsContext(coconstraints)
                    }
                    classLoader.getResourceAsStream("$profileLocation/slicings.xml").use { slicings ->
                        contextBuilder.useSlicingContext(slicings)
                    }
                    classLoader.getResourceAsStream("$profileLocation/value-sets.xml").use { valueSets ->
                        contextBuilder.useValueSetLibrary(valueSets)
                    }
                    classLoader.getResourceAsStream("$profileLocation/value-set-bindings.xml").use { valueSetBindings ->
                        contextBuilder.useVsBindings(valueSetBindings)
                    }

                    val validator = SyncHL7Validator(contextBuilder.validationContext)
                    hl7Validators[profileLocation] = validator
                    SyncHL7Validator(contextBuilder.validationContext)
                }
            }
        }
    }

    open val hl7ConformanceProfileLocation: String? = null

    override fun validate(message: Any): Boolean {
        if (message is Message && hl7ConformanceProfileLocation != null) {
            val validator = getHL7Validator(hl7ConformanceProfileLocation!!)
            return validateHL7(message, validator)
        }

        if (message is Bundle) {
            return validateFHIR(message)
        }

        throw RuntimeException("Message must be an HL7 message or a FHIR bundle")
    }

    open fun validateHL7(message: Message, validator: SyncHL7Validator): Boolean {
        val msgId = validator.profile().messages().keys().head()
        val report = validator.check(message.encodePreserveEncodingChars(), msgId)
        val errors = report.entries.values.flatten().filter { entry ->
            entry.classification == ERROR_CLASSIFICATION
        }
        return errors.isEmpty()
    }

    abstract fun validateFHIR(bundle: Bundle): Boolean
}

class NoopMessageValidator : AbstractMessageValidator() {

    override fun validateHL7(message: Message, validator: SyncHL7Validator): Boolean {
        return true
    }

    override fun validateFHIR(bundle: Bundle): Boolean {
        return true
    }
}

class MarsOtcElrValidator : AbstractMessageValidator() {

    override val hl7ConformanceProfileLocation: String = "metadata/hl7_validation/v251/radxmars"

    override fun validateFHIR(bundle: Bundle): Boolean {
        return true
    }
}