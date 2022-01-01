package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import gov.cdc.prime.router.REPORT_MAX_ITEMS
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.common.Environment
import java.net.HttpURLConnection
import kotlin.system.measureTimeMillis

/**
 * This file consists of all our load tests.
 * As of this writing, they all instantiate [LoadTestSimulator], a utility class for implementing load tests.
 */

/**
 * Very simple test.
 * Also, you can use this as a smoke test for the [LoadTestSimulator].
 */
class QuickLoad : LoadTestSimulator() {
    override val name = "quick-load"
    override val description = "A quick load test just to make sure the darn load tester is working."
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        setup(environment, options)
        val afterActionId = getMostRecentActionId()
        val results = mutableListOf<SimulatorResult>()
        var elapsedTime = measureTimeMillis {
            results += runOneSimulation(quick_simulation, environment, options)
        }
        return teardown(results, elapsedTime, afterActionId, options.asyncProcessMode)
    }
}

class ProductionLoad : LoadTestSimulator() {
    override val name = "production-load"
    override val description = "Simulate a Production Load"
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        setup(environment, options)
        val afterActionId = getMostRecentActionId()
        println("Max action id: $afterActionId")
        val results = mutableListOf<SimulatorResult>()
        var elapsedTime = measureTimeMillis {
            results += productionSimulation(environment, options)
            results += productionSimulation(environment, options)
            results += runOneSimulation(typicalStracSubmission, environment, options) // strac
            results += productionSimulation(environment, options)
            results += productionSimulation(environment, options)
            results += productionSimulation(environment, options)
        }
        return teardown(results, elapsedTime, afterActionId, options.asyncProcessMode)
    }

    /**
     * Runs a large batch test to see what throughput is like when we have multiple batches of 1000
     */
    private fun bigBatchTest(environment: Environment, options: CoolTestOptions): List<SimulatorResult> {
        ugly("Sends a large batch batch time is set to 60 minutes.")
        val results = arrayListOf<SimulatorResult>()
        results += runOneSimulation(twentyThread_bigBatch, environment, options)
//        results += runOneSimulation(twentyThread_bigBatch, environment, options)
//        results += runOneSimulation(twentyThread_bigBatch, environment, options)
//        results += runOneSimulation(twentyThread_bigBatch, environment, options)
//        results += runOneSimulation(twentyThread_bigBatch, environment, options)
//        results += runOneSimulation(twentyThread_bigBatch, environment, options)
        return results
    }
}

class DbConnectionsLoad : LoadTestSimulator() {
    override val name = "dbconnections-load"
    override val description = "Attempt to swamp ReportStream with data."
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        setup(environment, options)
        val afterActionId = getMostRecentActionId()
        val results = mutableListOf<SimulatorResult>()
        var elapsedTime = measureTimeMillis {
            results += runOneSimulation(twentyThreadsX100, environment, options)
            results += runOneSimulation(twentyThreadsX100, environment, options)
            results += runOneSimulation(twentyThreadsX100, environment, options)
            results += runOneSimulation(twentyThreadsX100, environment, options)
            results += runOneSimulation(twentyThreadsX100, environment, options)
        }
        return teardown(results, elapsedTime, afterActionId, options.asyncProcessMode)
    }
}

class LongLoad : LoadTestSimulator() {
    override val name = "long-load"
    override val description = "A very long high load test"
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        setup(environment, options)
        val afterActionId = getMostRecentActionId()
        val results = mutableListOf<SimulatorResult>()
        var elapsedTime = measureTimeMillis {
            repeat(10) {
                results += productionSimulation(environment, options)
                results += productionSimulation(environment, options)
                results += runOneSimulation(typicalStracSubmission, environment, options) // strac
                results += productionSimulation(environment, options)
                results += productionSimulation(environment, options)
                results += productionSimulation(environment, options)
            }
        }
        return teardown(results, elapsedTime, afterActionId, options.asyncProcessMode)
    }
}

class Huge : CoolTest() {
    override val name = "huge"
    override val description = "Submit $REPORT_MAX_ITEMS line csv file, wait, confirm via db.  Slow."
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val fakeItemCount = REPORT_MAX_ITEMS
        ugly("Starting huge Test: Attempting to send a report with $fakeItemCount items. This is terrapin slow.")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            csvReceiver.name,
            options.dir,
            Report.Format.CSV
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                simpleRepSender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            bad("***Huge Test FAILED***:  response code $responseCode")
        }
        val reportId = getReportIdFromResponse(json)
            ?: return bad("***$name Test FAILED***: A report ID came back as null")
        echo("Id of submitted report: $reportId")
        waitABit(30, environment)
        return pollForLineageResults(
            reportId = reportId,
            receivers = listOf(csvReceiver),
            totalItems = fakeItemCount,
            asyncProcessMode = options.asyncProcessMode
        )
    }
}

// Bigger than Huge
class TooBig : CoolTest() {
    override val name = "toobig"
    override val description = "Submit ${REPORT_MAX_ITEMS + 1} lines, which should be an error.  Slower ;)"
    override val status = TestStatus.LOAD

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        val fakeItemCount = REPORT_MAX_ITEMS + 1
        ugly("Starting toobig test: Attempting to send a report with $fakeItemCount items. This is slllooooowww.")
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simpleRepSender,
            fakeItemCount,
            receivingStates,
            csvReceiver.name,
            options.dir,
            Report.Format.CSV
        )
        echo("Created datafile $file")
        // Now send it to ReportStream.
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                simpleRepSender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        echo(json)
        try {
            val tree = jacksonObjectMapper().readTree(json)
            val firstError = (tree["errors"][0]) as ObjectNode
            if (firstError["details"].textValue().contains("rows")) {
                return good("toobig Test passed.")
            } else {
                return bad("***toobig Test Test FAILED***: Did not find the error")
            }
        } catch (e: Exception) {
            return bad("***toobig Test FAILED***: Unable to parse json response")
        }
    }
}