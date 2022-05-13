package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.hl7.fhir.r4.model.Bundle

/**
 * Process data into/from FHIR.
 */
class ProcessFhirCommands : CliktCommand(
    name = "fhirdata",
    help = "Process data into/from FHIR"
) {

    /**
     * The input file to process.
     */
    private val inputFile by option("-i", "--input-file", help = "Input file to process")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * Optional output file.  if no output file is specified then the output is printed to the screen.
     */
    private val outputFile by option("-o", "--output-file", help = "output file")
        .file()

    override fun run() {
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")
        val actionLogger = ActionLogger()
        // Check on the extension of the file for supported operations
        when (inputFile.extension.uppercase()) {
            "HL7" -> {
                val messages = HL7Reader(actionLogger).getMessages(contents)
                if (messages.size > 1) throw CliktError("Only one HL7 message is supported.")
                val fhirBundle = HL7toFhirTranslator.getInstance().translate(messages[0])
                outputResult(fhirBundle, actionLogger)
            }
            else -> throw CliktError("File extension ${inputFile.extension} is not supported.")
        }
    }

    /**
     * Output a [fhirResult] fire bundle data and [actionLogger] logs to the screen or a file.
     */
    private fun outputResult(fhirResult: Bundle, actionLogger: ActionLogger) {
        // Pretty print the JSON output
        val jsonObject = JacksonMapperUtilities.defaultMapper
            .readValue(FhirTranscoder.encode(fhirResult), Any::class.java)
        val prettyText = JacksonMapperUtilities.defaultMapper.writeValueAsString(jsonObject)

        // Write the output to the screen or a file.
        if (outputFile != null) {
            outputFile!!.writeText(prettyText, Charsets.UTF_8)
            TermUi.echo("Wrote output to ${outputFile!!.absolutePath}")
        } else {
            TermUi.echo("-- FHIR OUTPUT ------------------------------------------")
            TermUi.echo(prettyText)
            TermUi.echo("-- END FHIR OUTPUT --------------------------------------")
        }

        actionLogger.errors.forEach { TermUi.echo("ERROR: ${it.detail.message}") }
        actionLogger.warnings.forEach { TermUi.echo("ERROR: ${it.detail.message}") }
    }
}