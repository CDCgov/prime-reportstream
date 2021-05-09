package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.SenderAPI
import gov.cdc.prime.router.tokens.SenderUtils
import gov.cdc.prime.router.tokens.TokenAuthentication
import java.io.File

/**
 * Parent class of cli utilities that help our Senders.
 */
abstract class SenderUtilsCommand(
    name: String,
    help: String,
) : CliktCommand(name = name, help = help) {

    // todo this was sinfully copied from SettingsCommands.kt.  Factor out common code, and get rid of this.
    // I've tried to just extract out the stuff related to '--env' only here.
    // Note that ./prime test uses this same --env, so its usage needs to be combined with this as well.
    private val env by option(
        "-e", "--env",
        metavar = "<name>",
        envvar = "PRIME_ENVIRONMENT",
        help = "Connect to <name> environment.\nChoose between [local|test|staging|prod]"
    )
        .choice("local", "test", "staging", "prod")
        .default("local", "local environment")

    data class Environment(
        val name: String,
        val baseUrl: String,
        val useHttp: Boolean = false,
        val oktaApp: OktaCommand.OktaApp? = null
    )

    fun getEnvironment(): Environment {
        return environments.find { it.name == env } ?: abort("bad environment")
    }

    fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }

    companion object {
        val environments = listOf(
            Environment("local", "localhost:7071", useHttp = true),
            Environment("test", "test.prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_TEST),
            Environment("staging", "staging.prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_TEST),
            Environment("prod", "prime.cdc.gov", oktaApp = OktaCommand.OktaApp.DH_PROD),
        )

        fun formPath(
            environment: Environment,
            endpoint: String,
        ): String {
            val protocol = if (environment.useHttp) "http" else "https"
            return "$protocol://${environment.baseUrl}/api/$endpoint"
        }


    }
}

class TokenUrl : SenderUtilsCommand(
    name = "reqtoken",
    help = "Use my private key to request a token from ReportStream"
) {
    val privateKeyFilename by option("--private-key",
        help = "Path to private key .pem file",
        metavar = "<private-keyfile>")
        .required()

    private val scope by option(
        "--scope",
        metavar = "<desired scope>",
        help = "Specify desired authorization scope.  Example:  'report' to request access to the 'report' endpoint."
    ).required()

    private val senderName by option(
        "--sender",
        metavar = "<full name>",
        help = "Specify full name of sender, as found in settings."
    ).required()

    override fun run() {
        val environment = getEnvironment()
        val privateKeyFile = File(privateKeyFilename)
        if (! privateKeyFile.exists()) {
            echo("Unable to fine pem file " + privateKeyFile.absolutePath)
            return
        }
        val privateKey = SenderUtils.readPrivateKeyPemFile(privateKeyFile)
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val sender = settings.findSender(senderName)
        if (sender == null) {
            echo("Unable to find sender full name (sender.organization) $senderName")
            return
        }
        // note:  using the sender fullName as the kid here.
        val senderToken = SenderUtils.generateSenderToken(sender, environment.baseUrl, privateKey,sender.fullName)
        val url = SenderUtils.generateSenderUrl(environment, senderToken, scope)
        echo("Using this URL to get an access token from ReportStream:")
        echo(url)

        val (httpStatus, response) = HttpUtilities.postHttp(url.toString(),"".toByteArray())
        echo("\nResponse status: $httpStatus")
        echo("\n$response\n")
    }

}

class AddPublicKey : SingleSettingCommand(
    name = "addkey",
    help = """
    Add a public key to an existing sender's setting

    Generate Elliptic Curve private + public key pair for use with ES384 signatures:
      openssl ecparam -genkey -name secp384r1 -noout -out my-es-keypair.pem
      openssl ec -in my-es-keypair.pem -pubout -out my-es-public-key.pem

    Generate RSA keyusing
      openssl genrsa -out my-rsa-keypair.pem 2048
            openssl rsa -in my-rsa-keypair.pem -outform PEM -pubout -out my-rsa-public-key.pem
    """.trimIndent(),
    settingType = SettingType.SENDER,
    operation = Operation.GET
) {
    val publicKeyFilename by option("--public-key",
        help = "Path to public key .pem file",
        metavar = "<public-pem-key-file>")
        .required()

    private val kid by option(
        "--kid",
        metavar = "<string key id>",
        help = """
            Specify desired key id for this key.
            When sender makes a token req, the kid in jwt must match this kid.
            If not set, use the sender fullname as the kid value
        """.trimIndent()
    )

    private val scope by option(
        "--scope",
        metavar = "<desired scope>",
        help = "Specify desired authorization scope.  Example:  'report' to request access to the 'report' endpoint."
    ).required()

    val doIt by option(
        "--doit",
        help = "Save the modified Sender setting to the database (default is to just print the modified setting)"
    ).flag(default = false)

    override fun run() {
        val environment = getEnvironment()
        val accessToken = getAccessToken(environment)
        val publicKeyFile = File(publicKeyFilename)
        if (! publicKeyFile.exists()) {
            echo("Unable to fine pem file " + publicKeyFile.absolutePath)
            return
        }
        if (!TokenAuthentication.isWellFormedScope(scope)) {
            echo("$scope is not a well formed scope value")
            return
        }
        val jwk = SenderUtils.readPublicKeyPemFile(publicKeyFile)
        jwk.kid = if (kid.isNullOrEmpty()) settingName else kid

        val origSenderJson = get(environment, accessToken, settingType, settingName)
        val origSender = Sender(jsonMapper.readValue(origSenderJson, SenderAPI::class.java))

        if (!TokenAuthentication.isValidScope(scope, origSender)) {
            echo("Sender full name in scope must match $settingName.  Instead got: $scope")
            return
        }

        val newSender = Sender(origSender, scope, jwk)
        val newSenderJson = jsonMapper.writeValueAsString(newSender)

        echo("*** Original Sender *** ")
        if (useJson) writeOutput(origSenderJson) else writeOutput(toYaml(origSenderJson, settingType))
        echo("*** End Original Sender *** ")
        echo("*** Modified Sender, including new key *** ")
        if (useJson) writeOutput(newSenderJson) else writeOutput(toYaml(newSenderJson, settingType))
        echo("*** End Modified Sender, including new key *** ")
        if (!doIt) {
            echo("""

            Nothing has been updated or changed. 
            To add the key permanently, review, then rerun this same command including the --doit option
        """.trimIndent())
            return
        }
        val response = put(environment, accessToken, settingType, settingName,newSenderJson)
        echo()
        echo(response)
        echo()
    }

}



