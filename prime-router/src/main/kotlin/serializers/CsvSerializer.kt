package gov.cdc.prime.router.serializers

import com.github.doyaaaaaken.kotlincsv.dsl.context.ExcessFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.context.InsufficientFieldsRowBehaviour
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.github.doyaaaaaken.kotlincsv.util.CSVFieldNumDifferentException
import com.github.doyaaaaaken.kotlincsv.util.CSVParseFormatException
import com.github.doyaaaaaken.kotlincsv.util.MalformedCSVException
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.AltValueNotDefinedException
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.REPORT_MAX_ITEMS
import gov.cdc.prime.router.REPORT_MAX_ITEM_COLUMNS
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
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
        val defaultOverrides: Map<String, String> = emptyMap()
    )

    private data class RowResult(
        val row: List<String>,
        val errors: List<ActionLogDetail>,
        val warnings: List<ActionLogDetail>,
    )

    fun readExternal(schemaName: String, input: InputStream, source: Source, sender: Sender? = null): ReadResult {
        return readExternal(schemaName, input, listOf(source), sender = sender)
    }

    fun readExternal(
        schemaName: String,
        input: InputStream,
        sources: List<Source>,
        destination: Receiver? = null,
        defaultValues: Map<String, String> = emptyMap(),
        sender: Sender? = null,
    ): ReadResult {
        val actionLogs = ActionLogger()
        val schema = metadata.findSchema(schemaName) ?: error("Internal Error: invalid schema name '$schemaName'")
        val rows = mutableListOf<Map<String, String>>()
        csvReader {
            quoteChar = '"'
            delimiter = ','
            skipEmptyLine = false
            insufficientFieldsRowBehaviour = InsufficientFieldsRowBehaviour.ERROR
            excessFieldsRowBehaviour = ExcessFieldsRowBehaviour.ERROR
        }.open(input) {
            try {
                readAllWithHeaderAsSequence().forEach { row: Map<String, String> ->
                    rows.add(row)
                    if (rows.size > REPORT_MAX_ITEMS) {
                        actionLogs.error(
                            InvalidReportMessage(
                                "Your file's row size of ${rows.size} exceeds the maximum of " +
                                    "$REPORT_MAX_ITEMS rows per file. Reduce the amount of rows in this file."
                            )
                        )
                        throw actionLogs.exception
                    }
                    if (row.size > REPORT_MAX_ITEM_COLUMNS) {
                        actionLogs.error(
                            InvalidReportMessage(
                                "Number of columns in your report exceeds the maximum of " +
                                    "$REPORT_MAX_ITEM_COLUMNS allowed. Adjust the excess columnar data in your report."
                            )
                        )
                        throw actionLogs.exception
                    }
                }
            } catch (ex: CSVFieldNumDifferentException) {
                actionLogs.error(
                    InvalidReportMessage("CSV file has an inconsistent number of columns on row: ${ex.csvRowNum}")
                )
                throw actionLogs.exception
            } catch (ex: CSVParseFormatException) {
                actionLogs.error(
                    InvalidReportMessage(
                        "There's an issue parsing your file on row: ${ex.rowNum}. " +
                            "For additional help, contact the ReportStream team at $REPORTSTREAM_SUPPORT_EMAIL."
                    )
                )
                throw actionLogs.exception
            } catch (ex: MalformedCSVException) {
                actionLogs.error(
                    InvalidReportMessage(
                        "There's an issue parsing your file (Error: ${ex.message}) " +
                            "For additional help, contact the ReportStream team at $REPORTSTREAM_SUPPORT_EMAIL."
                    )
                )
                throw actionLogs.exception
            }
        }

        if (rows.isEmpty()) {
            actionLogs.warn(InvalidReportMessage("No reports were found in CSV content"))
            return ReadResult(Report(schema, emptyList(), sources, destination, metadata = metadata), actionLogs)
        }

        val csvMapping = buildMappingForReading(schema, defaultValues, rows[0], actionLogs)

        // at this point there are no branches that can return a non null report and errors > 0
        if (actionLogs.hasErrors()) {
            throw actionLogs.exception
        }

        val mappedRows = rows.mapIndexedNotNull { index, row ->
            val rowIndex = index + 1
            val result = mapRow(schema, csvMapping, row, rowIndex, sender = sender)
            val trackingColumn = schema.findElementColumn(schema.trackingElement ?: "")
            var trackingId = if (trackingColumn != null) result.row[trackingColumn] else ""
            if (trackingId.isEmpty())
                trackingId = "row$rowIndex"

            val itemLogger = actionLogs.getItemLogger(rowIndex, trackingId)
            itemLogger.error(result.errors)
            itemLogger.warn(result.warnings)
            if (!actionLogs.hasErrors()) {
                result.row
            } else {
                null
            }
        }

        return ReadResult(Report(schema, mappedRows, sources, destination, metadata = metadata), actionLogs)
    }

    /**
     * Reads an internal format document from [input] using the schema [schemaName]and returns the Report object for
     * the report for a given [destination]
     * @param sources the list of sources for this report
     * @param blobReportId the report ID
     * @return the report
     */
    fun readInternal(
        schemaName: String,
        input: InputStream,
        sources: List<Source>,
        destination: Receiver? = null,
        blobReportId: ReportId? = null
    ): Report {
        val schema = metadata.findSchema(schemaName) ?: error("Internal Error: invalid schema name '$schemaName'")
        val rows = csvReader().readAllWithHeader(input).map {
            // For each element name, if it doesn't exist in the map, then we add it with a default.
            // This is so we can add/remove fields/elements anywhere in a schema and not break the process step
            // when a report has been queued and its schema changes before processing.
            schema.elements.map { element ->
                it.getOrDefault(element.name, element.default ?: "")
            }
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
        row: Map<String, String>,
        actionLogs: ActionLogger
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
        missingRequiredHeaders.forEach {
            actionLogs.error(
                InvalidReportMessage("Your file is missing ${schema.findElementByCsvName(it)?.fieldMapping} header.")
            )
        }
        missingOptionalHeaders.forEach {
            actionLogs.warn(InvalidReportMessage("Missing ${schema.findElementByCsvName(it)?.fieldMapping} header"))
        }
        ignoredHeaders.forEach {
            actionLogs.warn(InvalidReportMessage("Unexpected column header found, '$it' will be ignored"))
        }

        return CsvMapping(useCsv, defaultValues)
    }

    /**
     * For an input row from the CSV file map to a schema defined row by
     *
     *  1. Using values from the csv file
     *  2. Using a mapper defined by the schema
     *  3. Using the default defined by the schema
     *
     * If the element `canBeBlank` then only step 1 is used.
     *
     * Also, format values into the normalized format for the type
     */
    private fun mapRow(
        schema: Schema,
        csvMapping: CsvMapping,
        inputRow: Map<String, String>,
        rowIndex: Int,
        sender: Sender? = null,
    ): RowResult {
        // TODO Refactor this code to make the error and cardinality logic more readable and remove the use of failureValue
        val lookupValues = mutableMapOf<String, String>()
        val errors = mutableListOf<ActionLogDetail>()
        val warnings = mutableListOf<ActionLogDetail>()
        val failureValue = "**^^validationFail**"

        fun useCsv(element: Element): String? {
            val csvFields = csvMapping.useCsv[element.name] ?: return null
            val subValues = csvFields.map {
                // trim off the spaces here when creating the subvalue
                val value = inputRow.getValue(it.name).trim()
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
        schema.processValues(
            lookupValues, errors, warnings, csvMapping.defaultOverrides, rowIndex, sender,
            failureValue
        )

        // Output with value
        val outputRow = schema.elements.map { element ->
            lookupValues[element.name] ?: error("Internal Error: Second pass should have all values")
        }
        return RowResult(outputRow, errors, warnings)
    }

    companion object {
        const val REPORTSTREAM_SUPPORT_EMAIL = "reportstream@cdc.gov"
    }
}