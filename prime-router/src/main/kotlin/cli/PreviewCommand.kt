package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.output.TermUi
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.getOrElse
import com.github.kittinunf.result.map
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.messages.ReportFileMessage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name

class PreviewCommand : CliktCommand(
    name = "preview",
    help = "Preview the output for an input and a settings files. All settings files are in the YAML format."
) {
    // Command Line Parameters
    private val env by option(
        "--env",
        help = "Connect to <name> environment",
        metavar = "name",
        envvar = "PRIME_ENVIRONMENT"
    ).choice("local", "test", "staging", "prod").default("local", "local")

    private val inputFile by option(
        "-i", "--input-file",
        help = "Input file to be translated to the output",
        metavar = "<file>"
    ).file(mustExist = true, mustBeReadable = true, canBeSymlink = true, canBeDir = false).required()

    private val outDirectory by option(
        "-o", "--output-dir",
        help = "Output files into <dir>",
        metavar = "<dir>"
    ).required()

    private val senderFile by option(
        "--sender-file",
        metavar = "<file>",
        help = "Settings file for the sender"
    ).file(mustExist = true, mustBeReadable = true, canBeSymlink = true, canBeDir = false)

    private val senderName by option(
        "--sender-name",
        metavar = "<org-name>.<sender-name>",
        help = "Instead of a settings file, use the environment's settings"
    )

    private val receiverFile by option(
        "--receiver-file",
        metavar = "<file>",
        help = "Settings file for a receiver"
    ).file(mustExist = true, mustBeReadable = true, canBeSymlink = true, canBeDir = false)

    private val receiverName by option(
        "--receiver-name",
        metavar = "<org-name>.<receiver.name>",
        help = "Instead of a settings file, use the environment's settings"
    )

    private val organizationsFile by option(
        "--deep-organizations-file",
        metavar = "<file>",
        help = "A deep organizations.yml file for organizations, receivers and senders"
    ).file(mustExist = true, mustBeReadable = true, canBeSymlink = true, canBeDir = false)

    private val overwrite by option(
        "--overwrite",
        help = "Overwrite files if needed",
    ).flag(default = false)

    private val verbose by option(
        "-v", "--verbose",
        help = "Verbose logging of each HTTP operation to the console"
    ).flag(default = false)

    private val silent by option(
        "-s", "--silent",
        help = "Do not echo progress or prompt for confirmation"
    ).flag(default = false)

    private val jsonMapper = jacksonMapperBuilder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()

    private val environment = lazy { Environment.get(env) }
    private val accessToken = lazy {
        if (environment.value.oktaApp == null) {
            "dummy"
        } else {
            OktaCommand.fetchAccessToken(environment.value.oktaApp)
                ?: abort(
                    "Invalid access token. " +
                        "Run ./prime login to fetch/refresh your access token for the $env environment."
                )
        }
    }

    /**
     * Run the command
     */
    override fun run() {
        try {
            val previewReport = getPreviewFile()
            saveReportFiles(previewReport)
        } catch (e: Exception) {
            abort("Exception Error: ${e.message}")
        }
    }

    /**
     * Call the sender-files api and retrieve a list of sender files.
     */
    private fun getPreviewFile(): ReportFileMessage {
        // Setup
        val path = environment.value.formUrl("api/preview")
        val previewMessage = formPreviewBody()

        // Call the API
        verbose("POST $path with: \n$previewMessage")
        val (_, response, result) = Fuel
            .post(path.toString())
            .body(previewMessage)
            .authentication()
            .bearer(accessToken.value)
            .header(Headers.CONTENT_TYPE to HttpUtilities.jsonMediaType)
            .timeoutRead(CommandUtilities.API_TIMEOUT_MSECS)
            .responseString()

        return result.map {
            jsonMapper.readValue(it, ReportFileMessage::class.java)
        }.getOrElse {
            abort(
                """
                Error calling the preview API
                Status Code: ${response.statusCode}
                Message: ${response.responseMessage}
                Details: ${String(response.data)}
                """.trimIndent()
            )
        }
    }

    /**
     * Build parameters for a request from the command lines arguments
     */
    private fun formPreviewBody(): String {
        TODO()
    }

    /**
     * Save a report file message
     */
    private fun saveReportFiles(reportFile: ReportFileMessage) {
        TODO("$reportFile")
    }

    /**
     * If [dirPath] doesn't exist, create a directory
     */
    private fun createDirectory(dirPath: Path) {
        if (Files.exists(dirPath)) {
            if (Files.isDirectory(dirPath)) return else error("${dirPath.name} is not a directory")
        }
        Files.createDirectory(dirPath)
    }

    /**
     * Save a file at the [filePath] Path with [content]. Respect the [overwrite] flag.
     */
    private fun saveFile(filePath: Path, content: String) {
        if (!overwrite && Files.exists(filePath)) error("${filePath.fileName} already exists")
        echo("Writing: $filePath")
        Files.writeString(filePath, content)
    }

    /**
     * Echo verbose information to the console respecting the --silent and --verbose flag
     */
    private fun echo(message: String) {
        if (!silent) TermUi.echo(message)
    }

    /**
     * Echo verbose information to the console respecting the --silent and --verbose flag
     */
    private fun verbose(message: String) {
        if (verbose) TermUi.echo(message)
    }

    /**
     * Abort the program with the message
     */
    private fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }
}