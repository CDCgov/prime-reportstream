package gov.cdc.prime.router

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import java.io.OutputStream
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.text.DecimalFormat


object Hl7Converter {
    val context = DefaultHapiContext()
    val phoneNumberUtil = PhoneNumberUtil.getInstance()

    fun write(table: MappableTable, outputStream: OutputStream) {
        // Dev Note: HAPI doesn't support a batch of messages, so this code creates
        // these segments by hand
        //
        outputStream.write(createFHS(table).toByteArray())
        outputStream.write(createBHS(table).toByteArray())
        table.rowIndices.map {
            val message = createMessage(table, it)
            outputStream.write(message.toByteArray())
        }
        outputStream.write(createBTS(table).toByteArray())
        outputStream.write(createFTS(table).toByteArray())
    }

    internal fun createMessage(table: MappableTable, row: Int): String {
        val message = ORU_R01()
        message.initQuickstart("ORU", "R01", "D")
        buildMessage(message, table, row)
        return context.pipeParser.encode(message)
    }

    private fun buildMessage(message: ORU_R01, table: MappableTable, row: Int) {
        val terser = Terser(message)
        setLiterals(terser)
        table.schema.elements.forEach { element ->
            setElement(terser, table, row, element)
        }
    }

    private fun setElement(terser: Terser, table: MappableTable, row: Int, element: Element) {
        val value = table.getStringWithDefault(row, element.name)
        val hl7Field = element.hl7Field ?: return
        val pathSpec = formPathSpec(hl7Field)
        when (element.type) {
            Element.Type.ID_CLIA -> {
                terser.set(pathSpec, value)
                terser.set(nextComponent(pathSpec), "CLIA")
            }
            Element.Type.HD -> {
                terser.set(pathSpec, value)
                terser.set(nextComponent(pathSpec), "ISO")
            }
            Element.Type.CODE -> setCodeElement(terser, value, pathSpec, element)
            Element.Type.TELEPHONE -> setTelephoneElement(terser, value, pathSpec, element)
            else -> terser.set(pathSpec, value)
        }
    }

    private fun setCodeElement(terser: Terser, value: String, pathSpec: String, element: Element) {
        val valueSetName = element.valueSet ?: error("Expecting a valueSet for ${element.name}")
        val valueSet = Metadata.findValueSet(valueSetName) ?: error("Cannot find $valueSetName")
        when (valueSet.system) {
            ValueSet.SetSystem.HL7 -> {
                // if it is a component spec then set all sub-components
                if (isField(pathSpec)) {
                    terser.set("$pathSpec-1", value)
                    terser.set("$pathSpec-2", valueSet.toDisplay(value))
                    terser.set("$pathSpec-3", valueSet.systemCode)
                } else {
                    terser.set(pathSpec, value)
                }
            }
            else -> {
                terser.set(pathSpec, value)
            }
        }
    }

    private fun setTelephoneElement(terser: Terser, value: String, pathSpec: String, element: Element) {
        val number = phoneNumberUtil.parse(value, "US")
        val national = DecimalFormat("0000000000").format(number.nationalNumber)
        val areaCode = national.substring(0, 3)
        val local = national.toString().substring(3, 10)

        terser.set(buildComponent(pathSpec, 2), "PH")
        if (number.hasCountryCode()) terser.set(buildComponent(pathSpec, 5), number.countryCode.toString())
        terser.set(buildComponent(pathSpec, 6), areaCode)
        terser.set(buildComponent(pathSpec, 7), local)
        if (number.hasExtension()) terser.set(buildComponent(pathSpec, 8), number.extension)
    }

    private fun setLiterals(terser: Terser) {
        terser.set("MSH-15", "NE")
        terser.set("MSH-16", "NE")
        terser.set("MSH-12", "2.5.1")

        terser.set("/PATIENT_RESULT/PATIENT/PID-1", "1")
        terser.set("/PATIENT_RESULT/ORDER_OBSERVATION/ORC-1", "RE")
    }

    private fun createFHS(table: MappableTable): String {
        val sendingApp = table.getStringWithDefault(0, "standard.sending_application")
        val sendingOid = table.getStringWithDefault(0, "standard.sending_application_id")
        val sendingFacilityName = table.getStringWithDefault(0, "standard.reporting_facility_name")
        val sendingCLIA = table.getStringWithDefault(0, "standard.reporting_facility_id")
        val receivingApplicationName = table.getStringWithDefault(0, "standard.receiving_application")
        val receivingApplicationId = table.getStringWithDefault(0, "standard.receiving_application_id")
        val receivingFacilityName = table.getStringWithDefault(0, "standard.receiving_facility")
        val receivingFacilityId = table.getStringWithDefault(0, "standard.receiving_facility_id")

        return "FHS|^~\\&|" +
                "$sendingApp^$sendingOid^ISO|" +
                "$sendingFacilityName^$sendingCLIA^CLIA|" +
                "$receivingApplicationName^$receivingApplicationId^ISO|" +
                "$receivingFacilityName^$receivingFacilityId^ISO|" +
                nowTimestamp() +
                "\r"
    }

    private fun createBHS(table: MappableTable): String {
        val sendingApp = table.getStringWithDefault(0, "standard.sending_application")
        val sendingOid = table.getStringWithDefault(0, "standard.sending_application_id")
        val sendingFacilityName = table.getStringWithDefault(0, "standard.reporting_facility_name")
        val sendingCLIA = table.getStringWithDefault(0, "standard.reporting_facility_id")
        val receivingApplicationName = table.getStringWithDefault(0, "standard.receiving_application")
        val receivingApplicationId = table.getStringWithDefault(0, "standard.receiving_application_id")
        val receivingFacilityName = table.getStringWithDefault(0, "standard.receiving_facility")
        val receivingFacilityId = table.getStringWithDefault(0, "standard.receiving_facility_id")

        return "BHS|^~\\&|" +
                "$sendingApp^$sendingOid^ISO|" +
                "$sendingFacilityName^$sendingCLIA^CLIA|" +
                "$receivingApplicationName^$receivingApplicationId^ISO|" +
                "$receivingFacilityName^$receivingFacilityId^ISO|" +
                nowTimestamp() +
                "\r"
    }

    private fun createBTS(table: MappableTable): String {
        return "BTS|${table.rowCount}\r"
    }

    private fun createFTS(table: MappableTable): String {
        return "FTS|1\r"
    }

    private fun nowTimestamp(): String {
        val timestamp = OffsetDateTime.now(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("YYYYMMDDhhmmssZZZ")
        return formatter.format(timestamp)
    }

    private fun buildComponent(spec: String, component: Int = 1): String {
        if (!isField(spec)) error("Not a component path spec")
        return "$spec-${component.toString()}"
    }

    private fun isField(spec: String): Boolean {
        val pattern = Regex("[A-Z][A-Z][A-Z]-[0-9]+$")
        return pattern.containsMatchIn(spec)
    }

    private fun nextComponent(spec: String, increment: Int = 1): String {
        val pattern = Regex("[A-Z][A-Z][A-Z]-[0-9]+-([0-9]+)$")
        val match = pattern.find(spec)?.groups?.get(1) ?: error("Did not find a match")
        val nextComponent = match.value.toInt() + increment
        return spec.replaceRange(match.range, nextComponent.toString())
    }

    private fun formPathSpec(spec: String): String {
        val segment = spec.substring(0, 3)
        val components = spec.substring(3)
        return when (segment) {
            "ORC" -> "/PATIENT_RESULT/ORDER_OBSERVATION/ORC$components"
            "SPM" -> "/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM$components"
            "PID" -> "/PATIENT_RESULT/PATIENT/PID$components"
            else -> spec
        }
    }
}