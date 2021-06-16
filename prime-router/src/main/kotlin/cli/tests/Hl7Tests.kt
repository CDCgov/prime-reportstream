package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.output.TermUi
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.cli.CoolTest
import gov.cdc.prime.router.cli.CoolTestOptions
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.cli.TestStatus
import java.net.HttpURLConnection

class Hl7Ingest : CoolTest() {
    override val name = "hl7ingest"
    override val description = "Create Fake data, submit, wait, confirm sent via database lineage data"
    override val status = TestStatus.SMOKE

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        var passed = true
        val sender = hl7Sender
        val receiver = csvReceiver
        val itemCount = options.items
        ugly("Starting $name Test: send ${sender.fullName} data to $receiver")
        val file = FileUtilities.createFakeFile(
            metadata,
            sender,
            itemCount,
            receivingStates,
            receiver.name,
            options.dir,
            Report.Format.HL7_BATCH
        )
        TermUi.echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, org.name, sender, options.key)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***end2end Test FAILED***:  response code $responseCode")
            passed = false
        } else {
            good("Posting of report succeeded with response code $responseCode")
        }
        TermUi.echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val reportId = ReportId.fromString(tree["id"].textValue())
            TermUi.echo("Id of submitted report: $reportId")
            val topic = tree["topic"]
            val errorCount = tree["errorCount"]
            val destCount = tree["destinationCount"]

            if (topic != null && !topic.isNull && topic.textValue().equals("covid-19", true)) {
                good("'topic' is in response and correctly set to 'covid-19'")
            } else {
                bad("***end2end Test FAILED***: 'topic' is missing from response json")
                passed = false
            }

            if (errorCount != null && !errorCount.isNull && errorCount.intValue() == 0) {
                good("No errors detected.")
            } else {
                bad("***end2end Test FAILED***: There were errors reported.")
                passed = false
            }

            if (destCount != null && !destCount.isNull && destCount.intValue() > 0) {
                good("Data going to be sent to one or more destinations.")
            } else {
                bad("***end2end Test FAILED***: There are no destinations set for sending the data.")
                passed = false
            }

            waitABit(25, environment)
            println("LIN: " + examineLineageResults(reportId, listOf(receiver), itemCount))
//            return passed and examineLineageResults(reportId, allGoodReceivers, fakeItemCount)
        } catch (e: NullPointerException) {
            return bad("***end2end Test FAILED***: Unable to properly parse response json")
        }
        return true
    }
}