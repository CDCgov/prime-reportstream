package gov.cdc.prime.router.serializers

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HL7Exception
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory
import ca.uhn.hl7v2.parser.ModelClassFactory
import ca.uhn.hl7v2.util.Terser
import gov.cdc.prime.router.Element
import gov.cdc.prime.router.ElementAndValue
import gov.cdc.prime.router.Hl7Configuration
import gov.cdc.prime.router.Mapper
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ResultDetail
import gov.cdc.prime.router.Schema
import gov.cdc.prime.router.Source
import gov.cdc.prime.router.TranslatorConfiguration
import gov.cdc.prime.router.ValueSet
import java.io.InputStream
import java.io.OutputStream
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Properties
import org.apache.logging.log4j.kotlin.Logging

class Hl7Serializer(val metadata: Metadata): Logging {
    data class Hl7Mapping(
        val mappedRows: Map<String, List<String>>,
        val rows: List<RowResult>,
        val errors: List<String>,
        val warnings: List<String>,
    )
    data class RowResult(
        val row: Map<String, List<String>>,
        val errors: List<String>,
        val warnings: List<String>,
    )

    private val hl7SegmentDelimiter: String = "\r"
    private val hapiContext = DefaultHapiContext()
    private val modelClassFactory: ModelClassFactory = CanonicalModelClassFactory(HL7_SPEC_VERSION)
    private val buildVersion: String
    private val buildDate: String
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSSSZZZ")
    private var hl7Config: Hl7Configuration? = null
    private val hdFieldMaximumLength: Int? get() = if (hl7Config?.truncateHDNamespaceIds == true) {
        HD_TRUNCATION_LIMIT
    } else {
        null
    }

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
    fun write(report: Report, outputStream: OutputStream, translatorConfig: TranslatorConfiguration? = null) {
        if (report.itemCount != 1)
            error("Internal Error: multiple item report cannot be written as a single HL7 message")
        val message = createMessage(report, 0)
        outputStream.write(message.toByteArray())
    }

    /**
     * Write a report with BHS and FHS segments and multiple items
     */
    fun writeBatch(
        report: Report,
        outputStream: OutputStream,
    ) {
        // Dev Note: HAPI doesn't support a batch of messages, so this code creates
        // these segments by hand
        outputStream.write(createHeaders(report).toByteArray())
        report.itemIndices.map {
            val message = createMessage(report, it)
            outputStream.write(message.toByteArray())
        }
        outputStream.write(createFooters(report).toByteArray())
    }

    /*
     * Read in a file
     */
    fun convertBatchMessagesToMap(message: String, schema: Schema): Hl7Mapping {
        val mappedRows: MutableMap<String, MutableList<String>> = mutableMapOf()
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val rowResults = mutableListOf<RowResult>()
        val reg = "(\r|\n)".toRegex()
        val cleanedMessage = reg.replace(message, "\r")
        val messageLines = cleanedMessage.split("\r")
        val nextMessage = StringBuilder()

        fun deconstructStringMessage() {
            val parsedMessage = convertMessageToMap(nextMessage.toString(), schema)
            errors.addAll(parsedMessage.errors)
            warnings.addAll(parsedMessage.warnings)
            // there is a chance that there's an empty row (for example, an empty line)
            // that won't parse. so we should skip that because it's not valid HL7
            if (parsedMessage.row.isEmpty())
                return
            rowResults.add(parsedMessage)
            nextMessage.clear()
            parsedMessage.row.forEach { (k, v) ->
                if (!mappedRows.containsKey(k))
                    mappedRows[k] = mutableListOf()

                mappedRows[k]?.addAll(v)
            }
        }

        messageLines.forEach {
            if (it.startsWith("FHS"))
                return@forEach
            if (it.startsWith("BHS"))
                return@forEach
            if (it.startsWith("BTS"))
                return@forEach
            if (it.startsWith("FTS"))
                return@forEach

            if (nextMessage.isNotEmpty() && it.startsWith("MSH")) {
                deconstructStringMessage()
            }
            nextMessage.append("$it\r")
        }

        // catch the last message
        if (nextMessage.isNotEmpty()) {
            deconstructStringMessage()
        }

        return Hl7Mapping(mappedRows, rowResults, errors, warnings)
    }

    fun convertMessageToMap(message: String, schema: Schema): Hl7Serializer.RowResult {
        // safely merge into the set. might not be necessary
        fun mergeIntoMappedRows(mappedRows: MutableMap<String, MutableSet<String>>, key: String, value: String) {
            if (!mappedRows.containsKey(key)) error("Map doesn't contain key $key")
            // get the existing values
            val existingValues = mappedRows[key] ?: emptySet()
            // if the existing value doesn't exist, add it in
            if (!existingValues.contains(value)) {
                // making sure an empty value doesn't blow things up if we already
                // have a value for that key
                if (existingValues.isEmpty() || value.isNotEmpty())
                    mappedRows[key]?.add(value)
            }
        }
        // query the terser and get a value
        fun queryTerserForValue(
            terser: Terser,
            terserSpec: String,
            elementName: String,
            mappedRows: MutableMap<String, MutableSet<String>>,
            errors: MutableList<String>,
            warnings: MutableList<String>
        ) {
            val parsedValue = try {
                terser.get(terserSpec)
            } catch (e: HL7Exception) {
                errors.add("Exception for $terserSpec: ${e.message}")
                null
            }
            // add the rows
            if (parsedValue.isNullOrEmpty()) {
                warnings.add("Blank for $terserSpec - $elementName")
            }
            mergeIntoMappedRows(mappedRows, elementName, parsedValue ?: "")
        }
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        // key of the map is the column header, list is the values in the column
        val mappedRows: MutableMap<String, MutableSet<String>> = mutableMapOf()
        hapiContext.modelClassFactory = modelClassFactory
        val parser = hapiContext.pipeParser
        val reg = "(\r|\n)".toRegex()
        val cleanedMessage = reg.replace(message, "\r").trim()
        // if the message is empty, return a row result that warns of empty data
        if (cleanedMessage.isEmpty()) {
            logger.debug("Skipping empty message during parsing")
            return RowResult(emptyMap(), emptyList(), listOf("Cannot parse empty message"))
        }

        try {
            val hapiMsg = parser.parse(cleanedMessage)
            val terser = Terser(hapiMsg)
            schema.elements.forEach {
                if (!mappedRows.containsKey(it.name))
                    mappedRows[it.name] = mutableSetOf()

                if (it.hl7Field.isNullOrEmpty() && it.hl7OutputFields.isNullOrEmpty()) {
                    mappedRows[it.name]?.add("")
                    return@forEach
                }

                if (!it.hl7Field.isNullOrEmpty()) {
                    val terserSpec = when {
                        it.hl7Field.startsWith("MSH") -> "/${it.hl7Field}"
                        (it.hl7Field == "AOE") -> {
                            val question = it.hl7AOEQuestion!!
                            val countObservations = 10
                            // todo: map each AOE by the AOE question ID
                            for (c in 0 until countObservations) {
                                var spec = "/.OBSERVATION($c)/OBX-3-1"
                                val questionCode = try {
                                    terser.get(spec)
                                } catch (e: HL7Exception) {
                                    // todo: convert to result detail, maybe
                                    errors.add("Exception for $spec: ${e.message}")
                                    null
                                }
                                if (questionCode?.startsWith(question) == true) {
                                    spec = "/.OBSERVATION($c)/OBX-5"
                                    queryTerserForValue(terser, spec, it.name, mappedRows, errors, warnings)
                                }
                            }
                            "/.AOE"
                        }
                        else -> "/.${it.hl7Field}"
                    }

                    if (terserSpec != "/.AOE") {
                        queryTerserForValue(terser, terserSpec, it.name, mappedRows, errors, warnings)
                    } else {
                        if (mappedRows[it.name]?.isEmpty() == true) mappedRows[it.name]?.add("")
                    }
                } else {
                    it.hl7OutputFields?.forEach { h ->
                        val terserSpec = if (h.startsWith("MSH")) {
                            "/$h"
                        } else {
                            "/.$h"
                        }
                        queryTerserForValue(terser, terserSpec, it.name, mappedRows, errors, warnings)
                    }
                }
            }
        } catch (e: Exception) {
            val msg = "${e.localizedMessage} ${e.stackTraceToString()}"
            logger.error(msg)
            errors.add(msg)
        }

        // convert sets to lists
        val rows = mappedRows.keys.associateWith {
            (mappedRows[it]?.toList() ?: emptyList())
        }

        return RowResult(rows, errors, warnings)
    }

    fun readExternal(
        schemaName: String,
        input: InputStream,
        source: Source
    ): ReadResult {
        val errors = mutableListOf<ResultDetail>()
        val warnings = mutableListOf<ResultDetail>()
        val messageBody = input.bufferedReader().use { it.readText() }
        val schema = metadata.findSchema(schemaName) ?: error("Schema name $schemaName not found")
        val mapping = convertBatchMessagesToMap(messageBody, schema)
        val mappedRows = mapping.mappedRows
        errors.addAll(mapping.errors.map { ResultDetail(ResultDetail.DetailScope.ITEM, "", it) })
        warnings.addAll(mapping.warnings.map { ResultDetail(ResultDetail.DetailScope.ITEM, "", it) })
        mappedRows.forEach {
            logger.info("${it.key} -> ${it.value.joinToString()}")
        }
        val report = Report(schema, mappedRows, source)
        return ReadResult(report, errors, warnings)
    }

    internal fun createMessage(report: Report, row: Int): String {
        val message = ORU_R01()
        val hl7Config = report.destination?.translation as? Hl7Configuration?
        val processingId = if (hl7Config?.useTestProcessingMode == true) {
            "T"
        } else {
            "P"
        }
        message.initQuickstart(MESSAGE_CODE, MESSAGE_TRIGGER_EVENT, processingId)
        buildMessage(message, report, row, processingId)
        hapiContext.modelClassFactory = modelClassFactory
        return hapiContext.pipeParser.encode(message)
    }

    private fun buildMessage(
        message: ORU_R01,
        report: Report,
        row: Int,
        processingId: String = "T",
    ) {
        // set up our configuration
        val hl7Config = report.destination?.translation as? Hl7Configuration
        val suppressQst = hl7Config?.suppressQstForAoe ?: false
        val suppressAoe = hl7Config?.suppressAoe ?: false
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
        val terser = Terser(message)
        setLiterals(terser)
        // serialize the rest of the elements
        report.schema.elements.forEach { element ->
            val value = report.getString(row, element.name).let {
                if (it.isNullOrEmpty()) {
                    element.default ?: ""
                } else {
                    it
                }
            }

            if (suppressedFields.contains(element.hl7Field) && element.hl7OutputFields.isNullOrEmpty())
                return@forEach

            // some fields need to be blank instead of passing in UNK
            // so in this case we'll just go by field name and set the value to blank
            if (blanksForUnknownFields.contains(element.name) &&
                element.hl7Field != null &&
                (value.equals("ASKU", true) || value.equals("UNK", true))
            ) {
                setComponent(terser, element, element.hl7Field, "", report)
                return@forEach
            }

            if (element.hl7OutputFields != null) {
                element.hl7OutputFields.forEach outputFields@{ hl7Field ->
                    if (suppressedFields.contains(hl7Field))
                        return@outputFields
                    // some of our schema elements are actually subcomponents of the HL7 fields, and are individually
                    // text, but need to be truncated because they're the first part of an HD field. For example,
                    // ORC-2-2 and ORC-3-2, so we are manually pulling them aside to truncate them
                    val truncatedValue = if (
                        value.length > HD_TRUNCATION_LIMIT &&
                        element.type == Element.Type.TEXT &&
                        hl7Field in HD_FIELDS &&
                        hl7Config?.truncateHDNamespaceIds == true
                    ) {
                        value.substring(0, HD_TRUNCATION_LIMIT)
                    } else {
                        value
                    }
                    if (element.hl7Field != null && element.mapperRef != null && element.type == Element.Type.TABLE) {
                        setComponentForTable(terser, element, hl7Field, report, row)
                    } else {
                        setComponent(terser, element, hl7Field, truncatedValue, report)
                    }
                }
            } else if (element.hl7Field == "AOE" && element.type == Element.Type.NUMBER && !suppressAoe) {
                if (value.isNotBlank()) {
                    val units = report.getString(row, "${element.name}_units")
                    val date = report.getString(row, "specimen_collection_date_time") ?: ""
                    setAOE(terser, element, aoeSequence++, date, value, report, row, units, suppressQst)
                }
            } else if (element.hl7Field == "AOE" && !suppressAoe) {
                if (value.isNotBlank()) {
                    val date = report.getString(row, "specimen_collection_date_time") ?: ""
                    setAOE(terser, element, aoeSequence++, date, value, report, row, suppressQst = suppressQst)
                } else {
                    // if the value is null but we're defaulting
                    if (hl7Config?.defaultAoeToUnknown == true) {
                        val date = report.getString(row, "specimen_collection_date_time") ?: ""
                        setAOE(terser, element, aoeSequence++, date, "UNK", report, row, suppressQst = suppressQst)
                    }
                }
            } else if (element.hl7Field == "NTE-3") {
                setNote(terser, value)
            } else if (element.hl7Field == "MSH-7") {
                setComponent(terser, element, "MSH-7", formatter.format(report.createdDateTime), report)
            } else if (element.hl7Field == "MSH-11") {
                setComponent(terser, element, "MSH-11", processingId, report)
            } else if (element.hl7Field != null && element.mapperRef != null && element.type == Element.Type.TABLE) {
                setComponentForTable(terser, element, report, row)
            } else if (
                element.type == Element.Type.TEXT && !element.hl7Field.isNullOrEmpty() && element.hl7Field in HD_FIELDS
            ) {
                // some of our schema elements are actually subcomponents of the HL7 fields, and are individually
                // text, but need to be truncated because they're the first part of an HD field. For example,
                // ORC-2-2 and ORC-3-2, so we are manually pulling them aside to truncate them
                val truncatedValue = if (
                    value.length > HD_TRUNCATION_LIMIT &&
                    hl7Config?.truncateHDNamespaceIds == true
                ) {
                    value.substring(0, HD_TRUNCATION_LIMIT)
                } else {
                    value
                }
                setComponent(terser, element, element.hl7Field, truncatedValue, report)
            } else if (element.hl7Field != null) {
                setComponent(terser, element, element.hl7Field, value, report)
            }
        }
        // make sure all fields we're suppressing are empty
        suppressedFields.forEach {
            val pathSpec = formPathSpec(it)
            terser.set(pathSpec, "")
        }
        convertTimestampToDateTimeFields.forEach {
            val pathSpec = formPathSpec(it)
            val tsValue = terser.get(pathSpec)
            if (!tsValue.isNullOrEmpty()) {
                try {
                    val dtFormatter = DateTimeFormatter.ofPattern("yyyMMddHHmmss")
                    val parsedDate = OffsetDateTime.parse(tsValue, formatter).format(dtFormatter)
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
            val pathSpec = formPathSpec("MSH-4-2")
            terser.set(pathSpec, hl7Config?.reportingFacilityId)
        }
    }

    private fun setComponentForTable(terser: Terser, element: Element, report: Report, row: Int) {
        setComponentForTable(terser, element, element.hl7Field!!, report, row)
    }

    private fun setComponentForTable(terser: Terser, element: Element, hl7Field: String, report: Report, row: Int) {
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
            terser.set(pathSpec, mapper.apply(element, args, valuesForMapper) ?: "")
        }
    }

    private fun setComponent(
        terser: Terser,
        element: Element,
        hl7Field: String,
        value: String,
        report: Report
    ) {
        val hl7Config = report.destination?.translation as? Hl7Configuration?
        val hdFieldMaximumLength = if (hl7Config?.truncateHDNamespaceIds == true) {
            HD_TRUNCATION_LIMIT
        } else {
            null
        }
        val pathSpec = formPathSpec(hl7Field)
        when (element.type) {
            Element.Type.ID_CLIA -> {
                if (value.isNotEmpty()) {
                    terser.set(pathSpec, value)
                    terser.set(nextComponent(pathSpec), "CLIA")
                }
            }
            Element.Type.HD -> {
                if (value.isNotEmpty()) {
                    val hd = Element.parseHD(value, hdFieldMaximumLength)
                    if (hd.universalId != null && hd.universalIdSystem != null) {
                        terser.set("$pathSpec-1", hd.name)
                        terser.set("$pathSpec-2", hd.universalId)
                        terser.set("$pathSpec-3", hd.universalIdSystem)
                    } else {
                        terser.set(pathSpec, hd.name)
                    }
                }
            }
            Element.Type.EI -> {
                if (value.isNotEmpty()) {
                    val ei = Element.parseEI(value)
                    if (ei.universalId != null && ei.universalIdSystem != null) {
                        terser.set("$pathSpec-1", ei.name)
                        terser.set("$pathSpec-2", ei.namespace)
                        terser.set("$pathSpec-3", ei.universalId)
                        terser.set("$pathSpec-4", ei.universalIdSystem)
                    } else {
                        terser.set(pathSpec, ei.name)
                    }
                }
            }
            Element.Type.CODE -> setCodeComponent(terser, value, pathSpec, element.valueSet)
            Element.Type.TELEPHONE -> {
                if (value.isNotEmpty()) {
                    setTelephoneComponent(terser, value, pathSpec, element)
                }
            }
            Element.Type.EMAIL -> {
                if (value.isNotEmpty()) {
                    setEmailComponent(terser, value, element, hl7Config)
                }
            }
            Element.Type.POSTAL_CODE -> setPostalComponent(terser, value, pathSpec, element)
            else -> terser.set(pathSpec, value)
        }
    }

    private fun setCodeComponent(terser: Terser, value: String, pathSpec: String, valueSetName: String?) {
        if (valueSetName == null) error("Schema Error: Missing valueSet for '$pathSpec'")
        val valueSet = metadata.findValueSet(valueSetName)
            ?: error("Schema Error: Cannot find '$valueSetName'")
        when (valueSet.system) {
            ValueSet.SetSystem.HL7,
            ValueSet.SetSystem.LOINC,
            ValueSet.SetSystem.UCUM,
            ValueSet.SetSystem.SNOMED_CT -> {
                // if it is a component spec then set all sub-components
                if (isField(pathSpec)) {
                    if (value.isNotEmpty()) {
                        terser.set("$pathSpec-1", value)
                        terser.set("$pathSpec-2", valueSet.toDisplayFromCode(value))
                        terser.set("$pathSpec-3", valueSet.toSystemFromCode(value))
                        valueSet.toVersionFromCode(value)?.let {
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

    private fun setTelephoneComponent(terser: Terser, value: String, pathSpec: String, element: Element) {
        val parts = value.split(Element.phoneDelimiter)
        val areaCode = parts[0].substring(0, 3)
        val local = parts[0].substring(3)
        val country = parts[1]
        val extension = parts[2]

        if (element.nameContains("patient")) {
            // PID-13 is repeatable, which means we could have more than one phone #
            // or email etc, so we need to increment until we get empty for PID-13-2
            var rep = 0
            while (terser.get("/PATIENT_RESULT/PATIENT/PID-13($rep)-2")?.isEmpty() == false) {
                rep += 1
            }
            // primary residence number
            terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-1", "($areaCode)$local")
            terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-2", "PRN")
            // it's a phone
            terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-3", "PH")
            terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-5", country)
            terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-6", areaCode)
            terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-7", local)
            if (extension.isNotEmpty()) terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-8", extension)
        } else {
            // work phone number
            terser.set(buildComponent(pathSpec, 1), "($areaCode)$local")
            terser.set(buildComponent(pathSpec, 2), "WPN")
            // it's a phone
            terser.set(buildComponent(pathSpec, 3), "PH")
            terser.set(buildComponent(pathSpec, 5), country)
            terser.set(buildComponent(pathSpec, 6), areaCode)
            terser.set(buildComponent(pathSpec, 7), local)
            terser.set(buildComponent(pathSpec, 8), extension)
        }
    }

    private fun setEmailComponent(terser: Terser, value: String, element: Element, hl7Config: Hl7Configuration?) {
        // branch on element name. maybe we'll pass through ordering provider email information as well
        if (element.nameContains("patient_email")) {
            // for some state systems, they cannot handle repetition in the PID-13 field, despite what
            // the HL7 specification calls for. In that case, the patient email is not imported. A common
            // workaround is to shove the patient_email into PID-14 which is the business phone
            if (hl7Config?.usePid14ForPatientEmail == true) {
                // this is an email address
                terser.set("/PATIENT_RESULT/PATIENT/PID-14-2", "NET")
                // specifies it's an internet telecommunications type
                terser.set("/PATIENT_RESULT/PATIENT/PID-14-3", "Internet")
                terser.set("/PATIENT_RESULT/PATIENT/PID-14-4", value)
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
                terser.set("/PATIENT_RESULT/PATIENT/PID-13($rep)-4", value)
            }
        }
    }

    private fun setPostalComponent(terser: Terser, value: String, pathSpec: String, element: Element) {
        val zipFive = element.toFormatted(value, Element.zipFiveToken)
        terser.set(pathSpec, zipFive)
    }

    private fun setAOE(
        terser: Terser,
        element: Element,
        aoeRep: Int,
        date: String,
        value: String,
        report: Report,
        row: Int,
        units: String? = null,
        suppressQst: Boolean = false,
    ) {
        // if the value type is a date, we need to specify that for the AOE questions
        val valueType = if (element.type == Element.Type.DATE) {
            "DT"
        } else {
            "CWE"
        }
        terser.set(formPathSpec("OBX-1", aoeRep), (aoeRep + 1).toString())
        terser.set(formPathSpec("OBX-2", aoeRep), valueType)
        val aoeQuestion = element.hl7AOEQuestion
            ?: error("Schema Error: missing hl7AOEQuestion for '${element.name}'")
        setCodeComponent(terser, aoeQuestion, formPathSpec("OBX-3", aoeRep), "covid-19/aoe")

        when (element.type) {
            Element.Type.CODE -> setCodeComponent(terser, value, formPathSpec("OBX-5", aoeRep), element.valueSet)
            Element.Type.NUMBER -> {
                if (element.name != "patient_age") TODO("support other types of AOE numbers")
                if (units == null) error("Schema Error: expected age units")
                setComponent(terser, element, formPathSpec("OBX-5", aoeRep), value, report)
                setCodeComponent(terser, units, formPathSpec("OBX-6", aoeRep), "patient_age_units")
            }
            else -> setComponent(terser, element, formPathSpec("OBX-5", aoeRep), value, report)
        }

        terser.set(formPathSpec("OBX-11", aoeRep), "F")
        terser.set(formPathSpec("OBX-14", aoeRep), date)
        // some states want the observation date for the AOE questions as well
        terser.set(formPathSpec("OBX-19", aoeRep), report.getString(row, "test_result_date"))
        terser.set(formPathSpec("OBX-23-7", aoeRep), "XX")
        // many states can't accept the QST datapoint out at the end because it is nonstandard
        // we need to pass this in via the translation configuration
        if (!suppressQst) terser.set(formPathSpec("OBX-29", aoeRep), "QST")
        // all of these values must be set on the OBX AOE's for validation
        terser.set(formPathSpec("OBX-23-1", aoeRep), report.getStringByHl7Field(row, "OBX-23-1"))
        // set to a default value, but look below
        // terser.set(formPathSpec("OBX-23-6", aoeRep), report.getStringByHl7Field(row, "OBX-23-6"))
        terser.set(formPathSpec("OBX-23-10", aoeRep), report.getString(row, "testing_lab_clia"))
        terser.set(formPathSpec("OBX-15", aoeRep), report.getString(row, "testing_lab_clia"))
        terser.set(formPathSpec("OBX-24-1", aoeRep), report.getStringByHl7Field(row, "OBX-24-1"))
        terser.set(formPathSpec("OBX-24-2", aoeRep), report.getStringByHl7Field(row, "OBX-24-2"))
        terser.set(formPathSpec("OBX-24-3", aoeRep), report.getStringByHl7Field(row, "OBX-24-3"))
        terser.set(formPathSpec("OBX-24-4", aoeRep), report.getStringByHl7Field(row, "OBX-24-4"))
        terser.set(formPathSpec("OBX-24-5", aoeRep), report.getStringByHl7Field(row, "OBX-24-5"))
        terser.set(formPathSpec("OBX-24-9", aoeRep), report.getStringByHl7Field(row, "OBX-24-9"))
        // check for the OBX-23-6 value. it needs to be split apart
        val testingLabIdAssigner = report.getString(row, "testing_lab_id_assigner")
        if (testingLabIdAssigner?.contains("^") == true) {
            val testingLabIdAssignerParts = testingLabIdAssigner.split("^")
            testingLabIdAssignerParts.forEachIndexed { index, s ->
                terser.set(formPathSpec("OBX-23-6-${index + 1}", aoeRep), s)
            }
        }
    }

    private fun setNote(terser: Terser, value: String) {
        if (value.isBlank()) return
        terser.set(formPathSpec("NTE-3"), value)
        terser.set(formPathSpec("NTE-4-1"), "RE")
        terser.set(formPathSpec("NTE-4-2"), "Remark")
        terser.set(formPathSpec("NTE-4-3"), "HL70364")
        terser.set(formPathSpec("NTE-4-7"), HL7_SPEC_VERSION)
    }

    private fun setLiterals(terser: Terser) {
        // Value that NIST requires (although # is not part of 2.5.1)
        terser.set("MSH-12", HL7_SPEC_VERSION)
        terser.set("MSH-15", "NE")
        terser.set("MSH-16", "NE")
        terser.set("MSH-17", "USA")
        terser.set("MSH-18", "UNICODE UTF-8")
        terser.set("MSH-19", "")
        terser.set("MSH-20", "")
        /*
        terser.set("MSH-21-1", "PHLabReport-NoAck")
        terser.set("MSH-21-2", "ELR_Receiver")
        terser.set("MSH-21-3", "2.16.840.1.113883.9.11")
        terser.set("MSH-21-4", "ISO")
         */
        terser.set("SFT-1", SOFTWARE_VENDOR_ORGANIZATION)
        terser.set("SFT-2", buildVersion)
        terser.set("SFT-3", SOFTWARE_PRODUCT_NAME)
        terser.set("SFT-4", buildVersion)
        terser.set("SFT-6", buildDate)
        terser.set("/PATIENT_RESULT/PATIENT/PID-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-1", "RE")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBR-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-2", "CWE")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-23-7", "XX")
    }

    private fun createHeaders(report: Report): String {
        val hl7Config = report.destination?.translation as? Hl7Configuration?
        val hdFieldMaximumLength = if (hl7Config?.truncateHDNamespaceIds == true) {
            HD_TRUNCATION_LIMIT
        } else {
            null
        }
        val encodingCharacters = "^~\\&"
        val sendingApp = formatHD(
            Element.parseHD(report.getString(0, "sending_application") ?: "", hdFieldMaximumLength)
        )
        val sendingFacility = formatHD(
            Element.parseHD(report.getString(0, "sending_application") ?: "", hdFieldMaximumLength)
        )
        val receivingApp = formatHD(
            Element.parseHD(report.getString(0, "receiving_application") ?: "", hdFieldMaximumLength)
        )
        val receivingFacility = formatHD(
            Element.parseHD(report.getString(0, "receiving_facility") ?: "", hdFieldMaximumLength)
        )

        return "FHS|$encodingCharacters|" +
            "$sendingApp|" +
            "$sendingFacility|" +
            "$receivingApp|" +
            "$receivingFacility|" +
            nowTimestamp() +
            hl7SegmentDelimiter +
            "BHS|$encodingCharacters|" +
            "$sendingApp|" +
            "$sendingFacility|" +
            "$receivingApp|" +
            "$receivingFacility|" +
            nowTimestamp() +
            hl7SegmentDelimiter
    }

    private fun createFooters(report: Report): String {
        return "BTS|${report.itemCount}$hl7SegmentDelimiter" +
            "FTS|1$hl7SegmentDelimiter"
    }

    private fun nowTimestamp(): String {
        val timestamp = OffsetDateTime.now(ZoneId.systemDefault())
        return Element.datetimeFormatter.format(timestamp)
    }

    private fun buildComponent(spec: String, component: Int = 1): String {
        if (!isField(spec)) error("Not a component path spec")
        return "$spec-$component"
    }

    private fun isField(spec: String): Boolean {
        val pattern = Regex("[A-Z][A-Z][A-Z]-[0-9]+$")
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

    private fun formPathSpec(spec: String, rep: Int? = null): String {
        val segment = spec.substring(0, 3)
        val components = spec.substring(3)
        val repSpec = rep?.let { "($rep)" } ?: ""
        return when (segment) {
            "OBR" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBR$components"
            "ORC" -> "/PATIENT_RESULT/ORDER_OBSERVATION/ORC$components"
            "SPM" -> "/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM$components"
            "PID" -> "/PATIENT_RESULT/PATIENT/PID$components"
            "OBX" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION$repSpec/OBX$components"
            "NTE" -> "/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/NTE$components"
            else -> spec
        }
    }

    private fun formatHD(hdFields: Element.HDFields, separator: String = "^"): String {
        return if (hdFields.universalId != null && hdFields.universalIdSystem != null) {
            "${hdFields.name}$separator${hdFields.universalId}$separator${hdFields.universalIdSystem}"
        } else {
            hdFields.name
        }
    }

    private fun formatEI(eiFields: Element.EIFields, separator: String = "^"): String {
        return if (eiFields.namespace != null && eiFields.universalId != null && eiFields.universalIdSystem != null) {
            "${eiFields.name}$separator${eiFields.namespace}" +
                "$separator${eiFields.universalId}$separator${eiFields.universalIdSystem}"
        } else {
            eiFields.name
        }
    }

    companion object {
        const val HD_TRUNCATION_LIMIT = 20
        const val HL7_SPEC_VERSION: String = "2.5.1"
        const val MESSAGE_CODE = "ORU"
        const val MESSAGE_TRIGGER_EVENT = "R01"
        const val SOFTWARE_VENDOR_ORGANIZATION: String = "Centers for Disease Control and Prevention"
        const val SOFTWARE_PRODUCT_NAME: String = "PRIME Data Hub"
        val HD_FIELDS = listOf("MSH-4-1", "OBR-3-2", "OBR-2-2", "ORC-3-2", "ORC-2-2")
    }
}