package gov.cdc.prime.router.transport

import com.google.common.base.Preconditions
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.SoapTransportType
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.SoapCredential
import gov.cdc.prime.router.serializers.SoapEnvelope
import gov.cdc.prime.router.serializers.SoapObjectService
import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.logging.LogLevel
import io.ktor.client.features.logging.Logger.Companion
import io.ktor.client.features.logging.Logging
import io.ktor.client.features.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.StringReader
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * A SOAP transport that will connect to the endpoint and send a message in a serialized SOAP envelope
 */
class SoapTransport : ITransport {
    /**
     * Writes out the xml in a pretty way
     */
    private fun prettyPrintXmlResponse(value: String): String {
        val transformerFactory = TransformerFactory.newDefaultInstance()
        val transformer = transformerFactory.newTransformer().also {
            it.setOutputProperty(OutputKeys.INDENT, "yes")
        }
        val xmlOutput = StreamResult(StringWriter())
        transformer.transform(StreamSource(StringReader(value)), xmlOutput)
        return xmlOutput.writer.toString()
    }

    /**
     * Make a SOAP connection
     */
    private suspend fun connectToSoapService(
        message: String,
        soapEndpoint: String,
        soapAction: String,
        context: ExecutionContext
    ): String {
        context.logger.info("Invoking with:\n$message")
        HttpClient(Apache) {
            install(Logging) {
                logger = Companion.SIMPLE
                level = LogLevel.ALL
            }
            engine {
                followRedirects = true
                socketTimeout = 10_000
                connectTimeout = 10_000
                connectionRequestTimeout = 20_000
                customizeClient {
                }
            }
        }.use { client ->
            context.logger.info("Connecting to $soapEndpoint")
            val response: HttpResponse = client.post(soapEndpoint) {
                header("SOAPAction", soapAction)
                body = TextContent(
                    message,
                    contentType = ContentType.Text.Xml.withCharset(Charsets.UTF_8)
                )
            }
            context.logger.info(response.toString())
            val body: String = response.receive()
            context.logger.info(prettyPrintXmlResponse(body))
            return body
        }
    }

    /**
     * Sends the actual message to the endpoint
     */
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory
    ): RetryItems? {
        val soapTransportType = transportType as? SoapTransportType
            ?: error("Transport type passed in not of SOAPTransportType")
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        val fileName = header.reportFile.externalName
        context.logger.info(
            "Preparing to sending ${header.reportFile.reportId} " +
                "to ${soapTransportType.soapAction} at ${soapTransportType.endpoint}"
        )
        // based on who we are sending this report to, we need to get the credentials, and we also need
        // to create the actual implementation object based on who we're sending to
        val credential = lookupCredentials(receiver)
        val xmlObject = SoapObjectService.getXmlObjectForAction(soapTransportType, header, context, credential)
            ?: error("Unable to find a SOAP object for the namespaces provided")
        val soapEnvelope = SoapEnvelope(xmlObject, soapTransportType.namespaces ?: emptyMap())

        return try {
            runBlocking {
                launch {
                    connectToSoapService(
                        soapEnvelope.toXml(),
                        soapTransportType.endpoint,
                        soapTransportType.soapAction,
                        context
                    )
                }
            }
            // update the action history
            val msg = "Success: SOAP transport of $fileName to $soapTransportType"
            context.logger.info("Message successfully sent!")
            actionHistory.trackActionResult(msg)
            actionHistory.trackSentReport(
                receiver,
                sentReportId,
                fileName,
                soapTransportType.toString(),
                msg,
                header.reportFile.itemCount
            )
            actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
            // return null
            null
        } catch (t: Throwable) {
            context.logger.severe(t.localizedMessage)
            context.logger.severe(t.stackTraceToString())
            RetryToken.allItems
        }
    }

    /**
     * Fetch the credentials for a give [receiver].
     * @return the SOAP credential
     */
    fun lookupCredentials(receiver: Receiver): SoapCredential {
        Preconditions.checkNotNull(receiver.transport)
        Preconditions.checkArgument(receiver.transport is SoapTransportType)
        val soapTransportInfo = receiver.transport as SoapTransportType

        // if the transport definition has defined default
        // credentials use them, otherwise go with the
        // standard way by using the receiver full name
        val credentialName = soapTransportInfo.credentialName ?: receiver.fullName

        val credentialLabel = credentialName
            .replace(".", "--")
            .replace("_", "-")
            .uppercase()
        // Assumes credential will be cast as SftpCredential, if not return null, and thus the error case
        return CredentialHelper.getCredentialService().fetchCredential(
            credentialLabel, "SoapTransport", CredentialRequestReason.SOAP_UPLOAD
        ) as? SoapCredential?
            ?: error("Unable to find SOAP credentials for $credentialName connectionId($credentialLabel)")
    }
}