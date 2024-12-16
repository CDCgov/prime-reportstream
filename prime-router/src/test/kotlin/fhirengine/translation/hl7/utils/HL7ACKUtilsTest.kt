package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.cli.helpers.HL7DiffHelper
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test

class HL7ACKUtilsTest {

    inner class Fixture {
        val hl7DiffHelper = HL7DiffHelper()

        private val clock = Clock.fixed(
            LocalDateTime.of(2024, 9, 21, 0, 0).toInstant(ZoneOffset.UTC),
            ZoneId.of("UTC")
        )
        val utils = HL7ACKUtils(clock)
    }

    @AfterEach
    fun cleanUp() {
        unmockkAll()
    }

    @Test
    fun `generates properly formatted ACK response`() {
        val f = Fixture()

        val id = UUID.randomUUID()
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns id

        val incomingMessage = """
            MSH|^~\&|Epic|Hospital|LIMS|StatePHL|20241003000000||ORM^O01^ORM_O01|4AFA57FE-D41D-4631-9500-286AAAF797E4|T|2.5.1|||AL|NE
        """.trimIndent()
        val expectedMessage = """
            MSH|^~\&|ReportStream|CDC|Epic|Hospital|20240921000000+0000||ACK|$id|T|2.5.1|||NE|NE
            MSA|CA|4AFA57FE-D41D-4631-9500-286AAAF797E4
        """.trimIndent()
        val parsedIncomingMessage = HL7Reader.parseHL7Message(incomingMessage, null)

        val ack = f.utils.generateOutgoingACKMessage(parsedIncomingMessage)

        val expected = HL7Reader.parseHL7Message(expectedMessage, null)
        val actual = HL7Reader.parseHL7Message(ack, null)

        val diffs = f.hl7DiffHelper.diffHl7(expected, actual)
        if (diffs.isNotEmpty()) {
            println(diffs)
        }
        assertThat(diffs.size).isEqualTo(0)
    }
}