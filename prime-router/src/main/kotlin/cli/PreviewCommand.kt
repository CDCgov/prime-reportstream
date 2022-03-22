package gov.cdc.prime.router.cli

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import com.fasterxml.jackson.module.kotlin.kotlinModule
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
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.messages.PreviewMessage
import gov.cdc.prime.router.messages.PreviewResponseMessage
import gov.cdc.prime.router.messages.ReceiverMessage
import gov.cdc.prime.router.messages.SenderMessage
import org.apache.http.HttpStatus
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name

class PreviewCommand : CliktCommand(
    name = "preview",
    help = "Preview the translation for the provided input and settings files."
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

    private val overwrite by option(
        "--overwrite",
        help = "Overwrite files if needed",
    ).flag(default = false)

    private val senderNameOption by option(
        "--sender-name",
        metavar = "<org-name>.<sender-name>",
        help = "Instead of a settings file, use the environment's existing settings"
    )

    private val senderFile by option(
        "--sender-file",
        metavar = "<file>",
        help = "YAML settings file for the sender"
    ).file(mustExist = true, mustBeReadable = true, canBeSymlink = true, canBeDir = false)

    private val receiverNameOption by option(
        "--receiver-name",
        metavar = "<org-name>.<receiver.name>",
        help = "Instead of a settings file, use the environment's existing settings"
    )

    private val receiverFile by option(
        "--receiver-file",
        metavar = "<file>",
        help = "YAML settings file for a receiver"
    ).file(mustExist = true, mustBeReadable = true, canBeSymlink = true, canBeDir = false)

    private val organizationsFile by option(
        "--organizations-file",
        metavar = "<file>",
        help = "A deep organizations.yml file for organizations, receivers and senders"
    ).file(mustExist = true, mustBeReadable = true, canBeSymlink = true, canBeDir = false)

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
    private val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory())
        .registerModule(kotlinModule())
        .registerModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

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
            getPreview()
                .echoWarningsAndErrors()
                .saveReportFiles()
        } catch (e: PrintMessage) {
            // PrintMessage is the standard way to exit a command
            throw e
        } catch (e: Exception) {
            // Unexpected internal error
            abort("CLI Internal Error: ${e.message}")
        }
    }

    /**
     * Call the preview api and return the result, either a result or errors
     */
    private fun getPreview(): PreviewResponseMessage {
        // Setup
        val path = environment.value.formUrl("api/preview")
        val previewMessage = formPreviewBody()

        // Call the API
        verbose("POST $path with: \n$previewMessage")
        val (_, response, _) = Fuel
            .post(path.toString())
            .body(previewMessage)
            .authentication()
            .bearer(accessToken.value)
            .header(Headers.CONTENT_TYPE to HttpUtilities.jsonMediaType)
            .timeoutRead(CommandUtilities.API_TIMEOUT_MSECS * 10)
            .responseString()

        val body = String(response.body().toByteArray())
        return when (response.statusCode) {
            HttpStatus.SC_OK -> {
                jsonMapper.readValue(body, PreviewResponseMessage.Success::class.java)
            }
            HttpStatus.SC_BAD_REQUEST -> {
                jsonMapper.readValue(body, PreviewResponseMessage.Error::class.java)
            }
            else -> {
                abort(
                    """
                    Error calling the preview API
                    Status Code: ${response.statusCode}
                    Message: ${response.responseMessage}
                    Details: $body
                    """.trimIndent()
                )
            }
        }
    }

    /**
     * Build a preview request body from the command line's arguments
     */
    private fun formPreviewBody(): String {
        val sender = senderFile?.let {
            yamlMapper.readValue(it, SenderMessage::class.java)
        }
        val receiver = receiverFile?.let {
            yamlMapper.readValue(it, ReceiverMessage::class.java)
        }
        val deepOrganizations = organizationsFile?.let {
            yamlMapper.readValue(it, Array<DeepOrganization>::class.java)?.toList()
        }
        val senderName = senderNameOption
            ?: sender?.fullName
            ?: abort("Missing sender name")
        sender?.also {
            if (senderName != it.fullName)
                abort("Sender full-name $senderName does not match the sender file: ${it.fullName}")
        }
        val receiverName = receiverNameOption
            ?: receiver?.fullName
            ?: abort("Missing receiver name")
        receiver?.also {
            if (it.fullName != receiverName)
                abort("Receiver full-name $receiverName does not match receiver file: ${it.fullName}")
        }
        val inputContent = inputFile.let {
            inputFile.readText()
        }
        val previewMessage = PreviewMessage(
            senderName,
            sender,
            receiverName,
            receiver,
            deepOrganizations,
            inputContent
        )
        return jsonMapper.writeValueAsString(previewMessage)
    }

    /**
     * Echo the warnings and errors in the [PreviewResponseMessage]
     */
    private fun PreviewResponseMessage.echoWarningsAndErrors(): PreviewResponseMessage {
        when (this) {
            is PreviewResponseMessage.Success -> {
                warnings.forEach {
                    echo("Warning: $it")
                }
            }
            is PreviewResponseMessage.Error -> {
                echo(message)
                errors.forEach {
                    echo("Error: $it")
                }
                warnings.forEach {
                    echo("Warning: $it")
                }
            }
        }
        return this
    }

    /**
     * Save a report file in the response if present
     */
    private fun PreviewResponseMessage.saveReportFiles(): PreviewResponseMessage {
        when (this) {
            is PreviewResponseMessage.Success -> {
                createDirectory(Path(outDirectory))
                saveFile(Path(outDirectory, externalFileName), content)
            }
            is PreviewResponseMessage.Error -> {
                // No file
            }
        }
        return this
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
        if (!overwrite && Files.exists(filePath)) abort("${filePath.fileName} already exists")
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