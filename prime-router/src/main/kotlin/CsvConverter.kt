package gov.cdc.prime.router

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.InputStream
import java.io.OutputStream

/**
 * The CSV serializer is crafted to handle poor data. The logic depends on the type of the element and it cardinality.
 *
 * | Type         | Cardinality | no column on input                   | empty input value                    | Invalid input value | valid input value |
 * |--------------|-------------|--------------------------------------|--------------------------------------|---------------------|-------------------|
 * | FOO          | 0..1        | mapper -> default -> empty + warning | mapper -> default -> empty + warning | empty + warning     | value             |
 * | FOO          | 1..1        | mapper -> default -> error           | mapper -> default -> error           | error               | value             |
 * | FOO_OR_BLANK | 0..1        | mapper -> default -> empty + warning | empty                                | empty + warning     | value             |
 * | FOO_OR_BLANK | 1..1        | mapper -> default -> error           | empty                                | error               | value             |
 *
 */
class CsvConverter(val metadata: Metadata) {
    private data class CsvMapping(
        val useCsv: Map<String, List<Element.CsvField>>,
        val useMapper: Map<String, Pair<Mapper, List<String>>>,
        val useDefault: Map<String, String>,
        val errors: List<String>,
        val warnings: List<String>,
    )

    private data class RowResult(
        val row: List<String>,
        val errors: List<String>,
        val warnings: List<String>,
    )

    fun read(schemaName: String, input: InputStream, source: Source): ReadResult {
        return read(schemaName, input, listOf(source))
    }

    fun read(
        schemaName: String,
        input: InputStream,
        sources: List<Source>,
        destination: OrganizationService? = null,
        defaultValues: Map<String, String> = emptyMap(),
    ): ReadResult {
        val schema = metadata.findSchema(schemaName) ?: error("Internal Error: invalid schema name '$schemaName'")
        val rows: List<Map<String, String>> = csvReader().readAllWithHeader(input)
        val errors = mutableListOf<ResultDetail>()
        val warnings = mutableListOf<ResultDetail>()

        if (rows.isEmpty()) {
            return ReadResult(Report(schema, emptyList(), sources, destination), errors, warnings)
        }

        val csvMapping = buildMappingForReading(schema, defaultValues, rows[0])
        errors.addAll(csvMapping.errors.map { ResultDetail.report(it) })
        warnings.addAll(csvMapping.warnings.map { ResultDetail.report(it) })
        if (csvMapping.errors.isNotEmpty()) {
            return ReadResult(null, errors, warnings)
        }

        val mappedRows = rows.mapIndexedNotNull { index, row ->
            val result = mapRow(schema, csvMapping, row)
            val trackingColumn = schema.findElementColumn(schema.trackingElement ?: "")
            var trackingId = if (trackingColumn != null) result.row[trackingColumn] else ""
            if (trackingId.isEmpty())
                trackingId = "row$index"
            errors.addAll(result.errors.map { ResultDetail.item(trackingId, it) })
            warnings.addAll(result.warnings.map { ResultDetail.item(trackingId, it) })
            if (result.errors.isEmpty()) {
                result.row
            } else {
                null
            }
        }
        return ReadResult(Report(schema, mappedRows, sources, destination), errors, warnings)
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
        schema: Schema,
        defaultValues: Map<String, String>,
        row: Map<String, String>
    ): CsvMapping {
        fun rowContainsAll(fields: List<Element.CsvField>): Boolean {
            return fields.find { !row.containsKey(it.name) } == null
        }

        val useCsv = schema
            .elements
            .filter { it.csvFields != null && rowContainsAll(it.csvFields) }
            .map { it.name to it.csvFields!! }
            .toMap()
        val useMapper = schema
            .elements
            .filter { it.mapper?.isNotBlank() == true } // TODO: check for the presence of fields
            .map { it.name to Pair(it.mapperRef!!, it.mapperArgs!!) }
            .toMap()
        val useDefault = schema
            .elements
            .map { it.name to it.defaultValue(defaultValues) }
            .toMap()

        // Figure out what is missing or ignored
        val requiredHeaders = schema
            .filterCsvFields { it.cardinality == Element.Cardinality.ONE && it.default == null && it.mapper == null }
            .map { it.name }
            .toSet()
        val optionalHeaders = schema
            .filterCsvFields {
                (it.cardinality == null || it.cardinality == Element.Cardinality.ZERO_OR_ONE) &&
                    it.default == null && it.mapper == null
            }
            .map { it.name }
            .toSet()
        val headersWithDefault = schema.filterCsvFields { it.default != null || it.mapper != null }
            .map { it.name }
        val actualHeaders = row.keys.toSet()
        val missingRequiredHeaders = requiredHeaders - actualHeaders
        val missingOptionalHeaders = optionalHeaders - actualHeaders
        val ignoredHeaders = actualHeaders - requiredHeaders - optionalHeaders - headersWithDefault
        val errors = missingRequiredHeaders.map { "Missing '$it' header" }
        val warnings = missingOptionalHeaders.map { "Missing '$it' header" } +
            ignoredHeaders.map { "Unexpected '$it' header is ignored" }

        return CsvMapping(useCsv, useMapper, useDefault, errors, warnings)
    }

    /**
     * For a input row from the CSV file map to a schema defined row by
     *
     *  1. Using values from the csv file
     *  2. Using a mapper defined by the schema
     *  3. Using the default defined by the schema
     *
     * If the element `canBeBlank` then only step 1 is used.
     *
     * Also, format values into the normalized format for the type
     */
    private fun mapRow(schema: Schema, csvMapping: CsvMapping, inputRow: Map<String, String>): RowResult {
        val lookupValues = mutableMapOf<String, String>()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val placeholderValue = "**%%placeholder**"
        val failureValue = "**^^validationFail**"

        fun useCsv(element: Element): String? {
            val csvFields = csvMapping.useCsv[element.name] ?: return null
            val subValues = csvFields.map {
                val value = inputRow.getValue(it.name)
                Element.SubValue(it.name, value, it.format)
            }
            for (subValue in subValues) {
                if (subValue.value.isBlank()) {
                    return if (element.canBeBlank) "" else null
                }
                val error = element.checkForError(subValue.value, subValue.format)
                if (error != null) {
                    when (element.cardinality) {
                        Element.Cardinality.ONE -> errors += error
                        Element.Cardinality.ZERO_OR_ONE -> warnings += error
                    }
                    return failureValue
                }
            }
            return if (subValues.size == 1) {
                element.toNormalized(subValues[0].value, subValues[0].format)
            } else {
                element.toNormalized(subValues)
            }
        }

        fun useMapperPlaceholder(element: Element): String? {
            return if (csvMapping.useMapper[element.name] != null) placeholderValue else null
        }

        fun useMapper(element: Element): String? {
            val (mapper, args) = csvMapping.useMapper[element.name] ?: return null
            val valueNames = mapper.valueNames(element, args)
            val valuesForMapper = valueNames.map { elementName ->
                val valueElement = schema.findElement(elementName)
                    ?: error("Schema Error: Could not find element '$elementName' for mapper '${mapper.name}'")
                val value = lookupValues[elementName]
                    ?: error("Schema Error: No mapper input for $elementName")
                ElementAndValue(valueElement, value)
            }
            return mapper.apply(element, args, valuesForMapper)
        }

        fun useDefault(element: Element): String {
            return csvMapping.useDefault[element.name] ?: ""
        }

        // Build up lookup values
        schema.elements.forEach { element ->
            val value = useCsv(element) ?: useMapperPlaceholder(element) ?: useDefault(element)
            lookupValues[element.name] = value
        }

        // Output with value
        val outputRow = schema.elements.map { element ->
            var value = lookupValues[element.name] ?: error("Internal Error: Second pass should have all values")
            if (value == placeholderValue) {
                value = useMapper(element) ?: useDefault(element)
            }
            if (value.isBlank() && !element.canBeBlank) {
                when (element.cardinality) {
                    Element.Cardinality.ONE -> errors += "Empty value for '${element.name}'"
                    Element.Cardinality.ZERO_OR_ONE -> {}
                }
            }
            if (value == failureValue) {
                value = ""
            }
            value
        }
        return RowResult(outputRow, errors, warnings)
    }
}