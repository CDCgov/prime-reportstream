package gov.cdc.prime.router.cli

import ca.uhn.fhir.context.FhirContext
import ca.uhn.hl7v2.model.Message
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent

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

    /**
     * Schema location for the FHIR to HL7 conversion
     */
    private val fhirToHl7Schema by option("-s", "--schema", help = "Schema location for the FHIR to HL7 conversion")
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

            "FHIR", "JSON" -> {
                when {
                    fhirToHl7Schema == null ->
                        throw CliktError("You must specify a schema.")

                    !fhirToHl7Schema!!.canRead() ->
                        throw CliktError("Unable to read schema file ${fhirToHl7Schema!!.absolutePath}.")

                    else -> {
                        val bundle = FhirTranscoder.decode(contents)
                        val message = FhirToHl7Converter(
                            bundle, fhirToHl7Schema!!.name.split(".")[0], fhirToHl7Schema!!.parent
                        )
                            .convert()
                        outputResult(message)
                    }
                }
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

    /**
     * Output an HL7 [message] to the screen or a file.
     */
    private fun outputResult(message: Message) {
        val text = message.encode()
        if (outputFile != null) {
            outputFile!!.writeText(text, Charsets.UTF_8)
            TermUi.echo("Wrote output to ${outputFile!!.absolutePath}")
        } else {
            TermUi.echo("-- HL7 OUTPUT ------------------------------------------")
            text.split("\r").forEach { TermUi.echo(it) }
            TermUi.echo("-- END HL7 OUTPUT --------------------------------------")
        }
    }
}

/**
 * Process a FHIR path using a FHIR bundle as input. This command is useful to parse sample FHIR data to make
 * sure your FHIR path is correct in your schemas.
 */
class FhirPathCommand : CliktCommand(
    name = "fhirpath",
    help = "Input FHIR paths to be resolved using the input FHIR bundle"
) {

    /**
     * The input file to process.
     */
    private val inputFile by option("-i", "--input-file", help = "Input file to process")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    override fun run() {
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")
        val bundle = FhirTranscoder.decode(contents)
        echo("", true)
        echo("Using the FHIR bundle in ${inputFile.absolutePath}...", true)
        echo("Press CTRL-C or ENTER to exit.", true)

        // Loop until you press CTRL-C or ENTER at the prompt.
        while (true) {
            print("FHIR path> ") // This needs to be a print as an echo does not show on the same line
            val path = readln()

            // If no path then just quit.
            if (path.isBlank()) {
                echo("Exiting...")
                break
            }

            // Check the syntax for the FHIR path
            val pathExpression = try {
                FhirPathUtils.parsePath(path) ?: throw Exception()
            } catch (e: Exception) {
                echo("Invalid FHIR path specified.", true)
                null
            }

            if (pathExpression != null) {
                // Evaluate the path
                try {
                    val value = FhirPathUtils.evaluate(null, bundle, bundle, pathExpression)
                    // Note you can get collections
                    value.forEach {
                        val valueAsString =
                            when {
                                it.isPrimitive -> "Primitive: $it"

                                // Resource are large, so lets pretty print them
                                it is IBaseResource -> {
                                    val encodedResource = FhirContext.forR4().newJsonParser().encodeResourceToString(it)
                                    val jsonObject = JacksonMapperUtilities.defaultMapper
                                        .readValue(encodedResource, Any::class.java)
                                    JacksonMapperUtilities.defaultMapper.writeValueAsString(jsonObject)
                                }

                                // Bundle entries
                                it is BundleEntryComponent -> "Entry: ${it.fullUrl}"

                                // Non-base resources
                                else -> "Resource: $it"
                            }

                        // Print out the value, but add a dash to each collection entry if more than one
                        echo("${if (value.size > 1) "- " else ""}$valueAsString", true)
                    }
                    if (value.size > 1) echo("--- Return size = ${value.size}  ---")
                } catch (e: NotImplementedError) {
                    echo("One or more FHIR path functions specified are not implemented in the library")
                }
            }

            echo("----------------------------------------------------", true)
            echo("", true)
        }
    }
}