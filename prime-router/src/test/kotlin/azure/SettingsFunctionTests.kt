package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.UniversalPipelineSender
import gov.cdc.prime.router.tokens.OktaAuthentication
import gov.cdc.prime.router.tokens.TestDefaultJwt
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsFunctionTests : Logging {
    private val organizationName = "test-lab"
    private val oktaClaimsOrganizationName = "DHSender_$organizationName"
    private val otherOrganizationName = "test-lab-2"

    private val testOrg = Organization(
        "test",
        "test_org",
        Organization.Jurisdiction.FEDERAL,
        null,
        null,
        null,
        null,
        null
    )
    private val testSender = UniversalPipelineSender(
        "Test Sender",
        "test",
        Sender.Format.FHIR,
        allowDuplicates = true,
        customerStatus = CustomerStatus.INACTIVE,
        topic = Topic.FULL_ELR
    )
    private val testReceiver = Receiver(
        "default",
        testOrg.name,
        Topic.COVID_19,
        schemaName = ""
    )

    private fun buildClaimsMap(organizationName: String): Map<String, Any> {
        return mapOf(
            "sub" to "test",
            "organization" to listOf(organizationName)
        )
    }

    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    private fun mockFacade(): SettingsFacade {
        val mockFacade = mockkClass(SettingsFacade::class)

        every {
            mockFacade.findOrganization(any())
        } returns testOrg
        every {
            mockFacade.findReceiver(any())
        } returns testReceiver
        every {
            mockFacade.findSender(any())
        } returns testSender
        every {
            mockFacade.findSettingsAsJson(OrganizationAPI::class.java)
        } returns ""
        every {
            mockFacade.getLastModified()
        } returns OffsetDateTime.now()
        every {
            mockFacade.findSettingHistoryAsJson(any(), any())
        } returns ""
        every {
            mockFacade.findSettingAsJson(any(), OrganizationAPI::class.java, any())
        } returns ""
        every {
            mockFacade.findSettingAsJson(any(), ReceiverAPI::class.java, any())
        } returns ""
        every {
            mockFacade.findSettingAsJson(any(), Sender::class.java, any())
        } returns ""
        every {
            mockFacade.findSettingsAsJson(any(), ReceiverAPI::class.java)
        } returns Pair(SettingsFacade.AccessResult.SUCCESS, "")
        every {
            mockFacade.findSettingsAsJson(any(), Sender::class.java)
        } returns Pair(SettingsFacade.AccessResult.SUCCESS, "")
        every {
            mockFacade.putSetting(any(), any(), any(), ReceiverAPI::class.java, any())
        } returns Pair(SettingsFacade.AccessResult.SUCCESS, "")
        every {
            mockFacade.putSetting(any(), any(), any(), Sender::class.java, any())
        } returns Pair(SettingsFacade.AccessResult.SUCCESS, "")
        every {
            mockFacade.putSetting(any(), any(), any(), OrganizationAPI::class.java, any())
        } returns Pair(SettingsFacade.AccessResult.SUCCESS, "")

        return mockFacade
    }

    private fun setupSettingsFunctionForTesting(
        oktaClaimsOrganizationName: String,
        facade: SettingsFacade
    ): SettingsFunction {
        val claimsMap = buildClaimsMap(oktaClaimsOrganizationName)
        val settings = MockSettings()
        val sender = CovidSender(
            name = "default",
            organizationName = organizationName,
            format = Sender.Format.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )
        settings.senderStore[sender.fullName] = sender
        val sender2 = CovidSender(
            name = "default",
            organizationName = otherOrganizationName,
            format = Sender.Format.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "one"
        )
        settings.senderStore[sender2.fullName] = sender2

        val receiver = Receiver(
            "elr-secondary",
            organizationName,
            Topic.FULL_ELR,
            CustomerStatus.TESTING,
            "schema1"
        )
        settings.receiverStore[receiver.fullName] = receiver

        val receiver2 = Receiver(
            "test-lab-2",
            otherOrganizationName,
            Topic.FULL_ELR,
            CustomerStatus.TESTING,
            "schema1"
        )
        settings.receiverStore[receiver2.fullName] = receiver2

        mockkObject(OktaAuthentication.Companion)
        every { OktaAuthentication.Companion.decodeJwt(any()) } returns
            TestDefaultJwt(
                "a.b.c",
                Instant.now(),
                Instant.now().plusSeconds(60),
                claimsMap
            )

        return SettingsFunction(
            settingsFacade = facade
        )
    }

    private fun setupHttpRequestMessageForTesting(method: HttpMethod = HttpMethod.GET): MockHttpRequestMessage {
        val httpRequestMessage = MockHttpRequestMessage("{}", method)
        httpRequestMessage.httpHeaders += mapOf(
            "authorization" to "Bearer 111.222.333",
            "content-type" to HttpUtilities.jsonMediaType
        )

        return httpRequestMessage
    }

    @Test
    fun `test access user cannot view full organization settings (getOrganizations)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = settingsFunction.getOrganizations(httpRequestMessage)
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can view all organization settings (getOrganizations)`() {
        val settingsFunction = setupSettingsFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getOrganizations(httpRequestMessage)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getOrganizations(httpRequestMessage)
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot view their organization's settings (getOneOrganization)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = settingsFunction.getOneOrganization(
            httpRequestMessage,
            "$organizationName.elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access user cannot view another organization's settings (getOneOrganization)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        val response = settingsFunction.getOneOrganization(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can view any organization's settings (getOneOrganization)`() {
        val settingsFunction = setupSettingsFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getOneOrganization(
            httpRequestMessage,
            "$organizationName.elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getOneOrganization(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot update their organization's settings (updateOneOrganization)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        val response = settingsFunction.updateOneOrganization(
            httpRequestMessage,
            "$organizationName.elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access user cannot update another organization's settings (updateOneOrganization)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        val response = settingsFunction.updateOneOrganization(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can update any organization's settings (updateOneOrganization)`() {
        val settingsFunction = setupSettingsFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        var response = settingsFunction.updateOneOrganization(
            httpRequestMessage,
            "$organizationName.elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.updateOneOrganization(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot get their organization's senders (getSenders-getOneSender)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getSenders(
            httpRequestMessage,
            "$organizationName.elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        response = settingsFunction.getOneSender(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access user cannot get another organization's senders (getSenders-getOneSender)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getSenders(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        response = settingsFunction.getOneSender(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can get any organization's senders (getSenders-getOneSender)`() {
        val settingsFunction = setupSettingsFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getSenders(
            httpRequestMessage,
            "$organizationName.elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getSenders(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getOneSender(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "default"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getOneSender(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "default"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot update their organization's senders (updateOneSender)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        val response = settingsFunction.updateOneSender(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "default"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access user cannot update another organization's senders (updateOneSender)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        val response = settingsFunction.updateOneSender(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "default"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can update any organization's senders (updateOneSender)`() {
        val settingsFunction = setupSettingsFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        var response = settingsFunction.updateOneSender(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "default"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.updateOneSender(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "default"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot get their organization's receivers (getReceivers-getOneReceiver)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getReceivers(
            httpRequestMessage,
            "$organizationName.elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        response = settingsFunction.getOneReceiver(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access user cannot get another organization's receivers (getReceivers-getOneReceiver)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getReceivers(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        response = settingsFunction.getOneReceiver(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can get any organization's receivers (getReceivers-getOneReceiver)`() {
        val settingsFunction = setupSettingsFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getReceivers(
            httpRequestMessage,
            "$organizationName.elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getReceivers(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getOneReceiver(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getOneReceiver(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot update their organization's receivers (updateOneReceiver)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        val response = settingsFunction.updateOneReceiver(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access user cannot update another organization's receivers (updateOneReceiver)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        val response = settingsFunction.updateOneReceiver(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can update any organization's receivers (updateOneReceiver)`() {
        val settingsFunction = setupSettingsFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting(HttpMethod.PUT)
        var response = settingsFunction.updateOneReceiver(
            httpRequestMessage,
            organizationName,
            "elr-secondary"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.updateOneReceiver(
            httpRequestMessage,
            otherOrganizationName,
            "test-lab-2"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `test access user cannot get their organization's revision history (getSettingRevisionHistory)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "ORGANIZATION"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "RECEIVER"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "SENDER"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access user cannot get another organization's revision history (getSettingRevisionHistory)`() {
        val settingsFunction = setupSettingsFunctionForTesting(oktaClaimsOrganizationName, mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "ORGANIZATION"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "RECEIVER"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "SENDER"
        )
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `test access DHPrimeAdmins can get any organization's revision history (getSettingRevisionHistory)`() {
        val settingsFunction = setupSettingsFunctionForTesting("DHPrimeAdmins", mockFacade())
        val httpRequestMessage = setupHttpRequestMessageForTesting()
        var response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "ORGANIZATION"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "RECEIVER"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$organizationName.elr-secondary",
            "SENDER"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "ORGANIZATION"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "RECEIVER"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
        response = settingsFunction.getSettingRevisionHistory(
            httpRequestMessage,
            "$otherOrganizationName.test-lab-2",
            "SENDER"
        )
        assertThat(response.status).isEqualTo(HttpStatus.OK)
    }
}