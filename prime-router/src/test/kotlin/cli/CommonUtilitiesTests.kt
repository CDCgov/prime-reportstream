package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.cli.CommandUtilities.Companion.DiffRow
import gov.cdc.prime.router.cli.CommandUtilities.Companion.diffJson
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
}