package fhirengine.translation.hl7.utils.helpers

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

/**
 * Converts the birthdate in the [focus] element to an age. Based on what is passed in the optional [parameters], one
 * can specify whether this value is returned in years, months, or days. If left off, the method assumes years if it is
 * greater than one year, months if it is less than a year but greater than one month, and days if the value is less
 * than one month. There is also an optional param to pass a comparison date if you don't want to get the age based off
 * of how old they are today. [parameters] can contain the time unit and comparison date in either order;
 * (timeUnit, comparisonDate) and (comparisonDate, timeUnit) are both acceptable. [focus] must contain a date.
 * @return an age in years, months, or days.
 */
fun convertDateToAge(focus: MutableList<Base>, parameters: MutableList<MutableList<Base>>?): MutableList<Base> {
    val dateOfBirth: LocalDate =
        getLocalDate(
            focus.getOrNull(0) as? DateType
                ?: throw SchemaException("Must call the convertDateToAge function on a DateType.")
        )
    var ageUnit: TemporalPrecisionEnum? = null
    var referenceDate: LocalDate = LocalDate.now()

    /**
     * Populates local variables referenceDate and ageUnit from params.
     * The expected format for parameters is listOf(listOf(param1), listOf(param2)).
     */
    fun populateParams() {
        // validate resource
        if (!parameters.isNullOrEmpty()) {
            if (parameters.size > 2) {
                throw SchemaException("Cannot call the convertDateToAge function with more than two parameters.")
            }

            parameters.forEach { currentParamList ->
                if (currentParamList.size != 1) {
                    throw SchemaException("Can only pass a list(list(param), list(param)).")
                }
                when (val param = currentParamList[0]) {
                    is DateType -> referenceDate = getLocalDate(param)
                    is StringType -> {
                        try {
                            ageUnit = TemporalPrecisionEnum.valueOf(param.toString().uppercase())
                        } catch (e: IllegalArgumentException) {
                            throw SchemaException("Age unit must be one of: year, month, day.")
                        }
                    }
                    else ->
                        throw SchemaException("Parameters can only include one string param and/or one DateType param.")
                }
            }

            if (parameters.size == 2 && parameters[0][0]::class == parameters[1][0]::class) {
                throw SchemaException("Parameters can only include one string param and/or one DateType param.")
            }
        }
    }

    populateParams()

    return mutableListOf(
        if (ageUnit != null) {
            calculateAgeWithSpecifiedTimeUnit(dateOfBirth, referenceDate, ageUnit)
        } else {
            calculateAgeWithAssumption(dateOfBirth, referenceDate)
        }
    )
}

/**
 * This method calculates the time passed from [dateOfBirth] to [referenceDate] using the time unit specified
 * in [ageUnit].
 * @return an age in years, months, or days.
 */
internal fun calculateAgeWithSpecifiedTimeUnit(
    dateOfBirth: LocalDate,
    referenceDate: LocalDate,
    ageUnit: TemporalPrecisionEnum?
): Age {
    return when (ageUnit) {
        TemporalPrecisionEnum.DAY -> {
            createAge(ageUnit, BigDecimal(ChronoUnit.DAYS.between(dateOfBirth, referenceDate)))
        }
        TemporalPrecisionEnum.MONTH -> {
            createAge(ageUnit, BigDecimal(ChronoUnit.MONTHS.between(dateOfBirth, referenceDate)))
        }
        TemporalPrecisionEnum.YEAR -> {
            createAge(ageUnit, BigDecimal(ChronoUnit.YEARS.between(dateOfBirth, referenceDate)))
        }
        else -> throw SchemaException("Age unit must be one of: year, month, day")
    }
}

/**
 * This method calculates the time passed from the [referenceDate] to the [dateOfBirth] while assuming what unit the
 * user wants the time returned in. It assumes years if it is greater than one year, months if it is less than a year
 * but greater than one month, and days if the value is less than one month.
 * @return an age in years, months, or days.
 */
internal fun calculateAgeWithAssumption(dateOfBirth: LocalDate, referenceDate: LocalDate): Age {
    val period = Period.between(dateOfBirth, referenceDate)

    return when {
        period.years > 1 -> createAge(TemporalPrecisionEnum.YEAR, BigDecimal(period.years))
        period.months > 1 -> createAge(TemporalPrecisionEnum.MONTH, BigDecimal(period.months))
        period.days >= 0 -> createAge(TemporalPrecisionEnum.DAY, BigDecimal(period.days))
        else -> throw SchemaException("Must call the convertDateToAge function on a date in the past.")
    }
}

/**
 *  Takes a [date] and converts it to a LocalDate.
 *  Have to do plus one for the month because it is expecting 1 based, and we get zero based.
 *  @return DateType converted to LocalDate.
 */
internal fun getLocalDate(date: DateType): LocalDate =
    LocalDate.of(date.year, date.month + 1, date.day)

/**
 * Creates an Age of the given [ageValue] with the [ageUnit] properly tracked in the Age's unit and code.
 * @return the created Age.
 */
internal fun createAge(ageUnit: TemporalPrecisionEnum, ageValue: BigDecimal): Age {
    val age = Age()
    age.unit = ageUnit.toString().lowercase()
    age.value = ageValue
    age.code = when (ageUnit) {
        TemporalPrecisionEnum.DAY -> "d"
        TemporalPrecisionEnum.MONTH -> "mo"
        TemporalPrecisionEnum.YEAR -> "a"
        else -> throw SchemaException("Age unit must be one of: year, month, day.")
    }
    return age
}