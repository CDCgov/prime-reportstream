package gov.cdc.prime.router.fhirengine.translation.fhir

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.soywiz.korte.Template
import com.soywiz.korte.TemplateConfig
import gov.cdc.prime.router.fhirengine.encoding.getValue
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import java.io.File

/**
 * A template that discribes how to build a mapping from FHIR to another format
 *
 * @property fromFHIRPathResources The fhir path to use as the source resources for the template.
 * @property valueTemplate The template string that will be used to compute a resulting value for each source fhir resource.
 * @property toFieldTemplate the template string that will be used to compute the resulting field name for the destination format.
 */
data class MappingTemplate(
    val fromFHIRPathResources: String,
    // the template for the value
    val valueTemplate: String,
    val toFieldTemplate: String,
) {

    /**
     * Compile a template to make it ready for processing.
     *
     * @param bundle The input FHIR bundle to be converted.
     * @param config The TemplateConfig to be used in compiling, this includes things like functions that can be used within the template.
     */
    fun compile(bundle: Bundle, config: TemplateConfig): PrepairedMappingTemplate {
        val resources = bundle.getValue(fromFHIRPathResources)
        require(resources.size > 0) { "Failed to find elements for $fromFHIRPathResources" }
        return runBlocking {
            PrepairedMappingTemplate(
                resources,
                Template(valueTemplate, config),
                Template(toFieldTemplate, config),
            )
        }
    }

    companion object {
        /**
         * Load a set of MappingTemplates from a YAML file.
         *
         * @param path The path to the YAML file.
         */
        fun loadTemplate(path: String): List<MappingTemplate> {
            val mapper = ObjectMapper(YAMLFactory()).registerModule(
                KotlinModule.Builder()
                    .withReflectionCacheSize(512)
                    .configure(KotlinFeature.NullToEmptyCollection, false)
                    .configure(KotlinFeature.NullToEmptyMap, false)
                    .configure(KotlinFeature.NullIsSameAsDefault, false)
                    .configure(KotlinFeature.StrictNullChecks, false)
                    .build()
            )
            return mapper.readValue(File(path))
        }
    }
}

/**
 * The prepaired mapping template, ready to be used to create a new format.file
 *
 * At this stage a list of FHIR resources is ready
 * to be used by two templates to compute the resulting field : value mapping.
 *
 * @property fromFHIRPathResources The list of fhir resources computed from the FHIR path.
 * @property valueTemplate The compiled template for the destination value ready to be used.
 * @property fieldTemplate The compiled template for the destination field ready to be used.
 */
data class PrepairedMappingTemplate(
    val fromFHIRPathResources: List<Base>,
    val valueTemplate: Template,
    val fieldTemplate: Template,
) {

    /**
     * Process the PerpairedMappingTemplate into a list of Mappings that have explicit field and value values
     */
    fun process(): List<Mapping> {
        // NOTE could make `process` async and move runBlocking up
        return runBlocking {
            fromFHIRPathResources.mapIndexed { index, resource ->
                val field = fieldTemplate(mapOf("index" to index))
                val value = valueTemplate(
                    mapOf(
                        "index" to index,
                        "resource" to resource,
                    )
                )
                Mapping(
                    field,
                    value
                )
            }
        }
    }
}

/**
 * A mapping showing a field and the value for it.
 *
 * This works for any destination type where a property can be identified by a string, e.g. a uri, FHIR path, HL7 path, CSV header.
 *
 * @property field The identifier for the property to set in the destination format.
 * @property value The value to set in the destination format.
 */
data class Mapping(
    val field: String,
    val value: String,
) {
    companion object {
        /**
         * Generate a set of mappings from a bundle and a set of templates
         *
         * @param templates The list of MappingTemplates to use.
         * @param bundle The FHIR Bundle to use as the source data.
         * @param config The TemplateConfig to use for compilation.
         */
        fun generate(templates: List<MappingTemplate>, bundle: Bundle, config: TemplateConfig): List<Mapping> {
            return templates.flatMap { template ->
                val prepairedMapping = template.compile(bundle, config)
                prepairedMapping.process()
            }
        }
    }
}

/**
 * An interface for FHIRto<Format> Translators
 *
 * @param T The expected output format, e.g. an HL7 Message
 */
interface FHIRtoMap<T> {

    /**
     * A TemplateConfig to use for the Translator.
     */
    val config: TemplateConfig

    /**
     * A path to the template YML file to get the mappings from.
     */
    val templatePath: String

    /**
     * A function to convert a set of mappings into the expected format
     *
     * @param mappings A list of mappings to use in the translation.
     */
    fun translate(mappings: List<Mapping>): T

    /**
     * Translate a bundle into the expected format
     *
     * @param bundle The FHIR Bundle to use as the source data
     */
    fun translate(bundle: Bundle): T {
        val mappings = MappingTemplate
            .loadTemplate(templatePath)
            .generateMappings(bundle)
        return translate(mappings)
    }

    /**
     * A convenience function to generate a list of Mappings from a list of MappingTemplates
     *
     * @param bundle The FHIR Bundle to use as the source data.
     */
    fun List<MappingTemplate>.generateMappings(bundle: Bundle): List<Mapping> {
        return Mapping.generate(this, bundle, config)
    }
}