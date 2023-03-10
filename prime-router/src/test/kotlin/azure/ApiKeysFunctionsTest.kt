package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class ApiKeysFunctionsTest {
    var settings = MockSettings()
    val organization = Organization(
        "simple_report",
        "simple_report_org",
        Organization.Jurisdiction.FEDERAL,
        null,
        null,
        null,
        null,
        null
    )

    @BeforeEach
    fun setup() {
        settings = MockSettings()

        mockkObject(BaseEngine)
        every { BaseEngine.settingsProviderSingleton } returns settings

        mockkConstructor(DatabaseJtiCache::class)
        every { anyConstructed<DatabaseJtiCache>().isJTIOk(any(), any()) } returns true

        mockkConstructor(ActionHistory::class)
        every { anyConstructed<ActionHistory>().trackActionResult(ofType(String::class)) } returns Unit

        mockkObject(Scope)
    }

    @AfterEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test returns an empty list when no keys are set`() {
        settings.organizationStore.put(organization.name, organization)

        var httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "c@rlos.com")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        var response = ApiKeysFunctions().get(httpRequestMessage, "simple_report")
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
        val jsonResponse = JSONObject(response.body.toString())
        assertThat(jsonResponse.get("orgName")).isEqualTo("simple_report")
        assertThat(jsonResponse.getJSONArray("keys")).isEmpty()
    }
}