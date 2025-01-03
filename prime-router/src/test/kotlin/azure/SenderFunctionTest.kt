package gov.cdc.prime.router.azure

import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.SettingsProvider
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.UniversalPipelineSender
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SenderFunctionTest {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val metadata = UnitTestUtils.simpleMetadata
    val settings = mockkClass(SettingsProvider::class)
    val serializer = spyk(Hl7Serializer(metadata, settings))
    val blobMock = mockkClass(BlobAccess::class)
    val queueMock = mockkClass(QueueAccess::class)
    val timing1 = mockkClass(Receiver.Timing::class)

    val testOrganization = DeepOrganization(
        "phd",
        "test",
        Organization.Jurisdiction.FEDERAL,
        receivers = listOf(
            Receiver(
                "elr",
                "phd",
                Topic.TEST,
                CustomerStatus.INACTIVE,
                "one",
                timing = timing1
            )
        )
    )

    private fun makeEngine(metadata: Metadata, settings: SettingsProvider): WorkflowEngine = spyk(
        WorkflowEngine.Builder().metadata(metadata).settingsProvider(settings).databaseAccess(accessSpy)
            .blobAccess(blobMock).queueAccess(queueMock).hl7Serializer(serializer).build()
    )

    @BeforeEach
    fun reset() {
        clearAllMocks()

        // setup
        every { timing1.isValid() } returns true
        every { timing1.numberPerDay } returns 1
        every { timing1.maxReportCount } returns 1
        every { timing1.whenEmpty } returns Receiver.WhenEmpty()
    }

    @Test
    fun `test SenderFunction conditionCodeComparisonPostRequest ok`() {
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(testOrganization)
        val sender = UniversalPipelineSender(
            name = "Test Sender",
            organizationName = "testOrganization",
            format = MimeFormat.HL7,
            topic = Topic.FULL_ELR
        )

        val workflowEngine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val senderFunction = spyk(SenderFunction(workflowEngine, actionHistory))

        val testRequest = MockHttpRequestMessage("test")

        every { workflowEngine.settings.findSender("Test Sender") } returns sender

        testRequest.httpHeaders += mapOf(
            "client" to "Test Sender",
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assert(response.statusCode == 200)
    }

    @Test
    fun `test SenderFunction conditionCodeComparisonPostRequest with no sender`() {
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(testOrganization)

        val workflowEngine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val senderFunction = spyk(SenderFunction(workflowEngine, actionHistory))

        val testRequest = MockHttpRequestMessage("test")

        testRequest.httpHeaders += mapOf(
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assert(response.statusCode == 400)
    }

    @Test
    fun `test SenderFunction conditionCodeComparisonPostRequest with bad sender`() {
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(testOrganization)

        val workflowEngine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val senderFunction = spyk(SenderFunction(workflowEngine, actionHistory))

        val testRequest = MockHttpRequestMessage("test")

        every { workflowEngine.settings.findSender("Test sender") } returns null

        testRequest.httpHeaders += mapOf(
            "client" to "Test sender",
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assert(response.statusCode == 400)
    }
}