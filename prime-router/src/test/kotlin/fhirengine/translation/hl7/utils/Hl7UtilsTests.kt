package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ADT_A01
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import kotlin.test.Test

class Hl7UtilsTests {
    @Test
    fun `test get message type string`() {
        class Unsupported_ORU_R01 : ORU_R01()
        assertThat(HL7Utils.getMessageTypeString(ORU_R01())).isEqualTo(listOf("ORU", "R01"))
        assertThat(HL7Utils.getMessageTypeString(ADT_A01())).isEqualTo(listOf("ADT", "A01"))
        assertThat { HL7Utils.getMessageTypeString(Unsupported_ORU_R01()) }.isFailure()
    }

    @Test
    fun `test get message object`() {
        assertThat(HL7Utils.getMessage("ca.uhn.hl7v2.model.v251.message.ORU_R01")).isInstanceOf(ORU_R01::class.java)
        assertThat(HL7Utils.getMessage("ca.uhn.hl7v2.model.v251.message.ADT_A01")).isInstanceOf(ADT_A01::class.java)
        assertThat { HL7Utils.getMessage("java.lang.String") }.isFailure()
        assertThat { HL7Utils.getMessage("") }.isFailure()
    }

    @Test
    fun `test supports type`() {
        assertThat(HL7Utils.supports("ca.uhn.hl7v2.model.v251.message.ORU_R01")).isTrue()
        assertThat(HL7Utils.supports("ca.uhn.hl7v2.model.v251.message.ADT_A01")).isTrue()
        assertThat(HL7Utils.supports("java.lang.String")).isFalse()
        assertThat(HL7Utils.supports("")).isFalse()
    }

    @Test
    fun `test get message instance by type and version`() {
        val instance = HL7Utils.getMessageInstance("ca.uhn.hl7v2.model.v251.message.ORU_R01")
        assertThat(instance).isNotNull()
        instance.let {
            assertThat(it).isInstanceOf(Message::class.java)
            assertThat(it).isInstanceOf(ORU_R01::class.java)
            assertThat(it.encodingCharactersValue.length).isEqualTo(4)
            val terser = Terser(it)
            assertThat(terser.getSegment("MSH")).isNotNull()
            assertThat(terser.get("MSH-1")).isNotEmpty()
            assertThat(terser.get("MSH-2")).isNotEmpty()
            assertThat(terser.get("MSH-9-1")).isEqualTo("ORU")
            assertThat(terser.get("MSH-9-2")).isEqualTo("R01")
            assertThat(terser.get("MSH-9-3")).isEqualTo("ORU_R01")
            assertThat(terser.get("MSH-12")).isEqualTo("2.5.1")
        }

        assertThat { HL7Utils.getMessageInstance("java.lang.String") }.isFailure()
    }
}