package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.mordant.rendering.TextColors.green
import com.github.ajalt.mordant.rendering.TextColors.red
import gov.cdc.prime.router.config.validation.ConfigurationType
import gov.cdc.prime.router.config.validation.ConfigurationValidationFailure
import gov.cdc.prime.router.config.validation.ConfigurationValidationResult
import gov.cdc.prime.router.config.validation.ConfigurationValidationService
import gov.cdc.prime.router.config.validation.ConfigurationValidationServiceImpl
import gov.cdc.prime.router.config.validation.ConfigurationValidationSuccess
import java.io.File

class ValidateUtilities() {
    companion object ValidateUtilities {
        val service: ConfigurationValidationService = ConfigurationValidationServiceImpl()

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
        }

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
                }
            }
            echo("", true, false)
        }

        private fun isFailure(result: ConfigurationValidationResult<*>): Boolean {
            return result is ConfigurationValidationFailure
        }
    }
}