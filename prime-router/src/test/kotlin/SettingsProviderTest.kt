package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import gov.cdc.prime.router.tokens.Jwk
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Test
import java.security.interfaces.RSAPublicKey
import java.util.Base64

// TODO: https://github.com/CDCgov/prime-reportstream/issues/8659
// This test class be removed
class SettingsProviderTest {

    @Test
    fun `test get topic by jsonVal`() {
        assertThat(Topic.fromJsonValue("etor")).isEqualTo(Topic.ETOR)
        assertThat(Topic.fromJsonValue("full-elr")).isEqualTo(Topic.FULL_ELR)
        assertThat(Topic.fromJsonValue("covid-19")).isEqualTo(Topic.COVID_19)
        assertThat(Topic.fromJsonValue("monkeypox")).isEqualTo(Topic.MONKEYPOX)
        assertThat(Topic.fromJsonValue("CsvFileTests-topic")).isEqualTo(Topic.CSV_TESTS)
        assertThat(Topic.fromJsonValue("test")).isEqualTo(Topic.TEST)
        assertThat(Topic.fromJsonValue("otherString")).isNull()
        assertThat(Topic.fromJsonValue(null)).isNull()
    }

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
}