package gov.cdc.prime.router

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.InputStream
import java.io.OutputStream

/**
 * A converter differs from a serialization in that
 */
class CsvConverter(val metadata: Metadata) {
    private data class CsvMapping(
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
        defaultValues: Map<String, String> = emptyMap(),
    ): Report {
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(input)
        if (rows.isEmpty()) {
            return Report(schema, emptyList(), sources, destination)
        }
        val mapping = buildMappingForReading(metadata, schema, defaultValues, rows[0])
        val mappedRows = rows.map { mapRow(schema, mapping, it) }
        return Report(schema, mappedRows, sources, destination)
    }

    fun write(report: Report, output: OutputStream) {
        val schema = report.schema

        fun buildHeader(): List<String> = schema.csvFields.map { it.name }

        fun buildRows(): List<List<String>> {
            return report.itemIndices.map { row ->
                schema
                    .elements
                    .flatMap { element ->
                        if (element.csvFields != null) {
                            element.csvFields.map { field ->
                                val value = report.getString(row, element.name)
                                    ?: error("Internal Error: table is missing '${element.name} column")
                                element.toFormatted(value, field.format)
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

    private fun buildMappingForReading(
        metadata: Metadata,
        schema: Schema,
        defaultValues: Map<String, String>,
        row: Map<String, String>
    ): CsvMapping {
        val expectedHeaders = schema.csvFields.map { it.name }.toSet()
        val actualHeaders = row.keys.toSet()
        val missingHeaders = expectedHeaders.minus(actualHeaders)
        if (missingHeaders.isNotEmpty()) {
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
            .map { it.name to Pair(it.mapperRef!!, it.mapperArgs!!) }
            .toMap()
        val useDefault = schema
            .elements
            .filter { it.hasDefaultValue(defaultValues) }
            .map { it.name to it.defaultValue(defaultValues) }
            .toMap()
        return CsvMapping(useCsv, useMapper, useDefault)
    }

    /**
     * For a input row from the CSV file map to a schema defined row by
     *  1. Using values from the csv file
     *  2. Using a mapper defined by the schema
     *  3. Using the default defined by the schema
     *
     * Also, format values into the normalized format for the type
     */
    private fun mapRow(schema: Schema, csvMapping: CsvMapping, inputRow: Map<String, String>): List<String> {
        val lookupValues = mutableMapOf<String, String>()

        return schema.elements.map { element ->
            fun addToLookup(normalized: String): String {
                lookupValues[element.name] = normalized
                return normalized
            }

            when (element.name) {
                in csvMapping.useCsv -> {
                    val csvField = csvMapping.useCsv.getValue(element.name)
                    val value = inputRow.getValue(csvField.name)
                    val normalizedValue = element.toNormalized(value, csvField.format)
                    addToLookup(normalizedValue)
                }
                in csvMapping.useMapper -> {
                    val (mapper, args) = csvMapping.useMapper.getValue(element.name)
                    val valueNames = mapper.valueNames(element, args)
                    val mapperValues = valueNames.map { elementName ->
                        val valueElement = schema.findElement(elementName)
                            ?: error("Schema Error: Could not find element '$elementName' for mapper '${mapper.name}'")
                        val value = lookupValues[elementName]
                            ?: error("Internal Error: no lookup values for '$elementName' for mapper '${mapper.name}'")
                        ElementAndValue(valueElement, value)
                    }
                    val value = mapper.apply(element, args, mapperValues)
                        ?: csvMapping.useDefault.getOrDefault(element.name, "")
                    addToLookup(value)
                }
                in csvMapping.useDefault -> {
                    val value = csvMapping.useDefault.getValue(element.name)
                    addToLookup(value)
                }
                else -> {
                    error("Translate Error: '${schema.name}' element '${element.name}' does not have a value")
                }
            }
        }
    }
}