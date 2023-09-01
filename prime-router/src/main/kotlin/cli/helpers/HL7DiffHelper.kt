package gov.cdc.prime.router.cli.helpers

import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Composite
import ca.uhn.hl7v2.model.Group
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Primitive
import ca.uhn.hl7v2.model.Segment
import ca.uhn.hl7v2.model.Structure
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.model.Varies
import org.apache.commons.lang3.StringUtils

class HL7DiffHelper {

    fun filterNames(message: Message, names: Array<String>, map: MutableMap<String, Segment>) {
        names.filter { name -> message.getAll(name).isNotEmpty() }.forEach { messageName ->
            val children = message.getAll(messageName)
            children.forEachIndexed { index, c ->
                indexStructure(c, (index + 1).toString(), map)
            }
        }
    }

    /**
     * Does the diffing of the [input] message to the [output] message. Results are echoed on the command line.
     */
    fun diffHl7(input: Message, output: Message): List<Hl7Diff> {
        val inputMap: MutableMap<String, Segment> = mutableMapOf()
        val outputMap: MutableMap<String, Segment> = mutableMapOf()
        val differences: MutableList<Hl7Diff> = mutableListOf()

        val inputNames = input.names
        filterNames(input, inputNames, inputMap)

        val outputNames = output.names
        filterNames(output, outputNames, outputMap)

        val mapNumOfSegment = mutableMapOf<String, Int>()

        inputMap.forEach { (segmentIndex, segment) ->
            val outputSegment = outputMap[segmentIndex]
            if (outputSegment == null) {
                differences.add(
                    Hl7Diff(
                        segmentIndex,
                        "Output missing segment ${segment.name}",
                        "",
                        0,
                        0,
                        segment.name
                    )
                )
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
                                "Output missing field",
                                "",
                                i,
                                0,
                                segment.name
                            )
                        )
                        continue
                    }
                    if (outputFields.size > inputFields.size) {
                        outputFields.foldIndexed(differences) { index, differenceAccumulator, output ->
                            try {
                                inputFields[index]
                                differenceAccumulator
                            } catch (ex: IndexOutOfBoundsException) {
                                differenceAccumulator.add(
                                    Hl7Diff(
                                        segmentIndex,
                                        "Output had more repeating types for ${output.name}",
                                        "",
                                        i,
                                        index,
                                        segment.name
                                    )
                                )
                                differenceAccumulator
                            }
                        }
                    }
                    inputFields.foldIndexed(differences) { index, differenceAccumulator, input ->
                        try {
                            val outputField = outputFields[index]
                            val matches = compareHl7Type(
                                segmentIndex,
                                input,
                                outputField,
                                segment.name,
                                0,
                                0,
                                0
                            )
                            if (matches != null) {
                                differenceAccumulator.add(matches)
                                differenceAccumulator
                            } else {
                                differenceAccumulator
                            }
                        } catch (ex: IndexOutOfBoundsException) {
                            differenceAccumulator.add(
                                Hl7Diff(
                                    segmentIndex,
                                    "Input had more repeating types for ${input.name}",
                                    "",
                                    i,
                                    index,
                                    segment.name
                                )
                            )
                            differenceAccumulator
                        }
                    }
                }
            }
        }

        if (outputMap.size > inputMap.size) {
            outputMap.forEach { (segmentIndex) ->
                val segmentIndexParts = segmentIndex.split("-")
                val segmentType = segmentIndexParts[segmentIndexParts.size - 1]
                val segmentNumber = mapNumOfSegment[segmentType] ?: 0
                mapNumOfSegment[segmentType] = segmentNumber + 1
                val inputSegment = inputMap[segmentIndex]
                if (inputSegment == null) {
                    differences.add(
                        Hl7Diff(
                            segmentIndex,
                            "Input missing segment $segmentType",
                            "",
                            0,
                            0,
                            segmentType
                        )
                    )
                }
            }
        }

        return differences
    }

    fun compareHl7Type(
        segmentIndex: String,
        input: Type,
        output: Type,
        segmentType: String,
        segmentNumber: Int,
        fieldNum: Int,
        secondaryFieldNum: Int
    ): Hl7Diff? {
        return when {
            input is Primitive && output is Primitive && !StringUtils.equals(input.value, output.value) -> {
                return Hl7Diff(
                    segmentIndex,
                    input.value,
                    output.value,
                    fieldNum,
                    secondaryFieldNum,
                    segmentType
                )
            }

            input is Varies && output is Varies -> compareHl7Type(
                segmentIndex,
                input.data,
                output.data,
                segmentType,
                segmentNumber,
                fieldNum,
                secondaryFieldNum
            )

            input is Composite && output is Composite -> {
                val inputComponents = input.components.filter { !it.isEmpty }
                val outputComponents = output.components.filter { !it.isEmpty }
                val inputExtraComponents = input.extraComponents
                val outputExtraComponents = output.extraComponents
                if (inputComponents.size != outputComponents.size) {
                    return Hl7Diff(
                        segmentIndex,
                        "Difference in number of components.",
                        "",
                        fieldNum,
                        secondaryFieldNum,
                        segmentType
                    )
                } else if (inputExtraComponents.numComponents() != outputExtraComponents.numComponents()) {
                    return Hl7Diff(
                        segmentIndex,
                        "Difference in number of extra components.",
                        "",
                        fieldNum,
                        secondaryFieldNum,
                        segmentType
                    )
                } else {
                    inputComponents.zip(outputComponents).forEach { (i, o) ->
                        return compareHl7Type(
                            segmentIndex,
                            i,
                            o,
                            segmentType,
                            segmentNumber,
                            fieldNum,
                            secondaryFieldNum
                        )
                    }
                    return null
                }
            }

            input.javaClass != output.javaClass -> {
                return Hl7Diff(
                    segmentIndex,
                    "Difference in type of field, ${input.javaClass}, ${output.javaClass}.",
                    "",
                    fieldNum,
                    secondaryFieldNum,
                    segmentType
                )
            }

            else -> {
                return null
            }
        }
    }

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
    private fun indexStructure(structure: Structure, index: String, map: MutableMap<String, Segment>) {
        when (structure) {
            is Group -> {
                val childrenNames = structure.names.filter { cname -> structure.getAll(cname).isNotEmpty() }
                val children = childrenNames.map { structure.getAll(it) }
                children.forEach { childrenOfType ->
                    childrenOfType.forEachIndexed { i, child ->
                        indexStructure(child, "$index-${structure.name}(${i + 1})", map)
                    }
                }
            }

            is Segment -> {
                if (index.length > 1) {
                    map["${index.substring(2, index.length)}-${structure.name}(${index.substring(0, 1)})"] = structure
                } else {
                    map["${structure.name}(${index.substring(0, 1)})"] = structure
                }
            }
        }
    }

    data class Hl7Diff(
        val segmentIndex: String,
        val input: String,
        val output: String,
        val fieldNum: Int,
        val secondaryFieldNum: Int,
        val segmentType: String,
    ) {
        override fun toString(): String {
            val outputText = if (output.isEmpty()) {
                ""
            } else {
                ", $output."
            }

            return "Difference between messages at $segmentIndex.$fieldNum.$secondaryFieldNum " +
                " Differences: $input$outputText"
        }
    }
}