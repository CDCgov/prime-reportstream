package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.common.Environment
import java.net.HttpURLConnection

class TestSubmissionsAPI : CoolTest() {
    override val name = "end2end-submission"
    override val description = "Create fake data, submit, wait, confirm records via submission API."
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        initListOfGoodReceiversAndCounties(environment)
        var passed = true
        ugly("Starting $name Test: send ${simpleRepSender.fullName} data to $allGoodCounties")
        val fakeItemCount = allGoodReceivers.size * options.items
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            allGoodCounties,
            options.dir,
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(environment, file, simpleRepSender, options.key)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***end2end Test FAILED***:  response code $responseCode")
            passed = false
        } else {
            good("Posting of report succeeded with response code $responseCode")
        }
        echo(json)

        passed = passed and examineResponse(json)

        val actionId = getReportIdFromResponse(json)
        if (actionId != null) {
            passed = passed and pollForLineageResults(reportId, allGoodReceivers, fakeItemCount)
        }


        val reportId = getReportIdFromResponse(json)
        if (reportId != null) {
            passed = passed and pollForLineageResults(reportId, allGoodReceivers, fakeItemCount)
        }

        return passed
    }
}