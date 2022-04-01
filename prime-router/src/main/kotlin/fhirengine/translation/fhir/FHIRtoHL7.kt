package gov.cdc.prime.router.fhirengine.translation.fhir

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import com.soywiz.korte.Filter
import com.soywiz.korte.TemplateConfig
import gov.cdc.prime.router.fhirengine.encoding.getValue
import org.hl7.fhir.r4.model.InstantType
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

const val ORU_R01_Template = "./metadata/hl7_mapping/ORU_R01.yml"

class FHIRtoHL7(
    override val template: String = ORU_R01_Template
) : FHIRtoMap<Message> {

    override val config = TemplateConfig()

    init {
        config.register(
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

    override fun translate(mappings: List<Mapping>): Message {
        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "P")
        message.populate(mappings)
        return message
    }

    fun Message.populate(mappings: List<Mapping>) {
        val terser = Terser(this)
        mappings.forEach { mapping ->
            try {
                terser.set(mapping.field, mapping.value)
            } catch (e: HL7Exception) {
                throw IllegalStateException("${mapping.field}: ${e.message}", e)
            }
        }
    }
}