package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class FakeReportTests {
    @Test
    fun `test a coded fake`() {
        val rowContext = FakeReport.RowContext { null }
        val metadata = Metadata(
            valueSet = ValueSet("fake", ValueSet.SetSystem.LOCAL, values = listOf(ValueSet.Value(code = "AZ")))
        ).loadSchemas(
            Schema(
                "test", "topic",
                listOf(
                    Element("patient_state", type = Element.Type.CODE, valueSet = "fake")
                )
            )
        )
        val state = metadata.findSchema("test")?.findElement("patient_state") ?: fail("Lookup failure")
        val fakeValue = FakeReport(metadata).buildColumn(state, rowContext)
        assertEquals("AZ", fakeValue)
    }
}