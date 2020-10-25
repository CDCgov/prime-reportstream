package gov.cdc.prime.router

import com.github.javafaker.Faker
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Random

class FakeTable {
    companion object {
        fun randomChoice(vararg choices: String): String {
            val random = Random()
            return choices[random.nextInt(choices.size)]
        }

        fun buildColumn(element: Element): String {
            val faker = Faker()
            val address = faker.address()
            val patient_name = faker.name()

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
                Element.Type.CODED -> randomChoice(*(element.valueSet?.toTypedArray() ?: arrayOf("random CODED")))
                Element.Type.CODED_HL7 -> randomChoice(
                    *(element.valueSet?.toTypedArray() ?: arrayOf("random HL7"))
                )
                Element.Type.CODED_LONIC -> faker.idNumber().valid()
                Element.Type.CODED_SNOMED -> randomChoice(
                    *(element.valueSet?.toTypedArray() ?: arrayOf("random SNOMED"))
                )
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
                Element.Type.STATE -> address.stateAbbr()
                Element.Type.COUNTY -> "Any County"
                Element.Type.PERSON_NAME -> {
                    when {
                        element.nameContains("first") -> patient_name.firstName()
                        element.nameContains("last") -> patient_name.lastName()
                        element.nameContains("middle") -> patient_name.firstName()
                        element.nameContains("suffix") -> randomChoice(patient_name.suffix(), "")
                        else -> TODO()
                    }
                }
                Element.Type.TELEPHONE -> faker.phoneNumber().phoneNumber()
                Element.Type.EMAIL -> "${patient_name.username()}@email.com"
                null -> error("Invalid element type for ${element.name}")
            }
        }

        fun buildRow(schema: Schema): List<String> {
            return schema.elements.map { buildColumn(it) }
        }

        fun build(name: String, schema: Schema, count: Int = 10): MappableTable {
            val rows = (0 until count).map { buildRow(schema) }.toList()
            return MappableTable(name, schema, rows)
        }
    }
}