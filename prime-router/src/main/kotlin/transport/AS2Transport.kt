package gov.cdc.prime.router.transport

import com.helger.as2lib.client.AS2Client
import com.helger.as2lib.client.AS2ClientRequest
import com.helger.as2lib.client.AS2ClientSettings
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt
import com.helger.as2lib.crypto.ECryptoAlgorithmSign
import com.helger.as2lib.exception.WrappedAS2Exception
import com.helger.as2lib.processor.sender.AS2HttpResponseException
import com.helger.security.keystore.EKeyStoreType
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.AS2TransportType
import gov.cdc.prime.router.Metadata
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserJksCredential
import org.apache.hc.core5.util.Timeout
import org.apache.http.conn.ConnectTimeoutException
import org.apache.logging.log4j.kotlin.Logging
import java.net.ConnectException
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * The AS2 transport was built for communicating to the WA OneHealthNetwork. It is however a general transport protocol
 * that is used in other contexts and perhaps could be used elsewhere by other PHD in the US.
 * @param metadata optional metadata instance used for dependency injection
 *
 * See [Wikapedia AS2](https://en.wikipedia.org/wiki/AS2) for details on AS2.
 * See [PHAX as2-lib](https://github.com/phax/as2-lib) for the details on the library we use.
 */
class AS2Transport(val metadata: Metadata? = null) : ITransport, Logging {
    /**
     * The send a report or return [RetryItems]
     */
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        // DevNote: This code is similar to the SFTP code in structure
        //
        val as2Info = transportType as AS2TransportType
        val reportId = header.reportFile.reportId
        return try {
            if (header.content == null) error("No content to send for report $reportId")
            val receiver = header.receiver ?: error("No receiver defined for report $reportId")
            val credential = lookupCredentials(receiver.fullName)
            val externalFileName = Report.formExternalFilename(header, metadata)

            // Log the useful correlations of ids
            context.logger.info("${receiver.fullName}: Ready to send $reportId($sentReportId) to $externalFileName")

            // do all the AS2 work
            sendViaAS2(as2Info, credential, externalFileName, sentReportId, header.content)

            // Record the history of this transaction
            val msg = "${receiver.fullName}: Successful upload of $reportId"
            context.logger.info(msg)
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                receiver,
                sentReportId,
                null,
                as2Info.toString(),
                msg,
                header.reportFile.itemCount
            )
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
            null
        } catch (t: Throwable) {
            val receiverFullName = header.receiver?.fullName ?: "null"
            if (isErrorTransient(t)) {
                val msg = "FAILED AS2 upload of inputReportId $reportId to $as2Info (orgService = $receiverFullName);" +
                    "Retry all items; Exception: ${t.javaClass.canonicalName} ${t.localizedMessage}"
                context.logger.warning(msg)
                actionHistory.setActionType(TaskAction.send_warning)
                actionHistory.trackActionResult(msg)
                RetryToken.allItems
            } else {
                val msg = "FAILED AS2 upload of inputReportId $reportId to $as2Info (orgService = $receiverFullName);" +
                    "No retry; Exception: ${t.javaClass.canonicalName} ${t.localizedMessage}"
                // Dev note: Expecting severe level to trigger monitoring alerts
                context.logger.severe(msg)
                actionHistory.setActionType(TaskAction.send_error)
                actionHistory.trackActionResult(msg)
                null
            }
        }
    }

    /**
     * Do the work of sending a report over the AS2 transport.
     */
    fun sendViaAS2(
        as2Info: AS2TransportType,
        credential: UserJksCredential,
        externalFileName: String,
        sentReportId: ReportId,
        contents: ByteArray
    ) {
        val jks = Base64.getDecoder().decode(credential.jks)
        val settings = AS2ClientSettings()
            .setKeyStore(EKeyStoreType.PKCS12, jks, credential.jksPasscode)
            .setSenderData(as2Info.senderId, as2Info.senderEmail, credential.privateAlias)
            .setReceiverData(as2Info.receiverId, credential.trustAlias, as2Info.receiverUrl)
            .setEncryptAndSign(ECryptoAlgorithmCrypt.CRYPT_3DES, ECryptoAlgorithmSign.DIGEST_SHA256)
            .setConnectTimeout(Timeout.of(10_000, TimeUnit.MILLISECONDS))
            .setResponseTimeout(Timeout.of(20_000, TimeUnit.MILLISECONDS))
            .setMDNRequested(true)
        settings.setPartnershipName("${settings.senderAS2ID}_${settings.receiverAS2ID}")

        // Setup a messages id that includes the report id
        val messageId = "cdcprime-$sentReportId@${settings.senderAS2ID}_${settings.receiverAS2ID}"
        settings.messageIDFormat = messageId

        // Make a request for this payload
        val request = AS2ClientRequest(externalFileName)
            .setContentDescription(as2Info.contentDescription)
            .setContentType(as2Info.mimeType)
            .setFilename(externalFileName)
            .setData(contents.toString(Charsets.UTF_8), Charsets.UTF_8)

        // Send it
        val response = AS2Client().sendSynchronous(settings, request)

        // Check the response
        if (response.hasException())
            throw response.exception!!
        if (!response.hasMDN())
            error("AS2 upload for $externalFileName: No MDN in response")
        if (response.mdnDisposition?.contains("processed") != true)
            error("AS2 Upload for $externalFileName: Bad MDN ${response.mdnDisposition} ")
    }

    /**
     * From the credential service get a JKS for [receiverFullName].
     */
    fun lookupCredentials(receiverFullName: String): UserJksCredential {
        val credentialLabel = CredentialHelper.formCredentialLabel(fromReceiverName = receiverFullName)
        return CredentialHelper.getCredentialService().fetchCredential(
            credentialLabel, "AS2Transport", CredentialRequestReason.AS2_UPLOAD
        ) as? UserJksCredential?
            ?: error("Unable to find AS2 credentials for $receiverFullName connectionId($credentialLabel)")
    }

    /**
     * Look at the [ex] exception and determine if the error is possibly transient and it is worth retrying.
     */
    private fun isErrorTransient(ex: Throwable): Boolean {
        return when {
            // Connection to service is down, possibly a service down or under load situation
            ex is WrappedAS2Exception && ex.cause is ConnectException -> true
            ex is WrappedAS2Exception && ex.cause is ConnectTimeoutException -> true
            ex is AS2HttpResponseException && ex.code == HttpStatus.SERVICE_UNAVAILABLE.value() -> true
            ex is AS2HttpResponseException && ex.code == HttpStatus.TOO_MANY_REQUESTS.value() -> true
            // Assume everything else is not transient
            else -> false
        }
    }
}