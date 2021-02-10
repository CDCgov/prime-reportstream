package gov.cdc.prime.router

import com.github.javafaker.Faker
import java.text.SimpleDateFormat
import java.util.Random
import java.util.concurrent.TimeUnit

class FakeReport(val metadata: Metadata) {
    class RowContext(
        findLookupTable: (String) -> LookupTable? = { null },
        reportState: String? = null,
        val schemaName: String? = null,
        reportCounty: String? = null
    ) {
        val faker = Faker()
        val patientName = faker.name()
        val schoolName: String = faker.university().name()
        val equipmentModel = randomChoice(
            "BinaxNOW COVID-19 Ag Card",
            "BD Veritor System for Rapid Detection of SARS-CoV-2*"
        )

        val state = reportState ?: randomChoice("FL", "PA", "TX", "AZ", "ND", "CO")
        val county = reportCounty ?: findLookupTable("fips-county")?.let {
            when (state) {
                "AZ" -> randomChoice("Pima", "Yuma")
                "PA" -> randomChoice("Bucks", "Chester", "Montgomery")
                else -> randomChoice(it.filter("State", state, "County"))
            }
        }
    }

    internal fun buildColumn(
        element: Element,
        context: RowContext,
    ): String {
        val faker = context.faker
        return when (element.type) {
            Element.Type.CITY -> faker.address().city()
            Element.Type.POSTAL_CODE -> faker.address().zipCode().toString()
            Element.Type.TEXT -> {
                when {
                    element.nameContains("name_of_testing_lab") -> "Any lab USA"
                    element.nameContains("lab_name") -> "Any lab USA"
                    element.nameContains("facility_name") -> "Any facility USA"
                    element.nameContains("name_of_school") -> randomChoice("", context.schoolName)
                    element.nameContains("reference_range") -> randomChoice("", "Normal", "Abnormal", "Negative")
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
            Element.Type.BLANK -> ""
            Element.Type.TEXT_OR_BLANK -> randomChoice("", faker.lorem().characters(5, 10))
            Element.Type.NUMBER -> faker.number().numberBetween(1, 10).toString()
            Element.Type.DATE -> {
                val date = when {
                    element.nameContains("DOB") -> faker.date().birthday(0, 100)
                    else -> faker.date().past(10, TimeUnit.DAYS)
                }
                val formatter = SimpleDateFormat(Element.datePattern)
                formatter.format(date)
            }
            Element.Type.DATETIME -> {
                val date = faker.date().past(10, TimeUnit.DAYS)
                val formatter = SimpleDateFormat(Element.datetimePattern)
                formatter.format(date)
            }
            Element.Type.DURATION -> TODO()
            Element.Type.CODE -> {
                when (element.name) {
                    "specimen_source_site_code" -> "71836000"
                    "test_result_status" -> randomChoice("F", "C")
                    else -> {
                        val altValues = element.altValues
                        val valueSet = element.valueSetRef
                        // if the code defines alternate values in the schema we need to
                        // output them here
                        val possibleValues = if (altValues?.isNotEmpty() == true) {
                            altValues.map { it.code }.toTypedArray()
                        } else {
                            valueSet?.values?.map { it.code }?.toTypedArray() ?: arrayOf("")
                        }

                        randomChoice(*possibleValues)
                    }
                }
            }
            Element.Type.TABLE, Element.Type.TABLE_OR_BLANK -> {
                val lookupTable = element.tableRef
                    ?: error("LookupTable ${element.table} is not available")
                when (element.table) {
                    "LIVD-SARS-CoV-2-2021-01-20" -> {
                        if (element.tableColumn == null) return ""
                        lookupTable.lookupValue("Model", context.equipmentModel, element.tableColumn)
                            ?: error(
                                "Schema Error: Could not lookup ${context.equipmentModel} " +
                                    "to ${element.tableColumn}"
                            )
                    }
                    "fips-county" -> {
                        when {
                            element.nameContains("state") -> context.state
                            element.nameContains("county") -> context.county ?: ""
                            else -> TODO("Add this column in a table")
                        }
                    }
                    else -> TODO("Add this table")
                }
            }
            Element.Type.HD -> {
                element.default ?: "0.0.0.0.1"
            }
            Element.Type.EI -> {
                element.default ?: "SomeEntityID"
            }
            Element.Type.ID -> faker.numerify("######")
            Element.Type.ID_CLIA -> faker.numerify("##D#######") // Ex, 03D1021379
            Element.Type.ID_DLN -> faker.idNumber().valid()
            Element.Type.ID_SSN -> faker.idNumber().validSvSeSsn()
            Element.Type.ID_NPI -> faker.numerify("##########")
            Element.Type.STREET -> faker.address().streetAddress()
            Element.Type.STREET_OR_BLANK -> ""
            Element.Type.PERSON_NAME -> {
                when {
                    element.nameContains("first") -> context.patientName.firstName()
                    element.nameContains("last") -> context.patientName.lastName()
                    element.nameContains("middle") -> context.patientName.firstName() // no middle name in faker
                    element.nameContains("suffix") -> randomChoice(context.patientName.suffix(), "")
                    else -> TODO()
                }
            }
            Element.Type.TELEPHONE -> {
                val csvField = element.csvFields?.get(0)
                val phoneNumberFormat = csvField?.format ?: "2#########:1:"
                faker.numerify(phoneNumberFormat)
            }
            Element.Type.EMAIL -> "${context.patientName.username()}@email.com"
            null -> error("Invalid element type for ${element.name}")
        }
    }

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
                mapperRef.apply(element, refAndArgs.second, evs) ?: ""
            }
            else -> buildColumn(element, rowContext)
        }
    }

    private fun buildRow(schema: Schema, targetState: String? = null, targetCounty: String? = null): List<String> {
        val context = RowContext(metadata::findLookupTable, targetState, schemaName = schema.name, targetCounty)
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
        targetCounties: String? = null
    ): Report {
        val counties = targetCounties?.split(",")
        val states = targetStates?.split(",")
        val rows = (0 until count).map {
            buildRow(schema, roundRobinChoice(states), roundRobinChoice(counties))
        }.toList()
        return Report(schema, rows, listOf(source))
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