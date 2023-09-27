package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.cli.helpers.HL7DiffHelper
import gov.cdc.prime.router.fhirengine.utils.HL7Reader

/**
 * Compare Hl7 files
 */
class ProcessHl7Commands : CliktCommand(
    name = "hl7data",
    help = "Compare HL7 Fields. This is the structure used for the segment numbering in th output: " +
        "https://hl7-definition.caristix.com/v2/HL7v2.5.1/TriggerEvents/ORU_R01."
) {
    /**
     * The file to compare to
     */
    private val starterFile by option("-s", "--starter-file", help = "Absolute path of the file to compare to")
        .file(true, canBeDir = false, mustBeReadable = true).required()

    /**
     * The file to use as comparison
     */
    private val comparisonFile by option(
        "-c",
        "--comparison-file",
        help = "Absolute path of the file to compare it with"
    )
        .file(true, canBeDir = false, mustBeReadable = true).required()

    private val hl7DiffHelper = HL7DiffHelper()

    /**
     * The run function is what runs when ./prime hl7data is run on the command line
     */
    override fun run() {
        val starterFile = starterFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (starterFile.isBlank()) throw CliktError("File ${this.starterFile.absolutePath} is empty.")

        val comparisonFile = comparisonFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (comparisonFile.isBlank()) throw CliktError("File ${this.comparisonFile.absolutePath} is empty.")

        val actionLogger = ActionLogger()
        val starterMessages = HL7Reader(actionLogger).getMessages(starterFile)
        val comparisonMessages = HL7Reader(actionLogger).getMessages(comparisonFile)

        starterMessages.forEachIndexed { counter, message ->
            val differences = hl7DiffHelper.diffHl7(message, comparisonMessages[counter])
            echo("-------diff output")
            echo("There were ${differences.size} differences between the input and output")
            differences.forEach { echo(it.toString()) }
        }
    }
}