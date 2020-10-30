package gov.cdc.prime.router

import com.github.javafaker.Faker
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Random

class FakeTable {
    companion object {
        private fun randomChoice(vararg choices: String): String {
            val random = Random()
            return choices[random.nextInt(choices.size)]
        }

        internal fun buildColumn(element: Element, findValueSet: (name: String) -> ValueSet?): String {
            val faker = Faker()
            val address = faker.address()
            val patientName = faker.name()

            return when (element.type) {
                Element.Type.CITY -> address.cityName()
                Element.Type.POSTAL_CODE -> address.zipCode().toString()
                Element.Type.TEXT -> {
                    when {
                        element.nameContains("lab_name") -> "Any lab USA"
                        else -> faker.lorem().characters(5, 10)
                    }
                }
                Element.Type.NUMBER -> faker.number().numberBetween(1, 10).toString()
                Element.Type.DATE -> {
                    val date = when {
                        element.nameContains("DOB") -> faker.date().birthday(0, 100)
                        else -> faker.date().past(10, TimeUnit.DAYS)
                    }
                    SimpleDateFormat("YYYYMMDD").format(date)
                }
                Element.Type.DATETIME -> {
                    val formatter = SimpleDateFormat("YYYYMMDDhhmm")
                    formatter.format(faker.date().past(10, TimeUnit.DAYS))
                }
                Element.Type.DURATION -> TODO()
                Element.Type.CODE -> {
                    val valueSet =
                        findValueSet(element.valueSet ?: "") ?: error("ValueSet ${element.valueSet} is not available}")
                    val possibleValues = valueSet.values.map {
                        when {
                            element.isCodeText -> it.display ?: "fake display"
                            element.isCode -> it.code ?: "fake code"
                            else -> error("element ${element.name} has is not a CODE type")
                        }
                    }.toTypedArray()
                    randomChoice(*possibleValues)
                }
                Element.Type.HD -> {
                    when {
                        element.nameContains("sending_application") -> "fake app"
                        else -> "fake description"
                    }
                }
                Element.Type.ID -> faker.idNumber().valid()
                Element.Type.ID_DLN -> faker.idNumber().valid()
                Element.Type.ID_SSN -> faker.idNumber().validSvSeSsn()
                Element.Type.STREET -> if (element.name.contains("2")) "" else address.streetAddress()
                Element.Type.STATE -> randomChoice("AZ", "FL", "PA")
                Element.Type.COUNTY -> "Any County"
                Element.Type.PERSON_NAME -> {
                    when {
                        element.nameContains("first") -> patientName.firstName()
                        element.nameContains("last") -> patientName.lastName()
                        element.nameContains("middle") -> patientName.firstName()
                        element.nameContains("suffix") -> randomChoice(patientName.suffix(), "")
                        else -> TODO()
                    }
                }
                Element.Type.TELEPHONE -> faker.phoneNumber().cellPhone()
                Element.Type.EMAIL -> "${patientName.username()}@email.com"
                null -> error("Invalid element type for ${element.name}")
            }
        }

        private fun buildRow(schema: Schema): List<String> {
            return schema.elements.map { buildColumn(it, Metadata::findValueSet) }
        }

        fun build(name: String, schema: Schema, count: Int = 10): MappableTable {
            val rows = (0 until count).map { buildRow(schema) }.toList()
            return MappableTable(name, schema, rows)
        }
    }
}