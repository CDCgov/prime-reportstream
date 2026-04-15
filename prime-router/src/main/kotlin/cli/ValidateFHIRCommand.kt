package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.varargValues
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.fhirvalidation.RSFhirValidator
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.time.TimeSource

class ValidateFHIRCommand :
    CliktCommand(
        name = "validate-fhir",
        help = """
            A CLI command to validate FHIR files.
            
            Examples:
            ./gradlew primeCLI --args='validate-fhir --file path/to/file path/to/another/file'
        """.trimIndent()
    ) {

    val validator = RSFhirValidator()
    val timeSource = TimeSource.Monotonic

    private val files by option(
        "-f", "--file",
        help = "Path(s) to FHIR file(s) to validate."
    ).file(
        mustBeReadable = true,
        canBeFile = true,
        canBeDir = false
    ).varargValues().default(emptyList())

    override fun run() {
        // Todo make a level option
        val addProfiles = true
        val outStream = ByteArrayOutputStream()
        val printStream = PrintStream(outStream)
        try {
            files.forEach( {
                echo("\n\n\n\nValidating resource: " +
                    "$it ${if (addProfiles) "with Public Health profiles" else "with base R4 profiles"}")
                var mark1 = timeSource.markNow()
                val result = validator.validateResource(it)
                var mark2 = timeSource.markNow()
                echo("Done validating resource $it. Time: ${mark2 - mark1}")

                validator.printResults(result, 1, printStream)
                echo(outStream.toString())
            })
        } catch (e: Exception) {
            echo("Exception: ${e.message}")
        }
    }
}