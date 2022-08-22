package gov.cdc.prime.router.cli

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.FhirPathExecutionException
import ca.uhn.hl7v2.model.Message
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.CustomContext
import gov.cdc.prime.router.fhirengine.translation.hl7.utils.FhirPathUtils
import gov.cdc.prime.router.fhirengine.utils.FhirTranscoder
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.hl7.fhir.r4.model.Base
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.utils.FHIRLexer.FHIRLexerException

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
        // Note 8/18/2022: adding in a manual character delimeter replacement to make the primeCLI work with our HCA
        //  sample files. We may want to change this in the future, but for convenience this `replace` is here
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8).replace("^~\\&#", "^~\\&")
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")
        val actionLogger = ActionLogger()
        // Check on the extension of the file for supported operations
        when (inputFile.extension.uppercase()) {
            "HL7" -> {
                val messages = HL7Reader(actionLogger).getMessages(contents)
                if (messages.size > 1) throw CliktError("Only one HL7 message is supported.")
                // if a hl7 parsing failure happens, throw error and show the message
                if (messages.size == 1 && messages.first().toString().lowercase().contains("failed"))
                    throw CliktError("HL7 parser failure. ${messages.first()}")
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

    /**
     * A parser to print out the contents of a resource.
     */
    private val fhirResourceParser = FhirContext.forR4().newJsonParser()

    private var focusPath = "Bundle"
    private var focusResource: Base? = null
    private var fhirPathContext: CustomContext? = null

    init {
        fhirResourceParser.setPrettyPrint(true)
        fhirResourceParser.isOmitResourceId = true
        fhirResourceParser.isSummaryMode = true
    }

    override fun run() {
        fun printHelp() {
            echo("", true)
            echo("Using the FHIR bundle in ${inputFile.absolutePath}...", true)
            echo("Special commands:", true)
            echo(
                "\t!![FHIR path]                     - appends specified FHIR path to the end of the last path",
                true
            )
            echo("\tquit, exit                       - exit the tool", true)
            echo("\treset                            - Sets %resource to Bundle", true)
            echo("\tresource [=|:] [']<FHIR Path>['] - Sets %resource to a given FHIR path", true)
        }
        // Read the contents of the file
        val contents = inputFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (contents.isBlank()) throw CliktError("File ${inputFile.absolutePath} is empty.")
        val bundle = FhirTranscoder.decode(contents)
        focusResource = bundle
        fhirPathContext = CustomContext(
            bundle, mutableMapOf("rsext" to "https://reportstream.cdc.gov/fhir/StructureDefinition/")
        )
        printHelp()

        // Loop until you press CTRL-C or ENTER at the prompt.
        var lastPath = ""
        while (true) {
            echo("", true)
            echo("%resource = $focusPath")
            echo("Last path = $lastPath")
            print("FHIR path> ") // This needs to be a print as an echo does not show on the same line
            val input = readln()

            try {
                // Process the input checking for special/custom commands
                when {
                    input.isBlank() -> printHelp()

                    input == "quit" || input == "exit" ->
                        throw ProgramResult(0)

                    input.startsWith("resource") -> setFocusResource(input, bundle)

                    input == "reset" -> setFocusResource("Bundle", bundle)

                    else -> {
                        val path = if (input.startsWith("!!")) input.replace("!!", lastPath)
                        else input
                        if (path.isBlank()) printHelp()
                        else {
                            evaluatePath(path, bundle)
                            lastPath = path
                        }
                    }
                }
            } catch (e: FhirPathExecutionException) {
                echo("Invalid FHIR path specified.", true)
            }
        }
    }

    /**
     * Set the focus resource only is the path specified in the [input] string points to a resource or
     * reference in the [bundle].
     */
    private fun setFocusResource(input: String, bundle: Bundle) {
        fun setFocusPath(newPath: String) {
            focusPath = if (newPath.startsWith("%resource"))
                newPath.replace("%resource", focusPath)
            else newPath
        }

        val inputParts = input.split("=", ":", limit = 2)
        if (inputParts.size != 2 || inputParts[1].isBlank())
            echo("Setting %resource must be in the form of 'resource[= | :]<FHIR path>'")
        else {
            val path = inputParts[1].trim().trimStart('\'').trimEnd('\'')
            val pathExpression = FhirPathUtils.parsePath(path) ?: throw FhirPathExecutionException("Invalid FHIR path")
            val resourceList = FhirPathUtils.pathEngine.evaluate(
                fhirPathContext, focusResource!!, bundle, bundle, pathExpression
            )
            if (resourceList.size == 1) {
                setFocusPath(path)
                focusResource = resourceList[0] as Base
            } else
                echo(
                    "Resource path must evaluate to 1 resource, but got a collection of " +
                        "${resourceList.size} resources"
                )
        }
    }

    /**
     * Evaluate a FHIR path from the given [input] string in the [bundle].
     */
    private fun evaluatePath(input: String, bundle: Bundle) {
        // Check the syntax for the FHIR path
        try {
            val pathExpression = FhirPathUtils.parsePath(input) ?: throw FhirPathExecutionException("Invalid FHIR path")
            val values = try {
                FhirPathUtils.pathEngine.evaluate(fhirPathContext, focusResource, bundle, bundle, pathExpression)
            } catch (e: IndexOutOfBoundsException) {
                // This happens when a value for an extension is speced, but the extension does not exist.
                emptyList()
            }

            values.forEach {
                // Print out the value, but add a dash to each collection entry if more than one
                echo("${if (values.size > 1) "- " else ""}${fhirBaseAsString(it)}", true)
            }
            echo("Number of results = ${values.size} ----------------------------", true)
        } catch (e: NotImplementedError) {
            echo("One or more FHIR path functions specified are not implemented in the library")
        } catch (e: FHIRLexerException) {
            echo("Invalid FHIR path specified")
        }
    }

    /**
     * Convert a [value] that is a FHIR base to a string.
     * @return a string representing the contents of the FHIR base
     */
    private fun fhirBaseAsString(value: Base): String {
        return when {
            value.isPrimitive -> "Primitive: $value"

            // References
            value is Reference ->
                "Reference to ${value.reference} - use resolve() to navigate into it"

            // An extension
            value is Extension -> {
                "extension('${value.url}')"
            }

            // This base is a resource
            else ->
                fhirPropertiesAsString(value)
        }
    }

    /**
     * Generate a string representation of all the properties in a resource
     */
    private fun fhirPropertiesAsString(value: Base): String {
        val stringValue = StringBuilder()
        stringValue.append("{  ")
        value.children().forEach { property ->
            when {
                // Empty values
                property.values.isEmpty() ->
                    stringValue.append("")

                // An array
                property.isList -> {
                    stringValue.append("\n\t\"${property.name}\": [ \n")
                    property.values.forEach { value ->
                        stringValue.append("\t\t")
                        if (value is Extension) stringValue.append("extension('${value.url}'),")
                        else stringValue.append("$value,")
                        stringValue.append("\n")
                    }
                    stringValue.append("  ]")
                }

                // A reference
                property.values[0] is Reference -> {
                    stringValue.append("\n\t\"${property.name}\": ")
                    stringValue.append("Reference to ${(property.values[0] as Reference).reference}")
                }

                // An extension
                property.values[0] is Extension -> {
                    stringValue.append("\n\t\"${property.name}\": ")
                    stringValue.append("extension('${(property.values[0] as Extension).url}')")
                }

                // A primitive
                property.values[0].isPrimitive -> {
                    stringValue.append("\n\t\"${property.name}\": \"${property.values[0]}\"")
                }

                else -> {
                    stringValue.append("\n\t\"${property.name}\": ${property.values[0]}")
                }
            }
        }
        stringValue.append("\n}\n")
        return stringValue.toString()
    }
}