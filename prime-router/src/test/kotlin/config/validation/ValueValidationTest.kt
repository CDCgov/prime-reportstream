package gov.cdc.prime.router.config.validation

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isInstanceOf
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.constraints.minItems
import io.konform.validation.constraints.minLength
import io.konform.validation.constraints.minimum
import io.konform.validation.onEach
import kotlin.test.Test

class ValueValidationTest {

    private val validation = Validation {
        TestDataClass::string {
            minLength(2)
        }

        TestDataClass::nullable required {
            minimum(0)
        }

        TestDataClass::list {
            minItems(2)
            onEach {
                minLength(2)
            }
        }
    }

    @Test
    fun valid() {
        val test = TestDataClass(
            string = "string",
            nullable = 5,
            list = listOf(
                "test",
                "string"
            )
        )

        val validated = validation.validate(test)
        assertThat(validated).isInstanceOf<Valid<TestDataClass>>()
    }

    @Test
    fun invalid() {
        val test = TestDataClass(
            string = "a", // too short
            nullable = -5, // negative
            list = listOf(
                // list not long enough
                "1", // too short
            )
        )

        val invalid = validation.validate(test)
        assertThat(invalid).isInstanceOf<Invalid>()
        assertThat(invalid.errors).hasSize(4)
    }
}

private data class TestDataClass(val string: String, val nullable: Int?, val list: List<String>)