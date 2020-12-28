package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

internal class FakeReportTests {
    private val rowContext = FakeReport.RowContext { null }

    private val metadata = Metadata(
        valueSet = ValueSet("fake", ValueSet.SetSystem.LOCAL, values = listOf(ValueSet.Value(code = "AZ")))
    ).loadSchemas(
        Schema(
            "test", "topic",
            listOf(
                Element("patient_state", type = Element.Type.CODE, valueSet = "fake"),
                Element("patient_phone", type = Element.Type.TELEPHONE),
                Element(
                    "formatted_patient_phone", type = Element.Type.TELEPHONE,
                    csvFields = listOf(
                        Element.CsvField("Patient Phone", "##########")
                    )
                ),
                Element("postal_code", type = Element.Type.POSTAL_CODE),
                Element("patient_ssn", type = Element.Type.ID_SSN),
                Element("default_date", type = Element.Type.DATE),
            )
        )
    )

    @Test
    fun `test a coded fake`() {
        val rowContext = FakeReport.RowContext { null }
        val state = metadata.findSchema("test")?.findElement("patient_state") ?: fail("Lookup failure: patient_state")
        val fakeValue = FakeReport(metadata).buildColumn(state, rowContext)
        assertEquals("AZ", fakeValue)
    }

    @Test
    fun `test phone matches default pattern`() {
        val patientPhone = metadata
            .findSchema("test")
            ?.findElement("patient_phone") ?: fail("Lookup failure: patient_phone")
        val fakedNumber = FakeReport(metadata).buildColumn(patientPhone, rowContext)
        // default format for phones in FakeReport is "##########:1:". checking for that here
        // todo: update for different formats as we expand the offerings for other consumers
        val phoneRegex = "\\d{10}:1:".toRegex()
        assertTrue("Generated phone number doesn't match expected default pattern. Was $fakedNumber") {
            phoneRegex.matches(fakedNumber)
        }
    }

    @Test
    fun `test phone matches specified pattern`() {
        val formattedPatientPhone = metadata
            .findSchema("test")
            ?.findElement("formatted_patient_phone") ?: fail("Lookup failure: formatted_patient_phone")
        val fakedNumber = FakeReport(metadata).buildColumn(formattedPatientPhone, rowContext)
        // default format for phones in FakeReport is "##########:1:". checking for that here
        // todo: update for different formats as we expand the offerings for other consumers
        val phoneRegex = "\\d{10}".toRegex()
        assertTrue("Generated phone number doesn't match specified pattern. Was $fakedNumber") {
            phoneRegex.matches(fakedNumber)
        }
    }

    // todo: update when we provide different formats for different consumers
    @Test
    fun `test postal code matches pattern expected`() {
        val postalCodeElement = metadata.findSchema("test")
            ?.findElement("postal_code") ?: fail("Lookup failure: postal_code")
        val fakedPostalCode = FakeReport(metadata).buildColumn(postalCodeElement, rowContext)
        val postalCodeRegex = "^\\d{5}$".toRegex()
        val postalCodeRegex2 = "^\\d{5}-\\d{4}$".toRegex()
        assertTrue("Postal code generated does not match expected pattern. Was $fakedPostalCode") {
            postalCodeRegex.matches(fakedPostalCode) || postalCodeRegex2.matches(fakedPostalCode)
        }
    }

    @Test
    fun `test default date format matches`() {
        val defaultDateElement = metadata.findSchema("test")
            ?.findElement("default_date") ?: fail("Lookup failure: default_date")
        val fakedDate = FakeReport(metadata).buildColumn(defaultDateElement, rowContext)
        val defaultDateFormatRegex = "^\\d{8}$".toRegex()
        assertTrue("Date does not match expected format. Received: $fakedDate") {
            defaultDateFormatRegex.matches(fakedDate)
        }
    }
}