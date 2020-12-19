package gov.cdc.prime.router

import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentationTests {
    private val documentation = """
        ##### This is a test documentation field
        `I am a code example`
        > This is preformatted text
    """.trimIndent()
    private val elem = Element(name = "a", type = Element.Type.TEXT)
    private val elemWithDocumentation = Element(name = "a", type = Element.Type.TEXT, documentation = documentation)
    private val schema = Schema(
        name = "Test Schema",
        topic = "",
        elements = listOf(elem),
        description = "This is a test schema"
    )

    @Test
    @Ignore
    fun `test building documentation string from element`() {
        val expected = """
**Name**: a

**Usage**:          optional

**Type**:           TEXT

**Format**:         

---
"""

        val docString = DocumentationFactory.getElementDocumentation(elem)
        assertEquals(expected, docString, "The messages do not match")
    }

    @Test
    fun `test building documentation string from a schema`() {
        val expected = """
### Schema:         Test Schema
#### Description:   This is a test schema

---

**Name**: a

**Type**: TEXT

---
"""

        val actual = DocumentationFactory.getSchemaDocumentation(schema)
        assertEquals(expected, actual)
    }

    @Test
    fun `test building documentation for element with documentation value`() {
        val expected = """
**Name**: a

**Type**: TEXT

**Documentation**:

$documentation

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithDocumentation)
        assertEquals(expected, actual)
    }
}