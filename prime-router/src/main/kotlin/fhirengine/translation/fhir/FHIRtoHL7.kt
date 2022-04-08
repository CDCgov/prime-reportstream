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

/**
 * The path to the ORU_R01 template YML file
 */
const val ORU_R01_Template = "./metadata/hl7_mapping/ORU_R01.yml"

/**
 * A translator for a FHIR Bundle to -> HL7 Message
 *
 * @property templatePath The template YML file to use for translation defaults to ORU_R01
 */
class FHIRtoHL7(
    override val templatePath: String = ORU_R01_Template
) : FHIRtoMap<Message> {

    override val config = TemplateConfig()

    init {
        config.register(
            /**
             * Convert a FHIR InstantType to an OffsetDateTime
             */
            Filter("datetime") {
                val date = subject
                requireNotNull(date)
                require(date is InstantType) {
                    "In filter function datetime: Incorrect date type ${date.javaClass.kotlin.qualifiedName}"
                }
                date.value.toInstant().atOffset(ZoneOffset.ofHours(date.getTimeZone().getOffset(date.value.getTime())))
            },
            /**
             * Convert an OFfsetDateTime to an HL7 Formated date string
             */
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
            /**
             * Map an hl7 coding system to a fhir one.
             *
             * https://hl7-definition.caristix.com/v2/HL7v2.5/Tables/0396
             */
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

    /**
     * Translates a list of Mappings into an HL7 ORU_R01 Message
     *
     * @param mappings a list of Mappings containing the destination fields and values
     */
    override fun translate(mappings: List<Mapping>): Message {
        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "P")
        message.populate(mappings)
        return message
    }

    /**
     * Populate an HL7 Message with the values from a list of Mappings
     *
     * expects the Mappings `field` property to use proper HL7 syntax usable by a terser.
     * https://hapifhir.github.io/hapi-hl7v2/base/apidocs/ca/uhn/hl7v2/util/Terser.html
     *
     * @param mappings a list of Mappings containing the destination fields and values
     */
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