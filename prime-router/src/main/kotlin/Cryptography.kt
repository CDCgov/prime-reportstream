package gov.cdc.prime.router

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * The Cryptography is use for extra encryption of message to send over the wire.  It uses the Java crypto library
 * to encrypt or decrypt message using given KEY and IV.
 */
class Cryptography {

    /**
     * The decrypt function decrypts the encrypted data using given KEY and VI.
     *  @param algorithm - algorithm using to encrypt message
     *  @param cipherText - encrypted input data to be decrypted.
     *  @param key - Secret Key
     *  @param iv - Intialization Vector
     *  @return decrypted output data
     */
    fun decrypt(algorithm: String, cipherText: ByteArray, key: SecretKey, iv: IvParameterSpec): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, key, iv)
        return cipher.doFinal(cipherText)
    }

    /**
     * The encrypt function encrypts the input data using given KEY and VI.
     *  @param algorithm - algorithm using to encrypt message
     *  @param inputText - input data to be encrypted.
     *  @param key - Secret Key
     *  @param iv - Intialization Vector
     *  @return encrypted output data
     */
    fun encrypt(algorithm: String, inputText: ByteArray, key: SecretKey, iv: IvParameterSpec): ByteArray {
        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.doFinal(inputText)
    }
}