package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
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
            ./gradlew primeCLI --args='validate-yaml --type organizations --file path/to/file path/to/another/file'
            ./gradlew primeCLI --args='validate-yaml --type organizations --dir path/to/directory path/to/another/dir'
            ./gradlew primeCLI --args='validate-yaml --type organizations --dir path/to/directory --exclude-file path/to/excludedFile'
            ./gradlew primeCLI --args='validate-yaml --type organizations --dir path/to/directory --exclude-dir path/to/excludedDir'
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

    private val files by option(
        "-f", "--file",
        help = "Path to a YAML file to validate."
    ).file(
        mustBeReadable = true,
        canBeFile = true,
        canBeDir = false
    ).varargValues().default(emptyList())

    private val directories by option(
        "-d", "--dir",
        help = "Path to a directory containing multiple YAML files to validate."
    ).file(
        mustBeReadable = true,
        canBeFile = false,
        canBeDir = true
    ).varargValues().default(emptyList())

    private val excludeFiles by option(
        "--exclude-file",
        help = "Path to a YAML file to validate."
    ).file(
        mustBeReadable = true,
        canBeFile = true,
        canBeDir = false
    ).varargValues().default(emptyList())

    private val excludeDirectories by option(
        "--exclude-dir",
        help = "Path to a YAML file to validate."
    ).file(
        mustBeReadable = true,
        canBeFile = false,
        canBeDir = true
    ).varargValues().default(emptyList())

    val service: ConfigurationValidationService = ConfigurationValidationServiceImpl()

    override fun run() {
        printWarnings()

        val allFiles = directories.flatMap { directory ->
            FileUtils.listFiles(directory, arrayOf("yml"), true)
        } + files

        val filteredFiles = allFiles
            .filter { it.extension == "yml" }
            .filterNot { excludeFiles.contains(it) }
            .filterNot { isFileInDirectory(it, excludeDirectories) }

        if (filteredFiles.isEmpty()) {
            echo(red("No YAML files being validated!"), err = true)
            throw CliktError()
        }

        validateFiles(filteredFiles)
    }

    private fun printResult(file: File, result: ConfigurationValidationResult<*>) {
        when (result) {
            is ConfigurationValidationSuccess -> {
                echo(green("${file.path} is valid!"))
            }
            is ConfigurationValidationFailure -> {
                val output = """
                    |${file.path} is invalid!
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

    private fun validateFiles(files: List<File>) {
        val anyFailed = files.map {
            val result = service.validateYAML(type, it)
            printResult(it, result)
            isFailure(result)
        }.contains(true)
        if (anyFailed) {
            throw CliktError()
        }
    }

    private fun isFailure(result: ConfigurationValidationResult<*>): Boolean {
        return result is ConfigurationValidationFailure
    }

    private fun isFileInDirectory(file: File, directories: List<File>): Boolean {
        val paths = directories.map { it.absolutePath }
        return paths.any { file.absoluteFile.startsWith(it) }
    }

    private fun printWarnings() {
        files
            .filter { it.extension != "yml" }
            .forEach { file ->
                echo(yellow("${file.name} is not a YAML file! It will be skipped during validation."))
            }

        directories
            .associateWith { FileUtils.listFiles(it, arrayOf("yml"), true) }
            .filter { it.value.isEmpty() }
            .forEach { (directory, _) ->
                echo(yellow("${directory.path} contains no YAML files!"))
            }
    }
}