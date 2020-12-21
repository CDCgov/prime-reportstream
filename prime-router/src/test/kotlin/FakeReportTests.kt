package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class FakeReportTests {
    val rowContext = FakeReport.Companion.RowContext { null }

    @Test
    fun `test a coded fake`() {
        val state = Element("patient_state", type = Element.Type.CODE, valueSet = "fake")
        val valueSets = mapOf(
            "fake" to
                ValueSet("fake", ValueSet.SetSystem.LOCAL, values = listOf(ValueSet.Value(code = "AZ")))
        )
        assertEquals("AZ", FakeReport.buildColumn(state, rowContext, { valueSets[it] }, { null }))
    }

    @Test
    fun `test phone matches default pattern`() {
        val phoneNumber = Element("patient_phone", type = Element.Type.TELEPHONE)
        val fakedNumber = FakeReport.buildColumn(phoneNumber, rowContext)
        // default format for phones in FakeReport is "##########:1:". checking for that here
        // todo: update for different formats as we expand the offerings for other consumers
        val phoneRegex = "\\d{10}:1:".toRegex()
        assertTrue("Generated phone number doesn't match expected default pattern. Was $fakedNumber") {
            phoneRegex.matches(fakedNumber)
        }
    }

    @Test
    fun `test phone matches specified pattern`() {
        val phoneNumber = Element(
            "patient_phone",
            type = Element.Type.TELEPHONE,
            csvFields = listOf(Element.CsvField("Patient Phone", "##########"))
        )
        val fakedNumber = FakeReport.buildColumn(phoneNumber, rowContext)
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
        val postalCodeElement = Element("postal_code", type = Element.Type.POSTAL_CODE)
        val fakedPostalCode = FakeReport.buildColumn(postalCodeElement, rowContext)
        val postalCodeRegex = "^\\d{5}$".toRegex()
        val postalCodeRegex2 = "^\\d{5}-\\d{4}$".toRegex()
        assertTrue("Postal code generated does not match expected pattern. Was $fakedPostalCode") {
            postalCodeRegex.matches(fakedPostalCode) || postalCodeRegex2.matches(fakedPostalCode)
        }
    }

    @Test
    fun `test SSN number matches format`() {
        val ssnElement = Element("patient_ssn", type = Element.Type.ID_SSN)
        val fakedSSN = FakeReport.buildColumn(ssnElement, rowContext)
        val ssnRegex = "^\\d{9}$".toRegex()
        val ssnRegex2 = "^\\d{3}-\\d{2}-\\d{4}$".toRegex()
        assertTrue("SSN generated does not match expected pattern. Was $fakedSSN") {
            ssnRegex.matches(fakedSSN) || ssnRegex2.matches(fakedSSN)
        }
    }
}