package gov.cdc.prime.router

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import java.io.OutputStream
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Properties

class Hl7Converter(val metadata: Metadata) {
    val softwareVendorOrganization = "Centers for Disease Control and Prevention"
    val softwareProductName = "PRIME Data Hub"

    val hapiContext = DefaultHapiContext()
    val buildVersion: String
    val buildDate: String

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

    fun write(report: Report, outputStream: OutputStream) {
        // Dev Note: HAPI doesn't support a batch of messages, so this code creates
        // these segments by hand
        //
        outputStream.write(createHeaders(report).toByteArray())
        report.itemIndices.map {
            val message = createMessage(report, it)
            outputStream.write(message.toByteArray())
        }
        outputStream.write(createFooters(report).toByteArray())
    }

    internal fun createMessage(report: Report, row: Int): String {
        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "D")
        buildMessage(message, report, row)
        return hapiContext.pipeParser.encode(message)
    }

    private fun buildMessage(message: ORU_R01, report: Report, row: Int) {
        var aoeSequence = 1
        val terser = Terser(message)
        setLiterals(terser)
        report.schema.elements.forEach { element ->
            val value = report.getStringWithDefault(row, element.name)
            if (value.isEmpty()) return@forEach

            if (element.hl7OutputFields != null) {
                element.hl7OutputFields.forEach { hl7Field ->
                    setComponent(terser, element, hl7Field, value)
                }
            } else if (element.hl7Field == "AOE" && element.type == Element.Type.NUMBER) {
                val units = report.getStringWithDefault(row, "${element.name}_units")
                val date = report.getStringWithDefault(row, "specimen_collection_date_time")
                setAOE(terser, element, aoeSequence++, date, value, units)
            } else if (element.hl7Field == "AOE") {
                val date = report.getStringWithDefault(row, "specimen_collection_date_time")
                setAOE(terser, element, aoeSequence++, date, value)
            } else if (element.hl7Field == "NTE-3") {
                setNote(terser, value)
            } else if (element.hl7Field != null) {
                setComponent(terser, element, element.hl7Field, value)
            }
        }
    }

    private fun setComponent(terser: Terser, element: Element, hl7Field: String, value: String) {
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
                    val hd = Element.parseHD(value)
                    if (hd.universalId != null && hd.universalIdSystem != null) {
                        terser.set("$pathSpec-1", hd.name)
                        terser.set("$pathSpec-2", hd.universalId)
                        terser.set("$pathSpec-3", hd.universalIdSystem)
                    } else {
                        terser.set(pathSpec, hd.name)
                    }
                }
            }
            Element.Type.CODE -> setCodeComponent(terser, value, pathSpec, element.valueSet)
            Element.Type.TELEPHONE -> setTelephoneComponent(terser, value, pathSpec, element)
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
                        terser.set("$pathSpec-3", valueSet.systemCode)
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

        terser.set(buildComponent(pathSpec, 2), if (element.nameContains("patient")) "PRN" else "WPN")
        terser.set(buildComponent(pathSpec, 5), country)
        terser.set(buildComponent(pathSpec, 6), areaCode)
        terser.set(buildComponent(pathSpec, 7), local)
        if (extension.isNotEmpty()) terser.set(buildComponent(pathSpec, 8), extension)
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
        units: String? = null
    ) {
        terser.set(formPathSpec("OBX-1", aoeRep), (aoeRep + 1).toString())
        terser.set(formPathSpec("OBX-2", aoeRep), "CWE")

        val aoeQuestion = element.hl7AOEQuestion
            ?: error("Schema Error: missing hl7AOEQuestion for '${element.name}'")
        setCodeComponent(terser, aoeQuestion, formPathSpec("OBX-3", aoeRep), "covid-19/aoe")

        when (element.type) {
            Element.Type.CODE -> setCodeComponent(terser, value, formPathSpec("OBX-5", aoeRep), element.valueSet)
            Element.Type.NUMBER -> {
                if (element.name != "patient_age") TODO("support other types of AOE numbers")
                if (units == null) error("Schema Error: expected age units")
                setComponent(terser, element, formPathSpec("OBX-5", aoeRep), value)
                setCodeComponent(terser, units, formPathSpec("OBX-6", aoeRep), "patient_age_units")
            }
            else -> setComponent(terser, element, formPathSpec("OBX-5", aoeRep), value)
        }

        terser.set(formPathSpec("OBX-11", aoeRep), "F")
        terser.set(formPathSpec("OBX-14", aoeRep), date)
        terser.set(formPathSpec("OBX-29", aoeRep), "QST")
    }

    private fun setNote(terser: Terser, value: String) {
        terser.set(formPathSpec("NTE-3"), value)
        terser.set(formPathSpec("NTE-4-1"), "RE")
        terser.set(formPathSpec("NTE-4-2"), "Remark")
        terser.set(formPathSpec("NTE-4-3"), "HL70364")
        terser.set(formPathSpec("NTE-4-7"), "2.5.1")
    }

    private fun setLiterals(terser: Terser) {
        // Value that NIST requires (although # is not part of 2.5.1)
        terser.set("MSH-15", "NE")
        terser.set("MSH-16", "NE")
        terser.set("MSH-12", "2.5.1")
        terser.set("MSH-17", "USA")
        // Values that NIST requires (although they are not part 2.5.1)
        terser.set("MSH-21-1", "PHLabReportNoAck")
        terser.set("MSH-21-2", "ELR_Receiver")
        terser.set("MSH-21-3", "2.16.840.1.113883.9.11")
        terser.set("MSH-21-4", "ISO")

        terser.set("SFT-1", softwareVendorOrganization)
        terser.set("SFT-2", buildVersion)
        terser.set("SFT-3", softwareProductName)
        terser.set("SFT-4", buildVersion)
        terser.set("SFT-6", buildDate)

        terser.set("/PATIENT_RESULT/PATIENT/PID-1", "1")

        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-1", "RE")

        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBR-1", "1")

        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM-1", "1")

        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/OBSERVATION/OBX-2", "CWE")
    }

    private fun createHeaders(report: Report): String {
        val sendingApp = formatHD(Element.parseHD(report.getStringWithDefault(0, "sending_application")))
        val receivingApp = formatHD(Element.parseHD(report.getStringWithDefault(0, "receiving_application")))
        val receivingFacility = formatHD(Element.parseHD(report.getStringWithDefault(0, "receiving_facility")))

        return "FHS|^~\\&|" +
            "$sendingApp|" +
            "$receivingApp|" +
            "$receivingFacility|" +
            nowTimestamp() +
            "\r" +
            "BHS|^~\\&|" +
            "$sendingApp|" +
            "$receivingApp|" +
            "$receivingFacility|" +
            nowTimestamp() +
            "\r"
    }

    private fun createFooters(report: Report): String {
        return "BTS|${report.itemCount}\r" +
            "FTS|1\r"
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

    private fun formatHD(hdFields: Element.HDFields, seperator: String = "^"): String {
        return if (hdFields.universalId != null && hdFields.universalIdSystem != null) {
            "${hdFields.name}$seperator${hdFields.universalId}$seperator${hdFields.universalIdSystem}"
        } else {
            hdFields.name
        }
    }
}