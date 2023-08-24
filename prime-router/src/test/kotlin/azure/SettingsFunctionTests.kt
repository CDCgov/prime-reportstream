package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.history.db.SubmitterDatabaseAccess
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.apache.logging.log4j.kotlin.Logging
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsFunctionTests : Logging {
    val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    val connection = MockConnection(dataProvider)

    @BeforeEach
    fun reset() {
        clearAllMocks()
    }

    @Nested
    inner class TestGetDelivery {
        var settings = MockSettings()

        private val organization1 = Organization(
            "simple_report",
            "simple_report_org",
            Organization.Jurisdiction.FEDERAL,
            null,
            null,
            null,
            null,
            null
        )
        private val receiver1 = Receiver(
            "default",
            organization1.name,
            Topic.COVID_19,
            schemaName = ""
        )

        private val organization2 = Organization(
            "ignore",
            "simple_report_org",
            Organization.Jurisdiction.FEDERAL,
            null,
            null,
            null,
            null,
            null
        )

        private val receiver2 = Receiver(
            "default",
            organization2.name,
            Topic.COVID_19,
            schemaName = ""
        )

        @BeforeEach
        fun setUp() {
            mockkObject(BaseEngine)
            every { BaseEngine.settingsProviderSingleton } returns settings
            settings.organizationStore[organization1.name] = organization1
            settings.receiverStore[receiver1.fullName] = receiver1
            settings.organizationStore[organization2.name] = organization2
            settings.receiverStore[receiver2.fullName] = receiver2
            mockkObject(Metadata.Companion)
            every { Metadata.getInstance() } returns UnitTestUtils.simpleMetadata
        }

        @AfterEach
        fun tearDown() {
            unmockkObject(AuthenticatedClaims)
        }

        @Test
        fun `test getOrganizations`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().getOrganizations(httpRequestMessage)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test getOneOrganization`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().getOneOrganization(httpRequestMessage, organization1.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test updateOneOrganization`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().updateOneOrganization(httpRequestMessage, organization1.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test getSenders`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().getSenders(httpRequestMessage, organization1.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test getOneSender`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().getOneSender(httpRequestMessage, organization1.name, receiver1.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test updateOneSender`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().updateOneSender(httpRequestMessage, organization1.name, receiver1.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test getReceivers`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().getReceivers(httpRequestMessage, organization1.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test getOneReceiver`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().getOneReceiver(httpRequestMessage, organization1.name, receiver1.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test updateOneReceiver`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction().updateOneReceiver(httpRequestMessage, organization1.name, receiver1.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `test getSettingRevisionHistory`() {
            val httpRequestMessage = MockHttpRequestMessage("")
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            mockkConstructor(SubmitterDatabaseAccess::class)

            val response = SettingsFunction()
                .getSettingRevisionHistory(httpRequestMessage, organization1.name, "testSetting")
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }
}