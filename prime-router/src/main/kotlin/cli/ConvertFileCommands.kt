package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import de.m3y.kformat.Table
import de.m3y.kformat.table
import gov.cdc.prime.router.FileSource
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.serializers.CsvSerializer
import kotlin.io.path.inputStream
import kotlin.io.path.nameWithoutExtension

/**
 * Converts a file into a different output format
 */
class ConvertFileCommands(
    private val metadataInstance: Metadata? = null
) : CliktCommand(
    name = "convert-file",
    help = """
        
    """
) {
    /**
     *
     */
    private val tableHeaders by option(
        "--headers",
        metavar = "",
        help = ""
    )

    /**
     *
     */
    private val fieldNames by option(
        "--fields"
    ).required()

    /**
     *
     */
    private val inputSchema by option(
        "--input-schema"
    ).required()

    /**
     *
     */
    private val outputFile by option(
        "--output-file"
    ).path(mustExist = false, canBeDir = false)

    /**
     *
     */
    private val inputFile by option(
        "--input-file"
    ).path(mustExist = true, canBeDir = false).required()

    /**
     *
     */
    override fun run() {
        // get a metadata instance
        val metadata = metadataInstance ?: Metadata.getInstance()
        val schemaName = inputSchema.lowercase()
        val schema = metadata.findSchema(schemaName) ?: error("Schema $schemaName is not found")
        val csvSerializer = CsvSerializer(metadata)
        val report = csvSerializer.readExternal(
            schema.name,
            inputFile.inputStream(),
            FileSource(inputFile.nameWithoutExtension)
        ).report
        val fields = fieldNames.split(",")
        val rows = mutableListOf<Array<String>>()
        for (i in 0 until report.itemCount) {
            fields.map {
                report.getString(i, it) ?: ""
            }.toTypedArray().let { rows.add(it) }
        }
        // our headers
        val headers = (tableHeaders ?: fieldNames).split(",").toTypedArray()
        // a default value row
        val columnCount = headers.count()
        val defaultValues = Array(columnCount) { "" }
        if (rows.isEmpty()) {
            rows.add(defaultValues)
        }
        // render a table
        val table = table {
            hints {
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
            header(*headers)
            rows.map {
                row(*it)
            }
        }.render()
        // output it
        echo(table)
    }
}