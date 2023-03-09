package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.SenderUtils
import java.io.File

class AddPublicKey : SettingCommand(
    name = "addkey",
    help = """
    Add a public key to an existing sender's setting

    Prior to calling this, generate Elliptic Curve private + public key pair for use with ES384 signatures:
      openssl ecparam -genkey -name secp384r1 -noout -out my-es-keypair.pem
      openssl ec -in my-es-keypair.pem -pubout -out my-es-public-key.pem

    Or, generate RSA keyusing
      openssl genrsa -out my-rsa-keypair.pem 2048
            openssl rsa -in my-rsa-keypair.pem -outform PEM -pubout -out my-rsa-public-key.pem
    """.trimIndent(),
) {
    val publicKeyFilename by option(
        "--public-key",
        help = "Path to public key .pem file",
        metavar = "<public-pem-key-file>"
    )
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

    private val orgName by option(
        "--orgName",
        metavar = "<organization>",
        help = "Specify the name of the organization to add the key to"
    ).required()

    private val useJson by jsonOption

    val doIt by option(
        "--doit",
        help = "Save the modified Sender setting to the database (default is to just print the modified setting)"
    ).flag(default = false)

    override fun run() {
        val publicKeyFile = File(publicKeyFilename)
        if (! publicKeyFile.exists()) {
            echo("Unable to fine pem file " + publicKeyFile.absolutePath)
            return
        }
        if (!Scope.isWellFormedScope(scope)) {
            echo("$scope is not a well formed scope value")
            return
        }
        val jwk = SenderUtils.readPublicKeyPemFile(publicKeyFile)
        jwk.kid = if (kid.isNullOrEmpty()) orgName else kid

        val origOrganizationJson =
            get(environment, oktaAccessToken, SettingType.ORGANIZATION, orgName)
        val origOrganization = jsonMapper.readValue(origOrganizationJson, Organization::class.java)

        if (!Scope.isValidScope(scope, origOrganization)) {
            echo("Organization name in scope must match $orgName.  Instead got: $scope")
            return
        }

        // TODO: This is to support original functionality, may be able to just reassign scope and jwk on
        //  origOrganization instead of creating a new one
        val newOrganization = origOrganization.makeCopyWithNewScopeAndJwk(scope, jwk)
        val newOrganizationJson = jsonMapper.writeValueAsString(newOrganization)

        echo("** Original Organization **")
        if (useJson) writeOutput(origOrganizationJson) else writeOutput(
            toYaml(
                origOrganizationJson,
                SettingType.ORGANIZATION
            )
        )
        echo("** End Original Organization **")
        echo("*** Modified Organization, including new key *** ")
        if (useJson) writeOutput(newOrganizationJson) else writeOutput(
            toYaml(
                newOrganizationJson,
                SettingType.ORGANIZATION
            )
        )
        echo("*** End Modified Organization, including new key *** ")

        if (!doIt) {
            echo(
                """

            Nothing has been updated or changed. 
            To add the key permanently, review, then rerun this same command including the --doit option
                """.trimIndent()
            )
            return
        }
        val response = put(environment, oktaAccessToken, SettingType.ORGANIZATION, orgName, newOrganizationJson)
        echo()
        echo(response)
        echo()
    }
}

class TokenUrl : SettingCommand(
    name = "reqtoken",
    help = """
        Use my private key to request a token from ReportStream
        Example call:
            ./prime sender reqtoken --private-key my-es-keypair.pem --scope strac.default.report --name strac.default
    """.trimIndent(),
) {
    val privateKeyFilename by option(
        "--private-key",
        help = "Path to private key .pem file",
        metavar = "<private-keyfile>"
    )
        .required()

    private val scope by option(
        "--scope",
        metavar = "<desired scope>",
        help = "Specify desired authorization scope.  Example:  'report' to request access to the 'report' endpoint."
    ).required()

    private val settingName by nameOption

    override fun run() {
        val environment = Environment.get(env)
        val privateKeyFile = File(privateKeyFilename)
        if (! privateKeyFile.exists()) {
            echo("Unable to fine pem file " + privateKeyFile.absolutePath)
            return
        }
        val privateKey = SenderUtils.readPrivateKeyPemFile(privateKeyFile)
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val sender = settings.findSender(settingName)
        if (sender == null) {
            echo("Unable to find sender full name (organization.sender) $settingName")
            return
        }
        // note:  using the sender fullName as the kid here.
        val senderToken = SenderUtils.generateSenderToken(sender, environment.baseUrl, privateKey, sender.fullName)
        val url = environment.formUrl("api/token").toString()
        echo("Using this URL to get an access token from ReportStream:")
        echo(url)

        val body = SenderUtils.generateSenderUrlParameterString(senderToken, scope)
        echo("\nUsing this payload body to get an access token from ReportStream:")
        echo(body)

        val (httpStatus, response) = HttpUtilities.postHttp(url, body.toByteArray())
        echo("\nResponse status: $httpStatus")
        echo("\n$response\n")
    }
}