package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.ReportFunction
import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.cli.FileUtilities
import java.net.HttpURLConnection
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class Simulator : CoolTest() {
    override val name = "simulation"
    override val description = "Simulate a pattern of submissions to RS. "
    override val status = TestStatus.LOAD

    data class Simulation(
        val name: String,
        val numThreads: Int,
        val numSubmissionsPerThread: Int,
        val numItemsPerSubmission: Int,
        val targetStates: String,
        val targetReceiverNames: String, // redundant with targetStates.  Fix this.
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
                "Target Receivers:\t$targetReceiverNames\n" +
                "Delay between Submits:\t$millisBetweenSubmissions millis\n" +
                "Sending From:\t${sender.fullName}\n" +
                "Do Batch/Send too:\t$doBatchAndSend\n"
        }
    }

    data class SimulatorResult(
        var passed: Boolean = true,
        var totalMillisAllSubmissions: Long = 0,
        var totalSubmissionsCount: Int = -1,
        var avgSecsPerSubmissionString: String = "error",
        var rateString: String = "error",
        var elapsedMillisForWholeSimulation: Long = -1,
    ) {

        override fun toString(): String {
            return "Simulation Results: \n" +
                "Passed:$passed\n" +
                "Total Submissions:\t$totalSubmissionsCount\n" +
                "Total Seconds All Submissions:\t${totalMillisAllSubmissions / 1000} seconds\n" +
                "Elapsed Seconds for Simulation:\t${elapsedMillisForWholeSimulation} seconds\n" +
                "Processing Rate:\t$rateString submissions/second\n" +
                "Avg Speed per submission:\t$avgSecsPerSubmissionString seconds/item\n"
        }
    }

    fun runOneSimulation(
        params: Simulation,
        environment: ReportStreamEnv,
        options: CoolTestOptions
    ): SimulatorResult {
        var result = SimulatorResult()
        val file = FileUtilities.createFakeFile(
            CoolTest.metadata,
            CoolTest.settings,
            params.sender,
            params.numItemsPerSubmission,
            params.targetStates,
            params.targetReceiverNames,
            options.dir,
        )
        echo("Created datafile $file")
        val threads = mutableListOf<Thread>()
        ugly("$params")
        result.elapsedMillisForWholeSimulation = measureTimeMillis {
            for (threadNum in 1..params.numThreads) {
                val th = thread {
                    for (i in 1..params.numSubmissionsPerThread) {
                        val elapsedMillisOneSubmission = measureTimeMillis {
                            val (responseCode, json) =
                                HttpUtilities.postReportFile(
                                    environment,
                                    file,
                                    params.sender,
                                    options.key,
                                    (if (params.doBatchAndSend) null else ReportFunction.Options.SkipSend)
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
                        result.totalMillisAllSubmissions += elapsedMillisOneSubmission // hrm.  Not threadsafe.
                        Thread.sleep(params.millisBetweenSubmissions) // not counted as  part of elapsed time.
                        print(".")
                    }
                }
                threads.add(th)
            }
            threads.forEach { it.join() }
        }
        result.totalSubmissionsCount = params.numThreads * params.numSubmissionsPerThread
        result.avgSecsPerSubmissionString = String.format(
            "%.2f",
            (result.totalMillisAllSubmissions / 1000.0) / result.totalSubmissionsCount
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

    companion object {
        val primeThePump = Simulation(
            "Prime The Pump : Make sure Azure Functions are running",
            1,
            2,
            1,
            "HL7_NULL",
            "ignore.HL7_NULL",
            0,
            CoolTest.stracSender,
            true
        )

        val oneThread = Simulation(
            "1 Thread : Submit 50 tests as fast as possible, on 1 thread.",
            1,
            50,
            1,
            "HL7_NULL",
            "ignore.HL7_NULL",
            0,
            CoolTest.stracSender,
            true
        )
        val fiveThreads = Simulation(
            "5 Threads : Submit 50X5 = 250 tests as fast as possible, across 5 threads.",
            5,
            50,
            1,
            "HL7_NULL",
            "ignore.HL7_NULL",
            0,
            CoolTest.stracSender,
            true
        )
        val tenThreads = Simulation(
            "10 Threads : Submit 50X10 = 500 tests as fast as possible, across 10 threads.",
            10,
            50,
            1,
            "HL7_NULL",
            "ignore.HL7_NULL",
            0,
            CoolTest.stracSender,
            true
        )
    }

    override suspend fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting $name test.")
        echo("Priming the Pump by submitting twice")
        var result = runOneSimulation(primeThePump, environment, options)
        var passed = result.passed
        echo("Ready for the real test:")
        result = runOneSimulation(oneThread, environment, options)
        result = runOneSimulation(fiveThreads, environment, options)
        passed = passed and result.passed
        return passed
    }
}