package gov.cdc.prime.router

import com.github.javafaker.Faker
import java.text.SimpleDateFormat
import java.util.Random
import java.util.concurrent.TimeUnit

class FakeReport(val metadata: Metadata) {
    class RowContext(findLookupTable: (String) -> LookupTable?) {
        val faker = Faker()
        val patientName = faker.name()
        val equipmentModel = randomChoice(
            "BinaxNOW COVID-19 Ag Card",
            "BD Veritor System for Rapid Detection of SARS-CoV-2*"
        )

        val state = randomChoice("FL", "PA", "TX", "AZ")
        val county = findLookupTable("fips-county")?.let {
            if (state == "AZ") {
                randomChoice("Pima", "Yuma")
            } else {
                randomChoice(it.filter("State", state, "County"))
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
                    element.nameContains("lab_name") -> "Any lab USA"
                    element.nameContains("facility_name") -> "Any facility USA"
                    else -> faker.lorem().characters(5, 10)
                }
            }
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
                    else -> {
                        val valueSet = element.valueSetRef
                            ?: error("ValueSet ${element.valueSet} is not available}")
                        val possibleValues = valueSet.values.map { it.code }.toTypedArray()
                        randomChoice(*possibleValues)
                    }
                }
            }
            Element.Type.TABLE -> {
                val lookupTable = element.tableRef
                    ?: error("LookupTable ${element.table} is not available")
                when (element.table) {
                    "LIVD-2020-11-18" -> {
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
                "0.0.0.0.1"
            }
            Element.Type.ID -> faker.numerify("######")
            Element.Type.ID_CLIA -> faker.numerify("##D#######") // Ex, 03D1021379
            Element.Type.ID_DLN -> faker.idNumber().valid()
            Element.Type.ID_SSN -> faker.idNumber().validSvSeSsn()
            Element.Type.ID_NPI -> faker.numerify("##########")
            Element.Type.STREET -> if (element.name.contains("2")) "" else faker.address().streetAddress()
            Element.Type.PERSON_NAME -> {
                when {
                    element.nameContains("first") -> context.patientName.firstName()
                    element.nameContains("last") -> context.patientName.lastName()
                    element.nameContains("middle") -> context.patientName.firstName() // no middle name in faker
                    element.nameContains("suffix") -> randomChoice(context.patientName.suffix(), "")
                    else -> TODO()
                }
            }
            Element.Type.TELEPHONE -> faker.numerify("##########:1:") // faker.phoneNumber().cellPhone()
            Element.Type.EMAIL -> "${context.patientName.username()}@email.com"
            null -> error("Invalid element type for ${element.name}")
        }
    }

    private fun buildRow(schema: Schema): List<String> {
        val context = RowContext(metadata::findLookupTable)
        return schema.elements.map { buildColumn(it, context) }
    }

    fun build(schema: Schema, count: Int = 10, source: Source): Report {
        val rows = (0 until count).map { buildRow(schema) }.toList()
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
    }
}