package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import gov.cdc.prime.router.tokens.Jwk
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.security.interfaces.RSAPublicKey
import java.util.Base64
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

    val keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)
    val pubKey = keyPair.getPublic() as RSAPublicKey
    val jwk = Jwk(
        pubKey.getAlgorithm(),
        n = Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )
    val jwk2 = Jwk(
        pubKey.getAlgorithm(),
        n = Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )
    val scope = "simple_report.default.report"
    val scope2 = "simple_report.*.report"

    @BeforeEach
    fun setup() {
        settings = MockSettings()

        mockkObject(BaseEngine)
        every { BaseEngine.settingsProviderSingleton } returns settings
    }

    @AfterEach
    fun reset() {
        clearAllMocks()
    }

    @Test
    fun `Test returns an empty list when no keys are set`() {
        settings.organizationStore.put(organization.name, organization)

        val httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val response = ApiKeysFunctions().get(httpRequestMessage, "simple_report")
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
        val jsonResponse = JSONObject(response.body.toString())
        assertThat(jsonResponse.get("orgName")).isEqualTo("simple_report")
        assertThat(jsonResponse.getJSONArray("keys")).isEmpty()
    }

    @Test
    fun `Test returns keys if a prime admin`() {
        settings.organizationStore.put(organization.name, organization.makeCopyWithNewScopeAndJwk(scope, jwk))

        val httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val response = ApiKeysFunctions().get(httpRequestMessage, "simple_report")
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
        val jsonResponse = JSONObject(response.body.toString())
        assertThat(jsonResponse.get("orgName")).isEqualTo("simple_report")
        assertThat(jsonResponse.getJSONArray("keys")).isNotEmpty()
        assertThat(jsonResponse.getJSONArray("keys").length()).isEqualTo(1)
        assertThat(
            jsonResponse.getJSONArray("keys")
                .getJSONObject(0).getString("scope")
        ).isEqualTo(scope)
    }

    @Test
    fun `Test returns keys if an organization admin`() {
        settings.organizationStore.put(organization.name, organization.makeCopyWithNewScopeAndJwk(scope, jwk))

        val httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val response = ApiKeysFunctions().get(httpRequestMessage, "simple_report")
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
        val jsonResponse = JSONObject(response.body.toString())
        assertThat(jsonResponse.get("orgName")).isEqualTo("simple_report")
        assertThat(jsonResponse.getJSONArray("keys")).isNotEmpty()
        assertThat(jsonResponse.getJSONArray("keys").length()).isEqualTo(1)
        assertThat(
            jsonResponse.getJSONArray("keys")
                .getJSONObject(0).getString("scope")
        ).isEqualTo(scope)
    }

    @Test
    fun `Test returns multiple keys for multiple scopes`() {
        settings.organizationStore.put(
            organization.name,
            organization.makeCopyWithNewScopeAndJwk(scope, jwk).makeCopyWithNewScopeAndJwk(scope, jwk2)
                .makeCopyWithNewScopeAndJwk(scope2, jwk)
        )

        val httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val response = ApiKeysFunctions().get(httpRequestMessage, "simple_report")
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
        val jsonResponse = JSONObject(response.body.toString())
        assertThat(jsonResponse.getJSONArray("keys").length()).isEqualTo(2)
        assertThat(jsonResponse.getJSONArray("keys").map { obj -> (obj as JSONObject).getString("scope") })
            .isEqualTo(
                listOf(scope, scope2)
            )
        assertThat(jsonResponse.getJSONArray("keys").getJSONObject(0).getJSONArray("keys").length())
            .isEqualTo(2)
    }

    @Test
    fun `Test does not return keys if not part of the organization`() {
        settings.organizationStore.put(organization.name, organization.makeCopyWithNewScopeAndJwk(scope, jwk))

        val httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf("DHSender_different_orgAdmins"), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val response = ApiKeysFunctions().get(httpRequestMessage, "simple_report")
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `Test does not return keys if not an admin of the organization`() {
        settings.organizationStore.put(organization.name, organization.makeCopyWithNewScopeAndJwk(scope, jwk))

        val httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val response = ApiKeysFunctions().get(httpRequestMessage, "simple_report")
        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `Test returns a 404 if the org does not exist`() {
        settings.organizationStore.put(organization.name, organization.makeCopyWithNewScopeAndJwk(scope, jwk))

        val httpRequestMessage = MockHttpRequestMessage()

        val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

        val response = ApiKeysFunctions().get(httpRequestMessage, "missing_org")
        assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND)
    }
}