package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.common.Environment
import kotlinx.coroutines.delay
import org.jooq.impl.DSL
import java.io.File
import java.net.HttpURLConnection
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

/**
 * Simulator is intended to simulate a wide variety of heavy load situations.
 *
 * todo Separate this into a useful Utilities class, so that many different simulation tests can use it.
 */
class Simulator : CoolTest() {
    override val name = "simulator"
    override val description = "Simulate a pattern of submissions to RS. "
    override val status = TestStatus.LOAD

    /**
     * Input parameters to be sent to [runOneSimulation], representing a pattern of input submissions to ReportStream.
     */
    data class Simulation(
        val name: String,
        val numThreads: Int,
        val numSubmissionsPerThread: Int,
        val numItemsPerSubmission: Int,
        val targetStates: String,
        val targetCounties: String, // redundant with targetReceiverNames.
        val targetReceiverNames: String,
        val millisBetweenSubmissions: Long,
        val sender: Sender,
        val doBatchAndSend: Boolean = true,
    ) {
        override fun toString(): String {
            return "Simulation:\t$name\n" +
                "Simultaneous Threads:\t$numThreads\n" +
                "Submissions Per Thread:\t$numSubmissionsPerThread\n" +
                "Total Submissions:\t${numThreads * numSubmissionsPerThread}\n" +
                "Items Per Submission:\t$numItemsPerSubmission\n" +
                "Total Items Submitted:\t${numItemsPerSubmission * numThreads * numSubmissionsPerThread}\n" +
                "Target States:\t$targetStates\n" +
                "Target 'Counties':\t$targetCounties\n" +
                "Target Receivers:\t$targetReceiverNames\n" +
                "Delay between Submits:\t$millisBetweenSubmissions millis\n" +
                "Sending From:\t${sender.fullName}\n" +
                "Do Batch/Send too:\t$doBatchAndSend\n"
        }
    }

    /**
     * Result of a single run of [runOneSimulation]
     */
    data class SimulatorResult(
        val simulation: Simulation, // inputs to the simulation
        var passed: Boolean = true,
        var sumMillisAllSubmissions: Long = 0, // will be much longer than the elapsed clock time for the simulation.
        var totalSubmissionsCount: Int = -1,
        var totalItemsCount: Int = -1,
        var avgSecsPerSubmissionString: String = "error",
        var rateString: String = "error",
        var elapsedMillisForWholeSimulation: Long = -1,
    ) {

        override fun toString(): String {
            return "Simulation Results: \n" +
                "Passed:$passed\n" +
                "Total Submissions:\t$totalSubmissionsCount\n" +
                "Total Items Submitted:\t$totalItemsCount\n" +
                "Total Seconds All Submissions:\t${sumMillisAllSubmissions / 1000} seconds\n" +
                "Elapsed Seconds for Simulation:\t${elapsedMillisForWholeSimulation / 1000} seconds\n" +
                "Processing Rate:\t$rateString submissions/second\n" +
                "Avg Speed per submission:\t$avgSecsPerSubmissionString seconds/submission\n"
        }
    }

    /**
     * This is the main engine of the simulator: doing a single run, using the parametrics in [simulation].
     */
    fun runOneSimulation(
        simulation: Simulation,
        environment: Environment,
        options: CoolTestOptions
    ): SimulatorResult {
        var result = SimulatorResult(simulation)
        val file = FileUtilities.createFakeFile(
            metadata,
            settings,
            simulation.sender,
            simulation.numItemsPerSubmission,
            simulation.targetStates,
            simulation.targetCounties,
            options.dir,
        )
        echo("Created datafile $file")
        val threads = mutableListOf<Thread>()
        ugly("$simulation")
        result.elapsedMillisForWholeSimulation = measureTimeMillis {
            for (threadNum in 1..simulation.numThreads) {
                val th = thread {
                    for (i in 1..simulation.numSubmissionsPerThread) {
                        val elapsedMillisOneSubmission = measureTimeMillis {
                            var tryCount = 1
                            var responseCode: Int? = null
                            var json = ""

                            // try to send up to 3 times. Sometimes the azure environment rejects incoming records
                            //  (especially in staging)
                            while (tryCount <= 3) {
                                val (rCode, rJson) = sendReport(environment, file, simulation, options)
                                responseCode = rCode
                                json = rJson
                                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                                    break
                                } else {
                                    tryCount++
                                    println("Report submission failed, retry $tryCount")
                                }
                            }

                            // the message has either succeeded or we have tried 3 times and received failure
                            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                                echo(json)
                                result.passed =
                                    bad("$threadNum: ***Test FAILED***:  response code $responseCode")
                            } else {
                                val reportId = getReportIdFromResponse(json)
                                if (reportId == null) {
                                    result.passed = bad("$threadNum: ***Test FAILED***:  No reportId.")
                                }
                            }
                        }
                        result.sumMillisAllSubmissions += elapsedMillisOneSubmission // hrm.  Not threadsafe.
                        Thread.sleep(simulation.millisBetweenSubmissions) // not counted as  part of elapsed time.
                        print(".")
                    }
                }
                threads.add(th)
            }
            threads.forEach { it.join() }
        }
        result.totalSubmissionsCount = simulation.numThreads * simulation.numSubmissionsPerThread
        result.totalItemsCount = result.totalSubmissionsCount * simulation.numItemsPerSubmission
        result.avgSecsPerSubmissionString = String.format(
            "%.2f",
            (result.sumMillisAllSubmissions / 1000.0) / result.totalSubmissionsCount
        )
        result.rateString = String.format(
            "%.2f",
            result.totalSubmissionsCount.toDouble() / (result.elapsedMillisForWholeSimulation / 1000.0)
        )
        if (result.passed) {
            good("$result")
        } else {
            bad("$result")
        }
        return result
    }

    /**
     * Send a report
     */
    private fun sendReport(
        environment: Environment,
        file: File,
        simulation: Simulation,
        options: CoolTestOptions
    ): Pair<Int, String> {
        return HttpUtilities.postReportFile(
            environment,
            file,
            simulation.sender,
            options.asyncProcessMode,
            options.key,
            (if (simulation.doBatchAndSend) null else Options.SkipSend)
        )
    }

    /**
     * A library of useful [Simulation]
     */
    companion object {
        val primeThePump = Simulation(
            "primeThePump : Make sure Azure Functions are running by sending just two submissions",
            1,
            2,
            1,
            "IG",
            "HL7_NULL",
            "ignore.HL7_NULL",
            0,
            stracSender,
            false
        )

        val twentyThread_bigBatch = Simulation(
            "twentyThread_bigBatch: to be used by load testing.",
            20,
            5000,
            1,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            stracSender,
            true
        )

        val oneThreadX50 = Simulation(
            "oneThreadX50 : Submit 1X50 = 50 tests as fast as possible, on 1 thread.",
            1,
            50,
            1,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            stracSender,
            true
        )
        val fiveThreadsX50 = Simulation(
            "fiveThreadsX50: Submit 5X50 = 250 tests as fast as possible, across 5 threads.",
            5,
            50,
            1,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            stracSender,
            true
        )
        val fiveThreadsX100 = Simulation( // Meant to simulate a high load from a single-test sender
            "fiveThreadsX100 : Submit 5X100 = 500 tests as fast as possible, across 2 threads.",
            5,
            100,
            1,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            stracSender,
            true
        )
        val tenThreadsX50 = Simulation(
            "tenThreadsX50 : Submit 10X50 = 500 tests as fast as possible, across 10 threads.",
            10,
            50,
            1,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            stracSender,
            true
        )
        val twentyThreadsX50 = Simulation(
            "twentyThreadsX50 : Submit 20X50 = 1000 tests as fast as possible, across 20 threads.",
            20,
            50,
            1,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            stracSender,
            true
        )
        val spike = Simulation( // Simulate a sudden spike in sends all at once
            "Spike: 50 Threads each submitting once.",
            50,
            1,
            1,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            stracSender,
            true
        )
        val typicalStracSubmission = Simulation(
            "typicalStracSubmission: 1 thread submitting 500 items a bunch of times in quick succession.",
            1,
            10,
            500,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            stracSender,
            true
        )
        val simpleReport = Simulation( // Simulate a single large SimpleReport Send
            "simpleReport: One submission SimpleReport data, 1000 Items",
            1,
            1,
            1000,
            "IG",
            "EVERY_5_MINS",
            "ignore.EVERY_5_MINS",
            0,
            simpleRepSender,
            true
        )
    }

    fun setup(environment: Environment, options: CoolTestOptions) {
        ugly("Starting $name test.")
        val result = runOneSimulation(primeThePump, environment, options)
        if (!result.passed) {
            bad("$name test FAILED to primeThePump prior to actual test.")
            error("Can't continue to test")
        }
        echo("Ready for the real test:")
    }

    private suspend fun teardown(
        results: List<SimulatorResult>,
        entireTestMillis: Long,
        afterActionId: Int,
        isAsyncProcessMode: Boolean
    ): Boolean {
        val totalSubmissions = results.map { it.totalSubmissionsCount }.sum()
        val totalItems = results.map { it.totalItemsCount }.sum()
        val totalTime = results.map { it.elapsedMillisForWholeSimulation }.sum()
        val submissionRateString = String.format("%.2f", totalSubmissions.toFloat() / (totalTime / 1000.0))
        val itemsPerSecond: Double = totalItems.toDouble() / (totalTime / 1000.0)
        val itemRateString = String.format("%.2f", itemsPerSecond)
        val summary = "Simulation Done.   Summary:\n" +
            "Total Submissions submitted in Simulation runs:\t$totalSubmissions\n" +
            "Total Items submitted in Simulation runs:\t$totalItems\n" +
            "Total Millis for Simulation runs:\t$totalTime\n" +
            "Overall Submission Rate:\t$submissionRateString submissions/second\n" +
            "Overall Item Rate:\t$itemRateString items/second\n" +
            "Predicted Items per hour:\t${(itemsPerSecond * 3600.0).toInt()} items/hour\n" +
            "Total seconds for the entire simulation:\t${entireTestMillis / 1000} "
        var passed = results.map { it.passed }.reduce { acc, passed -> acc and passed } // any single fail = failed test
        if (passed) {
            good(summary)
        } else {
            bad(summary)
        }

        val receivingOrg = results.first().simulation.targetReceiverNames.split('.')[0]
        val receivingOrgSvc = results.first().simulation.targetReceiverNames.split('.')[1]
        println("")
        println("Simulator run complete. Verifying data - this could take up to 30 minutes.")

        echo("==== Verifying data from simulator. ====")
        // calculate expected number of items we should be looking for. More than this may be found if there is test
        //  overlap or more than one person is running this test against the same environment simultaneously
        val expectedResults = results.sumOf {
            it.simulation.numItemsPerSubmission *
                it.simulation.numSubmissionsPerThread *
                it.simulation.numThreads
        }
        println("Expecting $expectedResults total items. More than this may be found if other test")
        // if we are running in async mode, verify the correct number of 'process' records have been generated
        if (isAsyncProcessMode) {
            passed = passed && checkTimedResults(
                expectedResults,
                afterActionId,
                TaskAction.process,
                receivingOrg,
                receivingOrgSvc,
                maxPollSecs = 600
            )
        }

        // poll for batch results - wait for up to 7 minutes
        // TODO: Will this always be 1 batch? Should it determine results count based on what tests were run?
        // TODO: Should this dynamically determine how long to wait in case of 60_MIN receiver?
        passed = passed && checkTimedResults(
            expectedResults,
            afterActionId,
            TaskAction.batch,
            receivingOrg,
            receivingOrgSvc,
            maxPollSecs = 600
        )

        // poll for send results - wait for up to 7 minutes
        // TODO: Will this always be 1 batch? Should it determine results count based on what tests were run?
        // TODO: Should this dynamically determine how long to wait in case of 60_MIN receiver?
        passed = passed && checkTimedResults(
            expectedResults,
            afterActionId,
            TaskAction.send,
            receivingOrg,
            receivingOrgSvc,
            maxPollSecs = 600
        )

        return passed
    }

    /**
     * Meant to simulate a production load, minus strac since its just once a day.
     * Runs in a couple mins.
     */
    fun productionSimulation(environment: Environment, options: CoolTestOptions): List<SimulatorResult> {
        ugly("A test that simulates a high daytime load in Production")
        val results = arrayListOf<SimulatorResult>()
        results += runOneSimulation(fiveThreadsX100, environment, options) // cue
        results += runOneSimulation(simpleReport, environment, options) // simple_report
        results += runOneSimulation(fiveThreadsX100, environment, options) // more cue
        results += runOneSimulation(fiveThreadsX100, environment, options) // more cue
        results += runOneSimulation(spike, environment, options) // a big spike of cue
        results += runOneSimulation(fiveThreadsX100, environment, options) // more regular cue
        return results
    }

    /**
     * This set of [Simulation] is meant to mimic the old "parallel" test, but going bigger, and skipping some.
     */
    fun parallel(environment: Environment, options: CoolTestOptions): List<SimulatorResult> {
        ugly("A test mimics the old 'parallel' test.  Runs 1,5,10,20 threads.")
        val results = arrayListOf<SimulatorResult>()
        results += runOneSimulation(oneThreadX50, environment, options)
        results += runOneSimulation(fiveThreadsX50, environment, options)
        results += runOneSimulation(tenThreadsX50, environment, options)
        results += runOneSimulation(twentyThreadsX50, environment, options)
        return results
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

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        setup(environment, options)
        val afterActionId = getMostRecentActionId()
        println("Max action id: $afterActionId")

//        val results = mutableListOf<SimulatorResult>()
//
//        var elapsedTime = measureTimeMillis {
//            results += runOneSimulation(oneThreadX50, environment, options)
//            results += bigBatchTest(environment, options)
//        }

        // dev tests only. uncomment what you need to use when testing
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
     * Checks that at least the correct number of process records are in the Action table after running simulator.
     * There may be more than expected if other tests occur at the same time, but there should not be fewer.
     */
    private suspend fun checkTimedResults(
        expectedResults: Int,
        afterActionId: Int,
        taskToCheck: TaskAction,
        receivingOrg: String,
        receivingOrgService: String,
        maxPollSecs: Int = 180,
        pollSleepSecs: Int = 10
    ): Boolean {
        var resultsFound = 0

        var timeElapsedSecs = 0
        println("Polling for $expectedResults $taskToCheck items.  (Max poll time $maxPollSecs seconds)")
        val actualTimeElapsedMillis = measureTimeMillis {
            while (timeElapsedSecs <= maxPollSecs) {
                if (outputToConsole) {
                    for (i in 1..pollSleepSecs) {
                        delay(1000)
                        // Print out some contemplative dots to show we are waiting.
                        print(".")
                    }
                    echo()
                } else {
                    delay(pollSleepSecs.toLong() * 1000)
                }
                timeElapsedSecs += pollSleepSecs

                resultsFound = checkResultsQuery(afterActionId, taskToCheck, receivingOrg, receivingOrgService)

                if (resultsFound >= expectedResults) {
                    println("Found $resultsFound $taskToCheck items, finished looking.")
                    break
                } else
                    println("Found $resultsFound $taskToCheck items, checking again in $pollSleepSecs seconds")
            }
        }
        echo("Polling for $taskToCheck records finished in ${actualTimeElapsedMillis / 1000 } seconds")

        // verify results found count is greater than or equal to expected number
        val passed = resultsFound >= expectedResults

        if (passed) {
            good("Found at $resultsFound $taskToCheck items.")
        } else {
            bad("Did not find at least $expectedResults $taskToCheck items.")
        }

        return passed
    }

    private fun getMostRecentActionId(): Int {
        var actionId = 0
        db = WorkflowEngine().db
        db.transact { txn ->
            val ctx = DSL.using(txn)

            val sql = """select max(action_id)
                from action
              """

            actionId = ctx.fetchOne(
                sql
            )!!.into(Int::class.java)
        }
        return actionId
    }

    private fun checkResultsQuery(
        afterActionId: Int,
        actionType: TaskAction,
        receivingOrg: String,
        receivingOrgService: String
    ): Int {
        var itemsFound = 0
        db = WorkflowEngine().db
        db.transact { txn ->
            val ctx = DSL.using(txn)

            val sql = """select sum(rf.item_count)
              from  report_file as RF
              join action as A on A.action_id = RF.action_id
              where A.action_name = ?
              and a.action_id >= ?
              and rf.receiving_org = ?
              and rf.receiving_org_svc = ?
              """

            itemsFound = ctx.fetchOne(
                sql,
                actionType,
                afterActionId,
                receivingOrg,
                receivingOrgService
            )!!.into(Int::class.java)
        }
        return itemsFound
    }
}