package gov.cdc.prime.router.config.validation

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilterDefinition
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.HL7ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchemaElement
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathCustomResolver
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
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
    protected fun validFhirPath(path: String): Boolean {
        return FhirPathUtils.validatePath(path, customResolver)
    }
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
    private fun validateFilter(filter: String): Boolean {
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