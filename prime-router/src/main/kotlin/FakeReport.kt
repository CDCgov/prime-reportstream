package gov.cdc.prime.router

import com.github.javafaker.Faker
import com.github.javafaker.Name
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.Hl7Utilities
import gov.cdc.prime.router.common.NPIUtilities
import gov.cdc.prime.router.metadata.ConcatenateMapper
import gov.cdc.prime.router.metadata.ElementAndValue
import gov.cdc.prime.router.metadata.Mapper
import gov.cdc.prime.router.metadata.Mappers
import gov.cdc.prime.router.metadata.UseMapper
import org.apache.logging.log4j.kotlin.Logging
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.Random

/*
    The FakeDataService class was created to separate the logic
    of generating faked data from the FakeReport class which
    packages the faked data and creates a report out of it.

    The goal is to allow us to inject the FakeDataService into
    other areas and use its logic there, for example, generating
    synthetic data, which is data that originates from outside of
    prime and is then shuffled and/or faked
*/

private const val zipCodeData = "zip-code-data"

class FakeDataService : Logging {
    fun getFakeValueForElement(
        element: Element,
        context: FakeReport.RowContext,
    ): String {
        val faker = context.faker
        // creates fake text data
        fun createFakeText(element: Element): String {
            return when {
                element.nameContains("name_of_testing_lab") -> "Any lab USA"
                element.nameContains("lab_name") -> "Any lab USA"
                element.nameContains("sender_id") -> "${element.default}" // Allow the default to fill this in
                element.nameContains("facility_name") -> context.facilitiesName ?: "Any facility USA"
                element.nameContains("name_of_school") -> randomChoice("", context.schoolName)
                element.nameContains("reference_range") -> randomChoice("", "Normal", "Abnormal", "Negative")
                element.nameContains("result_format") -> "CWE"
                element.nameContains("patient_preferred_language") -> randomChoice("ENG", "FRE", "SPA", "CHI", "KOR")
                element.nameContains("patient_country") -> "USA"
                element.nameContains("site_of_care") -> if (context.facilitiesName.isNullOrEmpty()) {
                    randomChoice(
                        "airport", "assisted_living", "camp", "correctional_facility", "employer", "fqhc",
                        "government_agency", "hospice", "hospital", "lab", "nursing_home", "other",
                        "pharmacy", "primary_care", "shelter", "treatment_center", "university", "urgent_care"
                    )
                } else {
                    "k12"
                }
                element.nameContains("patient_age_and_units") -> {
                    val unit = randomChoice("months", "years", "days")
                    val value = when (unit) {
                        "months" -> faker.number().numberBetween(1, 18)
                        "days" -> faker.number().numberBetween(0, 364)
                        "years" -> faker.number().numberBetween(1, 120)
                        else -> TODO()
                    }

                    "$value $unit"
                }
                else -> faker.lorem().characters(5, 10)
            }
        }

        // creates a fake name for a person based on the patient name
        // in the row context
        fun createFakeName(element: Element): String {
            return when {
                element.nameContains("first") -> context.patientName.firstName()
                element.nameContains("last") -> context.patientName.lastName()
                element.nameContains("middle") -> context.patientName.firstName() // no middle name in faker
                element.nameContains("suffix") -> randomChoice(context.patientName.suffix(), "")
                else -> TODO()
            }
        }

        // creates a fake date and formats it according to the element's
        // provided formatting string
        fun createFakeDate(element: Element): String {
            val date = when {
                element.nameContains("DOB") -> faker.date().birthday(1, 100)
                else -> context.fakeDate
            }
            // Faker returns an older style Java date, which we need to convert to an
            // instance, and then also set to UTC, so we can then format it for our purposes.
            // The Java date object is super hard to work with and brittle.
            return date.toInstant().atZone(ZoneId.of("UTC")).let {
                DateUtilities.getDateAsFormattedString(
                    it,
                    DateUtilities.datePattern
                )
            }
        }

        // creates a fake date time and then formats it
        fun createFakeDateTime(): String {
            // Faker returns an older style Java date, which we need to convert to an
            // instance, and then also set to UTC, so we can then format it for our purposes.
            // The Java date object is super hard to work with and brittle.
            val date = context.fakeDate.toInstant().atZone(ZoneId.of("UTC"))
            return DateUtilities.getDateAsFormattedString(date, DateUtilities.datetimePattern)
        }

        // creates a fake phone number for the element
        fun createFakePhoneNumber(element: Element): String {
            val csvField = element.csvFields?.get(0)
            val phoneNumberFormat = csvField?.format ?: "2#########:1:"
            return faker.numerify(phoneNumberFormat)
        }

        // creates a fake email for the patient name that is part
        // of the row context
        fun createFakeEmail(): String {
            return "${context.patientName.username()}@email.com"
        }

        // ID values typically come from valuesets, so we pass in the element
        // and then we examine both the alt values and the value set for the element
        // and we then pull a random value from all the potential values available.
        // the rationale here is that if an element specifies alt values, they are preferred
        // to the values held in the value set collection, and so we use them first
        fun createFakeValueFromValueSet(element: Element): String {
            val altValues = element.altValues
            val valueSet = element.valueSetRef
            return when (element.name) {
                "value_type" -> "CWE"
                else -> {
                    // if the code defines alternate values in the schema we need to
                    // output them here
                    val possibleValues = if (altValues?.isNotEmpty() == true) {
                        altValues.map { it.code }.toTypedArray()
                    } else {
                        if (element.cardinality?.name == "ZERO_OR_ONE") {
                            // Pick random code from the ValueSet.Value and add ""
                            val code = valueSet?.values?.asSequence()?.shuffled()?.take(1)?.map { it.code }
                                ?.toList()?.toTypedArray() ?: arrayOf("")
                            code.plus("")
                        } else {
                            valueSet?.values?.map { it.code }?.toTypedArray() ?: arrayOf("")
                        }
                    }

                    randomChoice(*possibleValues)
                }
            }
        }

        // code values typically come from valuesets, so we pass in the element
        // and then we examine both the alt values and the value set for the element
        // and we then pull a random value from all the potential values available.
        // the rationale here is that if an element specifies alt values, they are preferred
        // to the values held in the value set collection, and so we use them first
        fun createFakeCodeValue(element: Element): String {
            return when (element.name) {
                "specimen_source_site_code" -> "71836000"
                "test_result_status" -> randomChoice("F", "C")
                "processing_mode_code" -> "P"
                "value_type" -> "CWE"
                "test_result" ->
                    // Reduce the choice to between detected, not detected, and uncertain for more typical results
                    randomChoice("260373001", "260415000", "419984006")
                else -> {
                    createFakeValueFromValueSet(element)
                }
            }
        }

        // table values work in a similar fashion to the valuesets, but are
        // more flexible and allow for more filtering.
        fun createFakeTableValue(element: Element): String {
            val lookupTable = element.tableRef
                ?: error("LookupTable ${element.table} is not available")
            return when {
                element.table?.startsWith("LIVD-SARS-CoV-2") == true -> {
                    if (element.tableColumn == null) return ""
                    lookupTable.FilterBuilder().equalsIgnoreCase("Model", context.equipmentModel)
                        .findSingleResult(element.tableColumn)
                        ?: error(
                            "Schema Error: Could not lookup ${context.equipmentModel} " +
                                "to ${element.tableColumn}"
                        )
                }
                element.table?.startsWith("LIVD-Supplemental") == true -> {
                    if (element.tableColumn == null)
                        return ""
                    element.default ?: ""
                }
                element.table == "fips-county" -> {
                    when {
                        element.nameContains("state") -> context.state
                        element.nameContains("county") -> context.county
                        (element.default == null) -> ""
                        else -> {
                            logger.warn("Add this column to the ${element.table} table")
                            ""
                        }
                    }
                }
                element.table == zipCodeData -> {
                    when {
                        element.nameContains("state") -> context.state
                        element.nameContains("county") -> context.county
                        element.nameContains("zip") -> context.zipCode
                        element.nameContains("city") -> context.city
                        (element.default == null) -> ""
                        else -> {
                            logger.warn("Add this column to the ${element.table} table")
                            ""
                        }
                    }
                }
                else -> TODO("Add this table ${element.table}")
            }
        }

        // now that we've created all our functions, we can call them in our
        // when statement here, depending on the type of the element passed in.
        // each element has a type, and depending on the type defined on the
        // element, we call into some of the functions above
        return when (element.type) {
            Element.Type.CITY -> context.city
            Element.Type.POSTAL_CODE -> context.zipCode
            Element.Type.TEXT -> createFakeText(element)
            Element.Type.BLANK -> ""
            Element.Type.TEXT_OR_BLANK -> randomChoice("", createFakeText(element))
            Element.Type.NUMBER -> faker.number().numberBetween(1, 10).toString()
            Element.Type.DATE -> createFakeDate(element)
            Element.Type.DATETIME -> createFakeDateTime()
            Element.Type.DURATION -> TODO()
            Element.Type.CODE -> createFakeCodeValue(element)
            Element.Type.TABLE, Element.Type.TABLE_OR_BLANK -> createFakeTableValue(element)
            Element.Type.HD -> element.default ?: "0.0.0.0.1"
            Element.Type.EI -> element.default ?: "SomeEntityID"
            Element.Type.ID -> createFakeValueFromValueSet(element).ifBlank { faker.numerify("######") }
            Element.Type.ID_CLIA -> faker.numerify("##D#######") // Ex, 03D1021379
            Element.Type.ID_DLN -> faker.idNumber().valid()
            Element.Type.ID_SSN -> faker.idNumber().validSvSeSsn()
            Element.Type.ID_NPI -> NPIUtilities.generateRandomNPI(faker)
            Element.Type.STREET -> faker.address().streetAddress()
            Element.Type.STREET_OR_BLANK -> ""
            Element.Type.PERSON_NAME -> createFakeName(element)
            Element.Type.TELEPHONE -> createFakePhoneNumber(element)
            Element.Type.EMAIL -> createFakeEmail()
            null -> error("Element type is null for ${element.name}")
        }
    }

    // gives us the ability to pull a random choice from a
    // variadic array of strings passed in
    private fun randomChoice(vararg choices: String): String {
        val random = Random()
        return choices[random.nextInt(choices.size)]
    }
}

class FakeReport(val metadata: Metadata, val locale: Locale? = null) {
    private val fakeDataService: FakeDataService = FakeDataService()

    class RowContext(
        val localMetadata: Metadata,
        reportState: String? = null,
        val schemaName: String? = null,
        reportCounty: String? = null,
        includeNcesFacilities: Boolean = false,
        locale: Locale? = null
    ) {
        val findLookupTable = localMetadata::findLookupTable
        val faker = if (locale == null) Faker() else Faker(locale)
        val patientName: Name = faker.name()
        val fakeDate: Date = Date(Date().time - 3600000 * 5 * 24) // Set to past 5 days (3600000 miliseconds/day)
        val schoolName: String = faker.university().name()
        val equipmentModel = randomChoice(
            // Use only equipment that have equipment UID and equipment UID type to pass quality gate for HL7 messages
            "LumiraDx SARS-CoV-2 Ag Test",
            "BD Veritor System for Rapid Detection of SARS-CoV-2"
        )
        // find our state
        val state: String = reportState ?: randomChoice("FL", "PA", "TX", "AZ", "ND", "CO", "LA", "NM", "VT", "GU")
        // find our county
        val county: String = reportCounty ?: findLookupTable("fips-county")?.let {
            when (state) {
                "AZ" -> randomChoice("Pima", "Yuma")
                "PA" -> randomChoice("Bucks", "Chester", "Montgomery")
                else -> randomChoice(
                    it.FilterBuilder().equalsIgnoreCase("State", state)
                        .findAllUnique("County")
                )
            }
        } ?: "Prime"
        // find our zipcode
        val zipCode: String = findLookupTable(zipCodeData)?.let {
            randomChoice(
                it.FilterBuilder().equalsIgnoreCase("state_abbr", state).isEqualTo("county", county)
                    .findAllUnique("zipcode")
            )
        } ?: faker.address().zipCode().toString()
        val city: String = findLookupTable(zipCodeData)?.let {
            randomChoice(
                it.FilterBuilder().equalsIgnoreCase("state_abbr", state).isEqualTo("county", county)
                    .isEqualTo("zipcode", zipCode).findAllUnique("city")
            )
        } ?: faker.address().city().toString()

        // Do a lazy init because this table may never be used and it is large
        private val ncesLookupTable = lazy {
            localMetadata.findLookupTable("nces_id") ?: error("Unable to find the NCES ID lookup table.")
        }

        val facilitiesName: String? = if (includeNcesFacilities && !zipCode.isNullOrEmpty()) {
            ncesLookupTable.value.lookupBestMatch(
                lookupColumn = "SCHNAME",
                searchColumn = "LZIP",
                searchValue = zipCode,
                canonicalize = { Hl7Utilities.canonicalizeSchoolName(it) },
                commonWords = listOf("ELEMENTARY", "JUNIOR", "HIGH", "MIDDLE")
            )
        } else {
            null
        }
    }

    internal fun buildColumn(element: Element, context: RowContext): String {
        return fakeDataService.getFakeValueForElement(element, context)
    }

    // mapped columns often refer back to non-mapped columns in the schema. in the past, for faked
    // values we have just hard coded in the name of the element and let the faker process it,
    // but that led to some problems with reading data back in and out via our testing process.
    // therefore the method below was created, which actually looks to see if the field uses a mapper,
    // and if it does, it attempts to invoke the mapper.
    // right now, only the UseMapper and ConcatenateMapper are implemented.
    // other mapper types will need to be filled in as required.
    internal fun buildMappedColumn(element: Element, rowContext: RowContext): String {
        if (element.mapper.isNullOrEmpty()) error("Cannot build a mapped column without a mapper.")
        if (rowContext.schemaName.isNullOrEmpty()) error("Cannot fake a mapped column without the schema name")

        val schema = metadata.findSchema(rowContext.schemaName)
        val mapper = element.mapper
        val refAndArgs: Pair<Mapper, List<String>> = mapper.let {
            val (name, args) = Mappers.parseMapperField(it)
            val ref: Mapper = metadata.findMapper(name)
                ?: error("Schema Error: Could not find mapper '$name' in element '{$element.name}'")
            Pair(ref, args)
        }

        return when (val mapperRef: Mapper = refAndArgs.first) {
            is UseMapper, is ConcatenateMapper -> {
                // find element
                val elementNames = refAndArgs.second
                val useElements = elementNames.map {
                    schema?.findElement(it)
                }

                val evs = useElements.map {
                    if (it == null) return ""

                    ElementAndValue(it, buildColumn(it, rowContext))
                }

                // get fake data for element
                mapperRef.apply(element, refAndArgs.second, evs).value ?: ""
            }
            else -> buildColumn(element, rowContext)
        }
    }

    private fun buildRow(
        schema: Schema,
        targetState: String? = null,
        targetCounty: String? = null,
        includeNcesFacilities: Boolean = false
    ): List<String> {
        val context = RowContext(
            metadata,
            targetState,
            schemaName = schema.name,
            targetCounty,
            includeNcesFacilities,
            locale
        )
        return schema.elements.map {
            if (it.mapper.isNullOrEmpty())
                buildColumn(it, context)
            else
                buildMappedColumn(it, context)
        }
    }

    fun build(
        schema: Schema,
        count: Int = 10,
        source: Source,
        targetStates: String? = null,
        targetCounties: String? = null,
        includeNcesFacilities: Boolean = false
    ): Report {
        val counties = targetCounties?.split(",")
        val states = if (targetStates.isNullOrEmpty()) {
            metadata.findLookupTable("fips-county")?.FilterBuilder()?.findAllUnique("State")
                ?.toList()
        } else {
            targetStates.split(",")
        }
        val rows = (0 until count).map {
            buildRow(schema, roundRobinChoice(states), roundRobinChoice(counties), includeNcesFacilities)
        }.toList()
        return Report(schema, rows, listOf(source), metadata = metadata)
    }

    companion object {
        private fun randomChoice(vararg choices: String): String {
            val random = Random()
            return choices[random.nextInt(choices.size)]
        }

        private fun randomChoice(choices: List<String>): String {
            if (choices.isEmpty()) return ""
            val random = Random()
            return choices[random.nextInt(choices.size)]
        }

        private fun randomChoices(vararg choices: String): List<String> {
            val random = Random()
            if (choices.isEmpty() || random.nextBoolean()) return emptyList()

            val selectedValues = mutableListOf<String>()
            val limit = random.nextInt(choices.size)
            for (i in 0..limit) {
                selectedValues.add(choices[random.nextInt(choices.size)])
            }

            return selectedValues.toList()
        }

        /**
         * The round robin choices is a nice option for building data with a predictable set of values.
         * eg, if I know I have two state choices, and I generate 10 rows, I'm guaranteed 5 of each.
         * Very nice for generating fake data for automated tests.
         */
        // Is hashing on an entire list a good idea?
        var iteratorStore = mutableMapOf<List<String>, Int>()
        private fun roundRobinChoice(list: List<String>?): String? {
            if (list == null) return null
            val next = ((iteratorStore[list] ?: -1) + 1) % list.size
            iteratorStore[list] = next
            return list[next]
        }
    }
}