package gov.cdc.prime.router.serializers

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.ValueSet
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.OffsetDateTime

class RedoxSerializer(val metadata: Metadata) {
    private data class JsonField(
        val path: String,
        val column: Int? = null,
        val element: Element? = null,
        val value: String? = null,
        val useCurrentTime: Boolean = false
    ) {
        private val index = path.lastIndexOf('.')
        val base: String = if (index != -1) path.slice(0 until index) else ""
        val name: String = if (index != -1) path.slice(index + 1 until path.length) else ""
    }

    private enum class JsonGroupType { END_OBJECT, END_ARRAY, START_OBJECT, START_ARRAY }
    private data class JsonGroup(val type: JsonGroupType, val name: String? = null)

    private val factory = JsonFactory()
    private val fields = mutableMapOf<String, List<JsonField>>()
    private val transitions = mutableMapOf<String, List<List<JsonGroup>>>()

    fun write(report: Report, outputStream: OutputStream) {
        val fields = getFields(report.schema)
        val transitions = getTransitions(report.schema.name, fields)

        val messages = report.itemIndices.map {
            val row = report.getRow(it)
            createMessage(fields, transitions, row)
        }
        // NDJSON format
        val out = messages.joinToString("\n")
        outputStream.write(out.toByteArray())
    }

    private fun getFields(schema: Schema): List<JsonField> {
        val lookupName = schema.name

        @Synchronized
        fun buildFields(): List<JsonField> {

            val constants = listOf(
                JsonField("Meta.DataModel", value = "Results"),
                JsonField("Meta.EventType", value = "NewUnsolicited"),
                JsonField("Meta.EventDateTime", useCurrentTime = true)
            )
            val newFields = schema
                .elements
                .flatMapIndexed { column, element ->
                    element.redoxOutputFields?.map {
                        JsonField(it, column, element)
                    } ?: emptyList()
                }.plus(constants)
                .sortedBy { it.base }
            fields[lookupName] = newFields
            return newFields
        }

        return fields[lookupName] ?: buildFields()
    }

    private fun getTransitions(name: String, fields: List<JsonField>): List<List<JsonGroup>> {
        fun diff(previousBase: String, currentBase: String): Pair<List<String>, List<String>> {
            val previousSegs = if (previousBase.isNotEmpty()) previousBase.split('.') else emptyList()
            val currentSegs = if (currentBase.isNotEmpty()) currentBase.split('.') else emptyList()
            var i = 0
            while (i < previousSegs.size && i < currentSegs.size && previousSegs[i] == currentSegs[i])
                i += 1
            return Pair(previousSegs.takeLast(previousSegs.size - i), currentSegs.takeLast(currentSegs.size - i))
        }

        @Synchronized
        fun buildTransitions(): List<List<JsonGroup>> {
            val newTransitions = mutableListOf<List<JsonGroup>>()
            var previous = JsonField("")
            for (current in fields.plus(JsonField(""))) {
                val transition = mutableListOf<JsonGroup>()
                var (ending, starting) = diff(previous.base, current.base)
                while (ending.isNotEmpty()) {
                    val end = ending.last()
                    ending = ending.dropLast(1)
                    when {
                        end.startsWith("[") -> transition += JsonGroup(JsonGroupType.END_OBJECT)
                        end.endsWith("]") -> {
                            transition += JsonGroup(JsonGroupType.END_OBJECT)
                            transition += JsonGroup(JsonGroupType.END_ARRAY)
                        }
                        else -> transition += JsonGroup(JsonGroupType.END_OBJECT)
                    }
                }
                while (starting.isNotEmpty()) {
                    val start = starting.first()
                    starting = starting.drop(1)
                    when {
                        start.startsWith("[") -> transition += JsonGroup(JsonGroupType.START_OBJECT)
                        start.endsWith("]") -> {
                            transition += JsonGroup(JsonGroupType.START_ARRAY, start.substringBefore('['))
                            transition += JsonGroup(JsonGroupType.START_OBJECT)
                        }
                        else -> transition += JsonGroup(JsonGroupType.START_OBJECT, start)
                    }
                }
                newTransitions += transition
                previous = current
            }
            transitions[name] = newTransitions
            return newTransitions
        }

        return transitions[name] ?: buildTransitions()
    }

    private fun createMessage(fields: List<JsonField>, transitions: List<List<JsonGroup>>, row: List<String>): String {
        val out = ByteArrayOutputStream()
        out.use {
            val generator = factory.createGenerator(out)
            generator.use {
                generator.writeStartObject()
                for (i in fields.indices) {
                    writeTransition(generator, transitions[i])
                    writeField(generator, fields[i], row)
                }
                writeTransition(generator, transitions.last())
            }
        }
        return out.toString()
    }

    private fun writeTransition(to: JsonGenerator, transition: List<JsonGroup>) {
        transition.forEach {
            when (it.type) {
                JsonGroupType.END_OBJECT -> to.writeEndObject()
                JsonGroupType.END_ARRAY -> to.writeEndArray()
                JsonGroupType.START_OBJECT ->
                    if (it.name != null)
                        to.writeObjectFieldStart(it.name)
                    else
                        to.writeStartObject()
                JsonGroupType.START_ARRAY ->
                    if (it.name != null)
                        to.writeArrayFieldStart(it.name)
                    else
                        to.writeStartArray()
            }
        }
    }

    private fun writeField(to: JsonGenerator, field: JsonField, row: List<String>) {
        val value = field.value
            ?: if (field.useCurrentTime)
                OffsetDateTime.now().toString()
            else
                row[field.column!!]

        if (value.isBlank()) return

        to.writeFieldName(field.name)
        when (field.element?.type) {
            Element.Type.DATE -> {
                val date = LocalDate.parse(value, Element.dateFormatter)
                to.writeString(date.toString())
            }
            Element.Type.DATETIME -> {
                val date = OffsetDateTime.parse(value, Element.datetimeFormatter)
                to.writeString(date.toString())
            }
            Element.Type.NUMBER -> {
                to.writeNumber(value.toInt())
            }
            Element.Type.CODE -> {
                val formatted: String? = when {
                    field.element.valueSetRef?.system == ValueSet.SetSystem.SNOMED_CT ->
                        field.element.toFormatted(value, Element.caretToken)
                    field.element.altValues != null ->
                        field.element.toAltDisplay(value)
                    else ->
                        field.element.toFormatted(value, Element.displayToken)
                }
                when (formatted) {
                    null -> to.writeNull()
                    "true" -> to.writeBoolean(true)
                    "false" -> to.writeBoolean(false)
                    else -> to.writeString(formatted)
                }
            }
            Element.Type.POSTAL_CODE -> {
                val formatted = field.element.toFormatted(value, Element.zipFiveToken)
                to.writeString(formatted)
            }
            Element.Type.EMAIL -> {
                to.writeArray(arrayOf(value), 0, 1)
            }
            Element.Type.TELEPHONE -> {
                val e164 = field.element.toFormatted(value, Element.e164Token)
                to.writeString(e164)
            }
            else -> {
                to.writeString(value)
            }
        }
    }
}