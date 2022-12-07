package gov.cdc.prime.router.serializers

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.model.Type
import ca.uhn.hl7v2.model.Varies
import ca.uhn.hl7v2.model.v251.datatype.CE
import ca.uhn.hl7v2.model.v251.datatype.CWE
import ca.uhn.hl7v2.model.v251.datatype.DR
import ca.uhn.hl7v2.model.v251.datatype.DT
import ca.uhn.hl7v2.model.v251.datatype.NM
import ca.uhn.hl7v2.model.v251.datatype.SN
import ca.uhn.hl7v2.model.v251.datatype.TS
import ca.uhn.hl7v2.model.v251.datatype.XTN
import ca.uhn.hl7v2.model.v251.group.ORU_R01_OBSERVATION
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.parser.EncodingNotSupportedException
import ca.uhn.hl7v2.parser.ModelClassFactory
import ca.uhn.hl7v2.preparser.PreParser
import ca.uhn.hl7v2.util.Terser
import com.anyascii.AnyAscii
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLogDetail
import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.FieldPrecisionMessage
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.InvalidHL7Message
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Source
import gov.cdc.prime.router.ValueSet
import gov.cdc.prime.router.common.DateUtilities
import gov.cdc.prime.router.common.DateUtilities.formatDateTimeForReceiver
import gov.cdc.prime.router.common.Hl7Utilities
import gov.cdc.prime.router.common.StringUtilities.trimToNull
import gov.cdc.prime.router.metadata.ElementAndValue
import gov.cdc.prime.router.metadata.Mapper
import org.apache.logging.log4j.kotlin.Logging
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties
import java.util.TimeZone
import kotlin.math.min

class Hl7Serializer(
    val metadata: Metadata,
    val settings: SettingsProvider
) : Logging {
    /**
     * HL7 mapping of all messages in a report submission.
     */
    data class Hl7Mapping(
        val mappedRows: Map<String, List<String>>,
        val items: List<MessageResult>
    )

    /**
     * Result of one HL7 message.
     */
    data class MessageResult(
        val item: Map<String, List<String>>,
        val errors: MutableList<ActionLogDetail>,
        val warnings: MutableList<ActionLogDetail>
    )

    enum class ErrorType(val type: String) {
        INVALID_HL7_MESSAGE_DATE_VALIDATION("INVALID_HL7_MESSAGE_DATE_VALIDATION"),
        INVALID_HL7_MESSAGE_VALIDATION("INVALID_HL7_MESSAGE_VALIDATION"),
        INVALID_HL7_MESSAGE_FORMAT("INVALID_HL7_MESSAGE_FORMAT"),
        INVALID_HL7_PHONE_NUMBER("INVALID_HL7_PHONE_NUMBER")
    }

    private val hl7SegmentDelimiter: String = "\r"
    private val hapiContext = DefaultHapiContext()
    private val modelClassFactory: ModelClassFactory = CanonicalModelClassFactory(HL7_SPEC_VERSION)
    private val buildVersion: String
    private val buildDate: String

    init {
        val buildProperties = Properties()
        val propFileStream = this::class.java.classLoader.getResourceAsStream("build.properties")
            ?: error("Could not find the properties file")
        propFileStream.use {
            buildProperties.load(it)
            buildVersion = buildProperties.getProperty("buildVersion", "0.0.0.0")
            buildDate = buildProperties.getProperty("buildDate", "20200101")
        }
        hapiContext.modelClassFactory = modelClassFactory
    }

    /**
     * Write a report with a single item
     */
    fun write(report: Report, outputStream: OutputStream) {
        if (report.itemCount != 1) {
            error("Internal Error: multiple item report cannot be written as a single HL7 message")
        }
        val message = createMessage(report, 0)
        outputStream.write(message.toByteArray())
    }

    /**
     * Write a report with BHS and FHS segments and multiple items. [sendingApplicationReport],
     * [receivingApplicationReport], and [receivingFacilityReport] are used for empty batch generation and will be used
     * if passed in (instead of pulling them from the 1st record)
     */
    fun writeBatch(
        report: Report,
        outputStream: OutputStream,
        sendingApplicationReport: String? = null,
        receivingApplicationReport: String? = null,
        receivingFacilityReport: String? = null
    ) {
        // Dev Note: HAPI doesn't support a batch of messages, so this code creates
        // these segments by hand
        outputStream.write(
            createHeaders(
                report,
                sendingApplicationReport,
                receivingApplicationReport,
                receivingFacilityReport
            ).toByteArray()
        )
        report.itemIndices.map {
            val message = createMessage(report, it)
            outputStream.write(message.toByteArray())
        }
        outputStream.write(createFooters(report).toByteArray())
    }

    /**
     * Decode an HL7 batch [message] for a given [schema] and [sender].
     * @return the decoded HL7 data
     */
    fun convertBatchMessagesToMap(message: String, schema: Schema, sender: Sender? = null): Hl7Mapping {
        val mappedRows: MutableMap<String, MutableList<String>> = mutableMapOf()
        val rowResults = mutableListOf<MessageResult>()
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(message, hl7SegmentDelimiter)
        val messageLines = cleanedMessage.split(hl7SegmentDelimiter)
        val nextMessage = StringBuilder()
        var messageIndex = 1

        /**
         * Parse an HL7 [message] from a string.
         */
        fun parseStringMessage(message: String) {
            val parsedMessage = convertMessageToMap(message, messageIndex, schema, sender)
            if (parsedMessage.item.isNotEmpty() || parsedMessage.errors.isNotEmpty() ||
                parsedMessage.warnings.isNotEmpty()
            ) {
                rowResults.add(parsedMessage)
            }
            parsedMessage.item.forEach { (k, v) ->
                if (!mappedRows.containsKey(k)) {
                    mappedRows[k] = mutableListOf()
                }

                mappedRows[k]?.addAll(v)
            }
        }

        messageLines.forEach {
            if (it.startsWith("FHS")) {
                return@forEach
            }
            if (it.startsWith("BHS")) {
                return@forEach
            }
            if (it.startsWith("BTS")) {
                return@forEach
            }
            if (it.startsWith("FTS")) {
                return@forEach
            }

            if (nextMessage.isNotBlank() && it.startsWith("MSH")) {
                parseStringMessage(nextMessage.toString())
                nextMessage.clear()
                messageIndex++
            }

            if (it.isNotBlank()) {
                nextMessage.append("$it\r")
            }
        }

        // catch the last message
        if (nextMessage.isNotBlank()) {
            parseStringMessage(nextMessage.toString())
        }

        return Hl7Mapping(mappedRows, rowResults)
    }

    /**
     * Convert an HL7 [message] with [messageIndex] index based on the specified [schema] and [sender].
     * @return the resulting data
     */
    fun convertMessageToMap(message: String, messageIndex: Int, schema: Schema, sender: Sender? = null): MessageResult {
        val errors = mutableListOf<ActionLogDetail>()
        val warnings = mutableListOf<ActionLogDetail>()

        /**
         * Query the terser and get a value.
         * @param terser the HAPI terser
         * @param terserSpec the HL7 field to fetch as a terser spec
         * @return the value from the HL7 message or an empty string if no value found
         */
        fun queryTerserForValue(
            terser: Terser,
            terserSpec: String
        ): String {
            val parsedValue = try {
                terser.get(terserSpec) ?: if (terserSpec.contains("OBX") || terserSpec.contains("SPM")) {
                    terser.get("/.SPECIMEN" + terserSpec.replace(".", ""))
                } else {
                    null
                }
            } catch (e: HL7Exception) {
                errors.add(InvalidHL7Message("Unexpected error while parsing $terserSpec: ${e.message}"))
                null
            }

            return parsedValue ?: ""
        }

        // key of the map is the column header, list is the values in the column
        val mappedRows: MutableMap<String, String> = mutableMapOf()
        hapiContext.modelClassFactory = modelClassFactory
        val parser = hapiContext.pipeParser
        val reg = "[\r\n]".toRegex()
        val cleanedMessage = reg.replace(message, hl7SegmentDelimiter).trim()
        // if the message is empty, return a row result that warns of empty data
        if (cleanedMessage.isEmpty()) {
            logger.debug("Skipping empty message during parsing")
            warnings.add(
                InvalidHL7Message(
                    "Cannot parse empty HL7 message. Please " +
                        "refer to the HL7 specification and resubmit."
                )
            )
            return MessageResult(emptyMap(), errors, warnings)
        }

        val hapiMsg = try {
            // First check that we have an HL7 message we can parse.  Note some older messages may have
            // only MSH 9-1 and MSH-9-2, or even just MSH-9-1, so we need use those two fields to compare
            val msgType = PreParser.getFields(cleanedMessage, "MSH-9-1", "MSH-9-2")
            val altMsgType = PreParser.getFields(cleanedMessage, "MSH-9-3")
            when {
                msgType.isNullOrEmpty() || msgType[0] == null -> {
                    errors.add(FieldPrecisionMessage("MSH-9", "Missing required HL7 message type field."))
                    return MessageResult(emptyMap(), errors, warnings)
                }
                // traditional way for checking message type
                arrayOf("ORU", "R01") contentEquals msgType -> parser.parse(cleanedMessage)
                // there's an alternate message type field
                arrayOf("ORU_R01") contentEquals altMsgType -> parser.parse(cleanedMessage)
                else -> {
                    warnings.add(
                        FieldPrecisionMessage(
                            "ORU_R01",
                            "Unsupported HL7 message type."
                        )
                    )
                    return MessageResult(emptyMap(), errors, warnings)
                }
            }
        } catch (e: HL7Exception) {
            logger.error("${e.localizedMessage} ${e.stackTraceToString()}")
            when (e) {
                is EncodingNotSupportedException ->
                    // This exception error message is a bit cryptic, so let's provide a better one.
                    errors.add(
                        InvalidHL7Message(
                            "Invalid HL7 message format. Please " +
                                "refer to the HL7 specification and resubmit.",
                            ErrorType.INVALID_HL7_MESSAGE_FORMAT.type
                        )
                    )
                else ->
                    if (e.location?.toString() == "PID-29(0)") {
                        errors.add(
                            FieldPrecisionMessage(
                                e.location.toString(),
                                "Error parsing HL7 message: ${e.localizedMessage}",
                                ErrorType.INVALID_HL7_MESSAGE_DATE_VALIDATION.type
                            )
                        )
                    } else {
                        errors.add(
                            if (e.location != null) {
                                FieldPrecisionMessage(
                                    e.location.toString(),
                                    "Error parsing HL7 message: ${e.localizedMessage}",
                                    ErrorType.INVALID_HL7_MESSAGE_VALIDATION.type
                                )
                            } else {
                                InvalidHL7Message(
                                    "Error parsing HL7 message: ${e.localizedMessage}",
                                    ErrorType.INVALID_HL7_MESSAGE_VALIDATION.type
                                )
                            }
                        )
                    }
            }
            return MessageResult(emptyMap(), errors, warnings)
        }

        try {
            // check the observation group order and reorder if necessary to ensure test result is first
            val organizedHapiMsg = organizeObservationOrder(hapiMsg)
            val terser = Terser(organizedHapiMsg)

            val orc23 = terser.getSegment("/.ORC")
            logger.debug(orc23.name)

            // First, extract any data elements from the HL7 message.
            schema.elements.forEach { element ->
                // If there is no value for the key, then initialize it.
                if (!mappedRows.containsKey(element.name)) {
                    mappedRows[element.name] = ""
                }

                // Make a list of all the HL7 primary and alternate fields to look into.
                // Note that the hl7Fields list will be empty if no fields is specified
                val hl7Fields = ArrayList<String>()
                if (!element.hl7Field.isNullOrEmpty()) hl7Fields.add(element.hl7Field)
                if (!element.hl7OutputFields.isNullOrEmpty()) hl7Fields.addAll(element.hl7OutputFields)
                var value = ""
                for (i in 0 until hl7Fields.size) {
                    val hl7Field = hl7Fields[i]
                    value = when {
                        // Decode a phone number
                        element.type == Element.Type.TELEPHONE ||
                            element.type == Element.Type.EMAIL ->
                            decodeHl7TelecomData(terser, element, hl7Field)

                        // Decode a timestamp
                        element.type == Element.Type.DATETIME ||
                            element.type == Element.Type.DATE ->
                            decodeHl7DateTime(terser, element, hl7Field, warnings)

                        hl7Field == "NTE-3" -> decodeNTESegments(hapiMsg)

                        // Decode an AOE question
                        hl7Field == "AOE" ->
                            decodeAOEQuestion(element, hapiMsg)

                        // Process a CODE type field.  IMPORTANT: Must be checked after AOE as AOE is a CODE field
                        element.type == Element.Type.CODE -> {
                            val rawValue = queryTerserForValue(
                                terser,
                                getTerserSpec(hl7Field)
                            )
                            // This verifies the code received is good.  Note the translated value will be the same as
                            // the raw value for valuesets and altvalues
                            try {
                                when {
                                    rawValue.isBlank() -> ""

                                    element.altValues != null && element.altValues.isNotEmpty() ->
                                        element.toNormalized(rawValue, Element.altDisplayToken)

                                    !element.valueSet.isNullOrEmpty() ->
                                        element.toNormalized(rawValue, Element.codeToken)

                                    else -> rawValue
                                }
                            } catch (e: IllegalStateException) {
                                warnings.add(
                                    InvalidHL7Message(
                                        "The code $rawValue for field $hl7Field is " +
                                            "invalid. Please refer to the HL7 specification and " +
                                            "resubmit."
                                    )
                                )
                                ""
                            }
                        }

                        // No special case here, so get a value from an HL7 field
                        else ->
                            queryTerserForValue(
                                terser,
                                getTerserSpec(hl7Field)
                            )
                    }
                    if (value.isNotBlank()) break
                }

                if (value.isNotBlank()) {
                    mappedRows[element.name] = value
                }
            }

            // Second, we process all the element raw values through mappers and defaults.
            schema.processValues(
                mappedRows,
                errors,
                warnings,
                sender = sender,
                itemIndex = messageIndex
            )
        } catch (e: Exception) {
            val msg = "${e.localizedMessage} ${e.stackTraceToString()}"
            logger.error(msg)

            if ((e as NumberParseException).errorType.name == "NOT_A_NUMBER") {
                errors.add(
                    FieldPrecisionMessage(
                        "ORC-23(0)",
                        msg,
                        ErrorType.INVALID_HL7_PHONE_NUMBER.type
                    )
                )
            } else {
                errors.add(InvalidHL7Message(msg))
            }
        }

        // convert sets to lists
        val rows = mappedRows.keys.associateWith {
            if (mappedRows[it] != null) listOf(mappedRows[it]!!) else emptyList()
        }

        return MessageResult(rows, errors, warnings)
    }

    fun readExternal(
        schemaName: String,
        input: InputStream,
        source: Source,
        sender: Sender? = null
    ): ReadResult {
        val messageBody = input.bufferedReader().use { it.readText() }
        val schema = metadata.findSchema(schemaName) ?: error("Schema name $schemaName not found")
        val mapping = convertBatchMessagesToMap(messageBody, schema, sender = sender)
        val mappedRows = mapping.mappedRows

        mapping.mappedRows.forEach {
            logger.debug("${it.key} -> ${it.value.joinToString()}")
        }

        // Generate the action log
        val actionLogs = ActionLogger()
        mapping.items.forEachIndexed { index, messageResult ->
            val messageIndex = index + 1
            var trackingId = if (
                schema.trackingElement != null && messageResult.item.contains(schema.trackingElement)
            ) {
                messageResult.item[schema.trackingElement]?.firstOrNull() ?: ""
            } else ""
            if (trackingId.isEmpty()) {
                trackingId = "message$messageIndex"
            }
            val itemLogger = actionLogs.getItemLogger(messageIndex, trackingId)
            itemLogger.error(messageResult.errors)
            itemLogger.warn(messageResult.warnings)
        }

        if (actionLogs.hasErrors()) {
            throw ActionError(actionLogs.errors)
        } else {
            val report = Report(schema, mappedRows, source, metadata = metadata)
            return ReadResult(report, actionLogs)
        }
    }

    fun createMessage(report: Report, row: Int): String {
        val hl7Config = report.destination?.translation as? Hl7Configuration?
        val processingId = if (hl7Config?.useTestProcessingMode == true) {
            "T"
        } else {
            "P"
        }
        val message = buildMessage(report, row, processingId)
        hapiContext.modelClassFactory = modelClassFactory
        if (hl7Config?.replaceUnicodeWithAscii == true) {
            return unicodeToAscii(hapiContext.pipeParser.encode(message))
        }
        return hapiContext.pipeParser.encode(message)
    }

    /**
     * Create the ORU message from the internal report
     * @param report with message
     * @param row in report
     * @param processingId
     */
    fun buildMessage(
        report: Report,
        row: Int,
        processingId: String = "T"
    ): ORU_R01 {
        val message = ORU_R01()
        message.initQuickstart(MESSAGE_CODE, MESSAGE_TRIGGER_EVENT, processingId)

        val hl7Report = report.copy()
        // set up our configuration
        val hl7Config = hl7Report.destination?.translation as? Hl7Configuration
        val replaceValue = hl7Config?.replaceValue ?: emptyMap()
        val replaceValueAwithB = hl7Config?.replaceValueAwithB ?: emptyMap()
        val cliaForSender = hl7Config?.cliaForSender ?: emptyMap()
        val suppressQst = hl7Config?.suppressQstForAoe ?: false
        val suppressAoe = hl7Config?.suppressAoe ?: false
        val applyOTCDefault = hl7Config?.applyOTCDefault ?: false
        val useOrderingFacilityName = hl7Config?.useOrderingFacilityName
            ?: Hl7Configuration.OrderingFacilityName.STANDARD
        val stripInvalidCharactersRegex: Regex? = hl7Config?.stripInvalidCharsRegex?.let {
            Regex(hl7Config.stripInvalidCharsRegex)
        }

        // and we have some fields to suppress
        val suppressedFields = hl7Config
            ?.suppressHl7Fields
            ?.split(",")
            ?.map { it.trim() } ?: emptyList()
        // or maybe we're going to suppress UNK/ASKU for some fields
        val blanksForUnknownFields = hl7Config
            ?.useBlankInsteadOfUnknown
            ?.split(",")
            ?.map { it.lowercase().trim() } ?: emptyList()
        val convertTimestampToDateTimeFields = hl7Config
            ?.convertTimestampToDateTime
            ?.split(",")
            ?.map { it.trim() } ?: emptyList()

        // start processing
        var aoeSequence = 1
        var nteSequence = 0
        val terser = Terser(message)
        setLiterals(terser, report)
        // we are going to set up overrides for the elements in the collection if the valueset
        // needs to be overriden
        val reportElements = if (hl7Config?.valueSetOverrides.isNullOrEmpty()) {
            // there are no value set overrides, so we are going to just pass back out the
            // existing collection of schema elements
            hl7Report.schema.elements
        } else {
            // we do have valueset overrides, so we need to replace any elements in place
            hl7Report.schema.elements.map { elem ->
                // if we're dealing with a code type (which uses a valueset), check if we need to replace
                if (elem.isCodeType) {
                    // is there a replacement valueset in our collection?
                    val replacementValueSet = hl7Config?.valueSetOverrides?.get(elem.valueSet)
                    if (replacementValueSet != null) {
                        // inherit from the base element
                        val newElement = Element(elem.name, valueSet = elem.valueSet, valueSetRef = replacementValueSet)
                        newElement.inheritFrom(elem)
                    } else {
                        elem
                    }
                } else {
                    // this is not a code type, so return the base element
                    elem
                }
            }
        }

        if (applyOTCDefault) {
            applyOTCDefault(hl7Report, row)
        }

        // serialize the rest of the elements
        reportElements.forEach { element ->
            val value = report.getString(row, element.name).let {
                if (it.isNullOrEmpty() || it == "null") {
                    element.default ?: ""
                } else {
                    stripInvalidCharactersRegex?.replace(it, "") ?: it
                }
            }.trim()

            if (suppressedFields.contains(element.hl7Field) && element.hl7OutputFields.isNullOrEmpty()) {
                return@forEach
            }

            if (element.hl7Field == "AOE" && suppressAoe) {
                return@forEach
            }

            // some fields need to be blank instead of passing in UNK
            // so in this case we'll just go by field name and set the value to blank
            if (blanksForUnknownFields.contains(element.name) &&
                element.hl7Field != null &&
                (value.equals("ASKU", true) || value.equals("UNK", true))
            ) {
                setComponent(terser, element, element.hl7Field, repeat = null, value = "", hl7Report)
                return@forEach
            }

            if (element.hl7OutputFields != null) {
                element.hl7OutputFields.forEach outputFields@{ hl7Field ->
                    if (suppressedFields.contains(hl7Field)) {
                        return@outputFields
                    }
                    if (element.hl7Field != null && element.isTableLookup) {
                        setComponentForTable(terser, element, hl7Field, hl7Report, row, hl7Config)
                    } else {
                        setComponent(terser, element, hl7Field, repeat = null, value, hl7Report)
                    }
                }
            } else if (element.hl7Field == "AOE" && element.type == Element.Type.NUMBER && !suppressAoe) {
                if (value.isNotBlank()) {
                    val units = report.getString(row, "${element.name}_units")
                    // cast to local time if the receiver wants it done
                    val date = report.getString(row, "specimen_collection_date_time").let {
                        if (it == null) {
                            ""
                        } else {
                            DateUtilities.parseDate(it).formatDateTimeForReceiver(report)
                        }
                    }
                    setAOE(terser, element, aoeSequence++, date, value, report, row, units, suppressQst)
                }
            } else if (element.hl7Field == "AOE" && !suppressAoe) {
                // cast to local time if the receiver wants it done
                val date = report.getString(row, "specimen_collection_date_time").let {
                    if (it == null) {
                        ""
                    } else {
                        DateUtilities.parseDate(it).formatDateTimeForReceiver(report)
                    }
                }
                if (value.isNotBlank()) {
                    setAOE(terser, element, aoeSequence++, date, value, report, row, suppressQst = suppressQst)
                } else {
                    // if the value is null but we're defaulting
                    if (hl7Config?.defaultAoeToUnknown == true) {
                        setAOE(terser, element, aoeSequence++, date, "UNK", report, row, suppressQst = suppressQst)
                    }
                }
            } else if (element.hl7Field == "ORC-21-1") {
                val truncatedValue = if (hl7Config?.truncateHl7Fields?.contains(element.hl7Field) == true) {
                    trimAndTruncateValue(value, element.hl7Field, hl7Config, terser)
                } else {
                    value
                }
                setOrderingFacilityComponent(
                    terser,
                    rawFacilityName = truncatedValue,
                    useOrderingFacilityName,
                    hl7Report,
                    row
                )
            } else if (element.hl7Field == "NTE-3" && value.isNotBlank()) {
                setNote(terser, nteSequence++, value)
            } else if (element.hl7Field == "MSH-7") {
                // put the created date time into local time if the receiver wants it done
                setComponent(
                    terser,
                    element,
                    "MSH-7",
                    repeat = null,
                    value = report.createdDateTime.formatDateTimeForReceiver(report),
                    report
                )
            } else if (element.hl7Field == "MSH-11") {
                setComponent(terser, element, "MSH-11", repeat = null, processingId, hl7Report)
            } else if (element.hl7Field != null && element.isTableLookup) {
                setComponentForTable(terser, element, hl7Report, row, hl7Config)
            } else if (!element.hl7Field.isNullOrEmpty()) {
                setComponent(terser, element, element.hl7Field, repeat = null, value, hl7Report)
            }
        }
        // make sure all fields we're suppressing are empty
        suppressedFields.forEach {
            val pathSpec = formPathSpec(it)
            terser.set(pathSpec, "")
        }

        if (hl7Config?.suppressNonNPI == true &&
            hl7Report.getString(row, "ordering_provider_id_authority_type") != "NPI"
        ) {
            // Suppress the ordering_provider_id if not an NPI
            for (hl7Field in listOf("ORC-12-1", "OBR-16-1", "ORC-12-9", "OBR-16-9", "ORC-12-13", "OBR-16-13")) {
                terser.set(formPathSpec(hl7Field), "")
            }
        }

        convertTimestampToDateTimeFields.forEach {
            // convert to local date time before converting if the receiver wants it so
            val pathSpec = formPathSpec(it)
            val tsValue = terser.get(pathSpec)
            if (!tsValue.isNullOrEmpty()) {
                try {
                    val parsedDate = DateUtilities
                        .parseDate(tsValue)
                        .formatDateTimeForReceiver(DateUtilities.DateTimeFormat.LOCAL, report)
                    terser.set(pathSpec, parsedDate)
                } catch (_: Exception) {
                    // for now do nothing
                }
            }
        }
        // check for reporting facility overrides
        if (!hl7Config?.reportingFacilityName.isNullOrEmpty()) {
            val pathSpec = formPathSpec("MSH-4-1")
            terser.set(pathSpec, hl7Config?.reportingFacilityName)
        }
        if (!hl7Config?.reportingFacilityId.isNullOrEmpty()) {
            var pathSpec = formPathSpec("MSH-4-2")
            terser.set(pathSpec, hl7Config?.reportingFacilityId)
            if (!hl7Config?.reportingFacilityIdType.isNullOrEmpty()) {
                pathSpec = formPathSpec("MSH-4-3")
                terser.set(pathSpec, hl7Config?.reportingFacilityIdType)
            }
        }

        // check for alt CLIA for out of state testing
        if (!hl7Config?.cliaForOutOfStateTesting.isNullOrEmpty()) {
            val testingStateField = "OBX-24-4"
            val pathSpecTestingState = formPathSpec(testingStateField)
            var originState = terser.get(pathSpecTestingState)

            if (originState.isNullOrEmpty()) {
                val orderingStateField = "ORC-24-4"
                val pathSpecOrderingState = formPathSpec(orderingStateField)
                originState = terser.get(pathSpecOrderingState)
            }

            if (!originState.isNullOrEmpty()) {
                val stateCode = hl7Report.destination?.let { settings.findOrganization(it.organizationName)?.stateCode }

                if (!originState.equals(stateCode)) {
                    val sendingFacility = "MSH-4-2"
                    val pathSpecSendingFacility = formPathSpec(sendingFacility)
                    terser.set(pathSpecSendingFacility, hl7Config?.cliaForOutOfStateTesting)
                }
            }
        }

        // get sender id for the record
        val senderID = hl7Report.getString(row, "sender_id") ?: ""

        // loop through CLIA resets
        cliaForSender.forEach { (sender, clia) ->
            try {
                // find that sender in the map
                if (sender.equals(senderID.trim(), ignoreCase = true) && clia.isNotEmpty()) {
                    // if the sender needs should have a specific CLIA then overwrite the CLIA here
                    val pathSpecSendingFacilityID = formPathSpec("MSH-4-2")
                    terser.set(pathSpecSendingFacilityID, clia)
                }
            } catch (e: Exception) {
                val msg = "${e.localizedMessage} ${e.stackTraceToString()}"
                logger.error(msg)
            }
        }

        replaceValueAwithBUsingTerser(
            replaceValueAwithB,
            terser,
            message.patienT_RESULT.ordeR_OBSERVATION.observationReps
        )
        replaceValue(replaceValue, terser, message.patienT_RESULT.ordeR_OBSERVATION.observationReps)
        return message
    }

    private fun applyOTCDefault(
        report: Report,
        row: Int
    ) {
        fun setAddress(
            field: String,
            address: String,
            city: String,
            state: String,
            zipCode: String,
            countyCode: String
        ) {
            if (address.isEmpty() &&
                city.isEmpty() &&
                state.isEmpty() &&
                zipCode.isEmpty() &&
                countyCode.isEmpty()
            ) {
                report.setString(row, field.plus("_street"), "11 Fake AtHome Test Street")
                report.setString(row, field.plus("_city"), "Yakutat")
                report.setString(row, field.plus("_state"), "AK")
                report.setString(row, field.plus("_zip_code"), "99689")
                report.setString(row, field.plus("_county_code"), "02282")
            }
        }
        report.setString(row, "reporting_facility_name", "PRIME OTC")
        report.setString(row, "reporting_facility_clia", "0OCDCPRIME")

        val testResultStatus = report.getString(row, "test_result_status") ?: ""
        if (testResultStatus.isEmpty()) {
            report.setString(row, "test_result_status", "F")
            report.setString(row, "order_result_status", "F")
            report.setString(row, "observation_result_status", "F")
        }

        val senderId = report.getString(row, "sender_id") ?: ""
        if (senderId.isNotEmpty()) {
            val comment = report.getString(row, "comment")
            report.setString(
                row,
                "comment",
                comment
                    .plus(" Original sending organization name: ")
                    .plus(senderId)
            )
        }

        val orderingProviderFirstName = report.getString(row, "ordering_provider_first_name") ?: ""
        if (orderingProviderFirstName.isEmpty()) {
            report.setString(row, "ordering_provider_first_name", "SA.OverTheCounter")
        }
        val orderingFacilityName = report.getString(row, "ordering_facility_name") ?: ""
        if (orderingFacilityName.isEmpty()) {
            report.setString(row, "ordering_facility_name", "SA.OverTheCounter")
        }
        val testingLabName = report.getString(row, "testing_lab_name") ?: ""
        if (testingLabName.isEmpty()) {
            report.setString(row, "testing_lab_name", "SA.OverTheCounter")
        }

        val orderingFacilityStreet = report.getString(row, "ordering_facility_street") ?: ""
        val orderingFacilityCity = report.getString(row, "ordering_facility_city") ?: ""
        val orderingFacilityState = report.getString(row, "ordering_facility_state") ?: ""
        val orderingFacilityZipCode = report.getString(row, "ordering_facility_zip_code") ?: ""
        val orderingFacilityCountyCode = report.getString(row, "ordering_facility_county_code") ?: ""

        setAddress(
            "ordering_facility",
            orderingFacilityStreet,
            orderingFacilityCity,
            orderingFacilityState,
            orderingFacilityZipCode,
            orderingFacilityCountyCode
        )

        val orderingFacilityPhoneNumber = report.getString(row, "ordering_facility_phone_number") ?: ""
        if (orderingFacilityPhoneNumber.isEmpty()) {
            report.setString(row, "ordering_facility_phone_number", "1111111111:1:")
        }

        val testingLabStreet = report.getString(row, "testing_lab_street") ?: ""
        val testingLabCity = report.getString(row, "testing_lab_city") ?: ""
        val testingLabState = report.getString(row, "testing_lab_state") ?: ""
        val testingLabZipCode = report.getString(row, "testing_lab_zip_code") ?: ""
        val testingLabCountyCode = report.getString(row, "testing_lab_county_code") ?: ""

        setAddress(
            "testing_lab",
            testingLabStreet,
            testingLabCity,
            testingLabState,
            testingLabZipCode,
            testingLabCountyCode
        )

        val testingLabClia = report.getString(row, "testing_lab_clia") ?: ""
        if (testingLabClia.isEmpty()) {
            report.setString(row, "testing_lab_clia", "00Z0000014")
        }
        val testingLabIdAssigner = report.getString(row, "testing_lab_id_assigner") ?: ""
        if (testingLabIdAssigner.isEmpty()) {
            report.setString(row, "testing_lab_id_assigner", "CLIA^2.16.840.1.113883.4.7^ISO")
        }
    }

    /**
     * The function goes through each segment in [replaceValueAwithBMap]
     * (SEGMENT: ["componentToReplace0": "newComponent0", "componentToReplace1": "newComponent1", ... ].
     * Next, it goes through fields and check if it is OBX of not, if it is, it replaces value of all OBXs
     * If it is not OBX segment, it will replace only that segment.
     *
     * @param replaceValueAwithBMap - String (SEGMENT: ["componentToReplace0": "newComponent0", ... ].
     * @param terser - message that contains HL7 to be working on.
     * @param observationRepeats - number of OBX segment.
     *
     * To understand the logic, you can follow in the Unit Test Hl7Serializer::
     */
    fun replaceValueAwithBUsingTerser(
        replaceValueAwithBMap: Map<String, Any>,
        terser: Terser,
        observationRepeats: Int
    ) {
        replaceValueAwithBMap.forEach segment@{ segment ->
            // Scan through segment(s)
            @Suppress("UNCHECKED_CAST")
            (segment.value as ArrayList<Map<String, String>>).forEach valuePairs@{ pairs ->
                val pathSpec = formPathSpec(segment.key)
                val componentInMessage = try {
                    terser.get(pathSpec) ?: ""
                } catch (e: Exception) {
                    return@segment
                }

                // Get field(s).  There could be more than one field separated by '~'
                val fields = pairs.values.first().trim().split(DEFAULT_REPETITION_SEPARATOR)

                var fieldRep = 0
                fields.forEach { field ->
                    // Get new components separate by ^ (second value of the value pair)
                    val components = field.trim().split(DEFAULT_COMPONENT_SEPARATOR)

                    // OBX segment can repeat. All repeats need to be looped
                    if (segment.key.length >= 3 && segment.key.substring(0, 3) == "OBX") {
                        for (i in 0..observationRepeats.minus(1)) {
                            val pathOBXSpec = formPathSpec(segment.key, i)
                            val valueInOBXMessage = terser.get(pathOBXSpec)
                            replaceValueAwithB(
                                valueInOBXMessage,
                                pairs,
                                components,
                                fields,
                                terser,
                                pathOBXSpec,
                                fieldRep
                            )
                        }
                    } else {
                        // Replace value if exact key from setting equal to key in message OR always replace
                        replaceValueAwithB(componentInMessage, pairs, components, fields, terser, pathSpec, fieldRep)
                    }
                    fieldRep++
                }
            }
        }
    }

    /**
     * The function checks to see if the existing field's value is the same as value wantting to replace.
     * or it is equal to '*" (wildcard).
     * It will replace the componentInMessageX with the newComponentX as following:
     *  If and only if the componentToReplaceX is equal to the componentInMassage or old component.
     *  If the componentToReplaceX is "*", it will replace regardless.
     *  If conponentToReplaceX contains prefix '*', it will retreive value from that component as value to replace.
     *      (e.g., ORC-2-1: [ "": "*MSH-10" ].  In this case, it will use value in MSH-10 to fill the blank of ORC-2-1
     *
     * @param componentInMessage - String (SEGMENT: ["componentToReplace0": "newComponent0", ... ].
     * @param pairs - exiting value to be replaced and new value to use to replace.
     * @param components - components of field
     * @param fields - fields of segment
     * @param terser - message that contains HL7 to be working on.
     * @param pathSpec -  HL7 pathSpec that point to the hl7 with the message.
     *
     * To understand the logic, you can follow in the Unit Test Hl7Serializer::
     */
    private fun replaceValueAwithB(
        componentInMessage: String?,
        pairs: Map<String, String>,
        components: List<String>,
        fields: List<String>,
        terser: Terser,
        pathSpec: String,
        fieldRep: Int
    ) {
        if (componentInMessage == pairs.keys.first().trim() || "*" == pairs.keys.first().trim()) {
            var componentRep = 1
            components.forEach { component ->
                val subComponents = component.split(DEFAULT_SUBCOMPONENT_SEPARATOR)
                if (subComponents.size > 1) {
                    // If there is subComponent separate by &, we need to handle them.
                    var suComponentRep = 1
                    subComponents.forEach { subComponent ->
                        if (fields.size > 1) {
                            terser.set(
                                "$pathSpec($fieldRep)-$componentRep-$suComponentRep",
                                subComponent
                            )
                        } else {
                            terser.set(
                                "$pathSpec-$componentRep-$suComponentRep",
                                subComponent
                            )
                        }
                        suComponentRep++
                    }
                } else {
                    // Here we check to see if the segment is absolute or indirect replacement.
                    // If the segment ID contains prefix of an '*', then get value from that segment to use as
                    // value replacement.
                    val value = if (component.isNotEmpty() && component.first().equals('*')) {
                        val refField = component.drop(1)
                        terser.get(formPathSpec(refField))
                    } else {
                        component
                    }

                    if (fields.size > 1) {
                        terser.set("$pathSpec($fieldRep)-$componentRep", value)
                    } else {
                        terser.set("$pathSpec-$componentRep", value)
                    }
                }
                componentRep++
            }
        }
    }

    /**
     * Loop through all [replaceValueMap] key value pairs to fill all non-empty
     * values in the [terser] message. Loop through the number OBX segments sent in
     * [observationRepeats]. Other segments should not repeat.
     */
    private fun replaceValue(
        replaceValueMap: Map<String, String>,
        terser: Terser,
        observationRepeats: Int
    ) {
        // after all values have been set or blanked, check for values that need replacement
        // isNotEmpty returns true only when a value exists. Whitespace only is considered a value
        replaceValueMap.forEach { element ->

            // value can be set as a comma separated list. First split the list .
            val valueList = element.value.split(",").map { it.trim() }
            var value = ""

            valueList.forEach { field ->

                // value could be a literal or a reference to a different HL7 field. When the terser.get fails
                // the assumption is to add the string as a literal
                val valueInMessage = try {
                    val pathSpec = formPathSpec(field)
                    terser.get(pathSpec)
                } catch (e: Exception) {
                    field
                }
                value = value.plus(valueInMessage)
            }

            // OBX segment can repeat. All repeats need to be looped
            if (element.key.length >= 3 && element.key.substring(0, 3) == "OBX") {
                for (i in 0..observationRepeats.minus(1)) {
                    val pathSpec = formPathSpec(element.key, i)
                    val valueInMessage = terser.get(pathSpec) ?: ""
                    if (valueInMessage.isNotEmpty()) {
                        terser.set(pathSpec, value)
                    }
                }
            } else {
                try {
                    val pathSpec = formPathSpec(element.key)
                    val valueInMessage = terser.get(pathSpec)
                    if (valueInMessage.isNotEmpty()) {
                        terser.set(pathSpec, value)
                    }
                } catch (e: Exception) {
                }
            }
        }
    }

    /**
     * Set the [terser]'s ORC-21 in accordance to the [useOrderingFacilityName] value.
     */
    fun setOrderingFacilityComponent(
        terser: Terser,
        rawFacilityName: String,
        useOrderingFacilityName: Hl7Configuration.OrderingFacilityName,
        report: Report,
        row: Int
    ) {
        when (useOrderingFacilityName) {
            // No overrides
            Hl7Configuration.OrderingFacilityName.STANDARD -> {
                setPlainOrderingFacility(terser, rawFacilityName)
            }

            // Override with NCES ID if available
            Hl7Configuration.OrderingFacilityName.NCES -> {
                val ncesId = getSchoolId(report, row, rawFacilityName)
                if (ncesId == null) {
                    setPlainOrderingFacility(terser, rawFacilityName)
                } else {
                    setNCESOrderingFacility(terser, rawFacilityName, ncesId)
                }
            }

            // Override with organization name if available
            Hl7Configuration.OrderingFacilityName.ORGANIZATION_NAME -> {
                val organizationName = report.getString(row, "organization_name").trimToNull() ?: rawFacilityName
                setPlainOrderingFacility(terser, organizationName)
            }
        }
    }

    /**
     * Set the [terser]'s ORC-21-1 with just the [rawFacilityName]
     */
    internal fun setPlainOrderingFacility(
        terser: Terser,
        rawFacilityName: String
    ) {
        terser.set(formPathSpec("ORC-21-1"), rawFacilityName.trim().take(50))
        // setting a default value for ORC-21-2 per PA's request.
        terser.set(formPathSpec("ORC-21-2"), DEFAULT_ORGANIZATION_NAME_TYPE_CODE)
    }

    /**
     * Set the [terser]'s ORC-21 in accordance to APHL guidance using the [rawFacilityName]
     * and the [ncesId] value.
     */
    internal fun setNCESOrderingFacility(
        terser: Terser,
        rawFacilityName: String,
        ncesId: String
    ) {
        // Implement APHL guidance for ORC-21 when NCES is known
        val facilityName = "${rawFacilityName.trim().take(32)}$NCES_EXTENSION$ncesId"
        terser.set(formPathSpec("ORC-21-1"), facilityName)
        terser.set(formPathSpec("ORC-21-6-1"), "NCES.IES")
        terser.set(formPathSpec("ORC-21-6-2"), "2.16.840.1.113883.3.8589.4.1.119")
        terser.set(formPathSpec("ORC-21-6-3"), "ISO")
        terser.set(formPathSpec("ORC-21-7"), "XX")
        terser.set(formPathSpec("ORC-21-10"), ncesId)
    }

    /**
     * Lookup the NCES id if the site_type is a k12 school
     */
    fun getSchoolId(report: Report, row: Int, rawFacilityName: String): String? {
        // This code only works on the COVID-19 schema or its extensions
        if (!report.schema.containsElement("ordering_facility_name")) return null
        // This recommendation only applies to k-12 schools
        if (report.getString(row, "site_of_care") != "k12") return null

        // NCES lookup is based on school name and zip code
        val zipCode = report.getString(row, "ordering_facility_zip_code", 5) ?: ""
        return ncesLookupTable.value.lookupBestMatch(
            lookupColumn = "NCESID",
            searchColumn = "SCHNAME",
            searchValue = rawFacilityName,
            filterColumn = "LZIP",
            filterValue = zipCode,
            canonicalize = { Hl7Utilities.canonicalizeSchoolName(it) },
            commonWords = listOf("ELEMENTARY", "JUNIOR", "HIGH", "MIDDLE")
        )
    }

    private fun setComponentForTable(
        terser: Terser,
        element: Element,
        report: Report,
        row: Int,
        config: Hl7Configuration? = null
    ) {
        setComponentForTable(terser, element, element.hl7Field!!, report, row, config)
    }

    private fun setComponentForTable(
        terser: Terser,
        element: Element,
        hl7Field: String,
        report: Report,
        row: Int,
        config: Hl7Configuration? = null
    ) {
        val lookupValues = mutableMapOf<String, String>()
        val pathSpec = formPathSpec(hl7Field)
        val mapper: Mapper? = element.mapperRef
        val args = element.mapperArgs ?: emptyList()
        val valueNames = mapper?.valueNames(element, args)
        report.schema.elements.forEach {
            lookupValues[it.name] = report.getString(row, it.name) ?: element.default ?: ""
        }
        val valuesForMapper = valueNames?.mapNotNull { elementName ->
            val valueElement = report.schema.findElement(elementName) ?: return@mapNotNull null
            val value = lookupValues[elementName] ?: return@mapNotNull null
            ElementAndValue(valueElement, value)
        }
        if (valuesForMapper == null) {
            terser.set(pathSpec, "")
        } else {
            val mappedValue = mapper.apply(element, args, valuesForMapper).value ?: ""
            val truncatedValue = trimAndTruncateValue(mappedValue, hl7Field, config, terser)
            // there are instances where we need to replace the DII value that comes from the LIVD
            // table with an OID that reflects that this is an equipment UID instead. NH raised this
            // as an issue, and the HHS spec on confluence supports their configuration, but we need
            // to isolate out this option, so we don't affect other states we're already in production with
            if (truncatedValue == "DII" && config?.replaceDiiWithOid == true && hl7Field == "OBX-18-3") {
                terser.set(formPathSpec("OBX-18-3"), OBX_18_EQUIPMENT_UID_OID)
                terser.set(formPathSpec("OBX-18-4"), "ISO")
            } else {
                terser.set(pathSpec, truncatedValue)
            }
        }
    }

    /**
     * Set the component to [value] in the [terser] for the passed [hl7Field].
     * [hl7Field] must match the internal [Element] formatting.
     * Set [repeat] for the repeated segment case.
     */
    private fun setComponent(
        terser: Terser,
        element: Element,
        hl7Field: String,
        repeat: Int?,
        value: String,
        report: Report
    ) {
        // Break down the configuration structure
        val hl7Config = report.destination?.translation as? Hl7Configuration?
        val phoneNumberFormatting = hl7Config?.phoneNumberFormatting
            ?: Hl7Configuration.PhoneNumberFormatting.STANDARD
        val pathSpec = formPathSpec(hl7Field, repeat)

        // All components should be trimmed and not blank.
        val trimmedValue = value.trimToNull() ?: return

        when (element.type) {
            Element.Type.ID_CLIA -> setCliaComponent(terser, trimmedValue, hl7Field, hl7Config)
            Element.Type.HD -> setHDComponent(terser, trimmedValue, pathSpec, hl7Field, hl7Config)
            Element.Type.EI -> setEIComponent(terser, trimmedValue, pathSpec, hl7Field, hl7Config)
            Element.Type.CODE -> setCodeComponent(
                terser,
                trimmedValue,
                pathSpec,
                element.valueSet,
                element.valueSetRef
            )
            Element.Type.TELEPHONE -> setTelephoneComponent(
                terser,
                trimmedValue,
                pathSpec,
                element,
                phoneNumberFormatting
            )
            Element.Type.EMAIL -> setEmailComponent(terser, trimmedValue, element, hl7Config)
            Element.Type.POSTAL_CODE -> setPostalComponent(terser, trimmedValue, pathSpec, element)
            Element.Type.DATE, Element.Type.DATETIME -> setDateTimeComponent(
                terser,
                trimmedValue,
                pathSpec,
                hl7Field,
                report,
                element
            )
            else -> {
                val truncatedValue = trimAndTruncateValue(trimmedValue, hl7Field, hl7Config, terser)
                terser.set(pathSpec, truncatedValue)
            }
        }
    }

    /** takes a value and does date time conversions on it in order to display it */
    internal fun setDateTimeComponent(
        terser: Terser,
        value: String,
        pathSpec: String,
        hl7Field: String,
        report: Report,
        element: Element
    ) {
        // get our configuration
        val hl7Config = report.destination?.translation as? Hl7Configuration
        // first allow the truncation to happen, so we carry that logic on down
        val truncatedValue = trimAndTruncateValue(value, hl7Field, hl7Config, terser)
        // if the value can't be parsed as a date, then we just pass through the value
        // we do this because there's a chance a date field could be set to `UNK` or
        // some other value, and we want to preserve data like that
        if (!DateUtilities.tryParse(truncatedValue)) {
            terser.set(pathSpec, truncatedValue)
            return
        }
        // if the type is date, then we need to force the format to YYYYMMDD only or
        // HL7 parsing will fail, otherwise it's going to use the default format
        val parsedValue = if (element.type == Element.Type.DATE) {
            DateUtilities
                .parseDate(truncatedValue)
                .formatDateTimeForReceiver(DateUtilities.DateTimeFormat.DATE_ONLY, report)
        } else {
            DateUtilities.parseDate(truncatedValue).formatDateTimeForReceiver(report)
        }
        // set the value, formatted for the receivers
        terser.set(pathSpec, parsedValue)
    }

    /**
     * Set the HD component specified by [hl7Field] in [terser] with [value].
     * Truncate appropriately according to [hl7Field] and [hl7Config]
     */
    internal fun setHDComponent(
        terser: Terser,
        value: String,
        pathSpec: String,
        hl7Field: String,
        hl7Config: Hl7Configuration?
    ) {
        val maxLength = getMaxLength(hl7Field, value, hl7Config, terser)
        val hd = Element.parseHD(value, maxLength)
        if (hd.universalId != null && hd.universalIdSystem != null) {
            // if we have entered this space, there is a chance we will not have accurately calculated
            // the max length for the HD namespace ID. Case in point, sending_application in the
            // COVID-19 schema is coded to go into the MSH-3 segment. when getMaxLength above is invoked
            // it doesn't accurately calculate max length because the max length for the entire field
            // is the sum of the three subfields. thus, an HD namespace ID can slide through untruncated.
            // we know that this could happen because the parseHD method does a split on the field, and if
            // there's more than one field, it fills in the universalId and universalIdSystem fields.
            // at this point now we do a final saving throw to see if maybe the subfield needs another trim.
            if (hl7Config?.truncateHDNamespaceIds == true) {
                val subpartMaxLength = getMaxLength("$hl7Field-1", value, hl7Config, terser)
                terser.set("$pathSpec-1", hd.name.trimAndTruncate(subpartMaxLength))
            } else {
                terser.set("$pathSpec-1", hd.name)
            }
            terser.set("$pathSpec-2", hd.universalId)
            terser.set("$pathSpec-3", hd.universalIdSystem)
        } else {
            terser.set(pathSpec, hd.name)
        }
    }

    /**
     * Set the EI component specified by [pathSpec] in [terser] with [value].
     * Truncate appropriately according to [hl7Field] and [hl7Config]
     */
    internal fun setEIComponent(
        terser: Terser,
        value: String,
        pathSpec: String,
        hl7Field: String,
        hl7Config: Hl7Configuration?
    ) {
        val maxLength = getMaxLength(hl7Field, value, hl7Config, terser)
        val ei = Element.parseEI(value)
        if (ei.universalId != null && ei.universalIdSystem != null) {
            terser.set("$pathSpec-1", ei.name.trimAndTruncate(maxLength))
            terser.set("$pathSpec-2", ei.namespace)
            terser.set("$pathSpec-3", ei.universalId)
            terser.set("$pathSpec-4", ei.universalIdSystem)
        } else {
            terser.set(pathSpec, ei.name.trimAndTruncate(maxLength))
        }
    }

    /**
     * Given the pathspec and the value, it will map that back to a valueset, or look up the valueset
     * based on the valueSetName, and fill in the field with the code
     */
    private fun setCodeComponent(
        terser: Terser,
        value: String,
        pathSpec: String,
        valueSetName: String?,
        elementValueSet: ValueSet? = null
    ) {
        if (valueSetName == null) error("Schema Error: Missing valueSet for '$pathSpec'")
        val valueSet = elementValueSet ?: metadata.findValueSet(valueSetName)
            ?: error("Schema Error: Cannot find '$valueSetName'")
        when (valueSet.system) {
            ValueSet.SetSystem.HL7,
            ValueSet.SetSystem.ISO,
            ValueSet.SetSystem.LOINC,
            ValueSet.SetSystem.UCUM,
            ValueSet.SetSystem.SNOMED_CT -> {
                // if it is a component spec then set all sub-components
                if (isField(pathSpec)) {
                    if (value.isNotEmpty()) {
                        // if a value in the valueset replaces something in the standard valueset
                        // we should default to that first, and then we will do all the other
                        // lookups based on that
                        val displayValue = valueSet.values.firstOrNull { v ->
                            v.replaces?.equals(value, true) == true
                        }?.code ?: value
                        terser.set("$pathSpec-1", displayValue)
                        terser.set("$pathSpec-2", valueSet.toDisplayFromCode(displayValue))
                        terser.set("$pathSpec-3", valueSet.toSystemFromCode(displayValue))
                        valueSet.toVersionFromCode(displayValue)?.let {
                            terser.set("$pathSpec-7", it)
                        }
                    }
                } else {
                    terser.set(pathSpec, value)
                }
            }
            else -> {
                terser.set(pathSpec, value)
            }
        }
    }

    /**
     * Set the [value] into the [hl7Field] in the [terser].
     * If [hl7Field] points to a universal HD field, set [value] as the Universal ID field
     * and set 'CLIA' as the Universal ID Type.
     * If [hl7Field] points to CE field, set [value] as the Identifier and 'CLIA' as the Text.
     */
    internal fun setCliaComponent(
        terser: Terser,
        value: String,
        hl7Field: String,
        hl7Config: Hl7Configuration? = null
    ) {
        if (value.isEmpty()) return
        val pathSpec = formPathSpec(hl7Field)
        val maxLength = getMaxLength(hl7Field, value, hl7Config, terser)
        terser.set(pathSpec, value.trimAndTruncate(maxLength))

        when (hl7Field) {
            in HD_FIELDS_UNIVERSAL -> {
                val nextComponent = nextComponent(pathSpec)
                terser.set(nextComponent, "CLIA")
            }
            in CE_FIELDS -> {
                // HD and CE don't have the same format. for the CE field, we have
                // something that sits in the middle between the CLIA and the field
                // that identifies this as a CLIA
                val nextComponent = nextComponent(pathSpec, 2)
                terser.set(nextComponent, "CLIA")
            }
        }
    }

    /**
     * Set the XTN component using [phoneNumberFormatting] to control details
     */
    internal fun setTelephoneComponent(
        terser: Terser,
        value: String,
        pathSpec: String,
        element: Element,
        phoneNumberFormatting: Hl7Configuration.PhoneNumberFormatting
    ) {
        if (value.isEmpty()) return
        val parts = value.split(Element.phoneDelimiter)
        val areaCode = parts[0].substring(0, 3)
        val local = parts[0].substring(3)
        val country = parts[1]
        val extension = parts[2]
        val localWithDash = if (local.length == 7) "${local.slice(0..2)}-${local.slice(3..6)}" else local

        /**
         * Validate phone number and return pair of region (US, CA, MX, AU) and phone number
         * @param [phoneNumber] phone number to validate.
         * @return region (US, CA, MX, AU) and phone number if valid.  Otherwise, return null, null.
         */
        fun parsePhoneNumber(phoneNumber: String): Pair<String?, Phonenumber.PhoneNumber?> {
            val phone = Element.tryParsePhoneNumber(phoneNumber) ?: Phonenumber.PhoneNumber().also {
                it.rawInput = phoneNumber
            }
            // in most cases, the phone number util will get us the correct region code for the phone number
            // but there are going to be cases where we have an invalid phone number and the `getRegionCodeForNumber`
            // function will fail, so I've hardcoded in the saving throw to try and guess the three most likely
            // possibilities
            val regionCode = phoneNumberUtil.getRegionCodeForNumber(phone) ?: when (phone.countryCode) {
                1 -> "US" // and CA and others in North America, but the phone number's invalid, so we're guessing now
                52 -> "MX" // Mexico
                61 -> "AU" // Australia
                else -> null
            }

            return Pair(regionCode, phone)
        }

        fun setComponents(pathSpec: String, component1: String) {
            // Note from the HL7 2.5.1 specification about components 1 and 2:
            // This component has been retained for backward compatibility only as of version 2.3.
            // Definition: Specifies the telephone number in a predetermined format that includes an
            // optional extension, beeper number and comment.
            // Format: [NN] [(999)]999-9999[X99999][B99999][C any text]
            // The optional first two digits are the country code. The optional X portion gives an extension.
            // The optional B portion gives a beeper code.
            // The optional C portion may be used for comments like, After 6:00.

            when (phoneNumberFormatting) {
                Hl7Configuration.PhoneNumberFormatting.STANDARD -> {
                    val phoneNumber = "($areaCode)$localWithDash" +
                        if (extension.isNotEmpty()) "X$extension" else ""
                    terser.set(buildComponent(pathSpec, 1), phoneNumber)
                    terser.set(buildComponent(pathSpec, 2), component1)
                }
                Hl7Configuration.PhoneNumberFormatting.ONLY_DIGITS_IN_COMPONENT_ONE -> {
                    terser.set(buildComponent(pathSpec, 1), "$areaCode$local")
                    terser.set(buildComponent(pathSpec, 2), component1)
                }
                Hl7Configuration.PhoneNumberFormatting.AREA_LOCAL_IN_COMPONENT_ONE -> {
                    // Added for backward compatibility
                    terser.set(buildComponent(pathSpec, 1), "($areaCode)$local")
                    terser.set(buildComponent(pathSpec, 2), component1)
                }
            }
            // it's a phone
            terser.set(buildComponent(pathSpec, 3), "PH")
            terser.set(buildComponent(pathSpec, 5), country)

            // If it is North America phone number with country code = "+1" US & CA, code = +52 MX, and +61 AU
            // then we fill PID-13-6, 7
            val (region, _) = parsePhoneNumber("$country$areaCode$local")
            // there are a lot of potential phone numbers that could start +1, and the google phone number
            // library will return the correct country code based on region AND exchange, which could break
            // this logic if the number is from say a Caribbean island
            if (Element.phoneRegions.contains(region)) {
                terser.set(buildComponent(pathSpec, 6), areaCode)
                terser.set(buildComponent(pathSpec, 7), local)
            } else {
                terser.set(buildComponent(pathSpec, 12), "+$country$areaCode$local")
            }
            if (extension.isNotEmpty()) terser.set(buildComponent(pathSpec, 8), extension)
        }

        if (element.nameContains("patient")) {
            // PID-13 is repeatable, which means we could have more than one phone #
            // or email etc., so we need to increment until we get empty for PID-13-2
            var rep = 0
            while (terser.get("/PATIENT_RESULT/PATIENT/PID-13($rep)-2")?.isEmpty() == false) {
                rep += 1
            }
            // if the first component contains an email value, we want to extract the values, and we want to then
            // put the patient phone number into rep 1 for PID-13. this means that the phone number will always
            // appear first in the list of repeats in PID-13
            if (rep > 0 && terser.get("/PATIENT_RESULT/PATIENT/PID-13(0)-2") == "NET") {
                // get the email back out
                val email = terser.get("/PATIENT_RESULT/PATIENT/PID-13(0)-4")
                // clear out the email value to ensure it's empty for the phone number repeat
                terser.set("/PATIENT_RESULT/PATIENT/PID-13(0)-4", "")
                // overwrite the first repeat
                setComponents("/PATIENT_RESULT/PATIENT/PID-13(0)", "PRN")
                // now write the second repeat
                terser.set("/PATIENT_RESULT/PATIENT/PID-13(1)-2", "NET")
                terser.set("/PATIENT_RESULT/PATIENT/PID-13(1)-3", "Internet")
                terser.set("/PATIENT_RESULT/PATIENT/PID-13(1)-4", email)
            } else {
                setComponents("/PATIENT_RESULT/PATIENT/PID-13($rep)", "PRN")
            }
        } else {
            setComponents(pathSpec, "WPN")
        }
    }

    private fun setEmailComponent(terser: Terser, value: String, element: Element, hl7Config: Hl7Configuration?) {
        // branch on element name. maybe we'll pass through ordering provider email information as well
        if (element.nameContains("patient_email")) {
            // for some state systems, they cannot handle repetition in the PID-13 field, despite what
            // the HL7 specification calls for. In that case, the patient email is not imported. A common
            // workaround is to shove the patient_email into PID-14 which is the business phone
            val truncatedValue = value.trimAndTruncate(XTN_MAX_LENGTHS[3])
            if (hl7Config?.usePid14ForPatientEmail == true) {
                // this is an email address
                terser.set("/PATIENT_RESULT/PATIENT/PID-14-2", "NET")
                // specifies it's an internet telecommunications type
                terser.set("/PATIENT_RESULT/PATIENT/PID-14-3", "Internet")
                terser.set("/PATIENT_RESULT/PATIENT/PID-14-4", truncatedValue)
            } else {
                // PID-13 is repeatable, which means we could have more than one phone #
                // or email etc, so we need to increment until we get empty for PID-13-2
                var rep = 0
                while (terser.get("/PATIENT_RESULT/PATIENT/PID-13($rep)-2")?.isEmpty() == false) {
                    rep += 1
                }
                // this is an email address
                terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-2", "NET")
                // specifies it's an internet telecommunications type
                terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-3", "Internet")
                terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-4", truncatedValue)
            }
        }
    }

    private fun setPostalComponent(terser: Terser, value: String, pathSpec: String, element: Element) {
        val zipFive = element.toFormatted(value, Element.zipFiveToken)
        terser.set(pathSpec, zipFive)
    }

    private fun setAOE(
        terser: Terser,
        elementOrg: Element,
        aoeRep: Int,
        date: String,
        value: String,
        report: Report,
        row: Int,
        units: String? = null,
        suppressQst: Boolean = false
    ) {
        val hl7Config = report.destination?.translation as? Hl7Configuration
        // if the value is UNK then we need to set data type to CODE and valueset = hl70136 (UNK)
        val element = when {
            value == "UNK" && elementOrg.name == "pregnant" ->
                elementOrg.copy(type = Element.Type.CODE, valueSet = "covid-19/pregnant_aoe")
            value == "UNK" -> elementOrg.copy(type = Element.Type.CODE, valueSet = "hl70136")
            else -> elementOrg
        }
        // if the value type is a date, we need to specify that for the AOE questions
        val valueType = when (element.type) {
            Element.Type.DATE -> "DT"
            Element.Type.NUMBER -> "NM"
            Element.Type.CODE -> "CWE"
            else -> "ST"
        }
        terser.set(formPathSpec("OBX-1", aoeRep), (aoeRep + 1).toString())
        terser.set(formPathSpec("OBX-2", aoeRep), valueType)
        val aoeQuestion = element.hl7AOEQuestion
            ?: error("Schema Error: missing hl7AOEQuestion for '${element.name}'")
        setCodeComponent(terser, aoeQuestion, formPathSpec("OBX-3", aoeRep), "covid-19/aoe")

        when (element.type) {
            Element.Type.CODE -> if (value == "UNK" && elementOrg.name == "pregnant") {
                // 261665006 is unknown code valueSet
                setCodeComponent(terser, "261665006", formPathSpec("OBX-5", aoeRep), element.valueSet)
            } else {
                setCodeComponent(terser, value, formPathSpec("OBX-5", aoeRep), element.valueSet)
            }
            Element.Type.NUMBER -> {
                if (element.name != "patient_age") TODO("support other types of AOE numbers")
                if (units == null) error("Schema Error: expected age units")
                setComponent(terser, element, "OBX-5", aoeRep, value, report)
                setCodeComponent(terser, units, formPathSpec("OBX-6", aoeRep), "patient_age_units")
            }
            else -> setComponent(terser, element, "OBX-5", aoeRep, value, report)
        }
        // convert to local date time if that's what the receiver wants
        val rawObx19Value = report.getString(row, "test_result_date").trimToNull()
        val obx19Value = if (rawObx19Value != null && rawObx19Value.uppercase() != "UNK") {
            DateUtilities.parseDate(rawObx19Value).formatDateTimeForReceiver(report)
        } else {
            rawObx19Value
        }
        terser.set(formPathSpec("OBX-11", aoeRep), report.getString(row, "observation_result_status"))
        terser.set(formPathSpec("OBX-14", aoeRep), date)
        // some states want the observation date for the AOE questions as well
        terser.set(formPathSpec("OBX-19", aoeRep), obx19Value)
        terser.set(formPathSpec("OBX-23-7", aoeRep), "XX")
        // many states can't accept the QST datapoint out at the end because it is nonstandard
        // we need to pass this in via the translation configuration
        if (!suppressQst) terser.set(formPathSpec("OBX-29", aoeRep), "QST")
        // all of these values must be set on the OBX AOE's for validation
        terser.set(
            formPathSpec("OBX-23-1", aoeRep),
            trimAndTruncateValue(
                report.getStringByHl7Field(row, "OBX-23-1") as String,
                "OBX-23-1",
                hl7Config,
                terser
            )
        )
        // set to a default value, but look below
        // terser.set(formPathSpec("OBX-23-6", aoeRep), report.getStringByHl7Field(row, "OBX-23-6"))
        terser.set(formPathSpec("OBX-23-10", aoeRep), report.getString(row, "testing_lab_clia"))
        terser.set(formPathSpec("OBX-15", aoeRep), report.getString(row, "testing_lab_clia"))
        terser.set(
            formPathSpec("OBX-24-1", aoeRep),
            trimAndTruncateValue(
                report.getStringByHl7Field(row, "OBX-24-1") as String,
                "OBX-24-1",
                hl7Config,
                terser
            )
        )
        terser.set(
            formPathSpec("OBX-24-2", aoeRep),
            trimAndTruncateValue(
                report.getStringByHl7Field(row, "OBX-24-2") as String,
                "OBX-24-2",
                hl7Config,
                terser
            )
        )
        terser.set(
            formPathSpec("OBX-24-3", aoeRep),
            trimAndTruncateValue(
                report.getStringByHl7Field(row, "OBX-24-3") as String,
                "OBX-24-3",
                hl7Config,
                terser
            )
        )
        terser.set(
            formPathSpec("OBX-24-4", aoeRep),
            trimAndTruncateValue(
                report.getStringByHl7Field(row, "OBX-24-4") as String,
                "OBX-24-4",
                hl7Config,
                terser
            )
        )
        // OBX-24-5 is a postal code as well. pad this for now
        // TODO: come up with a better way to repeat these segments
        terser.set(
            formPathSpec("OBX-24-5", aoeRep),
            report.getStringByHl7Field(row, "OBX-24-5")?.padStart(5, '0')
        )
        terser.set(formPathSpec("OBX-24-9", aoeRep), report.getStringByHl7Field(row, "OBX-24-9"))
        // check for the OBX-23-6 value. it needs to be split apart
        val testingLabIdAssigner = report.getString(row, "testing_lab_id_assigner")
        if (testingLabIdAssigner?.contains(DEFAULT_COMPONENT_SEPARATOR) == true) {
            val testingLabIdAssignerParts = testingLabIdAssigner.split(DEFAULT_COMPONENT_SEPARATOR)
            testingLabIdAssignerParts.forEachIndexed { index, s ->
                terser.set(formPathSpec("OBX-23-6-${index + 1}", aoeRep), s)
            }
        }
    }

    private fun setNote(terser: Terser, nteRep: Int, value: String) {
        if (value.isBlank()) return
        terser.set(formPathSpec("NTE-1", nteRep), nteRep.plus(1).toString())
        terser.set(formPathSpec("NTE-3", nteRep), value)
        terser.set(formPathSpec("NTE-4-1", nteRep), "RE")
        terser.set(formPathSpec("NTE-4-2", nteRep), "Remark")
        terser.set(formPathSpec("NTE-4-3", nteRep), "HL70364")
        terser.set(formPathSpec("NTE-4-7", nteRep), HL7_SPEC_VERSION)
    }

    /** set literal values in the HL7 */
    private fun setLiterals(terser: Terser, report: Report) {
        // Value that NIST requires (although # is not part of 2.5.1)
        terser.set("MSH-12", HL7_SPEC_VERSION)
        // todo: we will need to update this when we start accepting ACKs
        terser.set("MSH-15", "NE")
        terser.set("MSH-16", "NE")
        terser.set("MSH-17", "USA")
        // todo: update this in case we convert a message to ASCII
        terser.set("MSH-18", "UNICODE UTF-8")
        // our primary message language is English
        terser.set("MSH-19-1", "ENG")
        terser.set("MSH-19-2", "English")
        terser.set("MSH-19-3", "ISO")
        terser.set("MSH-20", "")
        terser.set("SFT-1", SOFTWARE_VENDOR_ORGANIZATION)
        terser.set("SFT-2", buildVersion)
        terser.set("SFT-3", SOFTWARE_PRODUCT_NAME)
        terser.set("SFT-4", buildVersion)
        // convert to local date time if the receiver wants it so. this makes it no longer
        // a literal, but it will live here for now
        terser.set("SFT-6", DateUtilities.parseDate(buildDate).formatDateTimeForReceiver(report))
        terser.set("/PATIENT_RESULT/PATIENT/PID-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-1", "RE")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBR-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-2", "CWE")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-23-7", "XX")
    }

    /**
     * Get a new truncation limit accounting for the encoding of HL7 special characters.
     * @param value string value to search for HL7 special characters
     * @param truncationLimit the starting limit
     * @return the new truncation limit or starting limit if no special characters are found
     */
    internal fun getTruncationLimitWithEncoding(value: String, truncationLimit: Int?): Int? {
        return if (truncationLimit != null) {
            val regex = "[&^~|]".toRegex()
            val endIndex = min(value.length, truncationLimit)
            val matchCount = regex.findAll(value.substring(0, endIndex)).count()

            if (matchCount > 0) {
                truncationLimit.minus(matchCount.times(2))
            } else {
                truncationLimit
            }
        } else {
            truncationLimit
        }
    }

    /**
     * Trim and truncate the [value] according to the rules in [hl7Config] for [hl7Field].
     * [terser] provides hl7 standards
     */
    internal fun trimAndTruncateValue(
        value: String,
        hl7Field: String,
        hl7Config: Hl7Configuration?,
        terser: Terser
    ): String {
        val maxLength = getMaxLength(hl7Field, value, hl7Config, terser)
        return value.trimAndTruncate(maxLength)
    }

    /**
     * Calculate for [hl7Field] and [value] the length to truncate the value according to the
     * truncation rules in [hl7Config]. The [terser] is used to determine the HL7 specification length.
     */
    internal fun getMaxLength(hl7Field: String, value: String, hl7Config: Hl7Configuration?, terser: Terser): Int? {
        // get the fields to truncate
        val hl7TruncationFields = hl7Config
            ?.truncateHl7Fields
            ?.uppercase()
            ?.split(",")
            ?.map { it.trim() }
            ?: emptyList()

        // The & character in HL7 is a sub sub field separator. A validly
        // produced HL7 message should escape & characters as \T\ so that
        // the HL7 parser doesn't interpret these as sub sub field separators.
        // Because of this reason, all string values should go through the getTruncationLimitWithEncoding
        // so that string values that contain sub sub field separators (^&~) will be properly truncated.
        return when {
            // This special case takes into account special rules needed by jurisdiction
            hl7Config?.truncateHDNamespaceIds == true && hl7Field in HD_FIELDS_LOCAL -> {
                getTruncationLimitWithEncoding(value, HD_TRUNCATION_LIMIT)
            }
            // For the fields listed here use the hl7 max length
            hl7Field in hl7TruncationFields -> {
                getTruncationLimitWithEncoding(value, getHl7MaxLength(hl7Field, terser))
            }
            // In general, don't truncate. The thinking is that
            // 1. the max length of the specification is "normative" not system specific.
            // 2. ReportStream is a conduit and truncation is a loss of information
            // 3. Much of the current HHS guidance implies lengths longer than the 2.5.1 minimums
            // 4. Later hl7 specifications, relax the minimum length requirements
            else -> null
        }
    }

    /**
     * Given the internal field or component specified in [hl7Field], return the maximum string length
     * according to the HL7 specification. The [terser] provides the HL7 specifications
     */
    internal fun getHl7MaxLength(hl7Field: String, terser: Terser): Int? {
        fun getMaxLengthForCompositeType(type: Type, component: Int): Int? {
            val typeName = type.name
            val table = HL7_COMPONENT_MAX_LENGTH[typeName] ?: return null
            return if (component <= table.size) table[component - 1] else null
        }

        // Dev Note: this function is work in progress.
        // It is meant to be a general function for all fields and components,
        // but only has support for the cases of current COVID-19 schema.
        val segmentName = hl7Field.substring(0, 3)
        val segmentSpec = formSegSpec(segmentName)
        val segment = terser.getSegment(segmentSpec)
        val parts = hl7Field.substring(4).split("-").map { it.toInt() }
        val field = segment.getField(parts[0], 0)
        return when (parts.size) {
            // In general, use the values found in the HAPI library for fields
            1 -> segment.getLength(parts[0])
            // use our max-length tables when field and component is specified
            2 -> getMaxLengthForCompositeType(field, parts[1])
            // Add cases for sub-components here
            else -> null
        }
    }

    /**
     * Creates the headers for hl7 batch. Generally the [sendingApplicationReportIn], [receivingApplicationReportIn], and
     * [receivingFacilityReportIn] will come from the first item in the file. In the case of empty batch, it
     * must be passed in.
     */
    private fun createHeaders(
        report: Report,
        sendingApplicationReportIn: String? = null,
        receivingApplicationReportIn: String? = null,
        receivingFacilityReportIn: String? = null
    ): String {
        val sendingApplicationReport = sendingApplicationReportIn
            ?: (report.getString(0, "sending_application") ?: "")
        val receivingApplicationReport = receivingApplicationReportIn
            ?: (report.getString(0, "receiving_application") ?: "")
        val receivingFacilityReport = receivingFacilityReportIn
            ?: (report.getString(0, "receiving_facility") ?: "")

        var sendingAppTruncationLimit: Int? = null
        var receivingAppTruncationLimit: Int? = null
        var receivingFacilityTruncationLimit: Int? = null

        val hl7Config = report.destination?.translation as? Hl7Configuration?
        if (hl7Config?.truncateHDNamespaceIds == true) {
            sendingAppTruncationLimit = getTruncationLimitWithEncoding(sendingApplicationReport, HD_TRUNCATION_LIMIT)
            receivingAppTruncationLimit = getTruncationLimitWithEncoding(
                receivingApplicationReport,
                HD_TRUNCATION_LIMIT
            )
            receivingFacilityTruncationLimit = getTruncationLimitWithEncoding(
                receivingFacilityReport,
                HD_TRUNCATION_LIMIT
            )
        }

        val encodingCharacters = "^~\\&"
        val sendingApp = formatHD(
            Element.parseHD(sendingApplicationReport, sendingAppTruncationLimit)
        )
        val sendingFacility = formatHD(
            Element.parseHD(sendingApplicationReport, sendingAppTruncationLimit)
        )
        val receivingApp = formatHD(
            Element.parseHD(receivingApplicationReport, receivingAppTruncationLimit)
        )
        val receivingFacility = formatHD(
            Element.parseHD(receivingFacilityReport, receivingFacilityTruncationLimit)
        )

        return "FHS|$encodingCharacters|" +
            "$sendingApp|" +
            "$sendingFacility|" +
            "$receivingApp|" +
            "$receivingFacility|" +
            getNowTimestamp(report) +
            hl7SegmentDelimiter +
            "BHS|$encodingCharacters|" +
            "$sendingApp|" +
            "$sendingFacility|" +
            "$receivingApp|" +
            "$receivingFacility|" +
            getNowTimestamp(report) +
            hl7SegmentDelimiter
    }

    /**
     * helper method that gets the now timestamp for the report for the header segments
     */
    private fun getNowTimestamp(report: Report): String {
        val hl7Config = report.destination?.translation as? Hl7Configuration
        return DateUtilities.nowTimestamp(
            report.getTimeZoneForReport(),
            report.destination?.dateTimeFormat,
            hl7Config?.convertPositiveDateTimeOffsetToNegative,
            hl7Config?.useHighPrecisionHeaderDateTimeFormat
        )
    }

    /**
     * Creates the footers for the report
     */
    private fun createFooters(report: Report): String {
        return "BTS|${report.itemCount}$hl7SegmentDelimiter" +
            "FTS|1$hl7SegmentDelimiter"
    }

    private fun buildComponent(spec: String, component: Int = 1): String {
        if (!isField(spec)) error("Not a component path spec")
        return "$spec-$component"
    }

    private fun isField(spec: String): Boolean {
        // Support the SEG-# and the SEG-#(#) repeat pattern
        val pattern = Regex("[A-Z][A-Z][A-Z]-[0-9]+(?:\\([0-9]+\\))?$")
        return pattern.containsMatchIn(spec)
    }

    private fun nextComponent(spec: String, increment: Int = 1): String {
        val componentPattern = Regex("[A-Z][A-Z][A-Z]-[0-9]+-([0-9]+)$")
        componentPattern.find(spec)?.groups?.get(1)?.let {
            val nextComponent = it.value.toInt() + increment
            return spec.replaceRange(it.range, nextComponent.toString())
        }
        val subComponentPattern = Regex("[A-Z][A-Z][A-Z]-[0-9]+-[0-9]+-([0-9]+)$")
        subComponentPattern.find(spec)?.groups?.get(1)?.let {
            val nextComponent = it.value.toInt() + increment
            return spec.replaceRange(it.range, nextComponent.toString())
        }
        error("Did match on component or subcomponent")
    }

    internal fun formPathSpec(spec: String, rep: Int? = null): String {
        val segment = spec.substring(0, 3)
        val components = spec.substring(3)
        val segmentSpec = formSegSpec(segment, rep)
        return "$segmentSpec$components"
    }

    internal fun formSegSpec(segment: String, rep: Int? = null): String {
        val repSpec = rep?.let { "($rep)" } ?: ""
        return when (segment) {
            "OBR" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBR"
            "ORC" -> "/PATIENT_RESULT/ORDER_OBSERVATION/ORC"
            "SPM" -> "/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM"
            "PID" -> "/PATIENT_RESULT/PATIENT/PID"
            "OBX" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION$repSpec/OBX"
            "NTE" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/NTE$repSpec"
            else -> segment
        }
    }

    private fun formatHD(hdFields: Element.HDFields, separator: String = DEFAULT_COMPONENT_SEPARATOR): String {
        return if (hdFields.universalId != null && hdFields.universalIdSystem != null) {
            "${hdFields.name}$separator${hdFields.universalId}$separator${hdFields.universalIdSystem}"
        } else {
            hdFields.name
        }
    }

    /**
     * Coverts unicode string to ASCII string if any special characters are found.
     * This function takes a string parameter of unicode characters, checks to see if it has unicode special characters
     * (À,Á,Â,Ã,Ä,Å,È,É,Ê,Ë,Î,Ô,Ù,Ç, and many more), converts it to a string representation of ASCII characters if any
     * unicode special characters are found. AnyAscii is the open library that is used to perform this conversion.
     * @param message the string to convert to ASCII string representation
     * @return same string if no special characters are found or converted ASCII string if any special chars are found.
     * @link https://github.com/anyascii/anyascii
     */
    internal fun unicodeToAscii(
        message: String
    ): String {
        return AnyAscii.transliterate(message)
    }

    private fun formatEI(eiFields: Element.EIFields, separator: String = DEFAULT_COMPONENT_SEPARATOR): String {
        return if (eiFields.namespace != null && eiFields.universalId != null && eiFields.universalIdSystem != null) {
            "${eiFields.name}$separator${eiFields.namespace}" +
                "$separator${eiFields.universalId}$separator${eiFields.universalIdSystem}"
        } else {
            eiFields.name
        }
    }

    /**
     * Get a phone number from an XTN (e.g. phone number) field of an HL7 message.
     * @param terser the HL7 terser
     * @param element the element to decode
     * @param hl7Field the HL7 field name
     * @return the phone number or empty string
     */
    internal fun decodeHl7TelecomData(terser: Terser, element: Element, hl7Field: String): String {
        /**
         * Extract a phone number from a value [xtnValue] of an XTN HL7 field.
         * @return a normalized phone number or empty if no phone number was found
         */
        fun getTelecomValue(xtnValue: Type): String {
            var strValue = ""
            if (xtnValue is XTN) {
                when (element.type) {
                    Element.Type.TELEPHONE -> {
                        // If we have an area code or local number use the new fields, otherwise try the deprecated field
                        if (!xtnValue.areaCityCode.isEmpty || !xtnValue.localNumber.isEmpty) {
                            // If the phone number type is specified then make sure it is a phone, otherwise assume it is.
                            if (xtnValue.telecommunicationEquipmentType.isEmpty ||
                                xtnValue.telecommunicationEquipmentType.valueOrEmpty == "PH"
                            ) {
                                strValue = "${xtnValue.areaCityCode.value ?: ""}${xtnValue.localNumber.value ?: ""}:" +
                                    "${xtnValue.countryCode.value ?: ""}:${xtnValue.extension.value ?: ""}"
                            }
                        } else if (!xtnValue.telephoneNumber.isEmpty) {
                            strValue = element.toNormalized(xtnValue.telephoneNumber.valueOrEmpty)
                        }
                    }
                    Element.Type.EMAIL -> {
                        if (xtnValue.telecommunicationEquipmentType.isEmpty ||
                            xtnValue.telecommunicationEquipmentType.valueOrEmpty == "Internet"
                        ) {
                            strValue = element.toNormalized(xtnValue.emailAddress.valueOrEmpty)
                        }
                    }
                    else -> error("${element.type} is unsupported to decode telecom data.")
                }
            }
            return strValue
        }

        var telecomValue = ""

        // Get the field values by going through the terser segment.  This method gives us an
        // array with a maximum number of repetitions, but it may return multiple array elements even if
        // there is no data
        val fieldParts = getTerserSpec(hl7Field).split("-")
        if (fieldParts.size > 1) {
            val segment = terser.getSegment(fieldParts[0])
            val fieldNumber = fieldParts[1].toIntOrNull()
            if (segment != null && fieldNumber != null) {
                segment.getField(fieldNumber)?.forEach {
                    // The first phone number wins
                    if (telecomValue.isBlank()) {
                        telecomValue = getTelecomValue(it)
                    }
                }
            }
        }

        return telecomValue
    }

    /**
     * Get a date time from a TS date time field of an HL7 message.
     * @param terser the HL7 terser
     * @param element the element to decode
     * @param warnings the list of warnings
     * @return the date time or empty string
     */
    internal fun decodeHl7DateTime(
        terser: Terser,
        element: Element,
        hl7Field: String,
        warnings: MutableList<ActionLogDetail>
    ): String {
        var valueString = ""
        val fieldParts = getTerserSpec(hl7Field).split("-")
        if (fieldParts.size > 1) {
            val segment = terser.getSegment(fieldParts[0])
            val fieldNumber = fieldParts[1].toIntOrNull()
            if (segment != null && fieldNumber != null) {
                var dtm: Instant? = null
                var rawValue = ""
                when (val value = segment.getField(fieldNumber, 0)) {
                    // Timestamp
                    is TS -> {
                        // If the offset was not specified then set the timezone to UTC instead of the system default
                        // -99 is the value returned from HAPI when no offset is specified
                        if (value.time?.gmtOffset == -99) {
                            val cal = value.time?.valueAsCalendar
                            cal?.let { it.timeZone = TimeZone.getTimeZone("GMT") }
                            dtm = cal?.toInstant()
                        } else dtm = value.time?.valueAsDate?.toInstant()
                        rawValue = value.toString()
                    }
                    // Date range. For getting a date time, use the start of the range
                    is DR -> {
                        if (value.rangeStartDateTime?.time?.gmtOffset == -99) {
                            val cal = value.rangeStartDateTime?.time?.valueAsCalendar
                            cal?.let { it.timeZone = TimeZone.getTimeZone("GMT") }
                            dtm = cal?.toInstant()
                        } else dtm = value.rangeStartDateTime?.time?.valueAsDate?.toInstant()
                        rawValue = value.toString()
                    }
                    is DT -> {
                        dtm = LocalDate.of(value.year, value.month, value.day)
                            .atStartOfDay(ZoneId.systemDefault()).toInstant()
                        rawValue = value.toString()
                    }
                }

                dtm?.let {
                    // Now check to see if we have all the precision we want
                    when (element.type) {
                        Element.Type.DATETIME -> {
                            valueString = DateTimeFormatter.ofPattern(DateUtilities.datetimePattern)
                                .format(OffsetDateTime.ofInstant(dtm, ZoneId.of("Z")))
                            val r = Regex("^[A-Z]+\\[[0-9]{12,}\\.?[0-9]{0,4}[+-][0-9]{4}]\$")
                            if (!r.matches(rawValue)) {
                                warnings.add(
                                    FieldPrecisionMessage(
                                        element.fieldMapping,
                                        "Timestamp for ${element.name} should be precise. Reformat " +
                                            "to either the HL7 v2.4 TS or ISO 8601 standard format."
                                    )
                                )
                            }
                        }
                        Element.Type.DATE -> {
                            valueString = DateTimeFormatter.ofPattern(DateUtilities.datePattern)
                                .format(OffsetDateTime.ofInstant(dtm, ZoneId.of("Z")))
                            // Note that some schema fields of type date could be derived from HL7 date time fields
                            val r = Regex("^[A-Z]+\\[[0-9]{8,}.*")
                            if (!r.matches(rawValue)) {
                                warnings.add(
                                    FieldPrecisionMessage(
                                        element.fieldMapping,
                                        "Date for ${element.name} should provide more " +
                                            "precision. Reformat as YYYYMMDD."
                                    )
                                )
                            }
                        }
                        else -> throw IllegalStateException("${element.type} not supported by decodeHl7DateTime")
                    }
                }
            }
        }
        return valueString
    }

    /**
     * Organize the order of the Observation segments ensurign that the first iteration contains the test result
     * @param message the HAPI message
     * @return a hapi message with the observations ordered
     */
    fun organizeObservationOrder(
        message: Message
    ): Message {
        val oruR01: ORU_R01 = (message as? ORU_R01) ?: return message

        // get OBX-3 from first OBX segment
        val loincRepOne = oruR01
            .patienT_RESULT.ordeR_OBSERVATION.observation.obx.observationIdentifier.identifier?.toString() ?: ""

        // if first OBX segment contains the test result then just return the message
        if (!checkLIVDValueExists("Test Performed LOINC Code", loincRepOne)) {
            var resultObservation: ORU_R01_OBSERVATION? = null
            // loop through the observations and check each for the test result
            oruR01.patienT_RESULT.ordeR_OBSERVATION.observationAll.forEachIndexed { index, observation ->
                val loinc = observation.obx.observationIdentifier.identifier.toString()
                // search the LOINC code against the LIVD table
                if (checkLIVDValueExists("Test Performed LOINC Code", loinc)) {
                    resultObservation = observation
                    // remove the observation group including the OBX and any NTE segments
                    oruR01.patienT_RESULT.ordeR_OBSERVATION.removeOBSERVATION(index)
                    // insert the observation group as the first iteration
                    oruR01.patienT_RESULT.ordeR_OBSERVATION.insertOBSERVATION(resultObservation, 0)
                    return@forEachIndexed
                }
            }

            // if an OBX is found with the test result then reset the OBX set IDs sequentially
            if (resultObservation != null) {
                oruR01.patienT_RESULT.ordeR_OBSERVATION.observationAll.forEachIndexed { index, observation ->
                    observation.obx.setIDOBX.value = index.plus(1).toString()
                }
            }

            return oruR01
        } else {
            return message
        }
    }

    /**
     * Positive checks that a value is present in a column in the LIVD table
     * @param column is the search column
     * @param value is the value to search for
     * @return a bool indicating is 1 or more rows were identified after filtering on params
     */
    fun checkLIVDValueExists(column: String, value: String): Boolean {
        return if (livdLookupTable.value.hasColumn(column)) {
            val rowCount = livdLookupTable.value.FilterBuilder().equalsIgnoreCase(column, value).filter().rowCount
            rowCount > 0
        } else {
            false
        }
    }

    /**
     * Gets the HAPI Terser spec from the provided [hl7Field] string.
     * @returns the HAPI Terser spec
     */
    internal fun getTerserSpec(hl7Field: String): String {
        return if (hl7Field.isNotBlank() && hl7Field.startsWith("MSH")) {
            "/$hl7Field"
        } else {
            "/.$hl7Field"
        }
    }

    companion object {
        /** the length to truncate HD values to. Defaults to 20 */
        const val HD_TRUNCATION_LIMIT = 20
        const val HL7_SPEC_VERSION: String = "2.5.1"
        const val MESSAGE_CODE = "ORU"
        const val MESSAGE_TRIGGER_EVENT = "R01"
        const val SOFTWARE_VENDOR_ORGANIZATION: String = "Centers for Disease Control and Prevention"
        const val SOFTWARE_PRODUCT_NAME: String = "PRIME ReportStream"
        const val NCES_EXTENSION = "_NCES_"
        const val OBX_18_EQUIPMENT_UID_OID: String = "2.16.840.1.113883.3.3719"

        /** the default org name type code. defaults to "L" */
        const val DEFAULT_ORGANIZATION_NAME_TYPE_CODE: String = "L"
        const val DEFAULT_COMPONENT_SEPARATOR = "^"
        const val DEFAULT_REPETITION_SEPARATOR = "~"
        const val DEFAULT_ESCAPE_CHARACTER = "\\"
        const val DEFAULT_SUBCOMPONENT_SEPARATOR = "&"

        val phoneNumberUtil: PhoneNumberUtil = PhoneNumberUtil.getInstance()

        /*
        From the HL7 2.5.1 Ch 2A spec...

        The Hierarchical Designator identifies an entity that has responsibility for managing or
        assigning a defined set of instance identifiers.

        The HD is designed to be used either as a local identifier (with only the <namespace ID> valued)
        or a publicly-assigned identifier, a UID (<universal ID> and <universal ID type> both valued)
         */

        /**
         * List of fields that have the local HD type.
         */
        val HD_FIELDS_LOCAL = listOf(
            "MSH-3-1", "MSH-4-1", "OBR-3-2", "OBR-2-2", "ORC-3-2", "ORC-2-2", "ORC-4-2",
            "PID-3-4-1", "PID-3-6-1", "SPM-2-1-2", "SPM-2-2-2"
        )

        /**
         * List of fields that have the universal HD type
         */
        val HD_FIELDS_UNIVERSAL = listOf(
            "MSH-3-2", "MSH-4-2", "OBR-3-3", "OBR-2-3", "ORC-3-3", "ORC-2-3", "ORC-4-3",
            "PID-3-4-2", "PID-3-6-2", "SPM-2-1-3", "SPM-2-2-3"
        )

        /**
         * List of fields that have a CE type. Note: this is only really used in places
         * where we need to put a CLIA marker in the field as well and there are a
         * lot of CE fields that are *NOT* CLIA fields, so use this correctly.
         */
        val CE_FIELDS = listOf("OBX-15-1")

        /** The max length for the formatted text type (FT) in HL7 */
        private const val maxFormattedTextLength = 65536

        // Component specific sub-component length from HL7 specification Chapter 2A
        private val CE_MAX_LENGTHS = arrayOf(20, 199, 20, 20, 199, 20)
        private val CWE_MAX_LENGTHS = arrayOf(20, 199, 20, 20, 199, 20, 10, 10, 199)
        private val CX_MAX_LENGTHS = arrayOf(15, 1, 3, 227, 5, 227, 5, 227, 8, 8, 705, 705)
        private val EI_MAX_LENGTHS = arrayOf(199, 20, 199, 6)
        private val EIP_MAX_LENGTHS = arrayOf(427, 427)
        private val HD_MAX_LENGTHS = arrayOf(20, 199, 6)
        private val XTN_MAX_LENGTHS = arrayOf(199, 3, 8, 199, 3, 5, 9, 5, 199, 4, 6, 199)
        private val XAD_MAX_LENGTHS = arrayOf(184, 120, 50, 50, 12, 3, 3, 50, 20, 20, 1, 53, 26, 26)
        private val XCN_MAX_LENGTHS =
            arrayOf(15, 194, 30, 30, 20, 20, 5, 4, 227, 1, 1, 3, 5, 227, 1, 483, 53, 1, 26, 26, 199, 705, 705)
        private val XON_MAX_LENGTHS = arrayOf(50, 20, 4, 1, 3, 227, 5, 227, 1, 20)
        private val XPN_MAX_LENGTHS = arrayOf(194, 30, 30, 20, 20, 6, 1, 1, 483, 53, 1, 26, 26, 199)

        /**
         * Component length table for composite HL7 types taken from HL7 specification Chapter 2A.
         */
        val HL7_COMPONENT_MAX_LENGTH = mapOf(
            "CE" to CE_MAX_LENGTHS,
            "CWE" to CWE_MAX_LENGTHS,
            "CX" to CX_MAX_LENGTHS,
            "EI" to EI_MAX_LENGTHS,
            "EIP" to EIP_MAX_LENGTHS,
            "HD" to HD_MAX_LENGTHS,
            "XAD" to XAD_MAX_LENGTHS,
            "XCN" to XCN_MAX_LENGTHS,
            "XON" to XON_MAX_LENGTHS,
            "XPN" to XPN_MAX_LENGTHS,
            "XTN" to XTN_MAX_LENGTHS
            // Extend further here
        )

        /**
         * List of ordering provider id fields
         */
        val ORDERING_PROVIDER_ID_FIELDS = listOf("ORC-12", "OBR-16")

        // Do a lazy init because this table may never be used, and it is large
        val ncesLookupTable = lazy {
            Metadata.getInstance().findLookupTable("nces_id") ?: error("Unable to find the NCES ID lookup table.")
        }

        /**
         * Lazy init of LIVD lookup table because this table may never be used
         */
        val livdLookupTable = lazy {
            Metadata.getInstance().findLookupTable("LIVD-SARS-CoV-2") ?: error(
                "Unable to find the LIVD-SARS-CoV-2 lookup table."
            )
        }

        /**
         * Walks all the NTE segments and concatenates the values into a single NTE
         * segment we will write out later on when we deserialize. This is very hackish
         * and should not be considered a good or permanent solution
         */
        fun decodeNTESegments(message: Message): String {
            // cast the message to an ORU_R01, and if it's not that type of
            // message, just return an empty string
            val oruR01: ORU_R01 = (message as? ORU_R01) ?: return ""
            val sb = StringBuilder()

            // walk all the patient results, and the notes that are listed in there, appending to the stringbuilder
            oruR01.patienT_RESULTAll.forEach { patientResult ->
                patientResult.patient.nteAll.forEach { patientNote ->
                    sb.append(patientNote.comment.joinToString(" ") { it.value })
                    sb.append(" ")
                }
                // inside each patient record is the order observation, which also has notes
                patientResult.ordeR_OBSERVATIONAll.forEach { orderObservation ->
                    orderObservation.nteAll.forEach { orderObservationNote ->
                        sb.append(orderObservationNote.comment.joinToString(" ") { it.value })
                        sb.append(" ")
                    }
                    // and each observation also has its own notes
                    orderObservation.observationAll.forEach { observation ->
                        observation.nteAll.forEach { observationNote ->
                            sb.append(observationNote.comment.joinToString(" ") { it.value })
                            sb.append(" ") // trailing space is fine, we trim it below
                        }
                    }
                }
            }

            return sb.toString().trimAndTruncate(maxFormattedTextLength)
        }

        /**
         * decodeObxIdentifierValue looks at the OBX object's observation value, and
         * attempts to parse it. This can be one of many types, so we need to look
         * for the datatype and extract the value.
         * @param observationValue The OBX segment's observation value we're looking at
         */
        fun decodeObxIdentifierValue(observationValue: Varies): String {
            // the return value for `getObservationValue` is of type Varies, which means it could have
            // any one of a bunch of datatypes: CE, CWE, NM, etc.
            // is this a date?
            (observationValue.data as? DT).also { data ->
                if (data != null) {
                    return data.value
                }
            }
            // maybe is a number
            (observationValue.data as? NM).also { data ->
                if (data != null) {
                    return data.value
                }
            }
            // SN is a structured number, which is a way in HL7 to represent a range,
            // for example ">^100^" means greater than 100, and "^100^-^200^" is a range of 100 to 200
            (observationValue.data as? SN).also { data ->
                if (data != null) {
                    return data.num1.toString()
                }
            }
            // coded with exceptions value. most AOEs will be this
            (observationValue.data as? (CWE)).also { data ->
                if (data != null) {
                    return data.cwe1_Identifier.value
                }
            }
            // or a regular coded element
            (observationValue.data as? (CE)).also { data ->
                if (data != null) {
                    return data.ce1_Identifier.value
                }
            }
            // getting this far, we need to try and manually parse our the value
            // is a "Varies" which means HAPI encodes it to string as something like
            // "Varies[N^No^HL70136^^^^Vunknown]" which means we need to extract the
            // nugget of the data inside the brackets
            var aoeValue = observationValue.toString()
            if (aoeValue.startsWith("Varies[") && aoeValue.endsWith("]")) {
                aoeValue = aoeValue.substring(7)
                aoeValue = aoeValue.trimEnd(']')
            }
            // now we check for the component separator, which is typically "^"
            if (aoeValue.contains(DEFAULT_COMPONENT_SEPARATOR)) {
                val splitAoeValues = aoeValue.split(DEFAULT_COMPONENT_SEPARATOR)
                return splitAoeValues[0]
            }
            // well, I don't know what we have, so this is a save throw and just
            // returning the whole chunk
            return aoeValue
        }

        /**
         * Decode answers to AOE questions
         * @param element the element for the AOE question
         * @param message the HAPI message
         * @param repetitionIndex the index of the value to pull for the AOE. They can repeat
         * @return the value from the HL7 message or an empty string if no value found
         */
        fun decodeAOEQuestion(
            element: Element,
            message: Message,
            repetitionIndex: Int = 0
        ): String {
            // cast the message to an ORU_R01, and if it's not that type of
            // message, just return an empty string
            val oruR01: ORU_R01 = (message as? ORU_R01) ?: return ""
            // if the question value is null, then return
            val question = element.hl7AOEQuestion?.uppercase() ?: return ""
            // loop through the elements in the message
            oruR01.patienT_RESULTAll.forEach { patientResult ->
                // walk through the order observations, where AOEs live
                patientResult.ordeR_OBSERVATIONAll.forEach { orderObservation ->
                    // check each observation first. the AOE could be here
                    orderObservation.observationAll.forEach { observation ->
                        if (observation.obx.observationIdentifier.identifier.value?.uppercase() == question) {
                            return decodeObxIdentifierValue(observation.obx.getObservationValue(repetitionIndex))
                        }
                    }
                    // if we made it this far then it is probably under the specimen
                    orderObservation.specimenAll.forEach { specimen ->
                        specimen.obxAll.forEach { specimenObservation ->
                            if (specimenObservation.observationIdentifier.identifier.value?.uppercase() == question) {
                                return decodeObxIdentifierValue(
                                    specimenObservation.getObservationValue(repetitionIndex)
                                )
                            }
                        }
                    }
                }
            }
            // return a default value
            return ""
        }
    }
}

/**
 * Trim and truncate the string to the [maxLength] preserving as much of the non-whitespace as possible
 */
fun String.trimAndTruncate(maxLength: Int?): String {
    val startTrimmed = this.trimStart()
    val truncated = if (maxLength != null && startTrimmed.length > maxLength) {
        startTrimmed.take(maxLength)
    } else {
        startTrimmed
    }
    return truncated.trimEnd()
}