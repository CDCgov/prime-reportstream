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

        var comparisonFile = comparisonFile.inputStream().readBytes().toString(Charsets.UTF_8)
        if (comparisonFile.isBlank()) throw CliktError("File ${this.comparisonFile.absolutePath} is empty.")

        var reorderedFile = comparisonFile.substringBefore("OBX|2")
        var lines = comparisonFile.split("\r")
        val originalLineCount = lines.count()
        val aoeCodes = listOf(
            "95418-0",
            "95417-2",
            "11368-8",
            "95421-4",
            "95419-8",
            "82810-3",
            "76691-5",
            "75325-1",
            "92131-2",
            "85478-6",
            "85477-8",
        )
        aoeCodes.forEach { code ->
            if (code == "95417-2") {
                // including this one because this AOE only exists in the CP
                reorderedFile += "OBX|3|CWE|95417-2^First test for condition of interest^LN^^^^2.69||N^No^HL70136|" +
                    "|||||F|||20241231233722+0000|12D0112200||||20241231235222+0000||||Shadow Test Lab^^^^^^XX^^^" +
                    "12D0112200|285 E State St^Suite 201^Columbus^OH^43215^^^^39049\r"
            } else {
                val linesContainingCode = lines.filter { line -> line.contains(code) }
                if (linesContainingCode.isNotEmpty()) {
                    linesContainingCode.forEach {
                        reorderedFile += it + "\r"
                    }
                    lines = lines.filter { line -> !line.contains(code) }
                }
            }
        }
        lines.forEach { line ->
            if (line.contains("SPM") || line.contains("FTS") || line.contains("BTS")) {
                reorderedFile += line
            }
        }

        val outputLineCount = reorderedFile.split("\r").count()

        if (outputLineCount == originalLineCount) {
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
