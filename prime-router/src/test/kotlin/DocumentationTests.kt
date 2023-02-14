package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.docgenerators.DocumentationFactory
import gov.cdc.prime.router.docgenerators.MarkdownDocumentationFactory
import java.time.LocalDate
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * A collection of tests for the documentation generation
 */
class DocumentationTests {
    private val documentation =
        """
        #### This is a test documentation field
        `I am a code example`
        > This is preformatted text
        """.trimIndent()

    private val elem = Element(
        name = "a",
        type = Element.Type.TEXT,
        csvFields = Element.csvFields("Test TrackingElement")
    )
    private val elemWithDocumentation = Element(name = "a", type = Element.Type.TEXT, documentation = documentation)
    private val schema = Schema(
        name = "Test Schema",
        topic = Topic.TEST,
        elements = listOf(elem),
        trackingElement = "a",
        description = "Test Description",
        extends = "test/extends",
        basedOn = "TestBaseOn"
    )

    @Test
    fun `test getting output file name`() {
        // check a schema with no slash in the name
        Schema(
            name = "covid-19",
            topic = Topic.TEST
        ).also { schema ->
            DocumentationFactory.getOutputFileName(null, schema, false, "md").also {
                assertThat(it).isEqualTo("covid-19.md")
            }
            DocumentationFactory.getOutputFileName(null, schema, true, "html").also {
                val timestamp = LocalDate.now().format(DocumentationFactory.formatter)
                assertThat(it).isEqualTo("covid-19-$timestamp.html")
            }
        }

        // check a schema with a slash in the name
        Schema(
            name = "direct/cue-covid-19",
            topic = Topic.TEST
        ).also { schema ->
            DocumentationFactory.getOutputFileName(null, schema, false, "md").also {
                assertThat(it).isEqualTo("direct-cue-covid-19.md")
            }
            DocumentationFactory.getOutputFileName(null, schema, true, "csv").also {
                val timestamp = LocalDate.now().format(DocumentationFactory.formatter)
                assertThat(it).isEqualTo("direct-cue-covid-19-$timestamp.csv")
            }
        }

        // check an output file name
        Schema(
            name = "covid-19",
            topic = Topic.TEST
        ).also {
            DocumentationFactory.getOutputFileName("test-file-name", schema, false, "txt").also {
                assertThat(it).isEqualTo("test-file-name.txt")
            }
            DocumentationFactory.getOutputFileName("test-file-name", schema, true, "xlsx").also {
                val timestamp = LocalDate.now().format(DocumentationFactory.formatter)
                assertThat(it).isEqualTo("test-file-name-$timestamp.xlsx")
            }
        }
    }

    @Test
    @Ignore
    fun `test building documentation string from element`() {
        val expected =
            """
            **Name**: a
            
            **Type**:           TEXT     
            
            **Cardinality**:    [0..1]
            ---
            
            """.trimIndent()

        val docString = MarkdownDocumentationFactory.getElementDocumentation(elem)
        assertThat(docString).isEqualTo(expected)
    }

    @Test
    fun `test building documentation string from a schema`() {
        val expected =
            """
            ### Schema: Test Schema
            ### Topic: test
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
            
            """.trimIndent()

        val actual = MarkdownDocumentationFactory.getSchemaDocumentation(schema).joinToString(separator = "")
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test building documentation for element with documentation value`() {
        val expected = StringBuilder()

        expected.appendLine(
            """
            
            **Name**: a
            
            **ReportStream Internal Name**: a
            
            **Type**: TEXT
            
            **PII**: No
            
            **Cardinality**: [0..1]
            
            **Documentation**:
            """.trimIndent()
        )
        expected.appendLine(
            documentation
        )
        expected.appendLine("---")

        val actual = MarkdownDocumentationFactory.getElementDocumentation(elemWithDocumentation)
        assertThat(actual).isEqualTo(expected.toString())
    }

    @Test
    fun `test documentation for element with type CODE with Format $display`() {
        val elemWithTypeCode = Element(
            "a",
            type = Element.Type.CODE,
            csvFields = Element.csvFields("b", format = "\$display")
        )
        val expected =
            """
            
            **Name**: b
            
            **ReportStream Internal Name**: a
            
            **Type**: CODE
            
            **PII**: No
            
            **Format**: use value found in the Display column
            
            **Cardinality**: [0..1]
            
            ---
        
            """.trimIndent()
        val actual = MarkdownDocumentationFactory.getElementDocumentation(elemWithTypeCode)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test documentation for element with type CODE with Format $alt`() {
        val elemWithTypeCode = Element(
            "a",
            type = Element.Type.CODE,
            csvFields = Element.csvFields("b", format = "\$alt")
        )
        val expected =
            """
            
            **Name**: b
            
            **ReportStream Internal Name**: a
            
            **Type**: CODE
            
            **PII**: No
            
            **Format**: use value found in the Display column
            
            **Cardinality**: [0..1]
            
            ---
            
            """.trimIndent()
        MarkdownDocumentationFactory.getElementDocumentation(elemWithTypeCode).also { actual ->
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test documentation for element with type CODE without Format`() {
        val elemWithTypeCode = Element(
            "a",
            type = Element.Type.CODE,
            csvFields = Element.csvFields("b")
        )
        val expected =
            """
            
            **Name**: b
            
            **ReportStream Internal Name**: a
            
            **Type**: CODE
            
            **PII**: No
            
            **Format**: use value found in the Code column
            
            **Cardinality**: [0..1]
            
            ---
            
            """.trimIndent()
        MarkdownDocumentationFactory.getElementDocumentation(elemWithTypeCode).also { actual ->
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `test documentation for element with type TEXT with Format Testing`() {
        val elemWithTypeCode = Element(
            "a",
            type = Element.Type.TEXT,
            csvFields = Element.csvFields("b", format = "Testing")
        )
        val expected =
            """
            
            **Name**: b
            
            **ReportStream Internal Name**: a
            
            **Type**: TEXT
            
            **PII**: No
            
            **Format**: Testing
            
            **Cardinality**: [0..1]
            
            ---
            
            """.trimIndent()
        val actual = MarkdownDocumentationFactory.getElementDocumentation(elemWithTypeCode)
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
        val expected =
            """
            
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
            
            """.trimIndent()
        val actual = MarkdownDocumentationFactory.getElementDocumentation(elemWithTypeCode)
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
                    ">",
                    "Above absolute high-off instrument scale",
                    system = ValueSet.SetSystem.LOINC
                )
            )
        )
        val elemWithValuesSetValues = Element(name = "a", type = Element.Type.CODE, valueSetRef = valueSetA)
        val expected =
            """
            
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
            
            """.trimIndent()
        val actual = MarkdownDocumentationFactory.getElementDocumentation(elemWithValuesSetValues)
        assertThat(actual).isEqualTo(expected)
    }
}