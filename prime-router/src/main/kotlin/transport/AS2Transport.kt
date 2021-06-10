package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.AS2TransportType
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.UserJksCredential

import com.helger.as2lib.client.AS2Client
import com.helger.as2lib.client.AS2ClientRequest
import com.helger.as2lib.client.AS2ClientSettings
import com.helger.as2lib.crypto.ECryptoAlgorithmCrypt
import com.helger.as2lib.crypto.ECryptoAlgorithmSign
import com.helger.security.keystore.EKeyStoreType

import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

class AS2Transport : ITransport, Logging {
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val as2Info = transportType as AS2TransportType
        val reportId = header.reportFile.reportId
        return try {
            if (header.content == null) error("No content to send for report $reportId")
            val receiver = header.receiver ?: error("No receiver defined for report $reportId")
            val credential = lookupCredentials(receiver.fullName)
            val externalFileName = Report.formExternalFilename(header)
            context.logger.info("Ready to upload $externalFileName")

            sendReport(as2Info, credential, externalFileName, sentReportId, header.content)

            val msg = "Success: AS2 upload of $reportId to $as2Info"
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
            val msg =
                "FAILED AS2 upload of inputReportId $reportId to " +
                    "$as2Info (orgService = ${header.receiver?.fullName ?: "null"})" +
                    ", Exception: ${t.localizedMessage}"
            context.logger.warning(msg)
            actionHistory.setActionType(TaskAction.send_error)
            actionHistory.trackActionResult(msg)
            RetryToken.allItems
        }
    }

    companion object {
        const val TIMEOUT = 10_000

        fun sendReport(
            as2Info: AS2TransportType,
            credential: UserJksCredential,
            externalFileName: String,
            sentReportId: ReportId,
            contents: ByteArray
        ) {
            val jks = Base64.getDecoder().decode(credential.jks)
            val settings = AS2ClientSettings()
                .setKeyStore(EKeyStoreType.PKCS12, jks, credential.jksPasscode)
                .setSenderData (as2Info.senderId, as2Info.senderEmail, credential.idAlias)
                .setReceiverData(as2Info.receiverId, credential.trustAlias, as2Info.receiverUrl)
                .setEncryptAndSign(ECryptoAlgorithmCrypt.CRYPT_3DES, ECryptoAlgorithmSign.DIGEST_SHA256)
                .setConnectTimeoutMS(TIMEOUT)
                .setReadTimeoutMS(2*TIMEOUT)
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

        fun lookupCredentials(receiverFullName: String): UserJksCredential {
            // Covert to the upper case naming convention of the Client KeyVault
            val credentialLabel = receiverFullName
                .replace(".", "--")
                .replace("_", "-")
                .uppercase()

            return CredentialHelper.getCredentialService().fetchCredential(
                credentialLabel, "AS2Transport", CredentialRequestReason.AS2_UPLOAD
            ) as? UserJksCredential?
                ?: error("Unable to find AS2 credentials for $receiverFullName connectionId($credentialLabel)")
        }
    }
}