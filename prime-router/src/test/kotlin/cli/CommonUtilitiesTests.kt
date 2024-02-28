package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.doyaaaaaken.kotlincsv.client.CsvWriter
import gov.cdc.prime.router.cli.CommandUtilities.Companion.DiffRow
import gov.cdc.prime.router.cli.CommandUtilities.Companion.diffJson
import gov.cdc.prime.router.cli.FileUtilities.saveTableAsCSV
import io.mockk.clearConstructorMockk
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.verify
import java.io.OutputStream
import kotlin.test.Test

class CommonUtilitiesTests {

    @Test
    fun `test diffJson with simple json`() {
        val base = """
            {
                "a": "1",
                "b": "2"
            }
        """.trimIndent()

        val compare1 = """
            {
                "b": "2"
            }
        """.trimIndent()
        val diff1 = diffJson(base, compare1)
        assertThat(diff1).isEqualTo(listOf(DiffRow("a", "\"1\"", "")))

        val compare2 = """
            {
                "a": "1",
                "b": "2"
            }
        """.trimIndent()
        val diff2 = diffJson(base, compare2)
        assertThat(diff2).isEqualTo(listOf())

        val compare3 = """
            {
                "a": "1",
                "b": "2",
                "c": 1.1
            }
        """.trimIndent()
        val diff3 = diffJson(base, compare3)
        assertThat(diff3).isEqualTo(listOf(DiffRow("c", "", "1.1")))

        val compare4 = """
            {
                "a": "1",
                "b": false
            }
        """.trimIndent()
        val diff4 = diffJson(base, compare4)
        assertThat(diff4).isEqualTo(listOf(DiffRow("b", "\"2\"", "false")))
    }

    @Test
    fun `test diffJson with complex json`() {
        val base = """
            {
                "a": ["1", "2"],
                "b": {
                    "x": 1,
                    "y": true
                }
            }
        """.trimIndent()

        val compare1 = """
            {
                "a": ["1", "2"],
                "b": null
            }
        """.trimIndent()
        val diff1 = diffJson(base, compare1)
        assertThat(diff1).isEqualTo(
            listOf(
                DiffRow("b", "", "null"),
                DiffRow("b.x", "1", ""),
                DiffRow("b.y", "true", "")
            )
        )

        val compare2 = """
            {
                "a": ["1", "2", "3"],
                "b": {
                    "x": 1,
                    "y": true
                }
            }
        """.trimIndent()
        val diff2 = diffJson(base, compare2)
        assertThat(diff2).isEqualTo(
            listOf(
                DiffRow("a[2]", "", "\"3\""),
            )
        )
    }

    @Test
    fun `test saving table as csv`() {
        val table = listOf(
            mapOf("col1" to "x", "col2" to "y", "col3" to "z"),
            mapOf("col1" to "xx", "col2" to "yy", "col3" to "zz")
        )
        val expectedCSVRows = listOf(
            listOf("col1", "col2", "col3"),
            listOf("x", "y", "z"),
            listOf("xx", "yy", "zz")
        )
        val outputStream = mockk<OutputStream>()
        mockkConstructor(CsvWriter::class)
        every { anyConstructed<CsvWriter>().writeAll(any(), any<OutputStream>()) } just runs
        saveTableAsCSV(outputStream, table)
        verify(exactly = 1) { anyConstructed<CsvWriter>().writeAll(expectedCSVRows, any<OutputStream>()) }
        clearConstructorMockk(CsvWriter::class)
    }
}