package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import gov.cdc.prime.router.config.validation.ConfigurationType
import gov.cdc.prime.router.config.validation.ConfigurationValidationFailure
import gov.cdc.prime.router.config.validation.ConfigurationValidationResult
import gov.cdc.prime.router.config.validation.ConfigurationValidationService
import gov.cdc.prime.router.config.validation.ConfigurationValidationSuccess
import java.io.File

/**
 * Shared functionality for validating Configuration files through PrimeCLI
 */
class ValidateUtilities(val service: ConfigurationValidationService) {

    /**
     * Validates [files] of [type] and uses [echo] to output information on the result. Will throw a [CliktError]
     * on validation failure.
     */
    fun validateFiles(
        files: List<File>,
        type: ConfigurationType<*>,
        echo: (message: Any?, trailingNewLine: Boolean, err: Boolean) -> Unit,
    ) {
        val anyFailed = files.map {
            val result = service.validateYAML(type, it)
            printResult(it, result, echo)
            isFailure(result)
        }.contains(true)
        if (anyFailed) {
            throw CliktError()
        }
        echo(green("\n${files.size} YAML files validated!"), true, false)
    }

    /**
     * Uses [echo] to output the [result] of validation of the [file].
     */
    private fun printResult(
        file: File,
        result: ConfigurationValidationResult<*>,
        echo: (message: Any?, trailingNewLine: Boolean, err: Boolean) -> Unit,
    ) {
        when (result) {
            is ConfigurationValidationSuccess -> {
                echo(green("${file.path} is valid!"), true, false)
            }
            is ConfigurationValidationFailure -> {
                val output = """
                    |${file.path} is invalid!
                    |${"-".repeat(100)}
                    |${result.errors.joinToString("\n")}
                    |
                    |${result.cause?.stackTraceToString() ?: ""}
                """.trimMargin()
                echo(red(output), true, true)
                echo("", true, false)
            }
        }
    }

    /**
     * Returns whether the [result] is a [ConfigurationValidationFailure] or not.
     */
    private fun isFailure(result: ConfigurationValidationResult<*>): Boolean = result is ConfigurationValidationFailure
}