package gov.cdc.prime.router.cli

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.FhirPathExecutionException
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.util.Terser
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import fhirengine.engine.CustomFhirPathFunctions
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.fhirengine.engine.encodePreserveEncodingChars
import gov.cdc.prime.router.fhirengine.translation.HL7toFhirTranslator
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Context
import gov.cdc.prime.router.fhirengine.translation.hl7.FhirToHl7Converter
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
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
     * The format to output the data.
     */
    private val outputFormat by option("--output-format", help = "output format")
        .choice(Report.Format.HL7.toString(), Report.Format.FHIR.toString()).required()

    /**
     * The message number to use if the file is an HL7 batch message.
     */
    private val hl7ItemIndex by option(
        "--hl7-msg-index", help = "message number to use from an HL7 batch file, 0 based"
    ).int()

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
        val inputFileType = inputFile.extension.uppercase()
        when {
            // HL7 to FHIR conversion
            inputFileType == "HL7" && outputFormat == Report.Format.FHIR.toString() ->
                outputResult(convertToFhir(contents, actionLogger), actionLogger)

            // FHIR to HL7 conversion
            inputFileType == "FHIR" || inputFileType == "JSON" -> {
                outputResult(convertToHl7(contents))
            }

            // HL7 to FHIR to HL7 conversion
            inputFileType == "HL7" && outputFormat == Report.Format.HL7.toString() -> {
                val bundle = convertToFhir(contents, actionLogger)
                outputResult(convertToHl7(FhirTranscoder.encode(bundle)))
            }

            else -> throw CliktError("File extension ${inputFile.extension} is not supported.")
        }
    }

    /**
     * Convert a FHIR bundle as a [jsonString] to an HL7 message.
     * @return an HL7 message
     */
    private fun convertToHl7(jsonString: String): Message {
        return when {
            fhirToHl7Schema == null ->
                throw CliktError("You must specify a schema.")

            !fhirToHl7Schema!!.canRead() ->
                throw CliktError("Unable to read schema file ${fhirToHl7Schema!!.absolutePath}.")

            else -> {
                val bundle = FhirTranscoder.decode(jsonString)
                FhirToHl7Converter(
                    fhirToHl7Schema!!.name.split(".")[0], fhirToHl7Schema!!.parent,
                    context = FhirToHl7Context(CustomFhirPathFunctions())
                ).convert(bundle)
            }
        }
    }

    /**
     * Convert an HL7 message or batch as a [hl7String] to a FHIR bundle. [actionLogger] will contain any
     * warnings or errors from the reading of the HL7 data to HL7 objects.  Note that the --hl7-msg-index
     * is required for HL7 batch messages as this function only returns one FHIR bundle.
     * @return a FHIR bundle that represents the data in the one HL7 message
     */
    private fun convertToFhir(hl7String: String, actionLogger: ActionLogger): Bundle {
        val hasFiveEncodingChars = hl7MessageHasFiveEncodingChars(hl7String)
        // Some HL7 2.5.1 implementations have adopted the truncation character # that was added in 2.7
        // However, the library used to encode the HL7 message throws an error it there are more than 4 encoding
        // characters, so this work around exists for that scenario
        val stringToEncode = hl7String.replace("MSH|^~\\&#|", "MSH|^~\\&|")
        val messages = HL7Reader(actionLogger).getMessages(stringToEncode)
        if (messages.isEmpty()) throw CliktError("No HL7 messages were read.")
        val message = if (messages.size > 1) {
            if (hl7ItemIndex == null)
                throw CliktError("Only one HL7 message can be converted. Use the --hl7-msg-index.")
            else if (hl7ItemIndex!! < 0 || hl7ItemIndex!! >= messages.size)
                throw CliktError("Invalid HL7 message index. Must be a number 0 to ${messages.size - 1}.")
            else messages[hl7ItemIndex!!]
        } else messages[0]
        // if a hl7 parsing failure happens, throw error and show the message
        if (message.toString().lowercase().contains("failed"))
            throw CliktError("HL7 parser failure. $message")
        if (hasFiveEncodingChars) {
            val msh = message.get("MSH") as Segment
            Terser.set(msh, 2, 0, 1, 1, "^~\\&#")
        }
        return HL7toFhirTranslator.getInstance().translate(message)
    }

    /**
     * @return true if a message header (either the one at hl7ItemIndex or the first one if hl7ItemIndex is null) in the
     * given string contains MSH-2 of `^~\&#`, false otherwise
     */
    private fun hl7MessageHasFiveEncodingChars(hl7String: String): Boolean {
        // This regex should match `MSH|^~\&|` or `MSH|^~\&#`
        val mshStarts = "MSH\\|\\^~\\\\\\&[#|]".toRegex().findAll(hl7String)
        val index = hl7ItemIndex ?: 0
        mshStarts.forEachIndexed { i, matchResult ->
            if (i == index) {
                return matchResult.value == "MSH|^~\\&#"
            }
        }
        return false
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
            echo("Wrote output to ${outputFile!!.absolutePath}")
        } else {
            echo("-- FHIR OUTPUT ------------------------------------------")
            echo(prettyText)
            echo("-- END FHIR OUTPUT --------------------------------------")
        }

        actionLogger.errors.forEach { echo("ERROR: ${it.detail.message}") }
        actionLogger.warnings.forEach { echo("ERROR: ${it.detail.message}") }
    }

    /**
     * Output an HL7 [message] to the screen or a file.
     */
    private fun outputResult(message: Message) {
        val text = message.encodePreserveEncodingChars()
        if (outputFile != null) {
            outputFile!!.writeText(text, Charsets.UTF_8)
            echo("Wrote output to ${outputFile!!.absolutePath}")
        } else {
            echo("-- HL7 OUTPUT ------------------------------------------")
            text.split("\r").forEach { echo(it) }
            echo("-- END HL7 OUTPUT --------------------------------------")
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
     * Constants for the FHIR Path context.
     */
    private val constants by option(
        "-c", "--constants",
        help = "a constant in the form of key=value to be used in FHIR Path. Option can be repeated."
    ).associate()

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
        val constantList = mutableMapOf("rsext" to "'https://reportstream.cdc.gov/fhir/StructureDefinition/'")
        constants.entries.forEach {
            constantList[it.key] = it.value
        }
        echo("Using constants:")
        constantList.forEach { (name, value) ->
            echo("\t$name=$value")
        }
        fhirPathContext = CustomContext(bundle, bundle, constantList, CustomFhirPathFunctions())
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
            try {
                val path = inputParts[1].trim().trimStart('\'').trimEnd('\'')
                val pathExpression =
                    FhirPathUtils.parsePath(path) ?: throw FhirPathExecutionException("Invalid FHIR path")
                val resourceList = FhirPathUtils.pathEngine.evaluate(
                    fhirPathContext, focusResource!!, bundle, bundle, pathExpression
                )
                if (resourceList.size == 1) {
                    setFocusPath(path)
                    focusResource = resourceList[0] as Base
                    fhirPathContext?.let { it.focusResource = focusResource as Base }
                } else
                    echo(
                        "Resource path must evaluate to 1 resource, but got a collection of " +
                            "${resourceList.size} resources"
                    )
            } catch (e: Exception) {
                echo("Error evaluating resource path: ${e.message}")
            }
        }
    }

    /**
     * Evaluate a FHIR path from the given [input] string in the [bundle].
     */
    private fun evaluatePath(input: String, bundle: Bundle) {
        // Check the syntax for the FHIR path
        try {
            val values = try {
                FhirPathUtils.evaluate(fhirPathContext, focusResource!!, bundle, input)
            } catch (e: IndexOutOfBoundsException) {
                // This happens when a value for an extension is speced, but the extension does not exist.
                emptyList()
            } catch (e: SchemaException) {
                echo("Error evaluating path: ${e.message}")
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