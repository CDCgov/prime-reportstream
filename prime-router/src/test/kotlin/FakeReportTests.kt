package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class FakeReportTests {
    @Test
    fun `test a coded fake`() {
        val state = Element("standard.patient_state", type = Element.Type.CODE, valueSet = "fake")
        val valueSets = mapOf(
            "fake" to
                    ValueSet("fake", ValueSet.SetSystem.LOCAL, values = listOf(ValueSet.Value(code = "AZ")))
        )
        assertEquals("AZ", FakeReport.buildColumn(state) { valueSets[it] })
    }

    @Test
    fun `test a coded fake telephone`() {
        val phone = Element("standard.patient_state", type = Element.Type.TELEPHONE)
        val fake = FakeReport.buildColumn(phone) { null }
        val formatted = phone.toFormatted(fake, Element.CsvField("test", Element.defaultPhoneFormat))
        assertEquals(10, formatted.length)
    }

    @Test
    fun `test a bunch of fake fields`() {
        Metadata.loadSchemaCatalog("./src/test/unit_test_files")
        val schema = Metadata.findSchema("lab_test_results_schema") ?: fail("Should be have a schema")
        val fakeReport = FakeReport.build(schema, 10, TestSource)
        assertEquals(10, fakeReport.itemCount)
    }
}