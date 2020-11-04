package gov.cdc.prime.router

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.model.v251.message.ORU_R01
import ca.uhn.hl7v2.util.Terser
import java.io.OutputStream
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter


object Hl7Converter {
    val context = DefaultHapiContext()

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
        message.initQuickstart("ORU","R01", "D")
        buildMessage(message, table, row)
        return context.pipeParser.encode(message)
    }

    private fun buildMessage(message: ORU_R01, table: MappableTable, row: Int) {
        val terser = Terser(message)
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
                terser.set(nextSubcomponent(pathSpec), "CLIA")
            }
            Element.Type.HD -> {
                terser.set(pathSpec, value)
                terser.set(nextSubcomponent(pathSpec), "ISO")
            }
            else -> {
                terser.set(pathSpec, value)
            }
        }
    }

    private fun createFHS(table: MappableTable): String {
        val sendingApp = table.getStringWithDefault(0, "standard.sending_application")
        val sendingOid = table.getStringWithDefault(0, "standard.sending_application_id")
        val sendingFacilityName = table.getStringWithDefault(0, "standard.reporting_facility_name")
        val sendingCLIA = table.getStringWithDefault(0, "standard.sending_application_id")
        // TODO figure out the receiving facility
        return "FHS|^~\\&|" +
                "$sendingApp^$sendingOid^ISO|" +
                "$sendingFacilityName^$sendingCLIA^CLIA|" +
                "AZ.DOH.ELR^2.16.840.1.114222.4.3.3.2.9.3^ISO|" +
                "AZDOH^2.16.840.1.114222.4.1.142^ISO|" +
                nowTimestamp() +
                "\r"
    }

    private fun createBHS(table: MappableTable): String {
        val sendingApp = table.getStringWithDefault(0, "standard.sending_application")
        val sendingOid = table.getStringWithDefault(0, "standard.sending_application_id")
        val sendingFacilityName = table.getStringWithDefault(0, "standard.reporting_facility_name")
        val sendingCLIA = table.getStringWithDefault(0, "standard.sending_application_id")
        // TODO figure out the receiving facility
        return "BHS|^~\\&|" +
                "$sendingApp^$sendingOid^ISO|" +
                "$sendingFacilityName^$sendingCLIA^CLIA|" +
                "AZ.DOH.ELR^2.16.840.1.114222.4.3.3.2.9.3^ISO|" +
                "AZDOH^2.16.840.1.114222.4.1.142^ISO|" +
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

    private fun nextSubcomponent(spec: String): String {
        val pattern = Regex("-([0-9]+)$")
        val sub = pattern.matchEntire(spec)?.groupValues?.get(0) ?: error("no match of pattern")
        val nextComponent = sub.toInt() + 1
        return pattern.replace(spec, nextComponent.toString())
    }

    private fun formPathSpec(spec: String): String {
        val segment = spec.substring(0, 3)
        val components = spec.substring(3)
        return when (segment) {
            "SPM" -> "/PATIENT_RESULT/ORDER_OBSERVATION/SPECIMEN/SPM$components"
            "PID" -> "/PATIENT_RESULT/PATIENT/PID$components"
            else -> spec
        }
    }
}