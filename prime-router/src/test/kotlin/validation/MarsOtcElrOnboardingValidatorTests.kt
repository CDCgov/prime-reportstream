package gov.cdc.prime.router.validation

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.junit.jupiter.api.Test
import gov.cdc.prime.reportstream.shared.validation.MarsOtcElrOnboardingValidator

class MarsOtcElrOnboardingValidatorTests {

    private val validator = MarsOtcElrOnboardingValidator()

    @Test
    fun `test can detect an invalid RADxMARS message`() {
        val sampleMessageInputStream =
            this.javaClass.classLoader.getResourceAsStream("validation/marsotcelr/fail_onboarding_pass_prod.hl7")

        val sampleMessage = sampleMessageInputStream!!.bufferedReader().use { it.readText() }
        val messages = HL7Reader(ActionLogger()).getMessages(sampleMessage)
        val report = validator.validate(messages[0])
        assertThat(report.isValid()).isFalse()
    }

    @Test
    fun `test a valid RADxMARS message`() {
        val sampleMessageInputStream =
            this.javaClass.classLoader.getResourceAsStream("validation/marsotcelr/valid.hl7")

        val sampleMessage = sampleMessageInputStream!!.bufferedReader().use { it.readText() }
        val messages = HL7Reader(ActionLogger()).getMessages(sampleMessage)
        val report = validator.validate(messages[0])
        assertThat(report.isValid()).isTrue()
    }
}