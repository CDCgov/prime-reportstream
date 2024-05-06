package gov.cdc.prime.router

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val SALT_LENGTH_BYTE = 16

class Cryptography {

    val salt = getRandomNonce(SALT_LENGTH_BYTE)

    fun getRandomNonce(numBytes: Int): ByteArray {
        val nonce = ByteArray(numBytes)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    // AES 256 bits secret key derived from a password
    @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
    fun getAESKeyFromPassword(password: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")

        // iterationCount = 65536
        // keyLength = 256
        val spec: KeySpec = PBEKeySpec(password.toString(Charsets.UTF_8).toCharArray(), salt, 65536, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    fun decrypt(algorithm: String, cipherText: ByteArray, key: SecretKeySpec, iv: IvParameterSpec): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        val plainText = cipher.doFinal(cipherText)
        return plainText
    }

    fun encrypt(algorithm: String, inputText: ByteArray, key: SecretKeySpec, iv: IvParameterSpec): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        val cipherText = cipher.doFinal(inputText)
        return cipherText
    }
}