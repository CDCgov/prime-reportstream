package gov.cdc.prime.router.translation

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.soywiz.korte.Filter
import com.soywiz.korte.Template
import com.soywiz.korte.TemplateConfig
import gov.cdc.prime.router.encoding.getValue
import kotlinx.coroutines.runBlocking
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.InstantType
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun Terser.translate(mappings: List<FHIRtoHL7.Mapping>): Terser {
    mappings.forEach { mapping ->
        this.translate(mapping)
    }
    return this
}

fun Terser.translate(mapping: FHIRtoHL7.Mapping): Terser {
    try {
        this.set(mapping.hl7Path, mapping.value)
    } catch (e: HL7Exception) {
        throw IllegalStateException("${mapping.hl7Path}: ${e.message}", e)
    }
    return this
}

object FHIRtoHL7 {

    data class MappingTemplate(
        val fromFHIRPathResources: String,
        // the template for the value
        val valueTemplate: String,
        val toHL7Field: String,
    )

    data class Mapping(
        val hl7Path: String,
        val value: String,
    )

    val config = TemplateConfig()

    init {
        config.register(
            Filter("path") {
                val bundle = subject
                require(bundle is Bundle)
                val path = args[0]
                require(path is String)
                bundle.getValue(path)
            },
            Filter("datetime") {
                val date = subject
                requireNotNull(date)
                require(date is InstantType) {
                    "In filter function datetime: Incorrect date type ${date.javaClass.kotlin.qualifiedName}"
                }
                date.value.toInstant().atOffset(ZoneOffset.ofHours(date.getTimeZone().getOffset(date.value.getTime())))
            },
            Filter("formatDate") {
                val instant = subject
                requireNotNull(instant)
                require(instant is OffsetDateTime) {
                    "In filter function formatDate: Incorrect date type ${instant.javaClass.kotlin.qualifiedName}"
                }
                val pattern = args[0]
                require(pattern is String)
                val formatter = DateTimeFormatter.ofPattern(pattern)
                formatter.format(instant)
            },
            // TODO: This should be table based, there are a bunch of coded tables already
            Filter("hl7CodingSystem") {
                val system = subject
                requireNotNull(system)
                require(system is String) {
                    "In filter function hl7odingSystem: Incorrect date type ${system.javaClass.kotlin.qualifiedName}"
                }
                val systemMap = mapOf("http://loinc.org" to "LN")
                systemMap.get(system)
            },
        )
    }

    fun toORU_R01(bundle: Bundle): Message {
        val mappings = generateMapping(bundle, "./metadata/hl7_mapping/ORU_R01.yml")
        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "P")
        val terser = Terser(message)
        terser.translate(mappings)
        return message
    }

    fun generateMapping(bundle: Bundle, mappingFile: String): List<Mapping> {
        val translations = readMappings(mappingFile)
        return processMapping(translations, bundle)
    }

    fun readMappings(path: String): List<MappingTemplate> {
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

    fun processMapping(mappings: List<MappingTemplate>, bundle: Bundle): List<Mapping> {
        val processedMappings: MutableList<Mapping> = mutableListOf()
        mappings.forEach { mapping ->
            processedMappings.addAll(processMapping(mapping, bundle))
        }
        return processedMappings
    }

    fun processMapping(mapping: MappingTemplate, bundle: Bundle): List<Mapping> {
        val mappings: MutableList<Mapping> = mutableListOf()
        runBlocking {
            val hl7PathTemplate = Template(mapping.toHL7Field, config)
            val valueTemplate = Template(mapping.valueTemplate, config)
            val values = bundle.getValue(mapping.fromFHIRPathResources)
            require(values.size > 0) { "Failed to find elements for ${mapping.fromFHIRPathResources}" }
            values.forEachIndexed { index, resource ->
                val path = hl7PathTemplate(mapOf("index" to index))
                val value = valueTemplate(
                    mapOf(
                        "index" to index,
                        "resource" to resource,
                    )
                )
                mappings.add(
                    Mapping(
                        path,
                        value
                    )
                )
            }
        }
        return mappings
    }
}