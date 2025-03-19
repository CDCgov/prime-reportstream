package gov.cdc.prime.router.cli

import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.cli.helpers.HL7DiffHelper
import gov.cdc.prime.router.fhirengine.utils.HL7Reader

/**
 * Compare Hl7 files
 */
class ProcessHl7Commands :
    CliktCommand(
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

        var comparisonFile = comparisonFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (comparisonFile.isBlank()) throw CliktError("File ${this.comparisonFile.absolutePath} is empty.")

        // TODO: remove after shadow migrations are done. This is specifically for comparing bulk upload files
        if (comparisonFile.contains("ID12345-1")) {
            var reorderedFile = comparisonFile.substringBefore("MSH")
            val results = comparisonFile.split("MSH")
            for (i in 1..17) {
                val resultWithId = results.filter { result -> result.contains("ID12345-$i&") }
                val clean = resultWithId.first().replace("BTS|17\r", "").replace("FTS|1\r", "")
                reorderedFile += "MSH$clean"
            }
            reorderedFile += "BTS|17\n" + "FTS|1"
            comparisonFile = reorderedFile
        }

        val starterMessages = Hl7InputStreamMessageStringIterator(starterFile.byteInputStream()).asSequence()
            .map { rawItem ->
                HL7Reader.parseHL7Message(rawItem)
            }.toList()
        val comparisonMessages = Hl7InputStreamMessageStringIterator(comparisonFile.byteInputStream()).asSequence()
            .map { rawItem ->
                HL7Reader.parseHL7Message(rawItem)
            }.toList()

        starterMessages.forEachIndexed { counter, message ->
            val differences = hl7DiffHelper.diffHl7(message, comparisonMessages[counter])
            echo("-------diff output")
            echo("There were ${differences.size} differences between the input and output")
            differences.forEach { echo(it.toString()) }
        }
    }
}