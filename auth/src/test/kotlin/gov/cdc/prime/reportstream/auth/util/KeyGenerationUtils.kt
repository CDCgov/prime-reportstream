package gov.cdc.prime.reportstream.auth.util

import com.nimbusds.jose.jwk.RSAKey
import gov.cdc.prime.reportstream.shared.StringUtilities.base64Encode
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Handy RSA key generation scripts in JUnit test form for easy running
 */
class KeyGenerationUtils {

    /**
     * If you remove the @Ignore annotation below and run this test you can create
     * a random 2048-bit RSA key pair. This will be useful for key rotations.
     */
    @Ignore
    @Test
    fun generateAndPrintRSAKeyPair() {
        val (privateJWK, publicJWK) = generateRSAKeyPair()

        println("private JWK: $privateJWK")
        println("public JWK: $publicJWK")

        println("Encoded private JWK: ${privateJWK.toJSONString().base64Encode()}")
        println("Encoded public JWK: ${publicJWK.toJSONString().base64Encode()}")
    }

    companion object {
        fun generateRSAKeyPair(): Pair<RSAKey, RSAKey> {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(2048)
            val keyPair = keyGen.generateKeyPair()

            val privateJWK = RSAKey.Builder(keyPair.public as RSAPublicKey)
                .privateKey(keyPair.private as RSAPrivateKey)
                .build()
            val publicJWK = privateJWK.toPublicJWK()
            return Pair(privateJWK, publicJWK)
        }
    }
}