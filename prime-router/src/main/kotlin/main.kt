@file:Suppress("unused", "unused")

package gov.cdc.prime.router

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


class RouterCli : CliktCommand(name = "prime", help = "Send health messages to their destinations") {
    private val inputFiles by option("--input", help = "<file1>, <file2>, ...").split(",")
    private val inputSchemas by option("--input_schema", help = "<schema_name>, <schema_name>, ...").split(",")
    //val inputDirs by option("--input_dir", help = "<dir1>, <dir1>").split(",")

    private val validate by option("--validate", help = "Validate stream").flag(default = true)
    private val route by option("--route", help = "route to receivers lists").flag(default = false)
    private val partitionBy by option("--partition_by", help = "<col> to partition")
    private val send by option("--send", help = "send to a receiver if specified")

    private val outputFile by option("--output", help = "<file> not compatible with route or partition")
    private val outputDir by option("--output_dir", help = "<directory>")
    private val outputSchema by option("--output_schema", help = "<schema_name> or use input schema if not specified")

    private fun readMappableTablesFromFile(readBlock: (name: String, schema: Schema, stream: InputStream) -> MappableTable): List<MappableTable> {
        val inputMappableTables = ArrayList<MappableTable>()
        if (inputSchemas == null) error("Schema is not specified. Use the --inputSchema option")
        for (i in 0 until (inputFiles?.size ?: 0)) {
            val fileName = inputFiles!![i]
            val file = File(fileName)
            if (!file.exists()) error("$fileName does not exist")
            echo("Opened: ${file.absolutePath}")

            val schemaName =
                if (i < inputSchemas!!.size)
                    inputSchemas!![i]
                else
                    inputSchemas!!.last()
            val schema = Schema.schemas[schemaName] ?: error("Cannot find the $schemaName schema")

            inputMappableTables.add(readBlock(file.name, schema, file.inputStream()))
        }
        return inputMappableTables
    }

    private fun writeMappableTablesToFile(
        tables: List<MappableTable>,
        writeBlock: (table: MappableTable, stream: OutputStream) -> Unit
    ) {
        if (outputDir == null && outputFile == null) return
        tables.forEach { table ->
            val outputFile = File((outputDir ?: ".") + "/${table.name}")
            echo("Write to: ${outputFile.absolutePath}")
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }
            outputFile.outputStream().use {
                writeBlock(table, it)
            }
        }
    }

    private fun postHttp(address: String, block: (stream: OutputStream) -> Unit) {
        echo("Sending to: $address")
        val urlObj = URL(address)
        val connection = urlObj.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        val outputStream = connection.outputStream
        outputStream.use {
            block(it)
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            echo("connection: ${connection.responseCode}")
        }
    }

    private fun routeByReceivers(input: List<MappableTable>): List<MappableTable> {
        echo("partition by receiver")
        if (input.isEmpty()) return emptyList()
        var outputTables = input[0].routeByReceiver(Receiver.receivers)
        for (i in 1 until input.size) {
            val tablesForInput = input[i].routeByReceiver(Receiver.receivers)
            outputTables = outputTables.mapIndexed { index, mappableTable ->
                mappableTable.concat(mappableTable.name, tablesForInput[index])
            }
        }
        return outputTables
    }

    override fun run() {
        // Load the schema and receivers
        Schema.loadSchemaCatalog()
        Receiver.loadReceiversList()
        echo("Loaded schema and receivers")

        // Gather input tables
        val inputMappableTables = readMappableTablesFromFile { name, schema, stream ->
            MappableTable(name, schema, stream, MappableTable.StreamType.CSV)
        }

        // Transform tables
        val outputMappableTables: List<MappableTable> = when {
            route -> routeByReceivers(inputMappableTables)
            partitionBy != null -> error("Not implemented")
            else -> inputMappableTables
        }

        // Output tables
        writeMappableTablesToFile(outputMappableTables) { table, stream ->
            table.write(stream, MappableTable.StreamType.CSV)
        }
    }
}

fun main(args: Array<String>) = RouterCli().main(args)

