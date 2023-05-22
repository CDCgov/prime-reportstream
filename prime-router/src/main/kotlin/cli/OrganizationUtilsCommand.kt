package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.tokens.AuthUtils
import gov.cdc.prime.router.tokens.JwkSet
import gov.cdc.prime.router.tokens.Scope
import java.io.File

class AddPublicKey : SettingCommand(
    name = "addkey",
    help = """
    Add a public key to an existing organization's setting

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
            Specify desired key id for this key.  This value must be unique within the keys already added
            for the scope.
        """.trimIndent()
    ).required()

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
        help = "Save the modified Organization setting to the database (default is to just print the modified setting)"
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
        val jwk = AuthUtils.readPublicKeyPemFile(publicKeyFile)
        jwk.kid = kid

        val origOrganizationJson =
            get(environment, oktaAccessToken, SettingType.ORGANIZATION, orgName)
        val origOrganization = jsonMapper.readValue(origOrganizationJson, Organization::class.java)

        if (!Scope.isValidScope(scope, origOrganization)) {
            echo("Organization name in scope must match $orgName.  Instead got: $scope")
            return
        }

        if (!JwkSet.isValidKidForScope(origOrganization.keys, scope, kid)) {
            echo("kid: $kid must be unique for the requested scope: $scope")
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

class RemoveKey : SettingCommand(
    name = "removekey",
    help = """
    Removes a public key from an existing organization's setting
    """.trimIndent(),
) {
    private val kid by option(
        "--kid",
        metavar = "<string key id>",
        help = """
            Specify desired key id for this key.  This value must be unique within the keys already added
            for the scope.
        """.trimIndent()
    ).required()

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
        help = "Save the modified Organization setting to the database (default is to just print the modified setting)"
    ).flag(default = false)
    override fun run() {
        val origOrganizationJson =
            get(environment, oktaAccessToken, SettingType.ORGANIZATION, orgName)
        val origOrganization = jsonMapper.readValue(origOrganizationJson, Organization::class.java)
        if (!Scope.isValidScope(scope, origOrganization)) {
            echo("$scope is not valid")
            return
        }
        val jwkSetForScope = origOrganization.keys?.find { jwkSet -> jwkSet.scope == scope }
        val key = jwkSetForScope?.keys?.find { key -> key.kid == kid }
        if (key == null) {
            echo("Unable to find key for organization with kid: $kid and scope $scope")
            return
        }
        val updatedKeys = JwkSet.removeKeyFromScope(origOrganization.keys, scope, key)
        val newOrganization = Organization(origOrganization, updatedKeys)
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
        echo("*** End Modified Organization, after removing key *** ")
        if (!doIt) {
            echo(
                """

            Nothing has been updated or changed. 
            To remove the key permanently, review, then rerun this same command including the --doit option
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
            ./prime organization reqtoken --private-key my-es-keypair.pem --scope strac.default.report --name strac.default
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

    private val kid by option(
        "--kid",
        metavar = "<string key id>",
        help = """
            Specify desired key id for this key.  This value must be unique within the keys already added
            for the scope.
        """.trimIndent()
    ).required()

    private val settingName by nameOption

    override fun run() {
        val environment = Environment.get(env)
        val privateKeyFile = File(privateKeyFilename)
        if (! privateKeyFile.exists()) {
            echo("Unable to fine pem file " + privateKeyFile.absolutePath)
            return
        }
        val privateKey = AuthUtils.readPrivateKeyPemFile(privateKeyFile)
        val settings = FileSettings(FileSettings.defaultSettingsDirectory)
        val organization = settings.findOrganization(settingName)
        if (organization == null) {
            echo("Unable to find organization for: $settingName")
            return
        }

        val token = AuthUtils.generateOrganizationToken(organization, environment.baseUrl, privateKey, kid)
        val url = environment.formUrl("api/token").toString()
        echo("Using this URL to get an access token from ReportStream:")
        echo(url)

        val body = AuthUtils.generateOrganizationUrlParameterString(token, scope)
        echo("\nUsing this payload body to get an access token from ReportStream:")
        echo(body)

        val (httpStatus, response) = HttpUtilities.postHttp(url, body.toByteArray())
        echo("\nResponse status: $httpStatus")
        echo("\n$response\n")
    }
}