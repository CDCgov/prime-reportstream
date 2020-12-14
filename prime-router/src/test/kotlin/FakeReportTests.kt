package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals

internal class FakeReportTests {
    @Test
    fun `test a coded fake`() {
        val state = Element("patient_state", type = Element.Type.CODE, valueSet = "fake")
        val rowContext = FakeReport.Companion.RowContext { null }
        val valueSets = mapOf(
            "fake" to
                ValueSet("fake", ValueSet.SetSystem.LOCAL, values = listOf(ValueSet.Value(code = "AZ")))
        )
        assertEquals("AZ", FakeReport.buildColumn(state, rowContext, { valueSets[it] }, { null }))
    }
}