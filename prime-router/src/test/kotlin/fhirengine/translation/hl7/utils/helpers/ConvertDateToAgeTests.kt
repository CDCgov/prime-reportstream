package fhirengine.translation.hl7.utils.helpers

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.hl7.fhir.r4.model.Age
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.StringType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Calendar
import java.util.Date

class ConvertDateToAgeTests {
    @Test
    fun `test convertDateToAge - future date`() {
        val currentDate = Date()
        val birthdate = Calendar.getInstance()
        birthdate.time = currentDate
        // Set a birthdate in the future
        birthdate.add(Calendar.DATE, 2)

        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)),
                mutableListOf()
            )
        }
    }

    @Test
    fun `test convertDateToAge year`() {
        val currentDate = Date()
        val birthdate = Calendar.getInstance()
        birthdate.time = currentDate
        // Set a birthdate 76 years ago
        birthdate.add(Calendar.YEAR, -76)

        val results = listOf(
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)),
                mutableListOf()
            ),
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)),
                mutableListOf(mutableListOf(StringType("year")))
            ),
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)),
                null
            )
        )

        results.forEach { ageList ->
            val age = ageList[0]
            assertThat(age is Age).isEqualTo(true)
            if (age is Age) {
                assertThat(age.unit).isEqualTo("year")
                assertThat(age.value.toInt()).isEqualTo(76)
                assertThat(age.code).isEqualTo("a")
            }
        }
    }

    @Test
    fun `test convertDateToAge month`() {
        val currentDate = Date()
        val birthdate = Calendar.getInstance()
        birthdate.time = currentDate
        // Set a birthday 6 months in the past
        birthdate.add(Calendar.MONTH, -6)

        val result = listOf(
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)),
                mutableListOf()
            ),
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)),
                mutableListOf(mutableListOf(StringType("month")))
            ),
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)), null
            )
        )

        result.forEach { ageList ->
            val age = ageList[0]
            assertThat(age is Age).isEqualTo(true)
            if (age is Age) {
                assertThat(age.unit).isEqualTo("month")
                assertThat(age.value.toInt()).isEqualTo(6)
                assertThat(age.code).isEqualTo("mo")
            }
        }
    }

    @Test
    fun `test convertDateToAge 0 days`() {
        val someDate = Date(1687464192857)
        val results = listOf(
            // Explicitly look for "day"
            convertDateToAge(
                mutableListOf(DateType(someDate)),
                mutableListOf(mutableListOf(DateType(someDate)), mutableListOf(StringType("day")))
            ),
            // Get default response without specifying type
            convertDateToAge(
                mutableListOf(DateType(someDate)),
                mutableListOf(mutableListOf(DateType(someDate)))
            )
        )
        results.forEach { ageList ->
            val age = ageList[0]
            assertThat(age is Age).isEqualTo(true)
            if (age is Age) {
                assertThat(age.unit).isEqualTo("day")
                assertThat(age.value.toInt()).isEqualTo(0)
                assertThat(age.code).isEqualTo("d")
            }
        }
    }

    @Test
    fun `test convertDateToAge 4 days`() {
        val referenceDate = Calendar.getInstance()
        referenceDate.set(2023, 1, 1)
        val birthdate = Calendar.getInstance()
        birthdate.time = referenceDate.time
        // set a birthdate 4 days prior to the reference date
        birthdate.add(Calendar.DAY_OF_YEAR, -4)

        val results = listOf(
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)),
                mutableListOf(mutableListOf(DateType(referenceDate)), mutableListOf(StringType("day")))
            ),
            convertDateToAge(
                mutableListOf(DateType(birthdate.time)),
                mutableListOf(mutableListOf(StringType("day")), mutableListOf(DateType(referenceDate)))
            ),
        )

        results.forEach { ageList ->
            val age = ageList[0]
            assertThat(age is Age).isEqualTo(true)
            if (age is Age) {
                assertThat(age.unit).isEqualTo("day")
                assertThat(age.value.toInt()).isEqualTo(4)
                assertThat(age.code).isEqualTo("d")
            }
        }
    }

    @Test
    fun `test convertDateToAge 4 months as days`() {
        val referenceDate = Calendar.getInstance()
        referenceDate.set(2023, 1, 1)
        val birthdate = Calendar.getInstance()
        birthdate.timeInMillis = referenceDate.timeInMillis
        // Set a birthdate 4 months before the reference date
        birthdate.add(Calendar.MONTH, -4)

        val ageList = convertDateToAge(
            mutableListOf(DateType(birthdate.time)),
            mutableListOf(mutableListOf(StringType("day")), mutableListOf(DateType(referenceDate)))
        )

        val age = ageList[0]
        assertThat(age is Age).isEqualTo(true)
        if (age is Age) {
            assertThat(age.unit).isEqualTo("day")
            assertThat(age.value.toInt()).isEqualTo(123)
            assertThat(age.code).isEqualTo("d")
        }
    }

    @Test
    fun `test convertDateToAge - test bad inputs`() {
        val currentDate = Date()
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate.time
        calendar.add(Calendar.MONTH, -4)
        val birthDate = DateType(calendar.time)

        // Checks with invalid focus input
        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(),
                mutableListOf(mutableListOf(StringType("day")), mutableListOf(DateType(calendar.time))),
            )
        }

        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(StringType("non-date-input")),
                mutableListOf(mutableListOf(StringType("day")), mutableListOf(DateType(calendar.time))),
            )
        }

        // Checks with improperly set parameters
        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(birthDate),
                mutableListOf(
                    mutableListOf(StringType("day"), DateType(calendar.time)),
                    mutableListOf(StringType("day"), DateType(calendar.time))
                ),
            )
        }

        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(birthDate),
                mutableListOf(mutableListOf(StringType("day")), mutableListOf(DateTimeType(calendar.time))),
            )
        }

        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(birthDate),
                mutableListOf(mutableListOf(StringType("day")), mutableListOf(StringType("day"))),
            )
        }

        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(birthDate),
                mutableListOf(mutableListOf(DateType(calendar.time)), mutableListOf(DateType(calendar.time))),
            )
        }

        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(birthDate),
                mutableListOf(
                    mutableListOf(DateType(calendar.time)), mutableListOf(StringType("day")),
                    mutableListOf(StringType("day"))
                ),
            )
        }

        // Checks with invalid parameter values
        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(birthDate),
                mutableListOf(mutableListOf(StringType("minute")), mutableListOf(DateType(calendar.time))),
            )
        }

        assertThrows<SchemaException> {
            convertDateToAge(
                mutableListOf(birthDate),
                mutableListOf(mutableListOf(StringType("quarter")), mutableListOf(DateType(calendar.time))),
            )
        }
    }
}