package gov.cdc.prime.router.config.validation

import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.ReportStreamFilterDefinition
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathCustomResolver
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import io.konform.validation.Validation
import io.konform.validation.onEach
import kotlin.reflect.full.createInstance

/**
 * Container for the needed parts of the Konform validation definition
 */
object OrganizationValidation {

    // All potential Report Stream filter names
    private val allowedFilters = ReportStreamFilterDefinition::class
        .sealedSubclasses
        .map { it.createInstance() }
        .map { it.name }
        .flatMap {
            // list contains both camel-case and pascal-case
            listOf(
                it.replaceFirstChar(Char::uppercase),
                it.replaceFirstChar(Char::lowercase)
            )
        }

    // custom path resolver to ensure that custom-defined fhir functions are considered valid
    private val customResolver = FhirPathCustomResolver(CustomFhirPathFunctions())

    // Check to see if we have a valid filter structure
    private val validateFilter: (String) -> Boolean = { filter ->
        val isReportStreamFormat = allowedFilters.any { filter.startsWith(it) }
        // lazy to avoid doing this work if unnecessary
        val isFHIRPathFormat by lazy {
            runCatching {
                FhirPathUtils.pathEngine.hostServices = customResolver
                FhirPathUtils.parsePath(filter)
            }.isSuccess
        }

        isReportStreamFormat || isFHIRPathFormat
    }

    val validation: Validation<List<DeepOrganization>> = Validation {
        onEach {
            DeepOrganization::receivers onEach {
                Receiver::jurisdictionalFilter onEach {
                    addConstraint("Invalid jurisdictional filter format: {value}", test = validateFilter)
                }
                Receiver::qualityFilter onEach {
                    addConstraint("Invalid quality filter format: {value}", test = validateFilter)
                }
                Receiver::routingFilter onEach {
                    addConstraint("Invalid routing filter format: {value}", test = validateFilter)
                }
                Receiver::processingModeFilter onEach {
                    addConstraint("Invalid processing filter format: {value}", test = validateFilter)
                }
                Receiver::conditionFilter onEach {
                    addConstraint("Invalid condition filter format: {value}", test = validateFilter)
                }
            }
        }
    }
}