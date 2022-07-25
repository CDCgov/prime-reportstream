package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import kotlin.test.Test

class Hl7UtilsTests {
    @Test
    fun `test get message instance from enum value`() {
        assertThat(HL7Utils.SupportedMessages.ORU_R01_2_5_1.getMessageInstance()).isInstanceOf(ORU_R01::class.java)
    }

    @Test
    fun `test supports type`() {
        assertThat(HL7Utils.SupportedMessages.supports("ORU_R01", "2.5.1")).isTrue()
        assertThat(HL7Utils.SupportedMessages.supports("ORU_R01", "3")).isFalse()
        assertThat(HL7Utils.SupportedMessages.supports("ORU_R01", "SOMEVERSION")).isFalse()
        assertThat(HL7Utils.SupportedMessages.supports("ORU_R01", "")).isFalse()
        assertThat(HL7Utils.SupportedMessages.supports("ORU_R02", "2.5.1")).isFalse()
        assertThat(HL7Utils.SupportedMessages.supports("VAT", "2.5.1")).isFalse()
        assertThat(HL7Utils.SupportedMessages.supports("", "2.5.1")).isFalse()
        assertThat(HL7Utils.SupportedMessages.supports("VAT", "1")).isFalse()
        assertThat(HL7Utils.SupportedMessages.supports("", "")).isFalse()
    }

    @Test
    fun `test get message instance by type and version`() {
        val instance = HL7Utils.SupportedMessages.getMessageInstance("ORU_R01", "2.5.1")
        assertThat(instance).isNotNull()
        instance?.also {
            assertThat(it).isInstanceOf(Message::class.java)
            assertThat(it).isInstanceOf(ORU_R01::class.java)
            assertThat(it.encodingCharactersValue.length).isEqualTo(4)
        }

        assertThat(HL7Utils.SupportedMessages.getMessageInstance("ORU_R01", "3")).isNull()
        assertThat(HL7Utils.SupportedMessages.getMessageInstance("VAT", "2.5.1")).isNull()
    }

    @Test
    fun `test get support list as string`() {
        assertThat(HL7Utils.SupportedMessages.getSupportedListAsString()).isNotEmpty()
    }
}