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
     * Does the diffing of the [input] message to the [output] message.
     * @return The list of differences between the messages.
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
                        null,
                        null,
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
                                null,
                                null,
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
                                        "Output had more repeating types for ${output.name}, " +
                                            "input has ${inputFields.size} and output has ${outputFields.size}",
                                        "",
                                        i,
                                        index,
                                        null,
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
                            differenceAccumulator.addAll(
                                compareHl7Type(
                                    segmentIndex,
                                    input,
                                    outputField,
                                    segment.name,
                                    i,
                                    index + 1,
                                    null
                                )
                            )
                            differenceAccumulator
                        } catch (ex: IndexOutOfBoundsException) {
                            differenceAccumulator.add(
                                Hl7Diff(
                                    segmentIndex,
                                    "Input had more repeating types for ${input.name}, " +
                                        "input has ${inputFields.size} and output has ${outputFields.size}",
                                    "",
                                    i,
                                    index + 1,
                                    null,
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
                            null,
                            null,
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
        fieldNumber: Int,
        secondaryFieldNum: Int?,
        tertiaryFieldNumber: Int?
    ): List<Hl7Diff> {
        return when {
            input is Primitive && output is Primitive && !StringUtils.equals(input.value, output.value) -> {
                return listOf(
                    Hl7Diff(
                        segmentIndex,
                        input.value,
                        output.value,
                        fieldNumber,
                        secondaryFieldNum,
                        tertiaryFieldNumber,
                        segmentType
                    )
                )
            }

            input is Varies && output is Varies -> compareHl7Type(
                segmentIndex,
                input.data,
                output.data,
                segmentType,
                fieldNumber,
                secondaryFieldNum,
                tertiaryFieldNumber
            )

            input is Composite && output is Composite -> {
                val inputComponents = input.components.filter { !it.isEmpty }
                val outputComponents = output.components.filter { !it.isEmpty }
                val inputExtraComponents = input.extraComponents
                val outputExtraComponents = output.extraComponents
                if (inputComponents.size != outputComponents.size) {
                    return listOf(
                        Hl7Diff(
                            segmentIndex,
                            "Difference in number of components.",
                            "",
                            fieldNumber,
                            secondaryFieldNum,
                            tertiaryFieldNumber,
                            segmentType
                        )
                    )
                } else if (inputExtraComponents.numComponents() != outputExtraComponents.numComponents()) {
                    return listOf(
                        Hl7Diff(
                            segmentIndex,
                            "Difference in number of extra components.",
                            "",
                            fieldNumber,
                            secondaryFieldNum,
                            tertiaryFieldNumber,
                            segmentType
                        )
                    )
                } else {
                    val compositeDifferences = mutableListOf<Hl7Diff>()
                    inputComponents.zip(outputComponents).forEachIndexed { index, (i, o) ->
                        val tertiaryFieldNumber2 = if (secondaryFieldNum == null) {
                            null
                        } else {
                            index + 1
                        }
                        compositeDifferences.addAll(
                            compareHl7Type(
                                segmentIndex,
                                i,
                                o,
                                segmentType,
                                fieldNumber,
                                secondaryFieldNum ?: (index + 1),
                                tertiaryFieldNumber2
                            )
                        )
                    }
                    return compositeDifferences
                }
            }

            input.javaClass != output.javaClass -> {
                return listOf(
                    Hl7Diff(
                        segmentIndex,
                        "Difference in type of field, ${input.javaClass}, ${output.javaClass}.",
                        "",
                        fieldNumber,
                        secondaryFieldNum,
                        tertiaryFieldNumber,
                        segmentType
                    )
                )
            }

            else -> {
                return listOf()
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
        val secondaryFieldNumber: Int?,
        val tertiaryFieldNumber: Int?,
        val segmentType: String,
    ) {
        override fun toString(): String {
            val outputText = if (output.isEmpty()) {
                ""
            } else {
                ", $output."
            }

            val tertiaryFieldNumberText = if (tertiaryFieldNumber == null) {
                ""
            } else {
                ".$tertiaryFieldNumber"
            }

            val secondaryFieldNumberText = if (secondaryFieldNumber == null) {
                ""
            } else {
                ".$secondaryFieldNumber"
            }

            return "Difference between messages at $segmentIndex." +
                "$fieldNum$secondaryFieldNumberText$tertiaryFieldNumberText" +
                " Differences: $input$outputText"
        }
    }
}