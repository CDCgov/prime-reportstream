package gov.cdc.prime.router.common

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import org.junit.jupiter.api.Test

class MixedMultiPartTests {
    @Test
    fun `test generate boundary`() {
        val boundary = MixedMultiPart.generateBoundary()
        assertThat(boundary.length).isGreaterThan(10)
    }

    @Test
    fun `test single csv`() {
        val csvPart = MixedMultiPart.Part(
            contentType = "text/csv",
            fileName = "test.csv",
            body = """
                A, B, C
                1, 2, 3
            """.trimIndent(),
        )
        val boundary = MixedMultiPart.generateBoundary()
        val multiPart = MixedMultiPart(listOf(csvPart), boundary)
        val outputString = multiPart.serialize()
        assertThat(outputString.length).isGreaterThan(10)

        val actual = MixedMultiPart.deserialize(outputString, boundary)
        assertThat(actual.parts[0]).isEqualTo(csvPart)
    }

    @Test
    fun `test multiple parts`() {
        val csvPart = MixedMultiPart.Part(
            contentType = "text/csv",
            fileName = "test.csv",
            body = """
                A, B, C
                1, 2, 3
            """.trimIndent(),
        )
        val csvPart2 = MixedMultiPart.Part(
            contentType = "text/csv",
            fileName = "test2.csv",
            body = """
                X, Y, Z
                1, 2, 3
            """.trimIndent(),
        )
        val boundary = MixedMultiPart.generateBoundary()
        val multiPart = MixedMultiPart(listOf(csvPart, csvPart2), boundary)
        val outputString = multiPart.serialize()

        val actual = MixedMultiPart.deserialize(outputString, boundary)
        assertThat(actual.parts[0]).isEqualTo(csvPart)
        assertThat(actual.parts[1]).isEqualTo(csvPart2)
    }
}