package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.Options
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.cli.FileUtilities
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
        environment: ReportStreamEnv,
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
                            val (responseCode, json) =
                                HttpUtilities.postReportFile(
                                    environment,
                                    file,
                                    simulation.sender,
                                    options.key,
                                    (if (simulation.doBatchAndSend) null else Options.SkipSend)
                                )
                            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                                echo(json)
                                result.passed =
                                    bad("$threadNum: ***Parallel Test FAILED***:  response code $responseCode")
                            } else {
                                val reportId = getReportIdFromResponse(json)
                                if (reportId == null) {
                                    result.passed = bad("$threadNum: ***Parallel Test FAILED***:  No reportId.")
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

    fun setup(environment: ReportStreamEnv, options: CoolTestOptions) {
        ugly("Starting $name test.")
        val result = runOneSimulation(primeThePump, environment, options)
        if (!result.passed) {
            bad("$name test FAILED to primeThePump prior to actual test.")
            error("Can't continue to test")
        }
        echo("Ready for the real test:")
    }

    fun teardown(results: List<SimulatorResult>, entireTestMillis: Long): Boolean {
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
        val passed = results.map { it.passed }.reduce { acc, passed -> acc and passed } // any single fail = failed test
        if (passed) {
            good(summary)
        } else {
            bad(summary)
        }
        return passed
    }

    /**
     * Meant to simulate a production load, minus strac since its just once a day.
     * Runs in a couple mins.
     */
    fun productionSimulation(environment: ReportStreamEnv, options: CoolTestOptions): List<SimulatorResult> {
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
    fun parallel(environment: ReportStreamEnv, options: CoolTestOptions): List<SimulatorResult> {
        ugly("A test mimics the old 'parallel' test.  Runs 1,5,10,20 threads.")
        val results = arrayListOf<SimulatorResult>()
        results += runOneSimulation(oneThreadX50, environment, options)
        results += runOneSimulation(fiveThreadsX50, environment, options)
        results += runOneSimulation(tenThreadsX50, environment, options)
        results += runOneSimulation(twentyThreadsX50, environment, options)
        return results
    }

    override suspend fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        setup(environment, options)
        val results = mutableListOf<SimulatorResult>()
        var elapsedTime = measureTimeMillis {
            results += productionSimulation(environment, options)
            results += productionSimulation(environment, options)
            results += runOneSimulation(typicalStracSubmission, environment, options) // strac
            results += productionSimulation(environment, options)
            results += productionSimulation(environment, options)
            results += productionSimulation(environment, options)
        }
        return teardown(results, elapsedTime)
    }
}