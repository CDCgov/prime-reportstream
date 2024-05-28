package gov.cdc.prime.router

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import java.util.Base64
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test

class CryptographyTest {

    val algorithm = "AES/CBC/PKCS5Padding"

    // Given AES Key
    val aesKey = Base64.getDecoder().decode("BWHh9VPSzgjxwDeB52zFkSGQONBoOUqujMjsqzqur2I=")
    val iv = IvParameterSpec("E5q3I26Jtp3NTLUF".toByteArray())

    val crypto = Cryptography()
    val testPlainText = """
MSH|^~\&|EPICSTND|MB003480|PSC|MB|20230927071047|265108|ORM^O01^ORM_O01|550162|T|2.5.1||||||8859/1
PID|1||1300974^^^Doctors Hospital at Renaissance^MR||TEST^SCENERIO03BG^TWIN A^^^^CD:766|TEST|20230913120400|F^Female^HL70001||2076-8^Native Hawaiian or Other Pacific Islander^HL70005
ORC|NW|2801690163^HNAM_ORDERID||||||||||^^^^^^^^NPI&2.16.840.1.113883.4.6&ISO^L|||||||||Doctors Hospital at Renaissance^^^^^txdshslabNBS&2.16.840.1.114222.4.1.181960.2&ISO^FI^^^10859464
OBR|1|2801690163^HNAM_ORDERID||57128-1^Newborn Screening Panel AHIC^LN|||20231130123326|||2010346-006^Lopez^Martha^^L^^EI^^Doctors Hospital at Renaissance^^^^^""||||||
OBX|1|CWE|67704-7^Feeding types^LN||LA16915-3^Lactose formula^LN||||||F|||20231130123326
    """.trimIndent().toByteArray()

    @Test
    fun `cryptography test`() {
        // Encrypt the testPlainText to cipherText and check to make sure they are not equal
        val enKey: SecretKey = SecretKeySpec(aesKey, "AES")
        val cipherText = crypto.encrypt(algorithm, testPlainText, enKey, iv)
        assertThat(cipherText).isNotEqualTo(testPlainText)

        // Decrypt the cipherText and check to make sure they are equal to the original testPlainText
        val deKey: SecretKey = SecretKeySpec(aesKey, "AES")
        val plainText = crypto.decrypt(algorithm, cipherText, deKey, iv)
        assertThat(plainText).isEqualTo(testPlainText)
    }
}