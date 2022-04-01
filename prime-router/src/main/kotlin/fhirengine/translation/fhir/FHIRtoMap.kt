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

data class MappingTemplate(
    val fromFHIRPathResources: String,
    // the template for the value
    val valueTemplate: String,
    val toFieldTemplate: String,
) {
    fun process(bundle: Bundle, config: TemplateConfig): PrepairedMappingTemplate {
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

data class PrepairedMappingTemplate(
    val fromFHIRPathResources: List<Base>,
    // the template for the value
    val valueTemplate: Template,
    val fieldTemplate: Template,
) {
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

// NOTE: This is HL7 specific, but could be mirroed by a FHIR->CSV translator
// The only difference would be how the equivalent of hl7Field was interpreted.
data class Mapping(
    val field: String,
    val value: String,
) {
    companion object {
        fun generate(templates: List<MappingTemplate>, bundle: Bundle, config: TemplateConfig): List<Mapping> {
            return templates.flatMap { template ->
                val prepairedMapping = template.process(bundle, config)
                prepairedMapping.process()
            }
        }
    }
}

interface FHIRtoMap<T> {

    val config: TemplateConfig
    val template: String

    fun translate(bundle: Bundle): T {
        val mappings = MappingTemplate
            .loadTemplate(template)
            .generateMappings(bundle)
        return translate(mappings)
    }

    fun translate(mappings: List<Mapping>): T

    fun List<MappingTemplate>.generateMappings(bundle: Bundle): List<Mapping> {
        return Mapping.generate(this, bundle, config)
    }
}