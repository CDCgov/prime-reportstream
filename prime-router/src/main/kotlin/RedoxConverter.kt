package gov.cdc.prime.router

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Properties

class RedoxConverter(val metadata: Metadata) {
    private val softwareVendorOrganization = "Centers for Disease Control and Prevention"
    private val softwareProductName = "PRIME Data Hub"
    private val metaDataModelSegs = parsePath("Meta.DataModel")
    private val metaEventTypeSegs = parsePath("Meta.EventType")
    private val metaEventDateTimeSegs = parsePath("Meta.EventDateTime")
    private val metaSourceIdSegs = parsePath("Meta.Source.ID")

    private val buildVersion: String
    private val buildDate: String

    private val factory = JsonFactory()

    enum class AOEOptions { None, Normal, Notes }

    init {
        val buildProperties = Properties()
        val propFileStream = this::class.java.classLoader.getResourceAsStream("build.properties")
            ?: error("Could not find the properties file")
        propFileStream.use {
            buildProperties.load(it)
            buildVersion = buildProperties.getProperty("buildVersion", "0.0.0.0")
            buildDate = buildProperties.getProperty("buildDate", "20200101")
        }
    }

    fun write(report: Report, aoeOptions: AOEOptions, outputStream: OutputStream) {
        val messages = report.itemIndices.map {
            val row = report.getRow(it)
            createMessage(report.schema, aoeOptions, row)
        }
        val out = "[\n${messages.joinToString(", \n")}\n]"
        outputStream.write(out.toByteArray())
    }

    fun createMessage(schema: Schema, aoeOptions: AOEOptions, row: List<String>): String {
        val builder = Builder()
        setLiterials(builder)
        schema.elements.forEachIndexed { index, element ->
            val value = row[index]
            addElement(builder, element, value, aoeOptions)
        }
        return builder.build(factory)
    }

    fun setLiterials(builder: Builder) {
        builder.setString(metaDataModelSegs, "Results")
        builder.setString(metaEventTypeSegs, "NewUnsolicited")
        builder.setString(metaEventDateTimeSegs, OffsetDateTime.now().toString())
    }

    fun addElement(builder: Builder, element: Element, value: String, aoeOptions: AOEOptions) {
        // TODO: this is a place that could be optimized by doing once per schema
        val segmentsList = parsePaths(element.redoxOutputFields ?: emptyList())
        if (value.isBlank()) {
            segmentsList.forEach { builder.setNull(it) }
            return
        }
        when (element.type) {
            Element.Type.DATE -> {
                val date = LocalDate.parse(value, Element.dateFormatter)
                segmentsList.forEach { builder.setString(it, date.toString()) }
            }
            Element.Type.DATETIME -> {
                val date = OffsetDateTime.parse(value, Element.datetimeFormatter)
                segmentsList.forEach { builder.setString(it, date.toString()) }
            }
            Element.Type.NUMBER -> {
                segmentsList.forEach { builder.setNumber(it, value.toInt()) }
            }
            Element.Type.CODE -> {
                val formatted: String? = when {
                    element.valueSetRef?.system == ValueSet.SetSystem.SNOMED_CT ->
                        element.toFormatted(value, Element.caretToken)
                    element.altValues != null ->
                        element.toAltDisplay(value)
                    else ->
                        element.toFormatted(value, Element.displayToken)
                }
                segmentsList.forEach {
                    if (formatted == null)
                        builder.setNull(it)
                    else
                        builder.setString(it, formatted)
                }
            }
            Element.Type.POSTAL_CODE -> {
                val formatted = element.toFormatted(value, Element.zipFiveToken)
                segmentsList.forEach { builder.setString(it, formatted) }
            }
            Element.Type.EMAIL -> {
                segmentsList.forEach { builder.setStringArray(it, listOf(value)) }
            }
            Element.Type.TELEPHONE -> {
                val e164 = element.toFormatted(value, Element.e164Token)
                segmentsList.forEach { builder.setString(it, e164) }
            }
            else -> {
                segmentsList.forEach { builder.setString(it, value) }
            }
        }
    }

    data class Segment(val name: String, val isArray: Boolean)

    fun parsePaths(paths: List<String>): List<List<Segment>> {
        return paths.map { parsePath(it) }
    }

    fun parsePath(path: String): List<Segment> {
        if (path.isBlank()) return emptyList()
        val parts = path.split(".")
        return parts.flatMap {
            val leftIndex = it.indexOf("[")
            if (leftIndex == -1) {
                listOf(Segment(it, false))
            } else {
                val rightIndex = it.indexOf("]")
                if (rightIndex == -1) error("Schema Error: '$path'")
                val arrayName = it.slice(0 until leftIndex).trim()
                val arrayIndex = it.slice(leftIndex + 1 until rightIndex).trim()
                listOf(Segment(arrayName, true), Segment(arrayIndex, true))
            }
        }
    }

    class Builder() {
        open class BaseValue
        class IntValue(val value: Int) : BaseValue()
        class StringValue(val value: String) : BaseValue()
        class ObjectValue(val values: MutableMap<String, BaseValue> = mutableMapOf()) : BaseValue()
        class ArrayValue(val values: MutableMap<String, ObjectValue> = mutableMapOf()) : BaseValue()
        class StringArrayValue(val values: List<String>) : BaseValue()
        class NullValue() : BaseValue()

        val root = ObjectValue()

        fun setNull(segs: List<Segment>) {
            val obj = getObject(segs.slice(0 until segs.size - 1))
            obj.values[segs.last().name] = NullValue()
        }

        fun setNumber(segs: List<Segment>, value: Int) {
            val obj = getObject(segs.slice(0 until segs.size - 1))
            obj.values[segs.last().name] = IntValue(value)
        }

        fun setString(segs: List<Segment>, value: String) {
            val obj = getObject(segs.slice(0 until segs.size - 1))
            obj.values[segs.last().name] = StringValue(value)
        }

        fun setStringArray(segs: List<Segment>, value: List<String>) {
            val obj = getObject(segs.slice(0 until segs.size - 1))
            obj.values[segs.last().name] = StringArrayValue(value)
        }

        /**
         * get an object based on the list of segments. Create the object on the first fetch.
         */
        fun getObject(baseSegments: List<Segment>): ObjectValue {

            fun getArrayObject(array: ArrayValue, arrayIndex: String): ObjectValue {
                var obj = array.values[arrayIndex]
                if (obj == null) {
                    obj = ObjectValue()
                    array.values[arrayIndex] = obj
                }
                return obj
            }

            var current: ObjectValue = root
            var segmentIndex = 0
            while (segmentIndex < baseSegments.size) {
                val segment = baseSegments[segmentIndex++]
                current = when (val fieldValue = current.values[segment.name]) {
                    is ObjectValue -> fieldValue
                    is ArrayValue -> {
                        val arraySegment = baseSegments[segmentIndex++]
                        getArrayObject(fieldValue, arraySegment.name)
                    }
                    null -> {
                        if (segment.isArray) {
                            val arraySegment = baseSegments[segmentIndex++]
                            val array = ArrayValue()
                            current.values[segment.name] = array
                            getArrayObject(array, arraySegment.name)
                        } else {
                            val obj = ObjectValue()
                            current.values[segment.name] = obj
                            obj
                        }
                    }
                    else -> error("Internal Error: '${segment.name}'")
                }
            }
            return current
        }

        fun build(jsonFactory: JsonFactory): String {
            val out = ByteArrayOutputStream()
            out.use {
                val generator = jsonFactory.createGenerator(out)
                generator.use {
                    serializeObject(root, generator)
                }
            }
            return out.toString()
        }

        fun serializeObject(from: ObjectValue, to: JsonGenerator) {
            to.writeStartObject()
            from.values.forEach { (field, value) ->
                to.writeFieldName(field)
                when (value) {
                    is StringValue -> {
                        to.writeString(value.value)
                    }
                    is IntValue -> {
                        to.writeNumber(value.value)
                    }
                    is StringArrayValue -> {
                        to.writeStartArray()
                        value.values.forEach { to.writeString(it) }
                        to.writeEndArray()
                    }
                    is NullValue -> {
                        to.writeNull()
                    }
                    is ObjectValue -> {
                        serializeObject(value, to)
                    }
                    is ArrayValue -> {
                        to.writeStartArray(value.values.size)
                        value.values.forEach { serializeObject(it.value, to) }
                        to.writeEndArray()
                    }
                }
            }
            to.writeEndObject()
        }
    }
}