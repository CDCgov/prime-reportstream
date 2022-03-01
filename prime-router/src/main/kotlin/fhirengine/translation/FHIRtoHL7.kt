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
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Bundle
import java.io.File

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
        throw IllegalStateException(mapping.hl7Path, e)
    }
    return this
}

object FHIRtoHL7 {

    data class MappingTemplate(
        val hl7Path: String,
        val fhirPath: String,
        // the template for the value
        val value: String,
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
                bundle.getValue<IBase>(path)
            }
        )
    }

    fun toORU_R01(bundle: Bundle): Message {
        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "P")
        val terser = Terser(message)
        val translations = readMappings("./metadata/hl7_mapping/ORU_R01.yml")
        val mappings = processMapping(translations, bundle)
        terser.translate(mappings)
        return message
    }

    fun templateHL7(bundle: Bundle): String {
        return runBlocking {
            val templateFile = File("./metadata/hl7_templates/ORU_R01.hl7")
            val template = Template(templateFile.readText(), config)
            template(mapOf("bundle" to bundle))
        }
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
            val hl7PathTemplate = Template(mapping.hl7Path)
            val valueTemplate = Template(mapping.value)
            val values = bundle.getValue<IBase>(mapping.fhirPath)
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

    /*
    fun translate(resource: Resource): Segment {

    }
    */
}