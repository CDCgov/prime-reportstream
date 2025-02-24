package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpStatus
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
import gov.cdc.prime.router.cli.LookupTableCompareMappingCommand
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.metadata.ObservationMappingConstants
import gov.cdc.prime.router.serializers.Hl7Serializer
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SenderFunctionTest {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)
    val accessSpy = spyk(DatabaseAccess(connection))
    val metadata = UnitTestUtils.simpleMetadata
    val settings = mockkClass(SettingsProvider::class)
    val blobMock = mockkClass(BlobAccess::class)
    private val serializer = spyk(Hl7Serializer(metadata, settings))
    private val queueMock = mockkClass(QueueAccess::class)
    private val timing1 = mockkClass(Receiver.Timing::class)

    val REQ_BODY_TEST_CSV = "test code,test description,coding system\n" +
        "97097-0,SARS-CoV-2 (COVID-19) Ag [Presence] in Upper respiratory specimen by Rapid  immunoassay,LOINC\n" +
        "80382-5,Influenza virus A Ag [Presence] in Upper respiratory specimen by Rapid immunoassay,LOINC\n" +
        "12345,Flu B,LOCAL"

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

        val testRequest = MockHttpRequestMessage(REQ_BODY_TEST_CSV)

        every { workflowEngine.settings.findSender("Test Sender") } returns sender

        mockkObject(AuthenticatedClaims)
        val mockClaims = mockk<AuthenticatedClaims>()
        every { AuthenticatedClaims.authenticate(any()) } returns mockClaims
        every { mockClaims.authorizedForSendOrReceive(any(), any()) } returns true

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable(
                "observation-mapping",
                listOf(
                    listOf(
                        ObservationMappingConstants.TEST_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_KEY,
                        ObservationMappingConstants.CONDITION_CODE_SYSTEM_KEY,
                        ObservationMappingConstants.CONDITION_NAME_KEY
                    ),
                    listOf(
                        "00001",
                        "Some Condition Code",
                        "Condition Code System",
                        "Condition Name"
                    )
                )
            )
        )

        val codeToConditionMapping = listOf(
            mapOf(
                "test code" to "00001",
                "test description" to "test description 1",
                "coding system" to "Condition Code System",
                "mapped?" to "Y"
            ),
            mapOf(
                "testCode" to "00002",
                "testDescription" to "test description 2",
                "codingSystem" to "Another Condition Code System",
                "mapped" to "N"
            )
        )
        mockkObject(LookupTableCompareMappingCommand)
        every {
            LookupTableCompareMappingCommand.compareMappings(any(), any())
        } returns codeToConditionMapping

        testRequest.httpHeaders += mapOf(
            "client" to "Test Sender",
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assertEquals(HttpStatus.OK, response.status)
    }

    @Test
    fun `test SenderFunction conditionCodeComparisonPostRequest with no sender`() {
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(testOrganization)

        val workflowEngine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val senderFunction = spyk(SenderFunction(workflowEngine, actionHistory))

        val testRequest = MockHttpRequestMessage(REQ_BODY_TEST_CSV)

        testRequest.httpHeaders += mapOf(
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.status)
    }

    @Test
    fun `test SenderFunction conditionCodeComparisonPostRequest with bad sender`() {
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(testOrganization)

        val workflowEngine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val senderFunction = spyk(SenderFunction(workflowEngine, actionHistory))

        val testRequest = MockHttpRequestMessage(REQ_BODY_TEST_CSV)

        every { workflowEngine.settings.findSender("Test sender") } returns null

        mockkObject(AuthenticatedClaims)
        val mockClaims = mockk<AuthenticatedClaims>()
        every { AuthenticatedClaims.authenticate(any()) } returns mockClaims

        testRequest.httpHeaders += mapOf(
            "client" to "Test sender",
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assertEquals(HttpStatus.BAD_REQUEST, response.status)
    }

    @Test
    fun `test SenderFunction conditionCodeComparisonPostRequest with unauthenticated sender`() {
        val metadata = UnitTestUtils.simpleMetadata
        val settings = FileSettings().loadOrganizations(testOrganization)

        val workflowEngine = makeEngine(metadata, settings)
        val actionHistory = spyk(ActionHistory(TaskAction.receive))
        val senderFunction = spyk(SenderFunction(workflowEngine, actionHistory))

        val testRequest = MockHttpRequestMessage(REQ_BODY_TEST_CSV)

        every { workflowEngine.settings.findSender("Test sender") } returns null

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.authenticate(any()) } returns null

        testRequest.httpHeaders += mapOf(
            "client" to "Test sender",
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assertEquals(HttpStatus.UNAUTHORIZED, response.status)
    }

    @Test
    fun `test SenderFunction conditionCodeComparisonPostRequest with unauthorized sender`() {
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

        val testRequest = MockHttpRequestMessage(REQ_BODY_TEST_CSV)

        every { workflowEngine.settings.findSender("Test sender") } returns sender

        mockkObject(AuthenticatedClaims)
        val mockClaims = mockk<AuthenticatedClaims>()
        every { AuthenticatedClaims.authenticate(any()) } returns mockClaims
        every { mockClaims.authorizedForSendOrReceive(any(), any()) } returns false

        testRequest.httpHeaders += mapOf(
            "client" to "Test sender",
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assertEquals(HttpStatus.UNAUTHORIZED, response.status)
    }

    @Test
    fun `test SenderFunction conditionCodeComparisonPostRequest exception error`() {
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

        val testRequest = MockHttpRequestMessage(REQ_BODY_TEST_CSV)

        every { workflowEngine.settings.findSender("Test sender") } returns sender
        mockkObject(LookupTableCompareMappingCommand)
        every { LookupTableCompareMappingCommand.compareMappings(any(), any()) }.throws(Exception())

        testRequest.httpHeaders += mapOf(
            "client" to "Test sender",
            "content-length" to "4"
        )

        val response = senderFunction.conditionCodeComparisonPostRequest(testRequest)

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.status)
    }
}