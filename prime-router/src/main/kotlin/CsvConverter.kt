package gov.cdc.prime.router

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.InputStream
import java.io.OutputStream

/**
 * A converter differs from a serialization in that
 */
object CsvConverter {
    private data class Mapping(
        val useCsv: Map<String, Element.CsvField>,
        val useMapper: Map<String, Pair<Mapper, List<String>>>,
        val useDefault: Map<String, String>,
    )

    fun read(schema: Schema, input: InputStream, source: Source): Report {
        return read(schema, input, listOf(source))
    }

    fun read(
        schema: Schema,
        input: InputStream,
        sources: List<Source>,
        destination: OrganizationService? = null,
    ): Report {
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(input)
        if (rows.isEmpty()) {
            return Report(schema, emptyList(), sources, destination)
        }
        val mapping = buildMappingForReading(schema, rows[0])
        val mappedRows = rows.map { mapRow(schema, mapping, it) }
        return Report(schema, mappedRows, sources, destination)
    }

    fun write(report: Report, output: OutputStream) {
        val schema = report.schema

        fun buildHeader(): List<String> = schema.csvFields.map { it.name }

        fun buildRows(): List<List<String>> {
            return report.rowIndices.map { row ->
                schema
                    .elements
                    .flatMap { element ->
                        if (element.csvFields != null) {
                            element.csvFields.map { field ->
                                val value = report.getString(row, element.name)
                                    ?: error("Internal Error: table is missing '${element.name} column")
                                element.toFormatted(value, field)
                            }
                        } else {
                            emptyList()
                        }
                    }
            }
        }

        val allRows = listOf(buildHeader()).plus(buildRows())
        csvWriter {
            lineTerminator = "\n"
            outputLastLineTerminator = true
        }.writeAll(allRows, output)
    }

    private fun buildMappingForReading(schema: Schema, row: Map<String, String>): Mapping {
        val expectedHeaders = schema.csvFields.map { it.name }.toSet()
        val actualHeaders = row.keys.toSet()
        val missingHeaders = expectedHeaders.minus(actualHeaders)
        if (missingHeaders.size > 0) {
            error("CSV is missing headers for: ${missingHeaders.joinToString(", ")}")
        }
        val useCsv = schema
            .elements
            .filter { it.csvFields != null }
            .map { it.name to it.csvFields!!.first() } // TODO: be more flexible than first field
            .toMap()
        val useMapper = schema
            .elements
            .filter { it.mapper?.isNotBlank() == true }
            .map {
                val (name, args) = Mappers.parseMapperField(it.mapper!!)
                val mapper = Metadata.findMapper(name)
                    ?: error("Schema Error: ${schema.name} mapper $name is not found")
                it.name to Pair(mapper, args)
            }.toMap()
        val useDefault = schema
            .elements
            .filter { it.default?.isNotBlank() == true }
            .map { it.name to it.default!! }
            .toMap()
        return Mapping(useCsv, useMapper, useDefault)
    }

    /**
     * For a input row from the CSV file map to a schema defined row by
     *  1. Using values from the csv file
     *  2. Using a mapper defined by the schema
     *  3. Using the default defined by the schema
     *
     * Also, format values into the normalized format for the type
     */
    private fun mapRow(schema: Schema, mapping: Mapping, inputRow: Map<String, String>): List<String> {
        val lookupValues = mutableMapOf<String, String>()

        return schema.elements.map { element ->
            fun addToLookup(normalized: String): String {
                lookupValues[element.name] = normalized
                return normalized
            }

            when (element.name) {
                in mapping.useCsv -> {
                    val csvField = mapping.useCsv.getValue(element.name)
                    val value = inputRow.getValue(csvField.name)
                    addToLookup(element.toNormalized(value, csvField))
                }
                in mapping.useMapper -> {
                    val (mapper, args) = mapping.useMapper.getValue(element.name)
                    val elementNames = mapper.elementNames(args)
                    val mapperValues = elementNames.map {
                        it to (
                            lookupValues[it]
                                ?: error("Internal Error: no lookup values for '$it'")
                            )
                    }.toMap()
                    addToLookup(mapper.apply(args, mapperValues) ?: element.default ?: "")
                }
                in mapping.useDefault -> {
                    addToLookup(mapping.useDefault.getValue(element.name))
                }
                else -> {
                    error("Schema Error: '${schema.name}' element '${element.name}' does not have a value")
                }
            }
        }
    }
}