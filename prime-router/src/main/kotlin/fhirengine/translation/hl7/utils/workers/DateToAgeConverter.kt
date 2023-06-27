package gov.cdc.prime.router.fhirengine.translation.hl7.utils.workers

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.hl7.fhir.r4.model.Age
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.StringType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

class DateToAgeConverter(
    private val focus: MutableList<Base>,
    private val parameters: MutableList<MutableList<Base>>?
) {

    private enum class Codes { D, MO, A }
    private data class Params(
        var dateOfBirth: DateType,
        var ageUnit: TemporalPrecisionEnum? = null,
        var referenceDate: LocalDate? = LocalDate.now()
    )

    /**
     * Calls the functions to extract params, then use those to calculate the age
     * @return an age in years, months, or days
     */
    fun convertDateToAge(): MutableList<Base> {
        // get a params object
        val params = getParams()

        // use params object to calculate date
        return if (params.ageUnit != null) {
            return calculateAgeWithSpecifiedTimeUnit(params)
        } else {
            calculateAgeWithAssumption(params.dateOfBirth, params.referenceDate)
        }
    }

    /**
     * Extracts the dateOfBirth, referenceDate, and ageUnit from the focus and params and returns them in a usable format
     * @return The parameters dateOfBirth, referenceDate, and ageUnit in a Params object
     */
    private fun getParams(): Params {
        // validate resource
        val params: Params = (if (focus.first() is DateType) Params(focus.first() as DateType) else null)
            ?: throw SchemaException("Must call the convertDateToAge function on a DateType.")
        if (!parameters.isNullOrEmpty()) {
            if (parameters.size > 1) {
                throw SchemaException("Cannot accept more than one set of parameters")
            }
            if (parameters.first().size == 2 && parameters.first()[0]::class == parameters.first()[1]::class) {
                throw SchemaException(
                    "Must call the convertDateToAge function no more than one " +
                        "string param and/or one DateType param."
                )
            }
            if (parameters.first().size > 2) {
                throw SchemaException(
                    "Must call the convertDateToAge function no more than one " +
                        "string param and/or one DateType param."
                )
            }
            parameters.first().forEach { param ->
                when (param) {
                    is DateType -> params.referenceDate = convertDateToAgeGetLocalDate(param)
                    is StringType -> {
                        try {
                            params.ageUnit = TemporalPrecisionEnum.valueOf(param.toString().uppercase())
                        } catch (e: IllegalArgumentException) {
                            throw SchemaException("age unit must be one of: year, month, day")
                        }
                    }
                    else -> throw SchemaException(
                        "Must call the convertDateToAge function no more than one " +
                            "string param and/or one DateType param."
                    )
                }
            }
        }
        return params
    }

    /**
     * This method calculates the time passed from the [params].dateOfBirth to the [params].referenceDate using the time
     * unit specified in [params].ageUnit
     * @return an age in years, months, weeks, or days.
     */
    private fun calculateAgeWithSpecifiedTimeUnit(params: Params): MutableList<Base> {
        val age = Age()
        return when (params.ageUnit) {
            TemporalPrecisionEnum.DAY -> {
                age.unit = TemporalPrecisionEnum.DAY.toString().lowercase()
                age.value = BigDecimal(
                    ChronoUnit.DAYS.between(
                        convertDateToAgeGetLocalDate(params.dateOfBirth),
                        params.referenceDate
                    )
                )
                age.code = Codes.D.toString().lowercase()
                mutableListOf(age)
            }

            TemporalPrecisionEnum.MONTH -> {
                age.unit = TemporalPrecisionEnum.MONTH.toString().lowercase()
                age.value = BigDecimal(
                    ChronoUnit.MONTHS.between(
                        convertDateToAgeGetLocalDate(params.dateOfBirth),
                        params.referenceDate
                    )
                )
                age.code = Codes.MO.toString().lowercase()
                mutableListOf(age)
            }

            TemporalPrecisionEnum.YEAR -> {
                age.unit = TemporalPrecisionEnum.YEAR.toString().lowercase()
                age.value = BigDecimal(
                    ChronoUnit.YEARS.between(
                        convertDateToAgeGetLocalDate(params.dateOfBirth),
                        params.referenceDate
                    )
                )
                age.code = Codes.A.toString().lowercase()
                mutableListOf(age)
            }

            else -> throw SchemaException("Call with day, month, or year")
        }
    }

    /**
     * This method calculates the time passed from the [comparisonDate] to the [date] while assuming what unit the user
     * wants the time returned in. It assumes years if it is greater than one year, months if it is less than a year
     * but greater than one month, and days if the value is less than one month.
     * @return an age in years, months, weeks, or days.
     */
    private fun calculateAgeWithAssumption(
        date: DateType,
        comparisonDate: LocalDate?
    ): MutableList<Base> {
        val period = Period.between(
            convertDateToAgeGetLocalDate(date),
            comparisonDate
        )

        val age = Age()
        if (period.years > 1) {
            age.unit = TemporalPrecisionEnum.YEAR.toString().lowercase()
            age.value = BigDecimal(period.years)
            age.code = Codes.A.toString().lowercase()
        } else if (period.months > 1) {
            age.unit = TemporalPrecisionEnum.MONTH.toString().lowercase()
            age.value = BigDecimal(period.months)
            age.code = Codes.MO.toString().lowercase()
        } else if (period.days >= 0) {
            age.unit = TemporalPrecisionEnum.DAY.toString().lowercase()
            age.value = BigDecimal(period.days)
            age.code = Codes.D.toString().lowercase()
        } else {
            throw SchemaException("Must call the convertDateToAge function on a date in the past")
        }

        return mutableListOf(age)
    }

    /**
     *  Takes a [date] and converts it to a LocalDate.
     *  Have to do plus one for the month because it is expecting 1 based, and we get zero based
     *  @return DateType converted to LocalDate
     */
    private fun convertDateToAgeGetLocalDate(date: DateType): LocalDate? =
        LocalDate.of(date.year, date.month + 1, date.day)
}