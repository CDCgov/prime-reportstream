import com.github.ajalt.clikt.core.CliktCommand
import com.helger.as2lib.client.AS2Client
import com.helger.as2lib.client.AS2ClientRequest
import com.helger.as2lib.client.AS2ClientSettings
import com.helger.security.keystore.IKeyStoreType

/**
 * TODO - Subject of message? Anything like report id
 * TODO - Description of message?
 * TODO - MDN return? yes
 * TODO - Synchronous? yes
 * TODO - Local JKS is for signing
 * TODO - Encryption? Receiver certificate.
 * TODO - Sender email? prime@cdc.gov
 * TODO - AS2 ID for sender and receiver? Receiver ZZOHP
 * TODO - MimeType? text/x-hl7-ft
 * TODO - Charset? UTF-8
 *
 * URL https://onehealthport-as2.axwaycloud.com/exchange/ZZOHP
 */


class Hello: CliktCommand() {


    
    override fun run() {
        val client = AS2Client()
        val settings = AS2ClientSettings()
        settings.setKeyStore(byteArray)
        settings.setReceiverData("ZZOPH", "receiverKeyAlias", "URL")
        settings.setSenderData("CDCPRIMETEST", "prime@cdc.gov", "senderKeyAlias")
        val request = AS2ClientRequest(report.fileName)
        request.setData(inputStream)
        client.sendSynchronous(settings, request)
    }
}

fun main(args: Array<String>) = Hello().main(args)