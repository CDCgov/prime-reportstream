package gov.cdc.prime.router.validation

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.junit.jupiter.api.Test

class MarsOtcElrValidatorTests {

    private val validator = MarsOtcElrValidator()

    @Test
    fun `test can detect an invalid RADxMARS message`() {
        val sampleMessageInputStream =
            this.javaClass.classLoader.getResourceAsStream("validation/marsotcelr/sample_2.hl7")

        val sampleMessage = sampleMessageInputStream!!.bufferedReader().use { it.readText() }
        val message = HL7Reader.parseHL7Message(sampleMessage, null)
        val report = validator.validate(message)
        assertThat(report.isValid()).isFalse()
    }

    @Test
    fun `test valid RADxMARS message`() {
        val sampleMessageInputStream =
            this.javaClass.classLoader.getResourceAsStream("validation/marsotcelr/valid.hl7")

        val sampleMessage = sampleMessageInputStream!!.bufferedReader().use { it.readText() }
        val message = HL7Reader.parseHL7Message(sampleMessage, null)
        val report = validator.validate(message)
        assertThat(report.isValid()).isTrue()
    }

    @Test
    fun `test valid RADxMARS message with NIST invalid MSH-5-1, MSH-5-2, MSH-6-1, MSH-6-2 `() {
        val sampleMessageInputStream =
            this.javaClass.classLoader.getResourceAsStream("validation/marsotcelr/valid_altered_msh.hl7")

        val sampleMessage = sampleMessageInputStream!!.bufferedReader().use { it.readText() }
        val messages = HL7Reader(ActionLogger()).getMessages(sampleMessage)
        val report = validator.validate(messages[0])
        assertThat(report.isValid()).isTrue()
    }
}