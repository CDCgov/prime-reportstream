package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import kotlin.test.Test

class CsvUtilitiesTests {

    @Test
    fun `test cut`() {
        val input = """
            A,B,C
            11,12,13
            21,22,23
            31,32,33
        """.trimIndent()
        assertThat(CsvUtilities.cut(input, listOf(1)).trim()).isEqualTo(
            """
                A,B,C
                21,22,23
            """.trimIndent()
        )
        assertThat(CsvUtilities.cut(input, listOf(0, 2)).trim()).isEqualTo(
            """
                A,B,C
                11,12,13
                31,32,33
            """.trimIndent()
        )
        assertThat { CsvUtilities.cut("", listOf(3)) }.isFailure()
        assertThat(CsvUtilities.cut("", listOf()).trim()).isEmpty()
    }
}