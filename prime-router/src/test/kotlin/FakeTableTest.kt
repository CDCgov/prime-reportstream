package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.assertEquals

internal class FakeTableTest {
    @Test
    fun `test a coded fake`() {
        val state = Element("standard.patient_state", type = Element.Type.CODE, valueSet = "fake")
        val valueSets = mapOf(
            "fake" to
                    ValueSet("fake", ValueSet.SetSystem.LOCAL, values = listOf(ValueSet.Value(code = "AZ")))
        )
        assertEquals("AZ", FakeTable.buildColumn(state) { valueSets[it] })
    }
}