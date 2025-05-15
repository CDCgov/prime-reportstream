package gov.cdc.prime.router.fhirengine.azure

import gov.cdc.prime.reportstream.shared.Submission
import gov.cdc.prime.router.azure.SubmissionTableService
import gov.cdc.prime.router.azure.TableAccess
import gov.cdc.prime.router.common.TestcontainersUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.assertNotNull

@Testcontainers
class SubmissionTableServiceIntegrationTests {

    @Container
    val azuriteContainer = TestcontainersUtils.createAzuriteContainer(
        customImageName = "azurite_fhirreceiverintegration",
        customEnv = mapOf(
            "AZURITE_ACCOUNTS" to "devstoreaccount1:keydevstoreaccount1"
        )
    )

    private lateinit var submissionTableService: SubmissionTableService

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()

        mockkObject(TableAccess)
        every { TableAccess.getConnectionString() } returns getConnString()

        submissionTableService = SubmissionTableService.getInstance()
        submissionTableService.reset()
    }

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    private fun getConnString(): String {
        @Suppress("ktlint:standard:max-line-length")
        return """DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=keydevstoreaccount1;BlobEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10000)}/devstoreaccount1;QueueEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10001)}/devstoreaccount1;TableEndpoint=http://${azuriteContainer.host}:${azuriteContainer.getMappedPort(10002)}/devstoreaccount1;"""
    }

    /**
     * Test to ensure that multiple submissions can be inserted and retrieved correctly
     * in a thread-safe manner.
     *
     * 10 different submissions will be inserted concurrently, followed by retrievals to ensure
     * that all submissions were properly stored and retrieved from the "submission" table.
     */
    @Test
    @Ignore
    fun `test concurrent reset and submissions with simple threads`() {
        // List to hold submission objects
        val submissions = List(10) {
            val submissionId = UUID.randomUUID().toString()
            val status = "Accepted"
            val url = "https://anyblob.com"
            Submission(submissionId, status, url) // Create new Submission instance
        }

        // List to hold the created threads
        val threads = mutableListOf<Thread>()

        // Create and start 50 threads
        for (i in 1..50) {
            val thread = Thread {
                // Each thread randomly picks one of the submissions to insert
                val submission = submissions.random()

                try {
                    submissionTableService.insertSubmission(submission)
                    submissionTableService.reset() // Reset `tableAccess`
                    println("Thread ${Thread.currentThread().name} inserted and reset successfully")
                } catch (e: Exception) {
                    println("Thread ${Thread.currentThread().name} encountered an error: ${e.message}")
                }
            }
            thread.start()
            threads.add(thread) // Keep track of the thread
        }

        // Wait for all threads to complete using `join()`
        for (thread in threads) {
            thread.join()
        }

        // After all threads complete, verify that submissions were inserted successfully
        submissions.forEach { submission ->
            val retrievedSubmission = submissionTableService.getSubmission(submission.submissionId, submission.status)
            assertNotNull(retrievedSubmission, "Submission should not be null")
            assertEquals(submission.submissionId, retrievedSubmission.submissionId)
            assertEquals(submission.status, retrievedSubmission.status)
        }

        println("Test passed! Concurrent submission and reset operations are thread-safe.")
    }
}