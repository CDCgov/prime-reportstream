package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.credentials.CredentialManagement
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserJksCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.credentials.UserPpkCredential
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator
import java.util.Base64

class CredentialsCli : CredentialManagement, CliktCommand(
    name = "create-credential",
    help = """
        create credential JSON or persist to store
        
        Use this command to assist with creating credentials secrets for
        the secret store.
        
        By default, this command will only output the JSON that should be
        stored in the secret store. This is useful for generating the
        JSON blob that should be stored in Azure's Key Vault.
        
        If you add the --persist option with a key, the command will
        store the secret in your local secrets store. Your environment
        must be configured properly to persist your secrets. See
        getting_started.md for some ideas on how you can do that for
        your environment.
    """.trimIndent()
) {
    val type by option(help = "Type of credential to create")
        .groupChoice(
            "UserPass" to UserPassCredentialOptions(),
            "UserPpk" to UserPpkCredentialOptions(),
            "UserJks" to UserJksCredentialOptions()
        ).required()
    val persist by option(help = "credentialId to persist the secret under")

    init {
        // Ensure we are logging credentials service calls
        Configurator.setLevel("gov.cdc.prime.router.credentials", Level.DEBUG)
    }

    override fun run() {
        val credential = when (val it = type) {
            is UserPassCredentialOptions -> UserPassCredential(it.user, it.pass)
            is UserPpkCredentialOptions -> UserPpkCredential(it.user, it.file.readText(Charsets.UTF_8), it.filePass)
            is UserJksCredentialOptions -> {
                val jksEncoded = Base64.getEncoder().encodeToString(it.file.readBytes())
                UserJksCredential(it.user, jksEncoded, it.filePass, it.privateAlias, it.trustAlias)
            }
            else -> error("--type option is unknown")
        }

        echo("", trailingNewline = true)
        echo("Credential in JSON format: ", trailingNewline = true)
        echo("===", trailingNewline = true)
        echo(credential.toJSON(), trailingNewline = true)
        echo("===", trailingNewline = true)
        echo("", trailingNewline = true)

        persist?.let { credentialId ->
            credentialService.saveCredential(credentialId, credential, "CredentialsCli")
            credentialService.fetchCredential(
                credentialId, "CredentialCli", CredentialRequestReason.PERSIST_VERIFY
            ) ?: error("Failed to persist credential!")
        }
    }
}

sealed class CredentialConfig(name: String) : OptionGroup(name)

class UserPassCredentialOptions : CredentialConfig("Options for credential type 'UserPass'") {
    val user by option(help = "SFTP username").prompt(default = "")
    val pass by option(help = "SFTP password").prompt(default = "", requireConfirmation = true)
}

class UserPpkCredentialOptions : CredentialConfig("Options for credential type 'UserPpk'") {
    val user by option("--ppk-user", help = "Username to authenticate alongside the PPK").prompt(default = "")
    val file by option("--ppk-file", help = "Path to the PPK file").file(mustExist = true).required()
    val filePass by option("--ppk-file-pass", help = "Password to decrypt the PPK (optional)").prompt(default = "")
}

class UserJksCredentialOptions : CredentialConfig("Options for credential type 'UserJks'") {
    val user by option("--jks-user", help = "Username to authenticate alongside the JKS")
        .prompt(default = "")
    val file by option("--jks-file", help = "Path to the JKS file").file(mustExist = true)
        .required()
    val filePass by option("--jks-file-pass", help = "the JKS passcode (optional)")
        .prompt(default = "")
    val privateAlias by option("--jks-private-alias", help = "the JKS alias that points to the ID certificate")
        .default("cdcprime")
    val trustAlias by option("--jks-trust-alias", help = "the JKS alias that points to a trust certificate")
        .default("as2ohp")
}