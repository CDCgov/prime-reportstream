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
    private val elem = Element(
        name = "a", type = Element.Type.TEXT,
        csvFields = Element.csvFields("Test TrackingElement")
    )
    private val elemWithDocumentation = Element(name = "a", type = Element.Type.TEXT, documentation = documentation)
    private val schema = Schema(
        name = "Test Schema",
        topic = "Test Topic",
        elements = listOf(elem),
        trackingElement = "a",
        description = "Test Description",
        extends = "test/extends",
        basedOn = "TestBaseOn"
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
### Schema: Test Schema
### Topic: Test Topic
### Tracking Element: Test TrackingElement (a)
### Base On: [TestBaseOn](./TestBaseOn.md)
### Extends: [test/extends](./test-extends.md)
#### Description: Test Description

---

**Name**: Test TrackingElement

**ReportStream Internal Name**: a

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

**ReportStream Internal Name**: a

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
    fun `test documentation for element with type CODE with Format $display`() {
        val elemWithTypeCode = Element(
            "a",
            type = Element.Type.CODE,
            csvFields = Element.csvFields("b", format = "\$display")
        )
        val expected = """
**Name**: b

**ReportStream Internal Name**: a

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithTypeCode)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test documentation for element with type CODE with Format $alt`() {
        val elemWithTypeCode = Element(
            "a",
            type = Element.Type.CODE,
            csvFields = Element.csvFields("b", format = "\$alt")
        )
        val expected = """
**Name**: b

**ReportStream Internal Name**: a

**Type**: CODE

**PII**: No

**Format**: use value found in the Display column

**Cardinality**: [0..1]

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithTypeCode)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test documentation for element with type CODE without Format`() {
        val elemWithTypeCode = Element(
            "a",
            type = Element.Type.CODE,
            csvFields = Element.csvFields("b")
        )
        val expected = """
**Name**: b

**ReportStream Internal Name**: a

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithTypeCode)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test documentation for element with type TEXT with Format Testing`() {
        val elemWithTypeCode = Element(
            "a",
            type = Element.Type.TEXT,
            csvFields = Element.csvFields("b", format = "Testing")
        )
        val expected = """
**Name**: b

**ReportStream Internal Name**: a

**Type**: TEXT

**PII**: No

**Format**: Testing

**Cardinality**: [0..1]

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithTypeCode)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test documentation for element with type CODE and valueSet table with special char`() {

        val valueSetA = ValueSet(
            "a",
            ValueSet.SetSystem.HL7,
            values = listOf(ValueSet.Value(">", "Above absolute high-off instrument scale"))
        )

        val elemWithTypeCode = Element(name = "a", type = Element.Type.CODE, valueSetRef = valueSetA)
        val expected = """
**Name**: a

**ReportStream Internal Name**: a

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
&#62;|Above absolute high-off instrument scale|HL7

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithTypeCode)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test documentation for element with valueSet table generation`() {

        // Test case that contains element.system = HL7 and element.ValueSet.Values.system = LOINC
        val valueSetA = ValueSet(
            "a",
            ValueSet.SetSystem.HL7,
            values = listOf(
                ValueSet.Value(
                    ">", "Above absolute high-off instrument scale",
                    system = ValueSet.SetSystem.LOINC
                )
            )
        )
        val elemWithValuesSetValues = Element(name = "a", type = Element.Type.CODE, valueSetRef = valueSetA)
        val expected = """
**Name**: a

**ReportStream Internal Name**: a

**Type**: CODE

**PII**: No

**Format**: use value found in the Code column

**Cardinality**: [0..1]

**Value Sets**

Code | Display | System
---- | ------- | ------
&#62;|Above absolute high-off instrument scale|LOINC

---
"""
        val actual = DocumentationFactory.getElementDocumentation(elemWithValuesSetValues)
        assertThat(actual).isEqualTo(expected)
    }
}