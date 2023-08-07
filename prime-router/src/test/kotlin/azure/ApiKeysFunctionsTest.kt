package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import gov.cdc.prime.router.tokens.Jwk
import gov.cdc.prime.router.tokens.JwkSet
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import java.io.StringWriter
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import kotlin.test.Test

class ApiKeysFunctionsTest {
    var settings = MockSettings()
    var facade = mockk<SettingsFacade>()
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
    val keyPair2 = Keys.keyPairFor(SignatureAlgorithm.RS256)
    val pubKey2 = keyPair2.getPublic() as RSAPublicKey
    val keyPair3 = Keys.keyPairFor(SignatureAlgorithm.RS256)
    val pubKey3 = keyPair2.getPublic() as RSAPublicKey

    var encodedPubKey: String? = null
    val jwk = Jwk(
        pubKey.getAlgorithm(),
        kid = "key1",
        n = Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )
    val jwk2 = Jwk(
        pubKey.getAlgorithm(),
        kid = "key2",
        n = Base64.getUrlEncoder().encodeToString(pubKey2.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey2.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )

    val jwk3 = Jwk(
        pubKey.getAlgorithm(),
        kid = "key3",
        n = Base64.getUrlEncoder().encodeToString(pubKey3.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey3.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )
    val defaultReportScope = "simple_report.default.report"
    val wildcardReportScope = "simple_report.*.report"

    @BeforeEach
    fun setup() {
        settings = MockSettings()

        mockkObject(BaseEngine)
        every { BaseEngine.settingsProviderSingleton } returns settings

        val writer = StringWriter()
        val pemWriter = PemWriter(writer)
        pemWriter.writeObject(PemObject("PUBLIC KEY", pubKey.encoded))
        pemWriter.flush()
        pemWriter.close()
        encodedPubKey = writer.toString()

        mockkObject(SettingsFacade)
        every { SettingsFacade.common } returns facade
        every {
            facade.putSetting<OrganizationAPI>(
                organization.name,
                any<String>(),
                any<AuthenticatedClaims>(),
                OrganizationAPI::class.java,
            )
        } answers {
            val organizationAPI =
                JacksonMapperUtilities.defaultMapper.readValue(secondArg<String>(), OrganizationAPI::class.java)
            settings.organizationStore.put(organizationAPI.name, organizationAPI)
            Pair(SettingsFacade.AccessResult.SUCCESS, secondArg<String>())
        }
    }

    @AfterEach
    fun reset() {
        clearAllMocks()
        unmockkObject(AuthenticatedClaims)
    }

    @Nested
    inner class DeleteApiKeysTests {
        @Test
        fun `Test successfully delete a key`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk2)
            )
            val httpRequestMessage = MockHttpRequestMessage()
            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().delete(
                httpRequestMessage,
                organization.name,
                wildcardReportScope,
                jwk2.kid as String
            )

            assertThat(response.status).isEqualTo(HttpStatus.OK)
            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.size).isEqualTo(0)
        }

        @Test
        fun `Test successfully delete a key as an org admin`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk2)
            )
            val httpRequestMessage = MockHttpRequestMessage()
            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().delete(
                httpRequestMessage,
                organization.name,
                wildcardReportScope,
                jwk2.kid as String
            )

            assertThat(response.status).isEqualTo(HttpStatus.OK)
            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.size).isEqualTo(0)
        }

        @Test
        fun `Test fails delete a key as an org admin for a different org`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk2)
            )
            val httpRequestMessage = MockHttpRequestMessage()
            val jwt = mapOf("organization" to listOf("DHSender_watersAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().delete(
                httpRequestMessage,
                organization.name,
                wildcardReportScope,
                jwk2.kid as String
            )

            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.get(0)?.toRSAPublicKey()).isEqualTo(jwk2.toRSAPublicKey())
        }

        @Test
        fun `Test delete a key returns a 404 if the scope is not found`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk2)
            )
            val httpRequestMessage = MockHttpRequestMessage()
            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().delete(
                httpRequestMessage,
                organization.name,
                wildcardReportScope,
                jwk2.kid as String
            )

            assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(defaultReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.get(0)?.toRSAPublicKey()).isEqualTo(jwk2.toRSAPublicKey())
        }
        @Test
        fun `Test delete a key returns a 404 if the org does not exist`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk2)
            )
            val httpRequestMessage = MockHttpRequestMessage()
            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().delete(
                httpRequestMessage,
                "waters",
                "waters.*.report",
                jwk2.kid as String
            )

            assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Test
        fun `Test delete a key returns a 404 if the kid is not found`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk2)
            )
            val httpRequestMessage = MockHttpRequestMessage()
            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().delete(
                httpRequestMessage,
                organization.name,
                wildcardReportScope,
                jwk.kid as String
            )

            assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.get(0)?.toRSAPublicKey()).isEqualTo(jwk2.toRSAPublicKey())
        }

        @Test
        fun `Test delete a key return a bad request if the scope is not a valid scope`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk2)
            )
            val httpRequestMessage = MockHttpRequestMessage()
            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().delete(
                httpRequestMessage,
                organization.name,
                "ignore.*.report",
                jwk.kid as String
            )

            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.get(0)?.toRSAPublicKey()).isEqualTo(jwk2.toRSAPublicKey())
        }

        @Test
        fun `Test returns the error if one is encountered persisting the org`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk)
            )
            val httpRequestMessage = MockHttpRequestMessage()

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            every { facade.putSetting(organization.name, any(), claims, OrganizationAPI::class.java) } returns Pair(
                SettingsFacade.AccessResult.BAD_REQUEST,
                "Payload and path name do not match"
            )

            val response = ApiKeysFunctions().delete(
                httpRequestMessage,
                organization.name,
                wildcardReportScope,
                jwk.kid as String
            )

            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isEqualTo("Payload and path name do not match")
        }
    }

    @Nested
    inner class PostApiKeysTests {

        @Test
        fun `Test successfully adds a new key`() {
            settings.organizationStore.put(organization.name, organization)
            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.OK)

            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.get(0)?.toRSAPublicKey()).isEqualTo(jwk.toRSAPublicKey())
        }

        @Test
        fun `Test successfully adds a new key when not over the key limit`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk2)
            )

            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)

            assertThat(response.status).isEqualTo(HttpStatus.OK)

            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.size).isEqualTo(2)
            updatedOrg?.keys?.get(0)?.keys?.map { key -> key.toRSAPublicKey() }?.let {
                assertThat(
                    it
                        .toSet()
                ).isEqualTo(setOf(jwk.toRSAPublicKey(), jwk2.toRSAPublicKey()))
            }
        }

        @Test
        fun `Test successfully removes the oldest key and adds the new one when over the limit`() {
            mockkObject(JwkSet.Companion)
            every { JwkSet.Companion.getMaximumNumberOfKeysPerScope() } returns 2
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk2)
                    .makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk3)
            )

            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)

            assertThat(response.status).isEqualTo(HttpStatus.OK)

            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.size).isEqualTo(2)
            updatedOrg?.keys?.get(0)?.keys?.map { key -> key.toRSAPublicKey() }?.let {
                assertThat(
                    it
                        .toSet()
                ).isEqualTo(setOf(jwk3.toRSAPublicKey(), jwk.toRSAPublicKey()))
            }
            unmockkObject(JwkSet.Companion)
        }

        @Test
        fun `Test successfully adds a key as an admin for the org`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.OK)
            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys?.size).isEqualTo(1)
            assertThat(updatedOrg?.keys?.map { key -> key.scope }).isEqualTo(listOf(wildcardReportScope))
            assertThat(updatedOrg?.keys?.get(0)?.keys?.get(0)?.toRSAPublicKey()).isEqualTo(jwk.toRSAPublicKey())
        }

        @Test
        fun `Test only supports the wildcard report scope`() {
            settings.organizationStore.put(organization.name, organization)

            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = defaultReportScope
            httpRequestMessage.queryParameters["kid"] = "simple_report"

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isEqualTo("Request scope must be ${organization.name}.*.report")
        }

        @Test
        fun `Test only adds keys if scope is valid`() {
            settings.organizationStore.put(organization.name, organization)
            val invalidScope = "*.*.admin"
            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            // This is not a valid scope for the organization
            httpRequestMessage.queryParameters["scope"] = "*.*.admin"
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, "simple_report")
            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isEqualTo(
                "Organization name in scope must match ${organization.name}.  Instead got: $invalidScope"
            )
        }

        @Test
        fun `Test body is required`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage()
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isEqualTo("Body must be provided")
        }

        @Test
        fun `Test valid key must be provided`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage("Not a valid key")
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isEqualTo("Unable to parse public key: No PEM-encoded keys found")
        }

        @Test
        fun `Test kid is required`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope

            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isEqualTo("kid must be provided")
        }

        @Test
        fun `Test kid must be unique in the JwkSet that will be updated`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = jwk.kid ?: ""

            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isEqualTo("kid must be unique for the requested scope")
        }

        @Test
        fun `Test is unauthorized if not an admin for the org`() {
            settings.organizationStore.put(organization.name, organization)

            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf("DHSender_simple_report"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `Test is unauthorized if an admin for a different org`() {
            settings.organizationStore.put(organization.name, organization)

            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf("DHSender_watersAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `Test returns the error if one is encountered persisting the key`() {
            settings.organizationStore.put(organization.name, organization)
            val httpRequestMessage = MockHttpRequestMessage(encodedPubKey)
            httpRequestMessage.queryParameters["scope"] = wildcardReportScope
            httpRequestMessage.queryParameters["kid"] = organization.name

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims
            every { facade.putSetting(organization.name, any(), claims, OrganizationAPI::class.java) } returns Pair(
                SettingsFacade.AccessResult.BAD_REQUEST,
                "Payload and path name do not match"
            )

            val response = ApiKeysFunctions().post(httpRequestMessage, organization.name)
            assertThat(response.status).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body).isEqualTo("Payload and path name do not match")

            val updatedOrg = settings.organizationStore.get(organization.name)
            assertThat(updatedOrg?.keys).isNull()
        }
    }

    @Nested
    inner class GetApiKeysTests {
        @Test
        fun `Test returns an empty list when no keys are set`() {
            settings.organizationStore.put(organization.name, organization)

            val httpRequestMessage = MockHttpRequestMessage()

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            @Suppress("DEPRECATION")
            val response = ApiKeysFunctions().get(httpRequestMessage, organization.name)
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
            val jsonResponse = JSONObject(response.body.toString())
            assertThat(jsonResponse.get("orgName")).isEqualTo(organization.name)
            assertThat(jsonResponse.getJSONArray("keys")).isEmpty()
        }

        @Test
        fun `Test returns keys if a prime admin`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage()

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            @Suppress("DEPRECATION")
            val response = ApiKeysFunctions().get(httpRequestMessage, organization.name)
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
            val jsonResponse = JSONObject(response.body.toString())
            assertThat(jsonResponse.get("orgName")).isEqualTo(organization.name)
            assertThat(jsonResponse.getJSONArray("keys")).isNotEmpty()
            assertThat(jsonResponse.getJSONArray("keys").length()).isEqualTo(1)
            assertThat(
                jsonResponse.getJSONArray("keys")
                    .getJSONObject(0).getString("scope")
            ).isEqualTo(defaultReportScope)
        }

        @Test
        fun `Test returns keys if an organization admin`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage()

            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            @Suppress("DEPRECATION")
            val response = ApiKeysFunctions().get(httpRequestMessage, organization.name)
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
            val jsonResponse = JSONObject(response.body.toString())
            assertThat(jsonResponse.get("orgName")).isEqualTo("simple_report")
            assertThat(jsonResponse.getJSONArray("keys")).isNotEmpty()
            assertThat(jsonResponse.getJSONArray("keys").length()).isEqualTo(1)
            assertThat(
                jsonResponse.getJSONArray("keys")
                    .getJSONObject(0).getString("scope")
            ).isEqualTo(defaultReportScope)
        }

        @Test
        fun `Test returns multiple keys for multiple scopes`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
                    .makeCopyWithNewScopeAndJwk(defaultReportScope, jwk2)
                    .makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage()

            val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            @Suppress("DEPRECATION")
            val response = ApiKeysFunctions().get(httpRequestMessage, organization.name)
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
            val jsonResponse = JSONObject(response.body.toString())
            assertThat(jsonResponse.getJSONArray("keys").length()).isEqualTo(2)
            assertThat(jsonResponse.getJSONArray("keys").map { obj -> (obj as JSONObject).getString("scope") })
                .isEqualTo(
                    listOf(defaultReportScope, wildcardReportScope)
                )
            assertThat(jsonResponse.getJSONArray("keys").getJSONObject(0).getJSONArray("keys").length())
                .isEqualTo(2)
        }

        @Test
        fun `Test does not return keys if not part of the organization`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage()

            val jwt = mapOf("organization" to listOf("DHSender_different_orgAdmins"), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            @Suppress("DEPRECATION")
            val response = ApiKeysFunctions().get(httpRequestMessage, organization.name)
            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @Test
        fun `Test returns a 404 if the org does not exist`() {
            settings.organizationStore.put(
                organization.name,
                organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
            )

            val httpRequestMessage = MockHttpRequestMessage()

            val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
            val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

            mockkObject(AuthenticatedClaims)
            every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

            @Suppress("DEPRECATION")
            val response = ApiKeysFunctions().get(httpRequestMessage, "missing_org")
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @Nested
        inner class V1() {
            @Test
            fun `Test get keys`() {
                settings.organizationStore.put(
                    organization.name,
                    organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
                        .makeCopyWithNewScopeAndJwk(defaultReportScope, jwk2)
                        .makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk)
                )

                val httpRequestMessage = MockHttpRequestMessage()

                val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
                val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

                mockkObject(AuthenticatedClaims)
                every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

                val response = ApiKeysFunctions().getV1(httpRequestMessage, organization.name)
                assertThat(response.status).isEqualTo(HttpStatus.OK)
                val jsonResponse = JSONObject(response.body.toString())
                assertThat(jsonResponse.getJSONArray("data").length()).isEqualTo(2)
                assertThat(jsonResponse.getJSONArray("data").map { obj -> (obj as JSONObject).getString("scope") })
                    .isEqualTo(
                        listOf(defaultReportScope, wildcardReportScope)
                    )
                assertThat(jsonResponse.getJSONArray("data").getJSONObject(0).getJSONArray("keys").length())
                    .isEqualTo(2)
                val metaResponse = jsonResponse.getJSONObject("meta")
                assertThat(metaResponse.getString("type")).isEqualTo("PublicKey")
                assertThat(metaResponse.getInt("totalCount")).isEqualTo(2)
            }

            @Test
            fun `Test returns a 404 if the org does not exist`() {
                settings.organizationStore.put(
                    organization.name,
                    organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
                )

                val httpRequestMessage = MockHttpRequestMessage()

                val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
                val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

                mockkObject(AuthenticatedClaims)
                every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

                val response = ApiKeysFunctions().getV1(httpRequestMessage, "missing_org")
                assertThat(response.status).isEqualTo(HttpStatus.NOT_FOUND)
            }

            @Test
            fun `Test does not return keys if not part of the organization`() {
                settings.organizationStore.put(
                    organization.name,
                    organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
                )

                val httpRequestMessage = MockHttpRequestMessage()

                val jwt = mapOf("organization" to listOf("DHSender_different_orgAdmins"), "sub" to "test@cdc.gov")
                val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

                mockkObject(AuthenticatedClaims)
                every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

                val response = ApiKeysFunctions().getV1(httpRequestMessage, organization.name)
                assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED)
            }

            @Test
            fun `Test returns multiple keys for multiple scopes`() {
                settings.organizationStore.put(
                    organization.name,
                    organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
                        .makeCopyWithNewScopeAndJwk(defaultReportScope, jwk2)
                        .makeCopyWithNewScopeAndJwk(wildcardReportScope, jwk)
                )

                val httpRequestMessage = MockHttpRequestMessage()

                val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
                val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

                mockkObject(AuthenticatedClaims)
                every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

                val response = ApiKeysFunctions().getV1(httpRequestMessage, organization.name)
                assertThat(response.getStatus()).isEqualTo(HttpStatus.OK)
                val jsonResponse = JSONObject(response.body.toString())
                assertThat(jsonResponse.getJSONArray("data").length()).isEqualTo(2)
                assertThat(jsonResponse.getJSONArray("data").map { obj -> (obj as JSONObject).getString("scope") })
                    .isEqualTo(
                        listOf(defaultReportScope, wildcardReportScope)
                    )
                assertThat(jsonResponse.getJSONArray("data").getJSONObject(0).getJSONArray("keys").length())
                    .isEqualTo(2)
            }

            @Test
            fun `Test returns keys if an organization admin`() {
                settings.organizationStore.put(
                    organization.name,
                    organization.makeCopyWithNewScopeAndJwk(defaultReportScope, jwk)
                )

                val httpRequestMessage = MockHttpRequestMessage()

                val jwt = mapOf("organization" to listOf("DHSender_simple_reportAdmins"), "sub" to "test@cdc.gov")
                val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)

                mockkObject(AuthenticatedClaims)
                every { AuthenticatedClaims.Companion.authenticate(any()) } returns claims

                val response = ApiKeysFunctions().getV1(httpRequestMessage, organization.name)
                assertThat(response.status).isEqualTo(HttpStatus.OK)
                val jsonResponse = JSONObject(response.body.toString())
                assertThat(jsonResponse.getJSONArray("data")).isNotEmpty()
                assertThat(jsonResponse.getJSONArray("data").length()).isEqualTo(1)
                assertThat(
                    jsonResponse.getJSONArray("data")
                        .getJSONObject(0).getString("scope")
                ).isEqualTo(defaultReportScope)
            }
        }
    }
}