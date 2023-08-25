package gov.cdc.prime.router.cli

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Composite
import ca.uhn.hl7v2.model.Group
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Primitive
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.Structure
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.model.Varies
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.fhirengine.utils.HL7Reader
import org.apache.commons.lang3.StringUtils

/**
 * Compare Hl7 files
 */
class ProcessHl7Commands : CliktCommand(
    name = "hl7data",
    help = "Compare HL7 Fields. This is the structure used for the segment numbering in th output: " +
        "https://hl7-definition.caristix.com/v2/HL7v2.5.1/TriggerEvents/ORU_R01. If you just want to count from the " +
        "top, the number after is the number of that segment from the top of the file."
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
            diffHl7(message, comparisonMessages[counter])
        }
    }

    /**
     * Used to keep track of the differences between segments. [segmentIndex] gives the place the difference is
     * occurring at and [issue] holds the difference
     */
    data class Hl7Diff(val segmentIndex: String, val issue: String) {
        override fun toString(): String {
            return "Difference between messages at $segmentIndex: $issue"
        }
    }

    /**
     * Does the diffing of the [input] message to the [output] message. Results are echoed on the command line.
     */
    private fun diffHl7(input: Message, output: Message) {
        val inputMap: MutableMap<String, Segment> = mutableMapOf()
        val outputMap: MutableMap<String, Segment> = mutableMapOf()
        val differences: MutableList<Hl7Diff> = mutableListOf()

        val inputNames = input.names
        inputNames.filter { name -> input.getAll(name).isNotEmpty() }.forEach { iname ->
            val children = input.getAll(iname)
            children.forEachIndexed { index, c ->
                indexStructure(c, (index + 1).toString(), inputMap)
            }
        }
        val outputNames = output.names
        outputNames.filter { name -> output.getAll(name).isNotEmpty() }.forEach { iname ->
            val children = output.getAll(iname)
            children.forEachIndexed { index, c ->
                indexStructure(c, (index + 1).toString(), outputMap)
            }
        }

        fun compareHl7Type(
            segmentIndex: String,
            input: Type,
            output: Type,
            segmentType: String,
            segmentNumber: Int
        ): Boolean {
            return when {
                input is Primitive && output is Primitive && !StringUtils.equals(input.value, output.value) -> {
                    echo(
                        "$segmentIndex-$segmentType number $segmentNumber did not match on ${input.value}, " +
                            output.value
                    )
                    return false
                }

                input is Varies && output is Varies -> compareHl7Type(
                    segmentIndex,
                    input.data,
                    output.data,
                    segmentType,
                    segmentNumber
                )

                input is Composite && output is Composite -> {
                    val inputComponents = input.components.filter { !it.isEmpty }
                    val outputComponents = output.components.filter { !it.isEmpty }
                    val inputExtraComponents = input.extraComponents
                    val outputExtraComponents = output.extraComponents
                    if (inputComponents.size != outputComponents.size) {
                        echo("$segmentIndex-$segmentType number $segmentNumber did not match")
                        return false
                    } else if (inputExtraComponents.numComponents() != outputExtraComponents.numComponents()) {
                        echo("$segmentIndex-$segmentType number $segmentNumber did not match")
                        return false
                    } else {
                        inputComponents.zip(outputComponents).forEach { (i, o) ->
                            if (!compareHl7Type(segmentIndex, i, o, segmentType, segmentNumber)) {
                                echo("$segmentIndex-$segmentType number $segmentNumber did not match on $i, $o")
                                return false
                            }
                        }
                        return true
                    }
                }

                else -> {
                    return false
                }
            }
        }

        val mapNumOfSegment = mutableMapOf<String, Int>()

        inputMap.forEach { (segmentIndex, segment) ->
            // used to get the name of the segment as well as which number of that segment it is
            val segmentIndexParts = segmentIndex.split("-")
            val segmentType = segmentIndexParts[segmentIndexParts.size - 1]
            val num = mapNumOfSegment[segmentType]
            if (num == null) {
                mapNumOfSegment[segmentType] = 1
            } else {
                mapNumOfSegment[segmentType] = num + 1
            }

            val outputSegment = outputMap[segmentIndex]
            if (outputSegment == null) {
                differences.add(Hl7Diff(segmentIndex, "Output missing segment"))
                return@forEach
            } else {
                for (i in 1..segment.numFields()) {
                    val inputFields = segment.getField(i)
                    val outputFields = try {
                        outputSegment.getField(i)
                    } catch (ex: HL7Exception) {
                        differences.add(
                            Hl7Diff(
                                segmentIndex,
                                "Fields did not match, output was missing $segmentIndex.$i"
                            )
                        )
                        continue
                    }
                    inputFields.forEachIndexed { index, input ->
                        try {
                            val outputField = outputFields[index]
                            val matches = compareHl7Type(
                                segmentIndex,
                                input,
                                outputField,
                                segmentType,
                                mapNumOfSegment[segmentType]!!
                            )
                            if (!matches) {
                                differences.add(
                                    Hl7Diff(
                                        segmentIndex,
                                        "Field did not match at $segmentIndex.$i.$index, " +
                                            "$segmentType number ${mapNumOfSegment[segmentType]}"
                                    )
                                )
                            }
                        } catch (ex: IndexOutOfBoundsException) {
                            differences.add(
                                Hl7Diff(
                                    segmentIndex,
                                    "Input had more repeating types for ${input.name}"
                                )
                            )
                        }
                    }
                }
            }
        }

        echo("-------diff output")
        echo("There were ${differences.size} differences between the input and output")
        differences.forEach { echo(it) }
    }

    companion object {
        /**
         * Map the pieces of the structure to their index and name
         * ex.
         * ...
         * OBR|
         * OCR|
         * OBX|
         * SPM|
         * OBX|
         * OBR|
         * OBX|
         * The last OBX would be indexed as 2-1-1, 2 because it's in the second observation_result
         * This is the structure used: https://hl7-definition.caristix.com/v2/HL7v2.5.1/TriggerEvents/ORU_R01
         */
        fun indexStructure(structure: Structure, index: String, map: MutableMap<String, Segment>) {
            when (structure) {
                is Group -> {
                    val childrenNames = structure.names.filter { cname -> structure.getAll(cname).isNotEmpty() }
                    val children = childrenNames.map { structure.getAll(it) }
                    children.forEach { childrenOfType ->
                        childrenOfType.forEachIndexed { i, child ->
                            indexStructure(child, "$index-${i + 1}", map)
                        }
                    }
                }

                is Segment -> {
                    map["$index-${structure.name}"] = structure
                }
            }
        }
    }
}