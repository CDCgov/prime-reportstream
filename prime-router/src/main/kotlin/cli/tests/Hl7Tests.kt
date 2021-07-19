// ktlint-disable filename
package gov.cdc.prime.router.cli.tests

import com.github.ajalt.clikt.output.TermUi
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.cli.FileUtilities
import java.net.HttpURLConnection
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import gov.cdc.prime.router.azure.WorkflowEngine
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper



/**
 * Generates a fake HL7 report and sends it to ReportStream, waits some time then checks the lineage to make
 * sure the data was sent to the configured senders.
 */
class Hl7Ingest : CoolTest() {
    override val name = "hl7ingest"
    override val description = "Create HL7 Fake data, submit, wait, confirm sent via database lineage data"
    override val status = TestStatus.SMOKE

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        var passed = true
        val sender = hl7Sender
        val receivers = allGoodReceivers
        val itemCount = options.items * receivers.size
        ugly("Starting $name Test: send ${sender.fullName} data to $allGoodCounties")
        val file = FileUtilities.createFakeFile(
            metadata,
            sender,
            itemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
            Report.Format.HL7_BATCH
        )
        TermUi.echo("Created datafile $file")

        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, sender, options.key)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***$name Test FAILED***:  response code $responseCode")
            passed = false
        } else {
            good("Posting of report succeeded with response code $responseCode")
        }

        // Check the response from the endpoint
        TermUi.echo(json)
        passed = passed and examineResponse(json)

        // Now check the lineage data
        waitABit(25, environment)
        val reportId = getReportIdFromResponse(json)
        if (reportId != null) {
            passed = passed and examineLineageResults(reportId, receivers, itemCount)
        }

        return passed
    }
}

class BadHl7 : CoolTest() {
    override val name = "badhl7"
    override val description = "Submit badly formatted hl7 files - should get errors"
    override val status = TestStatus.SMOKE
    val failures = mutableListOf<String>()

    val strMessage = """MSH|^~\&|CDC PRIME - Atlanta, Georgia (Dekalb)^2.16.840.1.114222.4.1.237821^ISO|Avante at Ormond Beach^10D0876999^CLIA|PRIME_DOH|Prime Data Hub|20210210170737||ORU^R01^ORU_R01|371784|P|2.5.1|||NE|NE|USA||||PHLabReportNoAck^ELR_Receiver^2.16.840.1.113883.9.11^ISO
PID|1|ABC123DF|AND234DA_PID3|PID_4_ALTID|Patlast^Patfirst^Mid||19670202|F|||4505 21 st^^LAKE COUNTRY^MD^FO||222-555-8484|||||MF0050356/15|
ORC|RE|73a6e9bd-aaec-418e-813a-0ad33366ca85|73a6e9bd-aaec-418e-813a-0ad33366ca85|||||||||1629082607^Eddin^Husam^^^^^^CMS&2.16.840.1.113883.3.249&ISO^^^^NPI||^WPN^^^1^386^6825220|20210209||||||Avante at Ormond Beach|170 North King Road^^Ormond Beach^IG^32174^^^^12127|^WPN^^jbrush@avantecenters.com^1^407^7397506|^^^^32174
OBX|1|CWE|94558-4^SARS-CoV-2 (COVID-19) Ag [Presence] in Respiratory specimen by Rapid immunoassay^LN||260415000^Not detected^SCT|||N^Normal (applies to non-numeric results)^HL70078|||F|||202102090000-0600|||CareStart COVID-19 Antigen test_Access Bio, Inc._EUA^^99ELR||202102090000-0600||||Avante at Ormond Beach^^^^^CLIA&2.16.840.1.113883.19.4.6&ISO^^^^10D08761999^CLIA|170 North King Road^^Ormond Beach^FL^32174^^^^12127
SPM|1|b518ef23-1d9a-40c1-ac4b-ed7b438dfc4b||258500001^Nasopharyngeal swab^SCT||||718IG36000^Nasopharyngeal structure (body structure)^SCT^^^^2020-09-01|||||||||20201102063552-0500|20201102063552-0500"""
    val reg = "[\r\n]".toRegex()
    var cleanedMessage = reg.replace(strMessage, "\r")
    val outputDir = "./build/tmp/tmp.hl7"

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        val sender = hl7Sender
        val csv = """
            A,b
            1,2
        """.trimIndent()
        val foo = "*".repeat(60000000)
        val special_char = "!@#\\\\\\\$*()-_=+[]\\\\\\\\{};':,./<>?"
        val otcPairs = listOf(
            Pair("MISSING PATIENT LASTNAME", cleanedMessage.replace("Patlast", "")),
            Pair("MISSING ORDERING FACILITY STATE", cleanedMessage.replace("^IG^", "")),
            Pair("MISSING MESSAGE ID", cleanedMessage.replace("2.5.1", "")),
            Pair("MISSING TESTING LAB CLIA", cleanedMessage.replace("10D08761999", "")),
            Pair("MISSING PATIENT STATE", cleanedMessage.replace("^MD^", "^^")),
            Pair("INVALID DOB", cleanedMessage.replace("19670202", "19")),
            Pair("EMPTY", ""),
            Pair("CSV DATA", csv),
            Pair("Duplicate PID", cleanedMessage+"PID|||||Richards^Mary||19340428|F|||||||||||||||||||\\nPID|||||||19700510105000|M|||||||||||||||||||"),
            Pair("RANDOM TEXT", "foobar"),
            Pair("JSON", "{\"alive\": true}"),
            Pair("NON-UTF", "®"),
            Pair("EMOJI", "❤️ \uD83D\uDC94 \uD83D\uDC8C \uD83D\uDC95 \uD83D\uDC9E \uD83D\uDC93"),
            Pair("LARGE POST BODY", foo),
//            Pair("working", cleanedMessage),
//            Pair("QuickVue At-Home COVID-19 Test_Quidel Corporation", "OTC_PROCTORED_NYY"),
//            Pair("00810055970001", "OTC_PROCTORED_NUNKUNK"),
//            "ÅÍÎÏ˝ÓÔ\uF8FFÒÚÆ☃",
//            "OBR|1|A241Z^MESA_ORDPLC||P2^Procedure 2^ERL_MESA|||||||||xxx||Radiology^^^^R|7101^ESTRADA^JAIME^P^^DR|||||||||||1^once^^20000701^^R|||WALK|Modality Test 241||||||||||A||\n",
//            "PID|||||Richards^Mary||19340428|F|||||||||||||||||||\nPID|||||||19700510105000|M|||||||||||||||||||\n", /* multiple patient information*/
//            "~!@#\\\$^&*()-_=+[]\\\\{}|;':,./<>?",
//            "❤️ \uD83D\uDC94 \uD83D\uDC8C \uD83D\uDC95 \uD83D\uDC9E \uD83D\uDC93",
//            "<a href=\"javascript\\x0A:javascript:alert(1)\" id=\"fuzzelement1\">test</a>",
//            "'; EXEC sp_MSForEachTable 'DROP TABLE ?'; --",
//            "1'000'000,00"
        )

        for(pair in otcPairs) {
            val reFile = File(outputDir)
            val isNewFileCreated :Boolean = reFile.createNewFile()
            if(!isNewFileCreated){
                reFile.writeText("")
            }

            Files.write(reFile.toPath(), (pair.second).toByteArray(), StandardOpenOption.APPEND)

            if (!reFile.exists()) {
                error("Unable to find file ${reFile.absolutePath} to do badhl7 test")
            }

            val (responseCode, jsonResponse) = HttpUtilities.postReportFile(
                environment,
                reFile,
                sender,
                options.key
            )
            TermUi.echo("ResponseCode to POST: $responseCode")
//            TermUi.echo("Response to POST: $jsonResponse")
            val tree = jacksonObjectMapper().readTree(jsonResponse)
            if (responseCode >= 400 && tree["id"].isNull  && tree["errorCount"].intValue()!=0) {
                good("Test of Bad HL7 file ${pair.first} passed: Failure HttpStatus code was returned.")
            } else {
                bad("***badhl7 Tes of ${pair.first} FAILED: Expecting a failure HttpStatus. ***")
                failures.add("${pair.first}")
            }
        }

        if( failures.size == 0 ) {
            return true
        } else {
            return bad( "Tests FAILED: "+ failures)
        }
    }
}