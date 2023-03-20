package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import gov.cdc.prime.router.tokens.Jwk
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import java.security.interfaces.RSAPublicKey
import java.util.Base64

// TODO: https://github.com/CDCgov/prime-reportstream/issues/8659
// This test class be removed
class SettingsProviderTest {

    val keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256)
    val pubKey = keyPair.getPublic() as RSAPublicKey

    val scopeOne = "simple_report.default.report"
    val scopeTwo = "simple_report.default.*"

    val jwkOne = Jwk(
        pubKey.getAlgorithm(),
        n = Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )

    val jwkTwo = Jwk(
        pubKey.getAlgorithm(),
        n = Base64.getUrlEncoder().encodeToString(pubKey.getModulus().toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.getPublicExponent().toByteArray()),
        alg = "RS256",
        use = "sig",
    )

    val organization = Organization(
        name = "simple_report",
        description = "simple_report_description",
        jurisdiction = Organization.Jurisdiction.FEDERAL,
        null,
        null,
        null,
        null,
        null
    )

    val sender = CovidSender(
        "default",
        "simple_report",
        Sender.Format.CSV,
        CustomerStatus.INACTIVE,
        "default"
    )

    class EmptySettings(
        override val organizations: Collection<Organization>,
        override val senders: Collection<Sender>,
        override val receivers: Collection<Receiver>
    ) : SettingsProvider {

        override fun findOrganization(name: String): Organization? {
            return organizations.find { org -> org.name == name }
        }

        override fun findReceiver(fullName: String): Receiver? {
            throw NotImplementedError()
        }

        override fun findSender(fullName: String): Sender? {
            return senders.find { sender -> sender.fullName == fullName }
        }

        override fun findOrganizationAndReceiver(fullName: String): Pair<Organization, Receiver>? {
            throw NotImplementedError()
        }
    }

    @Test
    fun `test getKeys returns keys for the same scope set for organization and sender`() {
        val settings = EmptySettings(
            listOf(organization.makeCopyWithNewScopeAndJwk(scopeOne, jwkOne)),
            listOf(sender.makeCopyWithNewScopeAndJwk(scopeOne, jwkTwo)),
            emptyList()
        )

        val keys = settings.getKeys(sender.fullName)
        assertThat(keys).isNotNull()
        assertThat(keys?.size).isEqualTo(2)
        assertThat(keys?.map { key -> key.keys }?.flatten()).isEqualTo(listOf(jwkOne, jwkTwo))
    }

    @Test
    fun `test getKeys returns scopes from both organization and sender`() {
        val settings = EmptySettings(
            listOf(organization.makeCopyWithNewScopeAndJwk(scopeOne, jwkOne)),
            listOf(sender.makeCopyWithNewScopeAndJwk(scopeTwo, jwkOne)),
            emptyList()
        )

        val keys = settings.getKeys(sender.fullName)
        assertThat(keys).isNotNull()
        assertThat(keys?.size).isEqualTo(2)
        assertThat(keys?.map { key -> key.scope }).isEqualTo(listOf(scopeOne, scopeTwo))
        assertThat(keys?.map { key -> key.keys }?.flatten()?.toList()).isEqualTo(listOf(jwkOne, jwkOne))
    }
}