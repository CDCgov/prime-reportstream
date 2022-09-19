package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.result.map
import com.github.kittinunf.result.onError
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.SenderFilesFunction
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.messages.ReportFileMessage
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.name

class SenderFilesCommand : CliktCommand(
    name = "sender-files",
    help = "For a specified report, trace each item's ancestry and retrieve the source files submitted by senders."
) {
    // Command Line Parameters

    private val env by option(
        "--env", help = "Connect to <name> environment", metavar = "name", envvar = "PRIME_ENVIRONMENT"
    )
        .choice("local", "test", "staging", "prod")
        .default("local", "local")

    private val reportIdArg by option(
        "--report-id", help = "Report-id (uuid format) of the receiver report", metavar = "report-id"
    )

    private val reportFileNameArg by option(
        "--report-file-name", help = "File name of the receiver report", metavar = "file-name"
    )

    private val messageIdArg by option(
        "--message-id", help = "Message-id (uuid format) of the message", metavar = "message-id"
    )

    private val offsetArg by option(
        "--offset", help = "The offset into the receiver report for the first item.", metavar = "index"
    )

    private val limitArg by option(
        "--limit", help = "The maximum number of receiver items to retrieve", metavar = "count"
    )

    private val onlyReportItemsFlag by option(
        "--only-report-items", help = "Only include items that route to the receiver report",
    ).flag()

    private val outDirectory by option(
        "-o", "--output-dir",
        help = "Output files into <dir>",
        metavar = "<dir>"
    ).required()

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

    private val jsonMapper = JacksonMapperUtilities.allowUnknownsMapper

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
            val reportFiles = getSenderFiles()
            saveSenderFiles(reportFiles)
        } catch (e: Exception) {
            abort("Exception Error: ${e.message}")
        }
    }

    /**
     * Call the sender-files api and retrieve a list of sender files.
     */
    private fun getSenderFiles(): List<ReportFileMessage> {
        val path = environment.value.formUrl("api/sender-files")
        val params = buildParameters()
        verbose("GET $path with $params")
        val (_, response, result) = Fuel
            .get(path.toString(), params)
            .authentication()
            .bearer(accessToken.value)
            .header(Headers.CONTENT_TYPE to HttpUtilities.jsonMediaType)
            .timeoutRead(SettingsUtilities.requestTimeoutMillis)
            .responseString()
        return result.map {
            jsonMapper.readValue(response.data, Array<ReportFileMessage>::class.java)?.toList()
                ?: abort("Could not deserialize")
        }.onError {
            abort(
                """
                Error using the report-files API 
                Status Code: ${response.statusCode}
                Message: ${response.responseMessage}
                Details: ${String(response.data)}
                """.trimIndent()
            )
        }.get()
    }

    /**
     * Build parameters for a request from the command lines arguments
     */
    private fun buildParameters(): List<Pair<String, String>> {
        val params = mutableListOf<Pair<String, String>>()
        reportIdArg?.let { params.add(SenderFilesFunction.REPORT_ID_PARAM to it) }
        reportFileNameArg?.let { params.add(SenderFilesFunction.REPORT_FILE_NAME_PARAM to it) }
        messageIdArg?.let { params.add(SenderFilesFunction.MESSAGE_ID_PARAM to it) }
        offsetArg?.let { params.add(SenderFilesFunction.OFFSET_PARAM to it) }
        limitArg?.let { params.add(SenderFilesFunction.LIMIT_PARAM to it) }
        if (onlyReportItemsFlag) params.add(SenderFilesFunction.ONLY_REPORT_ITEMS to "true")
        return params
    }

    /**
     * Save a list of messages in [reportFiles] to the appropriate files under [outDirectory].
     */
    private fun saveSenderFiles(reportFiles: List<ReportFileMessage>) {
        createDirectory(Path(outDirectory))
        reportFiles.forEach { reportFileMessage ->
            val dirAndFileName = splitBlobUrl(reportFileMessage.origin?.bodyUrl ?: error("expected blobUrl"))
            if (dirAndFileName.size != 3) error("Path of blob does not have 2 directories as expected")
            createDirectory(Path(outDirectory, dirAndFileName[0]))
            createDirectory(Path(outDirectory, dirAndFileName[0], dirAndFileName[1]))
            var dateTimeDownload = ""
            if (!messageIdArg.isNullOrEmpty()) {
                dateTimeDownload = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern(
                        "yyyyMMddHHmmssSS"
                    )
                ).toString()
            }
            saveFile(
                Path(
                    outDirectory, dirAndFileName[0], dirAndFileName[1],
                    dateTimeDownload
                        .plus("_")
                        .plus(dirAndFileName[2])
                ),
                reportFileMessage.content
            )
        }
    }

    /**
     * Parse the [blobUrl] into component directories and file name
     */
    private fun splitBlobUrl(blobUrl: String): List<String> {
        val afterSlash = blobUrl.substringAfterLast("/")
        return afterSlash.split("%2F")
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
     * Save a file at the [filePath] Path with [content]. Respect the [overwrite] flag or if [messageIdArg] is not
     * blank. User may need multiple results from same file.
     */
    private fun saveFile(filePath: Path, content: String) {
        if (!overwrite && Files.exists(filePath)) {
            error("${filePath.fileName} already exists")
        } else {
            echo("Writing: $filePath")
            Files.writeString(filePath, content)
        }
    }

    /**
     * Echo verbose information to the console respecting the --silent and --verbose flag
     */
    private fun echo(message: String) {
        // clikt moved the echo command into the CliktCommand class, which means this needs to call
        // into the parent class, but Kotlin doesn't allow calls to super with default parameters
        if (!silent) super.echo(
            message,
            trailingNewline = true,
            err = false,
            currentContext.console.lineSeparator
        )
    }

    /**
     * Echo verbose information to the console respecting the --silent and --verbose flag
     */
    private fun verbose(message: String) {
        // clikt moved the echo command into the CliktCommand class, which means this needs to call
        // into the parent class, but Kotlin doesn't allow calls to super with default parameters
        if (verbose) super.echo(
            message,
            trailingNewline = true,
            err = false,
            currentContext.console.lineSeparator
        )
    }

    /**
     * Abort the program with the message
     */
    private fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }
}