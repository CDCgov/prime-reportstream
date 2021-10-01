package gov.cdc.prime.router.serializers

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.doyaaaaaken.kotlincsv.util.CSVFieldNumDifferentException
import com.github.doyaaaaaken.kotlincsv.util.CSVParseFormatException
import com.github.doyaaaaaken.kotlincsv.util.MalformedCSVException
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.ElementAndValue
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Mapper
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MissingFieldMessage
import gov.cdc.prime.router.REPORT_MAX_ERRORS
import gov.cdc.prime.router.REPORT_MAX_ITEMS
import gov.cdc.prime.router.REPORT_MAX_ITEM_COLUMNS
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.ResponseMessage
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Source
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
class CsvSerializer(val metadata: Metadata) {
    private data class CsvMapping(
        val useCsv: Map<String, List<Element.CsvField>>,
        val useMapper: Map<String, Pair<Mapper, List<String>>>,
        val useDefault: Map<String, String>,
        val errors: List<String>,
        val warnings: List<String>,
    )

    private data class RowResult(
        val row: List<String>,
        val errors: List<ResponseMessage>,
        val warnings: List<ResponseMessage>,
    )

    fun readExternal(schemaName: String, input: InputStream, source: Source): ReadResult {
        return readExternal(schemaName, input, listOf(source))
    }

    fun readExternal(
        schemaName: String,
        input: InputStream,
        sources: List<Source>,
        destination: Receiver? = null,
        defaultValues: Map<String, String> = emptyMap(),
    ): ReadResult {
        val schema = metadata.findSchema(schemaName) ?: error("Internal Error: invalid schema name '$schemaName'")
        val errors = mutableListOf<ResultDetail>()
        val warnings = mutableListOf<ResultDetail>()
        var rows = mutableListOf<Map<String, String>>()
        csvReader {
            quoteChar = '"'
            delimiter = ','
            skipEmptyLine = false
            skipMissMatchedRow = false
        }.open(input) {
            try {
                readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                    rows.add(row)
                    if (rows.size > REPORT_MAX_ITEMS) {
                        errors.add(
                            ResultDetail.report(
                                InvalidReportMessage.new(
                                    "Report rows ${rows.size} exceeds max allowed $REPORT_MAX_ITEMS rows"
                                )
                            )
                        )
                        return@open
                    }
                    if (row.size > REPORT_MAX_ITEM_COLUMNS) {
                        errors.add(
                            ResultDetail.report(
                                InvalidReportMessage.new(
                                    "Number of report columns ${row.size} exceeds max allowed $REPORT_MAX_ITEM_COLUMNS"
                                )
                            )
                        )
                        return@open
                    }
                }
            } catch (ex: CSVFieldNumDifferentException) {
                errors.add(
                    ResultDetail.report(
                        InvalidReportMessage.new(
                            "CSV file has an inconsistent number of columns on row: ${ex.csvRowNum}"
                        )
                    )
                )
            } catch (ex: CSVParseFormatException) {
                errors.add(
                    ResultDetail.report(InvalidReportMessage.new("General CSV parsing error on row: ${ex.rowNum}"))
                )
            } catch (ex: MalformedCSVException) {
                errors.add(
                    ResultDetail.report(InvalidReportMessage.new("General CSV parsing error: ${ex.message}"))
                )
            }
        }
        if (errors.size > 0) {
            return ReadResult(null, errors, warnings)
        }

        if (rows.isEmpty()) {
            warnings.add(ResultDetail.report(InvalidReportMessage.new("No reports were found in CSV content")))
            return ReadResult(Report(schema, emptyList(), sources, destination, metadata = metadata), errors, warnings)
        }

        val csvMapping = buildMappingForReading(schema, defaultValues, rows[0])
        errors.addAll(csvMapping.errors.map { ResultDetail.report(InvalidReportMessage.new(it)) })
        warnings.addAll(csvMapping.warnings.map { ResultDetail.report(InvalidReportMessage.new(it)) })
        if (errors.size > REPORT_MAX_ERRORS) {
            errors.add(
                ResultDetail.report(
                    InvalidReportMessage.new(
                        "Number of errors (${errors.size}) exceeded $REPORT_MAX_ERRORS.  Stopping further work."
                    )
                )
            )
            return ReadResult(null, errors, warnings)
        }
        if (csvMapping.errors.isNotEmpty()) {
            return ReadResult(null, errors, warnings)
        }

        val mappedRows = rows.mapIndexedNotNull { index, row ->
            val result = mapRow(schema, csvMapping, row)
            val trackingColumn = schema.findElementColumn(schema.trackingElement ?: "")
            var trackingId = if (trackingColumn != null) result.row[trackingColumn] else ""
            if (trackingId.isEmpty())
                trackingId = "row$index"
            errors.addAll(result.errors.map { ResultDetail.item(trackingId, it, index) })
            warnings.addAll(result.warnings.map { ResultDetail.item(trackingId, it, index) })
            if (result.errors.isEmpty()) {
                result.row
            } else {
                null
            }
        }
        if (errors.size > REPORT_MAX_ERRORS) {
            errors.add(
                ResultDetail.report(
                    InvalidReportMessage.new(
                        "Number of errors (${errors.size}) exceeded $REPORT_MAX_ERRORS.  Stopping."
                    )
                )
            )
            return ReadResult(null, errors, warnings)
        }
        return ReadResult(Report(schema, mappedRows, sources, destination, metadata = metadata), errors, warnings)
    }

    /**
     * Reads an internal format document (one that is in CSV, but matching the internal "kitchen sink" schema)
     * and returns the Report object for the file.
     * @param useDefaultsForMissing if a field is missing that the Report object expects, this instructs the
     * function to use the default value provided by the schema (or empty string) if it doesn't exist. This was
     * added because some older files might need to be reprocessed, and this allows that to happen without error
     * due to a missing column
     */
    fun readInternal(
        schemaName: String,
        input: InputStream,
        sources: List<Source>,
        destination: Receiver? = null,
        blobReportId: ReportId? = null,
        useDefaultsForMissing: Boolean = false
    ): Report {
        // find our schema
        val schema = metadata.findSchema(schemaName) ?: error("Internal Error: invalid schema name '$schemaName'")
        // get our rows
        val rows = if (useDefaultsForMissing) {
            // walk through the rows in the file with the header included
            csvReader().readAllWithHeader(input).map {
                // for each element name, if it doesn't exist in the map, then we add it with a default
                // doing it this ways means that even if someone adds a new element in the middle of the schema
                // (please don't), this should still be okay and it won't necessarily break
                schema.elements.map { element ->
                    it.getOrDefault(element.name, element.default ?: "")
                }
            }
        } else {
            csvReader().readAll(input).drop(1)
        }
        return Report(schema, rows, sources, destination, id = blobReportId, metadata = metadata)
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
                                    ?: error("Internal Error: table is missing ${element.fieldMapping} column")
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

    fun writeInternal(report: Report, output: OutputStream) {
        val schema = report.schema

        fun buildHeader(): List<String> = schema.elements.map { it.name }

        fun buildRows(): List<List<String>> {
            return report.itemIndices.map { row -> report.getRow(row) }
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
        val errors = missingRequiredHeaders.map {
            "Missing ${schema.findElementByCsvName(it)?.fieldMapping} header"
        }
        val warnings = missingOptionalHeaders.map {
            "Missing ${schema.findElementByCsvName(it)?.fieldMapping} header"
        } + ignoredHeaders.map { "Unexpected '$it' header is ignored" }

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
        val errors = mutableListOf<ResponseMessage>()
        val warnings = mutableListOf<ResponseMessage>()
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
                        else -> warnings += error
                        // else -> warnings += "$error - setting value to ''"
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
            val valuesForMapper = valueNames.mapNotNull { elementName ->
                val valueElement = schema.findElement(elementName) ?: return@mapNotNull null
                val value = lookupValues[elementName] ?: return@mapNotNull null
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
                    Element.Cardinality.ONE -> errors += MissingFieldMessage.new(element.fieldMapping)
                    Element.Cardinality.ZERO_OR_ONE -> {
                    }
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