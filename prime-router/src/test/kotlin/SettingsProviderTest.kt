package gov.cdc.prime.router

import gov.cdc.prime.router.tokens.Jwk
import io.jsonwebtoken.Jwts.SIG
import java.security.interfaces.RSAPublicKey
import java.util.Base64

// TODO: https://github.com/CDCgov/prime-reportstream/issues/8659
// This test class be removed
class SettingsProviderTest {
    val keyPair = SIG.RS256.keyPair().build()
    val pubKey = keyPair.public as RSAPublicKey

    val scopeOne = "simple_report.default.report"
    val scopeTwo = "simple_report.default.*"

    val jwkOne = Jwk(
        pubKey.algorithm,
        n = Base64.getUrlEncoder().encodeToString(pubKey.modulus.toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.publicExponent.toByteArray()),
        alg = "RS256",
        use = "sig",
    )

    val jwkTwo = Jwk(
        pubKey.algorithm,
        n = Base64.getUrlEncoder().encodeToString(pubKey.modulus.toByteArray()),
        e = Base64.getUrlEncoder().encodeToString(pubKey.publicExponent.toByteArray()),
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
        MimeFormat.CSV,
        CustomerStatus.INACTIVE,
        "default"
    )

    class EmptySettings(
        override val organizations: Collection<Organization>,
        override val senders: Collection<Sender>,
        override val receivers: Collection<Receiver>,
    ) : SettingsProvider {

        override fun findOrganization(name: String): Organization? = organizations.find { org -> org.name == name }

        override fun findReceiver(fullName: String): Receiver? = throw NotImplementedError()

        override fun findSender(fullName: String): Sender? = senders.find { sender -> sender.fullName == fullName }

        override fun findOrganizationAndReceiver(fullName: String): Pair<Organization, Receiver>? =
            throw NotImplementedError()
    }
}