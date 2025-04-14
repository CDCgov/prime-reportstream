package gov.cdc.prime.router.config.validation

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilterDefinition
import gov.cdc.prime.router.config.validation.models.HL7ToFHIRMappingMessageTemplate
import gov.cdc.prime.router.config.validation.models.HL7ToFHIRMappingResourceTemplate
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathCustomResolver
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import io.github.linuxforhealth.api.Condition
import io.github.linuxforhealth.core.expression.condition.CheckNotNull
import io.github.linuxforhealth.core.expression.condition.CheckNull
import io.github.linuxforhealth.core.expression.condition.CompoundAndCondition
import io.github.linuxforhealth.core.expression.condition.CompoundAndOrCondition
import io.github.linuxforhealth.core.expression.condition.CompoundORCondition
import io.github.linuxforhealth.core.expression.condition.SimpleBiCondition
import io.github.linuxforhealth.core.expression.condition.SimpleBooleanCondition
import io.konform.validation.Validation
import io.konform.validation.onEach
import kotlin.reflect.full.createInstance

/**
 * Container for the needed parts of the Konform validation definition
 */
abstract class KonformValidation<T> {

    abstract val validation: Validation<T>

    // custom path resolver to ensure that custom-defined fhir functions are considered valid
    private val customResolver = FhirPathCustomResolver(CustomFhirPathFunctions())

    /**
     * Is the FHIR path valid?
     */
    protected fun validFhirPath(path: String): Boolean = FhirPathUtils.validatePath(path, customResolver)
}

/**
 * Validations for Organizations
 */
object OrganizationValidation : KonformValidation<List<DeepOrganization>>() {

    // All potential Report Stream filter names
    private val allowedFilters = ReportStreamFilterDefinition::class
        .sealedSubclasses
        .map { it.createInstance() }
        .map { it.name }
        .flatMap {
            // list contains both camelCase and PascalCase
            listOf(
                it.replaceFirstChar(Char::uppercase),
                it.replaceFirstChar(Char::lowercase)
            )
        }

    // Check to see if we have a valid filter structure
    fun validateFilter(filter: String): Boolean {
        val isReportStreamFormat = allowedFilters.any { filter.startsWith(it) }
        return isReportStreamFormat || validFhirPath(filter)
    }

    override val validation: Validation<List<DeepOrganization>> = Validation {
        onEach {
            DeepOrganization::receivers onEach {
                Receiver::jurisdictionalFilter onEach {
                    addConstraint("Invalid jurisdictional filter format: {value}", test = ::validateFilter)
                }
                Receiver::qualityFilter onEach {
                    addConstraint("Invalid quality filter format: {value}", test = ::validateFilter)
                }
                Receiver::routingFilter onEach {
                    addConstraint("Invalid routing filter format: {value}", test = ::validateFilter)
                }
                Receiver::processingModeFilter onEach {
                    addConstraint("Invalid processing filter format: {value}", test = ::validateFilter)
                }
                Receiver::conditionFilter onEach {
                    addConstraint("Invalid condition filter format: {value}", test = ::validateFilter)
                }
                addConstraint("Receiver must only configure one kind of condition filter", test = { receiver ->
                    !(receiver.conditionFilter.isNotEmpty() && receiver.mappedConditionFilter.isNotEmpty())
                })
            }
        }
    }
}

/**
 * Validations for FHIR to FHIR transforms
 */
object FhirToFhirTransformValidation : KonformValidation<FhirTransformSchema>() {

    override val validation: Validation<FhirTransformSchema> = Validation {
        FhirTransformSchema::elements onEach {
            FhirTransformSchemaElement::condition ifPresent {
                addConstraint("Invalid FHIR path: {value}", test = ::validFhirPath)
            }
            FhirTransformSchemaElement::resource ifPresent {
                addConstraint("Invalid FHIR path: {value}", test = ::validFhirPath)
            }
            FhirTransformSchemaElement::bundleProperty ifPresent {
                addConstraint("Invalid FHIR path: {value}", test = ::validFhirPath)
            }
            FhirTransformSchemaElement::value ifPresent {
                onEach {
                    addConstraint("Invalid FHIR path: {value}", test = ::validFhirPath)
                }
            }
            addConstraint("Value and function cannot both be set") { element ->
                !(element.value != null && element.function != null)
            }
        }
    }
}

/**
 * Validations for FHIR to HL7 transforms
 */
object FhirToHL7MappingValidation : KonformValidation<HL7ConverterSchema>() {

    override val validation: Validation<HL7ConverterSchema> = Validation {
        HL7ConverterSchema::elements onEach {
            ConverterSchemaElement::condition ifPresent {
                addConstraint("Invalid FHIR path: {value}", test = ::validFhirPath)
            }
            ConverterSchemaElement::resource ifPresent {
                addConstraint("Invalid FHIR path: {value}", test = ::validFhirPath)
            }
            ConverterSchemaElement::value ifPresent {
                onEach {
                    addConstraint("Invalid FHIR path: {value}", test = ::validFhirPath)
                }
            }
        }
    }
}

/**
 * Validations for HL7 to FHIR message
 */
object HL7ToFHIRMappingMessageTemplateValidation : KonformValidation<HL7ToFHIRMappingMessageTemplate>() {

    override val validation: Validation<HL7ToFHIRMappingMessageTemplate> = Validation {
        // noop at the moment
    }
}

/**
 * Validations for HL7 to FHIR resource
 */
object HL7ToFHIRMappingResourceTemplateValidation : KonformValidation<HL7ToFHIRMappingResourceTemplate>() {

    override val validation: Validation<HL7ToFHIRMappingResourceTemplate> = Validation {
        HL7ToFHIRMappingResourceTemplate::flatConditions onEach {
            addConstraint("Invalid format for condition variable: {value}", test = ::validateConditionFormatting)
        }
    }

    /**
     * Ensure all variables in conditions are formatted correctly.
     */
    private fun validateConditionFormatting(condition: Condition): Boolean = when (condition) {
            is CheckNotNull -> isFormatted(condition.var1)
            is CheckNull -> isFormatted(condition.var1)
            is SimpleBiCondition -> isFormatted(condition.var1)
            is CompoundAndCondition -> checkCompoundCondition(condition.conditions)
            is CompoundORCondition -> checkCompoundCondition(condition.conditions)
            is CompoundAndOrCondition -> checkCompoundCondition(condition.conditions)
            // these don't contain variables
            is SimpleBooleanCondition -> true
            // Condition is not sealed
            else -> throw IllegalArgumentException("Condition is of unrecognized type: ${condition.javaClass.name}")
        }

    private fun isFormatted(str: String): Boolean = str.startsWith("$")

    /**
     * recurses back over validateConditionFormatting for individual conditions
     */
    private fun checkCompoundCondition(conditions: List<Condition>): Boolean = conditions.fold(true) { acc, cur ->
            acc && validateConditionFormatting(cur)
        }
}