package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import gov.cdc.prime.router.credentials.CredentialManagement
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserPassCredential
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.config.Configurator

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
            "UserPass" to UserTypeCredentialOptions()
        ).required()
    val persist by option(help = "credentialId to persist the secret under")

    init {
        // Ensure we are logging credentials service calls
        Configurator.setLevel("gov.cdc.prime.router.credentials", Level.DEBUG)
    }

    override fun run() {
        val credential = when (val it = type) {
            is UserTypeCredentialOptions -> UserPassCredential(it.user, it.pass)
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

class UserTypeCredentialOptions : CredentialConfig("Options for credential type 'UserPass'") {
    val user by option().prompt(default = "")
    val pass by option().prompt(default = "", requireConfirmation = true)
}