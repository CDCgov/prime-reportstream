package gov.cdc.prime.router.azure.observability

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.util.UUID
import kotlin.test.Test

class AzureCustomDimensionsSerializableTest {

    @Test
    fun `serialize data class to String map`() {
        val testDataClass = TestSerializable(
            string = "some string",
            int = 5,
            double = 3.14,
            nullable = null,
            boolean = true,
            char = 'a',
            uuid = UUID.fromString("bba31e00-fa17-4586-8ef6-bbbdea282744"),
            map = mapOf(
                "some key" to "some value"
            ),
            list = listOf("a", "b", "c"),
            nested = Nested("value1", 10)
        )

        val expected = mapOf(
            "string" to "some string",
            "int" to "5",
            "double" to "3.14",
            "nullable" to "null",
            "boolean" to "true",
            "char" to "a",
            "uuid" to "bba31e00-fa17-4586-8ef6-bbbdea282744",
            "map" to "{\"some key\":\"some value\"}",
            "list" to "[\"a\",\"b\",\"c\"]",
            "nested" to "{\"key1\":\"value1\",\"key2\":10}"
        )

        assertThat(testDataClass.serialize()).isEqualTo(expected)
    }
}

data class TestSerializable(
    val string: String,
    val int: Int,
    val double: Double,
    val nullable: String?,
    val boolean: Boolean,
    val char: Char,
    val uuid: UUID,
    val map: Map<String, String>,
    val list: List<String>,
    val nested: Nested,
) : AzureCustomDimensionsSerializable

data class Nested(val key1: String, val key2: Int)