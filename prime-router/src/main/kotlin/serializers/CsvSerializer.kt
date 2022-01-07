package gov.cdc.prime.router.serializers

import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.doyaaaaaken.kotlincsv.util.CSVFieldNumDifferentException
import com.github.doyaaaaaken.kotlincsv.util.CSVParseFormatException
import com.github.doyaaaaaken.kotlincsv.util.MalformedCSVException
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionEvent
import gov.cdc.prime.router.ActionEventDetail
import gov.cdc.prime.router.AltValueNotDefinedException
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MissingFieldMessage
import gov.cdc.prime.router.REPORT_MAX_ERRORS
import gov.cdc.prime.router.REPORT_MAX_ITEMS
import gov.cdc.prime.router.REPORT_MAX_ITEM_COLUMNS
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Source
import org.apache.logging.log4j.kotlin.Logging
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
class CsvSerializer(val metadata: Metadata) : Logging {
    private data class CsvMapping(
        val useCsv: Map<String, List<Element.CsvField>>,
        val defaultOverrides: Map<String, String> = emptyMap(),
        val errors: List<String>,
        val warnings: List<String>,
    )

    private data class RowResult(
        val row: List<String>,
        val errors: List<ActionEventDetail>,
        val warnings: List<ActionEventDetail>,
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
        val errors = mutableListOf<ActionEvent>()
        val warnings = mutableListOf<ActionEvent>()
        val rows = mutableListOf<Map<String, String>>()
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
                            ActionEvent.report(
                                InvalidReportMessage.new(
                                    "Your file's row size of ${rows.size} exceeds the maximum of $REPORT_MAX_ITEMS " +
                                        "rows per file. Reduce the amount of rows in this file."
                                ),
                                ActionEvent.Type.error
                            )
                        )
                        return@open
                    }
                    if (row.size > REPORT_MAX_ITEM_COLUMNS) {
                        errors.add(
                            ActionEvent.report(
                                InvalidReportMessage.new(
                                    "Number of columns in your report exceeds the maximum of $REPORT_MAX_ITEM_COLUMNS" +
                                        " allowed. Adjust the excess columnar data in your report."
                                ),
                                ActionEvent.Type.error
                            )
                        )
                        return@open
                    }
                }
            } catch (ex: CSVFieldNumDifferentException) {
                errors.add(
                    ActionEvent.report(
                        InvalidReportMessage.new(
                            "CSV file has an inconsistent number of columns on row: ${ex.csvRowNum}"
                        ),
                        ActionEvent.Type.error
                    )
                )
            } catch (ex: CSVParseFormatException) {
                errors.add(
                    ActionEvent.report(
                        InvalidReportMessage.new(
                            "There's an issue parsing your file on row: ${ex.rowNum}. " +
                                "For additional help, contact the ReportStream team at $REPORSTREAM_SUPPORT_EMAIL."
                        ),
                        ActionEvent.Type.error
                    )
                )
            } catch (ex: MalformedCSVException) {
                errors.add(
                    ActionEvent.report(
                        InvalidReportMessage.new(
                            "There's an issue parsing your file (Error: ${ex.message}) " +
                                "For additional help, contact the ReportStream team at $REPORSTREAM_SUPPORT_EMAIL."
                        ),
                        ActionEvent.Type.error
                    )
                )
            }
        }
        if (errors.size > 0) {
            // warnings is empty at this point
            throw ActionError(errors)
        }

        if (rows.isEmpty()) {
            warnings.add(
                ActionEvent.report(
                    InvalidReportMessage.new("No reports were found in CSV content"),
                    ActionEvent.Type.warning
                )
            )
            return ReadResult(Report(schema, emptyList(), sources, destination, metadata = metadata), errors, warnings)
        }

        val csvMapping = buildMappingForReading(schema, defaultValues, rows[0])
        errors.addAll(
            csvMapping.errors.map {
                ActionEvent.report(InvalidReportMessage.new(it), ActionEvent.Type.error)
            }
        )
        warnings.addAll(
            csvMapping.warnings.map {
                ActionEvent.report(InvalidReportMessage.new(it), ActionEvent.Type.warning)
            }
        )
        if (errors.size > REPORT_MAX_ERRORS) {
            errors.add(
                ActionEvent.report(
                    InvalidReportMessage.new(
                        "Report file failed: Number of errors exceeded threshold. Contact the ReportStream team at " +
                            "$REPORSTREAM_SUPPORT_EMAIL for assistance."
                    ),
                    ActionEvent.Type.error
                )
            )
            throw ActionError(errors + warnings)
        }
        // at this point there are no branches that can return a non null report and errors > 0
        if (csvMapping.errors.isNotEmpty()) {
            throw ActionError(errors + warnings)
        }

        val mappedRows = rows.mapIndexedNotNull { index, row ->
            val result = mapRow(schema, csvMapping, row, index)
            val trackingColumn = schema.findElementColumn(schema.trackingElement ?: "")
            var trackingId = if (trackingColumn != null) result.row[trackingColumn] else ""
            if (trackingId.isEmpty())
                trackingId = "row$index"
            errors.addAll(result.errors.map { ActionEvent.item(trackingId, it, index, ActionEvent.Type.error) })
            warnings.addAll(result.warnings.map { ActionEvent.item(trackingId, it, index, ActionEvent.Type.warning) })
            if (result.errors.isEmpty()) {
                result.row
            } else {
                null
            }
        }
        if (errors.size > REPORT_MAX_ERRORS) {
            errors.add(
                ActionEvent.report(
                    InvalidReportMessage.new(
                        "Report file failed: Number of errors exceeded threshold. Contact the ReportStream team at " +
                            "$REPORSTREAM_SUPPORT_EMAIL for assistance."
                    ),
                    ActionEvent.Type.error
                )
            )
            throw ActionError(errors + warnings)
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
                                try {
                                    element.toFormatted(value, field.format)
                                } catch (exc: AltValueNotDefinedException) {
                                    logger.warn(
                                        exc.toString() + "  Replacing '$value' with empty-string in" +
                                            " generated data for element ${element.name}, and continuing to process." +
                                            " Consider fixing by adding $value to the " +
                                            " alt valueset in schema ${schema.name}"
                                    )
                                    ""
                                } catch (e: Exception) {
                                    // When exceptions occur in toFormatted, its hard to tell what data caused them.
                                    // So we catch, log, and rethrow here.
                                    val usefulTrackingElementInfo = if (schema.trackingElement != null)
                                        "${schema.trackingElement}=" +
                                            report.getString(row, schema.trackingElement)
                                    else "[tracking element column missing]"
                                    logger.error(
                                        e.toString() +
                                            "  Exception in row with $usefulTrackingElementInfo:" +
                                            " schema ${schema.name} element ${element.name} = value '$value' "
                                    )
                                    throw e
                                }
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
            "Your file is missing ${schema.findElementByCsvName(it)?.fieldMapping} header."
        }
        val warnings = missingOptionalHeaders.map {
            "Missing ${schema.findElementByCsvName(it)?.fieldMapping} header"
        } + ignoredHeaders.map { "Unexpected column header found, '$it' will be ignored" }

        return CsvMapping(useCsv, defaultValues, errors, warnings)
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
    private fun mapRow(schema: Schema, csvMapping: CsvMapping, inputRow: Map<String, String>, index: Int): RowResult {
        val lookupValues = mutableMapOf<String, String>()
        val errors = mutableListOf<ActionEventDetail>()
        val warnings = mutableListOf<ActionEventDetail>()
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

        // Set all the raw data first.
        schema.elements.forEach { element ->
            lookupValues[element.name] = useCsv(element) ?: ""
        }

        // Now process the data through mappers and default values
        schema.elements.forEach { element ->
            lookupValues[element.name] = element.processValue(lookupValues, schema, csvMapping.defaultOverrides, index)
        }

        // Output with value
        val outputRow = schema.elements.map { element ->
            var value = lookupValues[element.name] ?: error("Internal Error: Second pass should have all values")
            if (value.isBlank() && !element.isOptional) {
                errors += MissingFieldMessage.new(element.fieldMapping)
            }
            if (value == failureValue) {
                value = ""
            }
            value
        }
        return RowResult(outputRow, errors, warnings)
    }

    companion object {
        const val REPORSTREAM_SUPPORT_EMAIL = "reportstream@cdc.gov"
    }
}