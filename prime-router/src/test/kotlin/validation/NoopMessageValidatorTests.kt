package gov.cdc.prime.router.validation

import assertk.assertThat
import assertk.assertions.isTrue
import ca.uhn.hl7v2.model.Message
import io.mockk.mockk
import org.hl7.fhir.r4.model.Bundle
import org.junit.jupiter.api.Test
import gov.cdc.prime.reportstream.shared.validation.NoopItemValidationResult
import gov.cdc.prime.reportstream.shared.validation.NoopItemValidator

class NoopMessageValidatorTests {

    @Test
    fun `test noop validation result is always valid`() {
        val noopValidationResult = NoopItemValidationResult()
        assertThat(noopValidationResult.isValid()).isTrue()
    }

    @Test
    fun `test NOOP validator always returns valid`() {
        val noopMessageValidator = NoopItemValidator()
        val mockHL7Message = mockk<Message>()
        val mockFHIRBundle = mockk<Bundle>()
        assertThat(noopMessageValidator.validate(mockHL7Message).isValid()).isTrue()
        assertThat(noopMessageValidator.validate(mockFHIRBundle).isValid()).isTrue()
    }
}