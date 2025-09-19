package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import org.apache.logging.log4j.kotlin.KotlinLogger
import org.apache.logging.log4j.kotlin.Logging
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Base64BinaryType
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DiagnosticReport
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.MarkdownType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.OidType
import org.hl7.fhir.r4.model.Property
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.model.UriType
import org.hl7.fhir.r4.model.UrlType
import org.hl7.fhir.r4.model.UuidType
import java.util.stream.Collectors
import java.util.stream.Stream

object FhirBundleUtils : Logging {
    private enum class StringCompatibleType(val typeAsString: kotlin.String) {
        Base64Binary("base64Binary"),
        Canonical("canonical"),
        Code("code"),
        Date("date"),
        DateTime("dateTime"),
        Id("id"),
        Instant("instant"),
        Integer("integer"),
        Markdown("markdown"),
        Oid("oid"),
        String("string"),
        Time("time"),
        Uri("uri"),
        Url("url"),
        Uuid("uuid"),
    }

    /**
     * Converts a [value] of type [sourceType] into a compatible Base of type [targetType]. Returns the original value
     * and logs an error if the conversion is not supported.
     */
    fun convertFhirType(
        value: Base,
        sourceType: String,
        targetType: String,
        logger: KotlinLogger = this.logger,
    ): Base {
        return if (sourceType == targetType || targetType == "*") {
            value
        } else if (targetType.contains("|")) {
            val targetTypes = targetType.split("|")
            targetTypes.forEach {
                if (sourceType == it || it == "*") {
                    return value
                }
                val attemptedValue = getValue(it, value, logger, sourceType, false)
                if (attemptedValue != null) {
                    return attemptedValue
                }
            }
            value
        } else if (StringCompatibleType.values().any { it.typeAsString == sourceType }) {
            getValue(targetType, value, logger, sourceType, true) ?: value
        } else {
            logger.debug("Conversion between $sourceType and $targetType not yet implemented.")
            value
        }
    }

    /**
    Determines if the [targetType] matches any of the options and, if it does, returns the [value] as that type. If not, uses the [logger] to log the [sourceType] and [targetType]. Will also log this error if there is an issue with compatibility and the [logIncompatibilityErrors] is true.
     */
    private fun getValue(
        targetType: String,
        value: Base,
        logger: KotlinLogger,
        sourceType: String,
        logIncompatibilityErrors: Boolean,
    ) = try {
        when (targetType) {
            StringCompatibleType.Base64Binary.typeAsString -> Base64BinaryType(value.primitiveValue())
            StringCompatibleType.Canonical.typeAsString -> CanonicalType(value.primitiveValue())
            StringCompatibleType.Code.typeAsString -> CodeType(value.primitiveValue())
            StringCompatibleType.Date.typeAsString -> DateType(value.primitiveValue())
            StringCompatibleType.DateTime.typeAsString -> DateTimeType(value.primitiveValue())
            StringCompatibleType.Id.typeAsString -> IdType(value.primitiveValue())
            StringCompatibleType.Instant.typeAsString -> InstantType(value.primitiveValue())
            StringCompatibleType.Markdown.typeAsString -> MarkdownType(value.primitiveValue())
            StringCompatibleType.Oid.typeAsString -> OidType(value.primitiveValue())
            StringCompatibleType.String.typeAsString -> StringType(value.primitiveValue())
            StringCompatibleType.Time.typeAsString -> TimeType(value.primitiveValue())
            StringCompatibleType.Uri.typeAsString -> UriType(value.primitiveValue())
            StringCompatibleType.Url.typeAsString -> UrlType(value.primitiveValue())
            StringCompatibleType.Uuid.typeAsString -> UuidType(value.primitiveValue())
            else -> {
                logger.debug("Conversion between $sourceType and $targetType not supported.")
                null
            }
        }
    } catch (e: Exception) {
        if (logIncompatibilityErrors) {
            logger.debug("Conversion between $sourceType and $targetType not supported.")
        }
        null
    }

    /**
     * Deletes a [resource] from a bundle, removes all references to the [resource] and any orphaned children.
     * If the [resource] being deleted is an [Observation] and that results in diagnostic reports having no
     * observations, the [DiagnosticReport] will be deleted
     */
    fun Bundle.deleteResource(resource: Base, removeOrphanedDiagnosticReport: Boolean = true) {
        val referencesToClean = mutableSetOf<String>()

        // build up all resources and their references in a map as a starting point
        fun generateAllReferencesMap() = this.entry.associate {
            it.fullUrl to it.getResourceReferences()
        }

        // recursive function to delete resource and orphaned children
        fun deleteResourceInternal(
            resourceInternal: Base,
            referencesMap: Map<String, List<String>> = generateAllReferencesMap(),
        ) {
            if (this.entry.find { it.fullUrl == resourceInternal.idBase } == null) {
                throw IllegalStateException("Cannot delete resource. FHIR bundle does not contain this resource")
            }

            // First remove the resource from the bundle
            this.entry.removeIf { it.fullUrl == resourceInternal.idBase }

            // add resource to set of references to clean up after recursion
            referencesToClean.add(resourceInternal.idBase)

            // Get the resource children references
            val resourceChildren = resourceInternal.getResourceReferences()

            // get all resources except the resource being removed and stick it in a map keyed off the fullUrl
            val allResources = this.entry.associateBy { it.fullUrl }

            // get all references for every remaining resource
            val remainingReferences = referencesMap - resourceInternal.idBase
            val flatRemainingReferences = remainingReferences.flatMap { it.value }.toSet()

            // remove orphaned children
            resourceChildren.forEach { child ->
                if (!flatRemainingReferences.contains(child)) {
                    allResources[child]?.let { entryToDelete ->
                        deleteResourceInternal(entryToDelete.resource, remainingReferences)
                    }
                }
            }
        }

        // Go through every resource and check if the resource has a reference to the resource being deleted
        fun cleanUpReferences() {
            this.entry
                .map { it.resource }
                .forEach { res ->
                    res.children().forEach { child ->
                        child
                            .values
                            .filterIsInstance<Reference>()
                            .filter { referencesToClean.contains(it.reference) }
                            .forEach { it.reference = null }
                    }
                }

            referencesToClean.clear()
        }

        // find diagnostic reports without any observations contained in the result field and delete
        fun cleanUpEmptyDiagnosticReports() {
            val diagnosticReportsToDelete = this.entry
                .map { it.resource }
                .filterIsInstance<DiagnosticReport>()
                .filter { it.result.none { it.reference != null } }

            diagnosticReportsToDelete.forEach { deleteResourceInternal(it) }
        }

        // delete provided resource and all references to it
        deleteResourceInternal(resource)
        cleanUpReferences()

        // The original use case of this function was just to remove Observations from a bundle
        // but has since expanded so this behavior is opt-in
        // TODO: Remove as part of https://github.com/CDCgov/prime-reportstream/issues/14568
        if (removeOrphanedDiagnosticReport) {
            // clean up empty Diagnostic Reports and references to them
            cleanUpEmptyDiagnosticReports()
        }

        cleanUpReferences()
    }

    /**
     * Gets all properties for a [Base] resource recursively and filters only its references
     *
     * @return a list of reference identifiers for a [Base] resource
     *
     */
    fun Base.getResourceReferences(): List<String> =
        filterReferenceProperties(this.getResourceProperties())

    /**
     * Gets all properties for a [Base] resource recursively
     *
     * @return a list of all [Property] for a [Base] resource
     */
    fun Base.getResourceProperties(): List<Property> = this.children().stream().flatMap {
        getChildProperties(it)
    }.collect(Collectors.toList())

    /*
     * The following was moved from the FhirBundleHelpers companion object, which carried with it a note that it existed
     * only to prevent an out of memory error that occurred within unit tests. See the note there for details.
     */

    /**
     * Filters the [properties] by only properties that have a value and are of type [Reference]
     *
     * @return a list containing only the references in [properties]
     */
    fun filterReferenceProperties(properties: List<Property>): List<String> = properties
        .filter { it.hasValues() }
        .flatMap { it.values }
        .filterIsInstance<Reference>()
        .map { it.reference }

    /**
     * Gets all child properties for a resource [property] recursively
     *
     * @return a flatmap stream of all child properties on a [property]
     */
    fun getChildProperties(property: Property): Stream<Property> = Stream.concat(
        Stream.of(property),
        property.values.flatMap { it.children() }.stream().flatMap { getChildProperties(it) }
    )
}