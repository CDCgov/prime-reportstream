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
    private val send by option("--send", help = "send to a receiver if specified").flag(default = false)

    private val outputFileName by option("--output", help = "<file> not compatible with route or partition")
    private val outputDir by option("--output_dir", help = "<directory>")
    private val outputSchema by option("--output_schema", help = "<schema_name> or use input schema if not specified")

    private fun readMappableTableFromFile(
        fileName: String,
        readBlock: (name: String, schema: Schema, stream: InputStream) -> MappableTable
    ): MappableTable {
        val schemaName = inputSchema.toLowerCase()
        val schema = Metadata.findSchema(schemaName) ?: error("Schema $schemaName is not found")
        val file = File(fileName)
        if (!file.exists()) error("$fileName does not exist")
        echo("Opened: ${file.absolutePath}")
        return readBlock(file.nameWithoutExtension, schema, file.inputStream())
    }

    private fun writeMappableTablesToFile(
        tables: List<Pair<MappableTable, Receiver.TopicFormat>>,
        writeBlock: (table: MappableTable, format: Receiver.TopicFormat, outputStream: OutputStream) -> Unit
    ) {
        if (outputDir == null && outputFileName == null) return
        tables.forEach { pair ->
            val outputFile = if (outputFileName != null) {
                File(outputFileName!!)
            } else {
                val ext = when(pair.second) {
                    Receiver.TopicFormat.CSV -> ".csv"
                    Receiver.TopicFormat.HL7 -> ".hl7"
                }
                File(outputDir ?: ".", "${pair.first.name}$ext")
            }
            echo("Write to: ${outputFile.absolutePath}")
            if (!outputFile.exists()) {
                outputFile.createNewFile()
            }
            outputFile.outputStream().use {
                writeBlock(pair.first, pair.second, it)
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

    private fun routeByReceivers(input: MappableTable): List<Pair<MappableTable, Receiver.TopicFormat>> {
        echo("partition by receiver")
        if (input.isEmpty()) return emptyList()
        return Metadata.receivers.filter {
            it.topic == input.schema.topic
        }.map { (name, _, schema, _, patterns, transforms, _, format) ->
            val outputName = "${name}-${input.name}"

            // Filter according to receiver patterns
            val filtered = input.filter(name = outputName, patterns = patterns)

            // Apply mapping to change schema
            val toTable: MappableTable = if (schema != filtered.schema.name) {
                val toSchema =
                    Metadata.findSchema(schema) ?: error("${schema} schema is missing from catalog")
                val mapping = filtered.schema.buildMapping(toSchema)
                filtered.applyMapping(outputName, mapping)
            } else {
                filtered
            }

            // Transform tables
            var transformed = toTable
            transforms.forEach { (transform, transformValue) ->
                when (transform) {
                    "deidentify" -> if (transformValue == "true") {
                        transformed = transformed.deidentify()
                    }
                }
            }

            Pair(transformed, format)
        }
    }

    override fun run() {
        // Load the schema and receivers
        Metadata.loadAll()
        echo("Loaded schema and receivers")

        // Gather input source
        val inputMappableTable: MappableTable = when (inputSource) {
            is InputSource.FileSource -> {
                readMappableTableFromFile((inputSource as InputSource.FileSource).fileName) { name, schema, stream ->
                    CsvConverter.read(name, schema, stream)
                }
            }
            is InputSource.DirSource -> TODO("Dir source is not implemented")
            is InputSource.FakeSource -> {
                val schema = Metadata.findSchema(inputSchema) ?: error("$inputSchema is an invalid schema name")
                FakeTable.build(
                    "fake-${schema.name.replaceRange(0, schema.name.lastIndexOf('/') + 1, "")}",
                    schema,
                    (inputSource as InputSource.FakeSource).count
                )
            }
            else -> {
                error("input source must be specified")
            }
        }

        // Transform tables
        val outputMappableTables: List<Pair<MappableTable, Receiver.TopicFormat>> = when {
            route -> routeByReceivers(inputMappableTable)
            else -> listOf(Pair(inputMappableTable, Receiver.TopicFormat.CSV))
        }

        // Output tables
        writeMappableTablesToFile(outputMappableTables) { table, format, stream ->
            when (format) {
                Receiver.TopicFormat.CSV -> CsvConverter.write(table, stream)
                Receiver.TopicFormat.HL7 -> Hl7Converter.write(table, stream)
            }
        }
    }
}

fun main(args: Array<String>) = RouterCli().main(args)

