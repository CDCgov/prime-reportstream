package gov.cdc.prime.as2exp

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.helger.as2lib.client.AS2Client
import com.helger.as2lib.client.AS2ClientRequest
import com.helger.as2lib.client.AS2ClientSettings
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt
import com.helger.as2lib.crypto.ECryptoAlgorithmSign
import com.helger.security.keystore.EKeyStoreType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Example program to send a message to One Health Port
 *
 * URL https://onehealthport-as2.axwaycloud.com/exchange/ZZOHP
 */

// AS2 endpoints
const val PROD_AS2_URL = "https://onehealthport-as2.axwaycloud.com/exchange/ZZOHP"
const val TEST_AS2_URL = "https://uat-onehealthport-as2.axwaycloud.com/exchange/ZZOHPUAT"
const val LOCAL_AS2_URL = "http://localhost:8000/pyas2/as2receive"

// AS2 Receiver IDS
const val PROD_AS2ID = "ZZOHP"
const val TEST_AS2ID = "ZZOHPUAT"
const val LOCAL_AS2ID = "p1as2"

// PRIME Sender IDS
const val PROD_PRIME_AS2ID = "7uycso49"
const val TEST_PRIME_AS2ID = "7uycso49"
const val LOCAL_PRIME_AS2ID = "CDCPRIMETEST"

// PRIME sender email
const val PRIME_SENDER_EMAIL = "reportstream@cdc.gov"
const val HL7_MIME_TYPE = "application/hl7-v2"
const val CONTENT_DESCRIPTION = "COVID-19 Electronic Lab Results"

// JKS key alias
const val PRIME_KEY_ALIAS = "cdcprime"
const val RECEIVER_KEY_ALIAS = "as2ohp"

class AS2ExpCommand: CliktCommand() {

    val payload by option("--payload", help="Payload file").file(mustExist = true).required()
    val keystore by option("--keystore", help="Keystore(.jks) file").file(mustExist = true).required()
    val keypass by option("--keypass", help="Keystore password").required()

    override fun run() {
        val logger: Logger = LoggerFactory.getLogger(AS2ExpCommand::class.java)
        logger.info("Sending one message")

        val client = AS2Client()
        val settings = AS2ClientSettings()
        settings.setKeyStore(EKeyStoreType.PKCS12, keystore, keypass)
        settings.setSenderData (TEST_PRIME_AS2ID, PRIME_SENDER_EMAIL, PRIME_KEY_ALIAS)
        settings.setReceiverData(TEST_AS2ID, RECEIVER_KEY_ALIAS, TEST_AS2_URL)
        settings.setPartnershipName("${settings.senderAS2ID}_${settings.receiverAS2ID}")

        // Encrypt and sign
        settings.setEncryptAndSign(ECryptoAlgorithmCrypt.CRYPT_3DES, ECryptoAlgorithmSign.DIGEST_SHA256)

        // Lot's options for a response. We will likely ignore, so don't request one
        settings.isMDNRequested = true

        //
        settings.connectTimeoutMS = 10_000
        settings.readTimeoutMS = 10_000

        // Insert report filename here for the subject
        val reportName = "wa-covid-19" +
                "-${DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDate.now())}" +
                "-${UUID.randomUUID()}"

        val request = AS2ClientRequest(reportName)
        request.contentType = HL7_MIME_TYPE
        request.setContentDescription(CONTENT_DESCRIPTION)
        request.setData(payload, Charsets.UTF_8)

        val response = client.sendSynchronous(settings, request)
        echo("MDN messageID: ${response.mdnMessageID}")
        echo("MDN message: ${response.mdn.toString()}")
        echo("response: ${response.asString}")
        echo("${response.mdnDisposition}")
    }
}

fun main(args: Array<String>) = AS2ExpCommand().main(args)