package gov.cdc.prime.router.azure

import gov.cdc.prime.router.Sender
import io.jsonwebtoken.Jwts
import java.util.Date
import java.util.UUID
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import java.net.URL
import java.security.PrivateKey
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemReader
import java.io.File
import java.io.FileReader
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.math.exp

class SenderUtils {

    companion object {

        /**
         * Generate a signed JWT, representing a request for authentication from a Sender, using a private key.
         * This is done by the Sender, not by ReportStream.   This method is here for testing, and as an example.
         */
        fun generateSenderToken(
            sender: Sender,
            baseUrl: String,
            privateKey: PrivateKey,
            keyId: String,
            expirationSecondsFromNow: Int = 300,
        ): String {
            val jws = Jwts.builder()
                .setHeaderParam("kid", keyId)  // kid
                .setHeaderParam("typ", "JWT")     // typ
                .setIssuer(sender.fullName)        // iss
                .setSubject(sender.fullName)       // sub
                .setAudience(baseUrl)   // aud
                .setExpiration(Date(System.currentTimeMillis() + expirationSecondsFromNow * 1000))  // exp
                .setId(UUID.randomUUID().toString())   // jti
                .signWith(privateKey).compact()
            return jws
        }

        fun generateSenderUrlParameters(senderToken: String): Map<String, String> {
            return mapOf<String, String>(
                "scope" to "reports",
                "grant_type" to "client_credentials",
                "client_assertion_type" to "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
                "client_assertion" to senderToken,
            )
        }

        fun generateSenderUrl(baseUrl: String, senderToken: String): URL {
            return URL(
                "http://localhost:7071/api/token?" +
                    generateSenderUrlParameters(senderToken)
                        .map { "${it.key}=${it.value}" }.joinToString("&")
            )
        }

        fun readPrivateKeyPemFile(pemFile: File): PrivateKey? {
            val factory: KeyFactory = KeyFactory.getInstance("EC")
            FileReader(pemFile).use { keyReader ->
                PemReader(keyReader).use { pemReader ->
                    val pemObject: PemObject = pemReader.readPemObject()
                    val content: ByteArray = pemObject.getContent()
                    val privKeySpec = PKCS8EncodedKeySpec(content)
                    return factory.generatePrivate(privKeySpec) as ECPrivateKey
                }
            }
            FileReader(pemFile).use { keyReader ->
                val pemParser = PEMParser(keyReader)
                val converter = JcaPEMKeyConverter()
                val privateKeyInfo: PrivateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject())
                return converter.getPrivateKey(privateKeyInfo)  //  as RSAPrivateKey
            }
        }
    }

}