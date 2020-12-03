package gov.cdc.prime.router

import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class DocumentationTests {
    private val elem = Element(name = "a", type = Element.Type.TEXT)
    private val schema = Schema(name = "Test Schema", topic = "", elements = listOf(elem), description = "This is a test schema")

    @Test
    fun `test this getting loaded`() {
        assertTrue(true, "I always pass")
    }

    @Test
    @Ignore
    fun `ignore me for now I do nothing`() {
        // I'm empty
    }

    @Test
    fun `test building documentation string from element`() {
        val expected = """
**Name**:           a

**Type**:           TEXT

**Format**:         

---"""

        val docString = DocumentationFactory.getElementDocumentation(elem)
        assertEquals(expected, docString, "The messages do not match")
    }

    @Test
    fun `test building documentation string from a schema`() {
        val expected = """
### Schema:         Test Schema
#### Description:   This is a test schema

---

**Name**:           a

**Type**:           TEXT

**Format**:         

---
"""

        val actual = DocumentationFactory.getSchemaDocumentation(schema)
        assertEquals(expected, actual)
    }
}