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
    /**
     * Does the diffing of the [input] message to the [output] message. Results are echoed on the command line.
     */
    companion object {
        fun diffHl7(input: Message, output: Message): List<Hl7Diff> {
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
                            segmentType,
                            segmentNumber
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
                                segmentType,
                                segmentNumber
                            )
                        } else if (inputExtraComponents.numComponents() != outputExtraComponents.numComponents()) {
                            return Hl7Diff(
                                segmentIndex,
                                "Difference in number of extra components.",
                                "",
                                fieldNum,
                                secondaryFieldNum,
                                segmentType,
                                segmentNumber
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

                    else -> {
                        return null
                    }
                }
            }

            val mapNumOfSegment = mutableMapOf<String, Int>()

            inputMap.forEach { (segmentIndex, segment) ->
                // used to get the name of the segment as well as which number of that segment it is
                val segmentIndexParts = segmentIndex.split("-")
                val segmentType = segmentIndexParts[segmentIndexParts.size - 1]
                val segmentNumber = mapNumOfSegment[segmentType] ?: 0
                mapNumOfSegment[segmentType] = segmentNumber + 1

                val outputSegment = outputMap[segmentIndex]
                if (outputSegment == null) {
                    differences.add(
                        Hl7Diff(
                            segmentIndex,
                            "Output missing segment",
                            "",
                            0,
                            0,
                            segmentType,
                            segmentNumber
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
                                    "Output missing segment",
                                    "",
                                    i,
                                    0,
                                    segmentType,
                                    segmentNumber
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
                                    mapNumOfSegment[segmentType]!!,
                                    0,
                                    0
                                )
                                if (matches != null) {
                                    differences.add(
                                        matches
                                    )
                                }
                            } catch (ex: IndexOutOfBoundsException) {
                                differences.add(
                                    Hl7Diff(
                                        segmentIndex,
                                        "Input had more repeating types for ${input.name}",
                                        "",
                                        i,
                                        index,
                                        segmentType,
                                        segmentNumber
                                    )
                                )
                            }
                        }
                    }
                }
            }

            return differences
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

    data class Hl7Diff(
        val segmentIndex: String,
        val input: String,
        val output: String,
        val fieldNum: Int,
        val secondaryFieldNum: Int,
        val segmentType: String,
        val segmentNumber: Int,
    ) {
        override fun toString(): String {
            val outputText = if (output == "") {
                ""
            } else {
                ", $output."
            }

            return "Difference between messages at $segmentIndex.$fieldNum.$secondaryFieldNum " +
                "($segmentType number $segmentNumber) Differences: $input$outputText"
        }
    }
}