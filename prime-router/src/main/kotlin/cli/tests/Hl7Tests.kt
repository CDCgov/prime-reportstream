// ktlint-disable filename
package gov.cdc.prime.router.cli.tests

import com.github.ajalt.clikt.output.TermUi
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.cli.FileUtilities
import java.net.HttpURLConnection
import java.io.File
import gov.cdc.prime.router.azure.WorkflowEngine

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

class Hl7End2End : CoolTest() {
    override val name = "hl7end2end"
    override val description = "This tests end-to-end: submits a minimal hl7 file and verifies the file on sftp matches an expected file"
    override val status = TestStatus.SMOKE

    override fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        var passed = false
        val sender = hl7Sender
        val filename = "minimal.hl7"
        ugly("Starting minimal file Test submitting with $filename")

        val inputFile = File("./src/test/hl7_test_files/input/$filename")
        val expectedFile = File("./src/test/hl7_test_files/expected/$filename")
        if (!inputFile.exists()) {
            error("Unable to find file ${inputFile.absolutePath} to do minimal hl7")
        }
        if (!expectedFile.exists()) {
            error("Unable to find file ${expectedFile.absolutePath} to do minimal hl7")
        }

        val (responseCode, json) = HttpUtilities.postReportFile(
            environment,
            inputFile,
            sender,
            options.key
        )
        TermUi.echo("Response to POST: $responseCode")

        waitABit(10, environment)
        db = WorkflowEngine().db
        val receiverName = hl7Receiver.name
        val reportId = getReportIdFromResponse(json)
            ?: return bad("***$name Test FAILED***: A report ID came back as null")

        TermUi.echo("Id of submitted report: $reportId")
        db.transact { txn ->
            val sftpFilename = sftpFilenameQuery(txn, reportId, receiverName)
            // If we get a file, compare the content with expected file.
            TermUi.echo("File on sftp server is: $sftpFilename")
            if (sftpFilename != null) {
                val compareResult = CompareHl7Data().compareHl7(
                    expectedFile.inputStream(),
                    File(options.sftpDir, sftpFilename).inputStream())

                if(compareResult.errors.size == 0){
                    passed = true
                }
            }
        }

        return passed
    }
}