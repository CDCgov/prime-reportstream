package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.rendering.TextColors.red
import com.github.ajalt.mordant.rendering.TextColors.yellow
import gov.cdc.prime.router.config.validation.ConfigurationType
import gov.cdc.prime.router.config.validation.ConfigurationValidationService
import gov.cdc.prime.router.config.validation.ConfigurationValidationServiceImpl
import org.apache.commons.io.FileUtils
import java.io.File

class ValidateFHIRCommand :
    CliktCommand(
        name = "validate-fhir",
        help = """
            A CLI command to validate FHIR files.
            
            Examples:
            ./gradlew primeCLI --args='validate-fhir --file path/to/file path/to/another/file'
        """.trimIndent()
    ) {

    private val typeChoices = ConfigurationType::class
        .sealedSubclasses
        .associate { it.simpleName!! to it.objectInstance!! }

    private val type by option(
        "-t", "--type",
        help = "Specify the type of FHIR file to validate."
    ).choice(
        choices = typeChoices,
        ignoreCase = true
    ).default(ConfigurationType.Organizations)

    private val files by option(
        "-f", "--file",
        help = "Path(s) to FHIR file(s) to validate."
    ).file(
        mustBeReadable = true,
        canBeFile = true,
        canBeDir = false
    ).varargValues().default(emptyList())

    private val directories by option(
        "-d", "--dir",
        help = "Path to a directory containing FHIR JSON file(s) to validate. Recursive."
    ).file(
        mustBeReadable = true,
        canBeFile = false,
        canBeDir = true
    ).varargValues().default(emptyList())

    private val excludeFiles by option(
        "--exclude-file",
        help = "Path to files to be excluded from validation."
    ).file(
        mustBeReadable = true,
        canBeFile = true,
        canBeDir = false
    ).varargValues().default(emptyList())

    private val excludeDirectories by option(
        "--exclude-dir",
        help = "Directories to be excluded from validation."
    ).file(
        mustBeReadable = true,
        canBeFile = false,
        canBeDir = true
    ).varargValues().default(emptyList())

    val service: ConfigurationValidationService = ConfigurationValidationServiceImpl()

    override fun run() {
        printWarnings()

        val allFiles = directories.flatMap { directory ->
            FileUtils.listFiles(directory, arrayOf("json"), true)
        } + files

        val filteredFiles = allFiles
            .filter { it.extension == "json" }
            .filterNot { excludeFiles.contains(it) }
            .filterNot { isFileInDirectory(it, excludeDirectories) }

        if (filteredFiles.isEmpty()) {
            echo(red("No FHIR files being validated!"), err = true)
            throw CliktError()
        }

        ValidateUtilities(ConfigurationValidationServiceImpl()).validateFiles(filteredFiles, type, ::echo)
    }

    private fun isFileInDirectory(file: File, directories: List<File>): Boolean {
        val paths = directories.map { it.absolutePath }
        return paths.any { file.absoluteFile.startsWith(it) }
    }

    private fun printWarnings() {
        files
            .filter { it.extension != "json" }
            .forEach { file ->
                echo(yellow("${file.name} is not a JSON file! It will be skipped during validation."))
            }

        directories
            .associateWith { FileUtils.listFiles(it, arrayOf("json"), true) }
            .filter { it.value.isEmpty() }
            .forEach { (directory, _) ->
                echo(yellow("${directory.path} contains no JSON files!"))
            }
    }
}