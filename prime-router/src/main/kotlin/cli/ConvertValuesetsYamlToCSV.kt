package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.ValueSet
import java.io.File
import java.io.FileOutputStream

/**
 * ConvertValuesetsYamlToCSV is the command line interface for the convert-valuesets-to-csv command.
 *
 * It takes the source YAML in sender-automation.valuesets and converts it to a parent/child table pair
 * for use with the sender_automation_value_set and sender_automation_value_set_row lookup tables
 *
 * NOTE: this is intended as a temporary, developer-only tool to be used during the course of the Valuesets Editor UI project.
 * Once the project is delivered to Production and found stable enough, this tool can/should be removed
 *
 * Usage:
 *
 *  ./prime convert-valuesets-to-csv -i ./metadata/valuesets/sender-automation.valuesets
 *
 */
class ConvertValuesetsYamlToCSV : CliktCommand(
    name = "convert-valuesets-to-csv",
    help = """
    This is a development tool that converts sender-automation.valuesets to two CSV files
    sender_automation_value_set.csv and sender_automation_value_set_row.csv at their
    default location (prime-reportstream/prime-router/metadata/tables/local/)
    """
) {
    /**
     * Command line option to specify full path to input file (sender-automation.valuesets)
     */
    protected val inputOption = option(
        "-i", "--input",
        help = "Input from file",
        metavar = "<file>"
    ).file(mustBeReadable = true).required()

    /**
     * Input file (typically sender-automation.valuesets)
     */
    private val inputFile by inputOption

    /**
     * Mapper for parsing input file YAML
     */
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )
    init {
        yamlMapper.registerModule(JavaTimeModule())
        yamlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    /**
     * Reads YAML from input file
     * @param inputFile YAML-formatted input file
     * @return list of ValueSet objects
     */
    private fun readYaml(inputFile: File): List<ValueSet> {
        val input = readInput(inputFile)
        return yamlMapper.readValue(input)
    }

    /**
     * Reads string content from an input file
     * @param inputFile YAML-formatted input file
     * @return content as a string
     */
    private fun readInput(inputFile: File): String {
        val input = String(inputFile.readBytes())
        if (input.isBlank()) return "Blank input"
        return input
    }

    /**
     * Cleans cell output before writing to CSV
     * @param token potential cell content
     * @return scrubbed cell content
     */
    private fun scrub(token: String?): String {
        if (token.isNullOrEmpty()) return ""

        // wrap value in quotes if it contains a comma
        var value = if (token.contains(",") == true) "\"" + token + "\"" else token

        // strip "sender-automation/" prefix from name column
        value = value.replace("sender-automation/", "")

        return value
    }

    /**
     * The actual work of converting sender-automation.valuesets to 2 CSV files
     */
    override fun run() {
        echo("Converting sender-automation.valuesets to CSV...")

        val savsOutput = StringBuilder()
        val savsValueOutput = StringBuilder()
        // header rows
        savsOutput.appendLine("name,system,referenceURL,reference,created_by,created_at")

        // detail rows
        val valueSets = readYaml(inputFile)
        valueSets.forEach { valueSet ->
            savsOutput.appendLine(
                "${scrub(valueSet.name)}," +
                    "${valueSet.system}," +
                    "${valueSet.referenceUrl}," +
                    "${scrub(valueSet.reference)}, ,"
            )
            savsValueOutput.appendLine("name,code,display,version")
            valueSet.values.forEach { value ->
                savsValueOutput.appendLine(
                    "${scrub(valueSet.name)}," +
                        "${value.code}," +
                        "${scrub(value.display)}," +
                        (value.version ?: scrub(valueSet.version))
                )
            }
            // write to valueset CSV file.
            val savsValueOutputFile = File("./metadata/tables/local/${scrub(valueSet.name)}.csv")
            val savsValueOutputStream = FileOutputStream(savsValueOutputFile)
            savsValueOutputStream.write(savsValueOutput.toString().toByteArray())
            savsValueOutputStream.close()
            savsValueOutput.clear()
        }

        // write to sender_automation_value_set CSV file.
        val savsOutputFile = File("./metadata/tables/local/sender_automation_value_set.csv")
        val savsOutputStream = FileOutputStream(savsOutputFile)
        savsOutputStream.write(savsOutput.toString().toByteArray())
        savsOutputStream.close()

        echo("Conversion complete.")
    }
}