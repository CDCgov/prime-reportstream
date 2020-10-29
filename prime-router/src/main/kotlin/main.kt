@file:Suppress("unused", "unused")

package gov.cdc.prime.router

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.int
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

sealed class Fruit {
    data class Oranges(val size: String) : Fruit()
    data class Apples(val count: Int) : Fruit()
}

sealed class InputSource {
    data class FileSource(val fileName: String) : InputSource()
    data class FakeSource(val count: Int) : InputSource()
    data class DirSource(val dirName: String) : InputSource()
}

class RouterCli : CliktCommand(
    name = "prime",
    help = "Send health messages to their destinations",
    printHelpOnEmptyArgs = true,
) {
    private val inputSource: InputSource? by mutuallyExclusiveOptions(
        option("--input", help = "<file1>").convert { InputSource.FileSource(it) },
        option("--input_fake", help = "fake the input").int().convert { InputSource.FakeSource(it) },
        option("--input_dir", help = "<dir>").convert { InputSource.DirSource(it) },
    ).single()
    private val inputSchema by option("--input_schema", help = "<schema_name>").required()

    private val validate by option("--validate", help = "Validate stream").flag(default = true)
    private val route by option("--route", help = "route to receivers lists").flag(default = false)
    private val partitionBy by option("--partition_by", help = "<col> to partition")
    private val send by option("--send", help = "send to a receiver if specified").flag(default = false)

    private val outputFileName by option("--output", help = "<file> not compatible with route or partition")
    private val outputDir by option("--output_dir", help = "<directory>")
    private val outputSchema by option("--output_schema", help = "<schema_name> or use input schema if not specified")

    private fun readMappableTableFromFile(
        fileName: String,
        readBlock: (name: String, schema: Schema, stream: InputStream) -> MappableTable
    ): MappableTable {
        val schemaName = inputSchema.toLowerCase()
        val schema = Schema.schemas[schemaName] ?: error("Schema $schemaName is not found")
        val file = File(fileName)
        if (!file.exists()) error("$fileName does not exist")
        echo("Opened: ${file.absolutePath}")
        return readBlock(file.nameWithoutExtension, schema, file.inputStream())
    }

    private fun writeMappableTablesToFile(
        tables: List<MappableTable>,
        writeBlock: (table: MappableTable, stream: OutputStream) -> Unit
    ) {
        if (outputDir == null && outputFileName == null) return
        tables.forEach { table ->
            val outputFile = if (outputFileName != null) {
                File(outputFileName!!)
            } else {
                File(outputDir ?: ".", "${table.name}.csv")
            }
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
        Schema.loadValueSetCatalog()
        echo("Loaded schema and receivers")

        // Gather input source
        val inputMappableTable: MappableTable = when (inputSource) {
            is InputSource.FileSource -> {
                readMappableTableFromFile((inputSource as InputSource.FileSource).fileName) { name, schema, stream ->
                    MappableTable(name, schema, stream, MappableTable.StreamType.CSV)
                }
            }
            is InputSource.DirSource -> TODO("Dir source is not implemented")
            is InputSource.FakeSource -> {
                val schema =
                    Schema.schemas[inputSchema.toLowerCase()] ?: error("$inputSchema is an invalid schema name")
                FakeTable.build("fake-${schema.name}", schema, (inputSource as InputSource.FakeSource).count)
            }
            else -> {
                error("input source must be specified")
            }
        }

        // Transform tables
        val outputMappableTables: List<MappableTable> = when {
            route -> routeByReceivers(listOf(inputMappableTable))
            partitionBy != null -> TODO("PartitionBy is not implemented")
            else -> listOf(inputMappableTable)
        }

        // Output tables
        writeMappableTablesToFile(outputMappableTables) { table, stream ->
            table.write(stream, MappableTable.StreamType.CSV)
        }
    }
}

fun main(args: Array<String>) = RouterCli().main(args)

