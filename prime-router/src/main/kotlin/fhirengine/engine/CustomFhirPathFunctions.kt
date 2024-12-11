package fhirengine.engine

import com.github.javafaker.Faker
import fhirengine.translation.hl7.utils.FhirPathFunctions
import gov.cdc.prime.router.FakeReport
import gov.cdc.prime.router.FakeReport.Companion.randomChoice
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.NPIUtilities
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.metadata.GeoData
import gov.cdc.prime.router.metadata.LivdLookup
import org.hl7.fhir.r4.fhirpath.FHIRPathUtilityClasses.FunctionDetails
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Device
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.StringType
import java.time.LocalDate
import java.util.Date
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.abs
import kotlin.random.Random

/**
 * Custom FHIR functions created by report stream to help map from FHIR -> HL7
 * only used in cases when the same logic couldn't be accomplished using the FHIRPath
 */
class CustomFhirPathFunctions : FhirPathFunctions {

    /**
     * Custom FHIR Function names used to map from the string used in the FHIR path
     * to the function name in the CustomFHIRFunctions class
     */
    enum class CustomFhirPathFunctionNames {
        LivdTableLookup,
        GetFakeValueForElement,
        FIPSCountyLookup,
        GetStateFromZipCode,
        ;

        companion object {
            /**
             * Get from a [functionName].
             * @return the function name enum or null if not found
             */
            fun get(functionName: String?): CustomFhirPathFunctionNames? {
                return try {
                    functionName?.let { CustomFhirPathFunctionNames.valueOf(it.replaceFirstChar(Char::titlecase)) }
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    /**
     * Get the function details for a given [functionName].
     * @return the function details
     */
    override fun resolveFunction(
        functionName: String?,
        additionalFunctions: FhirPathFunctions?,
    ): FunctionDetails? {
        return when (CustomFhirPathFunctionNames.get(functionName)) {
            CustomFhirPathFunctionNames.LivdTableLookup -> {
                FunctionDetails(
                    "looks up data in the LIVD table that match the information provided",
                    1,
                    1
                )
            }
            CustomFhirPathFunctionNames.GetFakeValueForElement -> {
                FunctionDetails(
                    "Returns a fake value based on the type requested",
                    1,
                    2
                )
            }
            CustomFhirPathFunctionNames.FIPSCountyLookup -> {
                FunctionDetails(
                    "looks up data in the fipps county table that match the information provided",
                    2,
                    2
                )
            }

            CustomFhirPathFunctionNames.GetStateFromZipCode -> {
                FunctionDetails(
                    "Looks up the states that match the given zip code",
                    0,
                    0
                )
            }

            else -> null
        }
    }

    /**
     * Execute the function on a [focus] resource for a given [functionName] and [parameters].
     * @return the function result
     */
    override fun executeFunction(
        focus: MutableList<Base>?,
        functionName: String?,
        parameters: MutableList<MutableList<Base>>?,
        additionalFunctions: FhirPathFunctions?,
    ): MutableList<Base> {
        check(focus != null)
        return (
            when (CustomFhirPathFunctionNames.get(functionName)) {
                CustomFhirPathFunctionNames.LivdTableLookup -> {
                    livdTableLookup(focus, parameters)
                }
                CustomFhirPathFunctionNames.GetFakeValueForElement -> {
                    getFakeValueForElement(parameters)
                }
                CustomFhirPathFunctionNames.FIPSCountyLookup -> {
                    fipsCountyLookup(parameters)
                }
                CustomFhirPathFunctionNames.GetStateFromZipCode -> {
                    getStateFromZipCode(focus)
                }
                else -> error(IllegalStateException("Tried to execute invalid FHIR Path function $functionName"))
            }
            )
    }

    /**
     * Get the LOINC Code from the LIVD table based on the device id, equipment model id, test kit name id, or the
     * element model name
     * @return a list with one value denoting the LOINC Code, or an empty list
     */
    fun livdTableLookup(
        focus: MutableList<Base>,
        parameters: MutableList<MutableList<Base>>?,
        metadata: Metadata = Metadata.getInstance(),
    ): MutableList<Base> {
        val lookupTable = metadata.findLookupTable(name = LivdLookup.livdTableName)

        if (focus.size != 1) {
            throw SchemaException("Must call the livdTableLookup function on a single observation")
        }

        val observation = focus.first()
        if (observation !is Observation) {
            throw SchemaException("Must call the livdTableLookup function on an observation")
        }

        var result: String? = ""
        // Maps to OBX 17 CWE.1 Which is coding[1].code
        val testPerformedCode = (observation as Observation?)?.code?.coding?.firstOrNull()?.code
        val deviceId = (observation as Observation?)?.method?.coding?.firstOrNull()?.code
        if (!deviceId.isNullOrEmpty()) {
            result = LivdLookup.find(
                testPerformedCode = testPerformedCode,
                deviceId = deviceId,
                tableRef = lookupTable,
                tableColumn = parameters!!.first().first().primitiveValue()
            )
        }

        // Maps to OBX 18 which is mapped to Device.identifier
        val equipmentModelId = (observation.device.resource as Device?)?.identifier?.firstOrNull()?.id
        if (result.isNullOrBlank() && !equipmentModelId.isNullOrEmpty()) {
            result = LivdLookup.find(
                testPerformedCode = testPerformedCode,
                equipmentModelId = equipmentModelId,
                tableRef = lookupTable,
                tableColumn = parameters!!.first().first().primitiveValue()
            )
        }

        val deviceName = (observation.device.resource as Device?)?.deviceName?.firstOrNull()?.name
        if (result.isNullOrBlank() && !deviceName.isNullOrBlank()) {
            result = LivdLookup.find(
                testPerformedCode = testPerformedCode,
                equipmentModelName = deviceName,
                tableRef = lookupTable,
                tableColumn = parameters!!.first().first().primitiveValue()
            )
        }

        return if (result.isNullOrBlank()) {
            mutableListOf(StringType(null))
        } else {
            mutableListOf(StringType(result))
        }
    }

    /**
     * Get a fake value to use for a specific type of field
     * @param focus not actually required here. What we call it on is completely irrelevant
     * @param parameters takes a field type which is based off of element type with an optional second param for state,
     * required for types city, postal code,
     * @param metadata used to get the geo data table
     * @return a string with the fake value
     */
    fun getFakeValueForElement(
        parameters: MutableList<MutableList<Base>>?,
        metadata: Metadata = Metadata.getInstance(),
    ): MutableList<Base> {
        val type = GeoData.DataTypes.valueOf(parameters!!.first().first().primitiveValue())
        if (type == GeoData.DataTypes.CITY || type == GeoData.DataTypes.COUNTY ||
            type == GeoData.DataTypes.POSTAL_CODE
        ) {
            if (parameters.size != 2) {
                throw SchemaException(
                    "Must call the getFakeValueForElement function for city or postal code with" +
                        " a state specified."
                )
            }
        }

        return mutableListOf(
            StringType(
                when (type) {
                    GeoData.DataTypes.CITY -> getRandomGeoValue(
                        parameters[1].first().primitiveValue(),
                        GeoData.ColumnNames.CITY,
                        metadata
                    )
                    GeoData.DataTypes.POSTAL_CODE -> getRandomGeoValue(
                        parameters[1].first().primitiveValue(),
                        GeoData.ColumnNames.ZIP_CODE,
                        metadata
                    )
                    GeoData.DataTypes.TESTING_LAB -> "Any lab USA"
                    GeoData.DataTypes.UUID -> UUID.randomUUID().toString()
                    GeoData.DataTypes.FACILITY_NAME -> "Any facility USA"
                    GeoData.DataTypes.NAME_OF_SCHOOL -> "Any Fake School"
                    GeoData.DataTypes.REFERENCE_RANGE -> randomChoice("", "Normal", "Abnormal", "Negative")
                    GeoData.DataTypes.RESULT_FORMAT -> "CWE"
                    GeoData.DataTypes.PATIENT_PREFERRED_LANGUAGE -> randomChoice("ENG", "FRE", "SPA", "CHI", "KOR")
                    GeoData.DataTypes.PATIENT_COUNTRY -> "USA"
                    GeoData.DataTypes.SITE_OF_CARE -> FakeReport.getRandomSiteOfCare()
                    GeoData.DataTypes.PATIENT_AGE_AND_UNITS -> getPatientAgeAndUnits()
                    GeoData.DataTypes.COUNTY -> getRandomGeoValue(
                        parameters[1].first().primitiveValue(),
                        GeoData.ColumnNames.COUNTY,
                        metadata
                    )
                    GeoData.DataTypes.EQUIPMENT_MODEL_NAME -> randomChoice(
                        "LumiraDx SARS-CoV-2 Ag Test",
                        "BD Veritor System for Rapid Detection of SARS-CoV-2"
                    )
                    GeoData.DataTypes.TEST_PERFORMED_CODE -> randomChoice(
                        "95209-3",
                        "94558-4"
                    )
                    GeoData.DataTypes.OTHER_TEXT -> "I am some random text"
                    GeoData.DataTypes.BLANK -> ""
                    GeoData.DataTypes.TEXT_OR_BLANK -> randomChoice("I am some random text", "")
                    GeoData.DataTypes.NUMBER -> Random.nextInt().toString().replace("-", "")
                    GeoData.DataTypes.DATE -> {
                        val minDay: Long = LocalDate.of(2000, 1, 1).toEpochDay()
                        val maxDay: Long = LocalDate.of(2023, 12, 31).toEpochDay()
                        val randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay)
                        LocalDate.ofEpochDay(randomDay).toString()
                    }
                    GeoData.DataTypes.BIRTHDAY -> {
                        val minDay: Long = LocalDate.of(1950, 1, 1).toEpochDay()
                        val maxDay: Long = LocalDate.of(2023, 12, 31).toEpochDay()
                        val randomDay = ThreadLocalRandom.current().nextLong(minDay, maxDay)
                        LocalDate.ofEpochDay(randomDay).toString()
                    }
                    GeoData.DataTypes.DATETIME -> DateUtilities.getDateAsFormattedString(
                        getRandomDate().toInstant(),
                        DateUtilities.datetimePattern
                    )
                    GeoData.DataTypes.HD -> "0.0.0.0.1"
                    GeoData.DataTypes.EI -> "SomeEntityID"
                    GeoData.DataTypes.ID -> Faker().numerify("######")
                    GeoData.DataTypes.ID_CLIA -> Faker().numerify("##D#######")
                    GeoData.DataTypes.ID_DLN -> Faker().idNumber().valid()
                    GeoData.DataTypes.ID_SSN -> Faker().idNumber().validSvSeSsn()
                    GeoData.DataTypes.ID_NPI -> NPIUtilities.generateRandomNPI(Faker())
                    GeoData.DataTypes.STREET -> Faker().address().streetAddress()
                    GeoData.DataTypes.PERSON_GIVEN_NAME -> {
                        val fullName = Faker().name().fullName()
                        fullName.substring(0, fullName.indexOf(" "))
                    }
                    GeoData.DataTypes.PERSON_FAMILY_NAME -> {
                        val fullName = Faker().name().fullName()
                        fullName.substring(fullName.lastIndexOf(' '), fullName.length)
                    }
                    GeoData.DataTypes.TELEPHONE -> Faker().numerify("12#########")
                    GeoData.DataTypes.EMAIL -> Faker().name().fullName()
                        .replace(" ", "")
                        .replace(".", "") + "@gmail.com"
                    GeoData.DataTypes.SPECIMEN_SOURCE_SITE_CODE -> "71836000"
                    GeoData.DataTypes.TEST_RESULT_STATUS -> randomChoice("F", "C")
                    GeoData.DataTypes.PROCESSING_MODE_CODE -> "P"
                    GeoData.DataTypes.VALUE_TYPE -> "CWE"
                    GeoData.DataTypes.TEST_RESULT -> randomChoice("260373001", "260415000", "419984006")
                    GeoData.DataTypes.STREET_ADDRESS_2 -> randomChoice("Apt. 305", "Suite 22", "Building 2")
                    GeoData.DataTypes.ID_NUMBER -> randomChoice("ABC123", "123LKJ", "bjh098")
                    GeoData.DataTypes.SOURCE_OF_COMMENT -> randomChoice("L", "O", "P")
                }
            )
        )
    }

    private fun getRandomGeoValue(
        stateAbbrv: String,
        columnsName: GeoData.ColumnNames,
        metadata: Metadata = Metadata.getInstance(),
    ): String? {
        val geoTable = metadata.findLookupTable(name = GeoData.geoTableName)
        return GeoData.pickRandomLocationInState(stateAbbrv, columnsName, geoTable)
    }

    private fun getPatientAgeAndUnits(): String {
        val unit = randomChoice("months", "years", "days")
        val value = when (unit) {
            "months" -> Random.nextInt(1, 18)
            "days" -> Random.nextInt(0, 364)
            "years" -> Random.nextInt(1, 120)
            else -> 5
        }

        return "$value $unit"
    }

    private fun getRandomDate(): Date {
        // Get an Epoch value roughly between 1940 and 2023
        // -946771200000L = January 1, 1940
        // Add up to 83 years to it (using modulus on the next long)
        val ms = -946771200000L + (abs(Random.nextLong()) % (83L * 365 * 24 * 60 * 60 * 1000))

        // Construct a date`
        return Date(ms)
    }

    /**
     * Get the FIPS Code from the FIPS County table based on the county name and state
     * @return a String with the FIPS Code
     */
    fun fipsCountyLookup(
        parameters: MutableList<MutableList<Base>>?,
        metadata: Metadata = Metadata.getInstance(),
    ): MutableList<Base> {
        val lookupTable = metadata.findLookupTable(name = "fips-county")
        var filters = lookupTable?.FilterBuilder() ?: error("Could not find table fips-county")

        // it says it cannot be null, but it can
        if (parameters == null || parameters.first().isNullOrEmpty()) {
            return mutableListOf(StringType(""))
        }

        val county = parameters.first().first().primitiveValue() ?: return mutableListOf(StringType(""))
        filters = filters.equalsIgnoreCase("County", county)

        // it says it cannot be null, but it can
        if (parameters[1].isNullOrEmpty()) {
            return mutableListOf(StringType(county))
        }
        val state = parameters[1].first().primitiveValue() ?: return mutableListOf(StringType(county))
        filters = filters.equalsIgnoreCase("State", state.uppercase())
        val result = filters.findSingleResult("FIPS")
        return if (!result.isNullOrBlank()) {
            mutableListOf(StringType(result))
        } else {
            mutableListOf(StringType(parameters.first().first().primitiveValue()))
        }
    }

    /**
     * Returns a comma-separated string of the states that
     * match the zip code stored in the [focus] element.
     * @return a mutable list containing the state abbreviations
     */
    fun getStateFromZipCode(
        focus: MutableList<Base>,
        metadata: Metadata = Metadata.getInstance(),
    ): MutableList<Base> {
        val lookupTable = metadata.findLookupTable("zip-code-data")
        var filters = lookupTable?.FilterBuilder() ?: error("Could not find table zip-code-data")

        val zipCode = focus[0].primitiveValue()
        filters = filters.isEqualTo("zipcode", zipCode)
        val result = filters.findAllUnique("state_abbr")

        val stateList = result.joinToString(",")
        return mutableListOf(StringType(stateList))
    }
}