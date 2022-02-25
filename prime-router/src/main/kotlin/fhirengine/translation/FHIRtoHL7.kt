package gov.cdc.prime.router.translation

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.util.Terser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.ibm.icu.text.MessageFormat
import gov.cdc.prime.router.encoding.getValue
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Bundle
import java.io.File

fun Message.translate(bundle: Bundle, mapping: FHIRtoHL7.Mapping): Message {
    // val message = ORU_R01()
    val values = bundle.getValue<IBase>(mapping.fhirPath)
    val terser = Terser(this)
    values.forEachIndexed { index, resource ->
        val path = MessageFormat.format(mapping.hl7Path, mapOf("index" to index))
        val value = MessageFormat.format(
            mapping.value,
            mapOf(
                "offset" to index + 1,
                "index" to index,
                "resource" to resource,
            )
        )
        try {
            terser.set(path, value)
        } catch (e: HL7Exception) {
            throw IllegalStateException(path, e)
        }
    }
    return this
}

object FHIRtoHL7 {

    data class Mapping(
        val hl7Path: String,
        val fhirPath: String,
        // the template for the value
        val value: String,
    )

    fun readMappings(path: String): List<Mapping> {
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