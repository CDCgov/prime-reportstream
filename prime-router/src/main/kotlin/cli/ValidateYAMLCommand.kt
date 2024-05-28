package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import gov.cdc.prime.router.config.validation.ConfigurationType
import gov.cdc.prime.router.config.validation.ConfigurationValidationFailure
import gov.cdc.prime.router.config.validation.ConfigurationValidationResult
import gov.cdc.prime.router.config.validation.ConfigurationValidationService
import gov.cdc.prime.router.config.validation.ConfigurationValidationServiceImpl
import gov.cdc.prime.router.config.validation.ConfigurationValidationSuccess
import org.apache.commons.io.FileUtils
import java.io.File

class ValidateYAMLCommand : CliktCommand(
        name = "validate-yaml",
        help = """
            A CLI command to validate YAML files' structure and values.
            
            Examples:
            ./gradlew primeCLI --args='validate-yaml --type organizations --file path/to/file'
            ./gradlew primeCLI --args='validate-yaml --type organizations --dir path/to/directory'
        """.trimIndent()
    ) {

    private val typeChoices = ConfigurationType::class
        .sealedSubclasses
        .associate { it.simpleName!! to it.objectInstance!! }

    private val type by option(
        "-t", "--type",
        help = "Specify the type of YAML file to validate."
    ).choice(
        choices = typeChoices,
        ignoreCase = true
    ).default(ConfigurationType.Organizations)

    private val file by option(
        "-f", "--file",
        help = "Path to a YAML file to validate."
    ).file(
        mustBeReadable = true,
        canBeFile = true,
        canBeDir = false
    )

    private val directory by option(
        "-d", "--dir",
        help = "Path to a directory containing multiple YAML files to validate."
    ).file(
        mustBeReadable = true,
        canBeFile = false,
        canBeDir = true
    )

    val service: ConfigurationValidationService = ConfigurationValidationServiceImpl()

    override fun run() {
        if (file != null) {
            handleFile(file!!)
        } else if (directory != null) {
            handleDirectory(directory!!)
        } else {
            throw BadParameterValue("Specify either a directory or a file")
        }
    }

    private fun printResult(file: File, result: ConfigurationValidationResult<*>) {
        when (result) {
            is ConfigurationValidationSuccess -> {
                echo(green("${file.name} is valid!"))
            }
            is ConfigurationValidationFailure -> {
                val output = """
                    |${file.name} is invalid!
                    |${"-".repeat(100)}
                    |${result.errors.joinToString("\n")}
                    |
                    |${result.cause?.stackTraceToString() ?: ""}
                """.trimMargin()
                echo(red(output), err = true)
            }
        }
        echo()
    }

    private fun handleFile(file: File) {
        if (file.extension != "yml") {
            throw BadParameterValue("Supplied file is not a YAML file")
        }
        val result = service.validateYAML(type, file)
        printResult(file, result)
        if (isFailure(result)) {
            throw CliktError()
        }
    }

    private fun handleDirectory(file: File) {
        val files = FileUtils.listFiles(file, arrayOf("yml"), true)
        if (files.isNotEmpty()) {
            val anyFailed = files.map {
                val result = service.validateYAML(type, it)
                printResult(it, result)
                isFailure(result)
            }.contains(true)
            if (anyFailed) {
                throw CliktError()
            }
        } else {
            throw BadParameterValue("Supplied directory contains no YAML files")
        }
    }

    private fun isFailure(result: ConfigurationValidationResult<*>): Boolean {
        return result is ConfigurationValidationFailure
    }
}