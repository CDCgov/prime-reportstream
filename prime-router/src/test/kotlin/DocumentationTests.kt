package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Ignore
import kotlin.test.Test

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

**Type**:           TEXT     

**Cardinality**:    [0..1]
---
"""

        val docString = DocumentationFactory.getElementDocumentation(elem)
        assertThat(docString).isEqualTo(expected)
    }

    @Test
    fun `test building documentation string from a schema`() {
        val expected = """
### Schema:         Test Schema
#### Description:   This is a test schema

---

**Name**: a

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

---
"""

        val actual = DocumentationFactory.getSchemaDocumentation(schema)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test building documentation for element with documentation value`() {
        val expected = """
**Name**: a

**Type**: TEXT

**PII**: No

**Cardinality**: [0..1]

**Documentation**:

$documentation

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithDocumentation)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test documentation for element with type CODE and valueSet table with special char`() {

        val valueSetA = ValueSet(
            "a",
            ValueSet.SetSystem.HL7,
            values = listOf(ValueSet.Value("&#62;", "Above absolute high-off instrument scale"))
        )

        val elemWithTypeCode = Element(name = "a", type = Element.Type.CODE, valueSetRef = valueSetA)
        val expected = """
**Name**: a

**Type**: CODE

**PII**: No

**Cardinality**: [0..1]

**Value Sets**

Code | Display
---- | -------
&#62;|Above absolute high-off instrument scale

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithTypeCode)
        assertThat(actual).isEqualTo(expected)
    }
}