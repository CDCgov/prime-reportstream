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
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.SoapCredential
import gov.cdc.prime.router.serializers.SoapEnvelope
import gov.cdc.prime.router.serializers.SoapObjectService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
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
class SoapTransport(private val httpClient: HttpClient? = null) : ITransport {
    /**
     * Writes out the xml in a pretty way. This is primarily for our log files.
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
     * Make a SOAP connection. We're using the KTOR library here to make a direct HTTP call.
     * I defaulted to using the [Apache] engine to do our connections. There are a bunch of
     * different libraries you could use for this, but the Apache one is fairly vanilla and
     * straightforward for our purposes.
     *
     * This is a suspend function, meaning it can get called as an async method, though we call it
     * a blocking way.
     *
     * @param message The contents of the file we want to send, after it's been wrapped in the [SoapEnvelope]
     * object and converted to XML
     * @param soapEndpoint The URL to post to when sending the message
     * @param soapAction The command to invoke on the remote server
     * @param context Really just here to get logging injected
     */
    private suspend fun connectToSoapService(
        message: String,
        soapEndpoint: String,
        soapAction: String,
        context: ExecutionContext,
        httpClient: HttpClient
    ): String {
        httpClient.use { client ->
            context.logger.info("Connecting to $soapEndpoint")

            // once we've created te client, we will use it to call post on the endpoint
            val response: HttpResponse = client.post(soapEndpoint) {
                // tell ktor to throw an exception if not successful
                expectSuccess = true
                // adds the SOAPAction header
                header("SOAPAction", soapAction)
                // we want to pass text in the body of our request. You need to do it this
                // way because the TextContent object sets other values like the charset, etc
                // Ktor will balk if you try to set it some other way
                setBody(
                    TextContent(
                        message,
                        // force the encoding to be UTF-8. PA had issues understanding the message
                        // unless it was explicitly set to UTF-8. Plus it's good to be explicit about
                        // these things
                        contentType = ContentType.Text.Xml.withCharset(Charsets.UTF_8)
                    )
                )
            }
            // get the response object
            val body: String = response.body()
            // return just the body of the message
            return prettyPrintXmlResponse(body)
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
        // verify that we have a SOAP transport type for our parameters. I think if we ever fell
        // into this scenario with different parameters there's something seriously wrong in the system,
        // but it is good to check.
        val soapTransportType = transportType as? SoapTransportType
            ?: error("Transport type passed in not of SOAPTransportType")
        // verify we have a receiver
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        // get the external file name to send to the client, if we need it
        val fileName = header.reportFile.externalName
        context.logger.info(
            "Preparing to send $sentReportId " +
                "to ${soapTransportType.soapAction} at ${soapTransportType.endpoint}"
        )
        // based on who we are sending this report to, we need to get the credentials, and we also need
        // to create the actual implementation object based on who we're sending to
        val credential = lookupCredentials(receiver)
        // calling the SOAP serializer in an attempt to be somewhat abstract here. this is based on the
        // namespaces provided to the SOAP transport info. It's kind of BS magical string garbage, but the
        // reality is that each client could have different objects we have to create for them, and these
        // objects might nest, and/or have different constructors, and it's really hard to be really generic
        // about this kind of stuff. So this is some semi-tight coupling we will just have to manage.
        // And honestly, if a client changes their SOAP endpoint, we'd (probably) need to do some coding
        // on our end, so incurring the maintenance cost here is (probably) okay.
        val xmlObject = SoapObjectService.getXmlObjectForAction(soapTransportType, header, context, credential)
            ?: error("Unable to find a SOAP object for the namespaces provided")
        // wrap the object in the generic envelope. At least this gets to be generic
        val soapEnvelope = SoapEnvelope(xmlObject, soapTransportType.namespaces ?: emptyMap())

        return try {
            // run our call to the endpoint in a blocking fashion
            runBlocking {
                launch {
                    val responseBody = connectToSoapService(
                        soapEnvelope.toXml(),
                        soapTransportType.endpoint,
                        soapTransportType.soapAction,
                        context,
                        httpClient ?: createDefaultHttpClient()
                    )
                    // update the action history
                    val msg = "Success: SOAP transport of $sentReportId to $soapTransportType:\n$responseBody"
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
                }
            }
            // return null
            null
        } catch (t: Throwable) {
            // If Ktor fails to connect, or the server returns an error code, it is thrown
            // as an exception higher up, which we catch and then track here. We do not need
            // to worry about capturing and parsing out the return value from the response
            // because Ktor treats errors as exceptions
            val msg = "FAILED SOAP of inputReportId ${header.reportFile.reportId} to " +
                "$soapTransportType (orgService = ${header.receiver.fullName})" +
                ", Exception: ${t.localizedMessage}"
            context.logger.severe(msg)
            context.logger.severe(t.stackTraceToString())
            // do some additional handling of the error here. if we are dealing with a 400 error, we
            // probably don't want to retry, and we need to stop now
            // if the error is a 500 we can do a retry, but both should probably throw a pager duty notification
            when (t) {
                is ClientRequestException -> {
                    (t).let {
                        context.logger.severe(
                            "Received ${it.response.status.value}: ${it.response.status.description} " +
                                "requesting ${it.response.request.url}. This is not recoverable. Will not retry."
                        )
                    }
                    actionHistory.setActionType(TaskAction.send_error)
                    actionHistory.trackActionResult(msg)
                    null
                }
                is ServerResponseException -> {
                    // this is largely duplicated code as below, but we may want to add additional
                    // instrumentation based on the specific error type we're getting. One benefit
                    // we can use now is getting the specific response information from the
                    // ServerResponseException
                    (t).let {
                        context.logger.severe(
                            "Received ${it.response.status.value}: ${it.response.status.description} " +
                                "from server ${it.response.request.url}. This may be recoverable. Will retry."
                        )
                    }
                    actionHistory.setActionType(TaskAction.send_warning)
                    actionHistory.trackActionResult(msg)
                    RetryToken.allItems
                }
                else -> {
                    // this is an unknown exception, and maybe not one related to ktor, so we should
                    // track, but try again
                    actionHistory.setActionType(TaskAction.send_warning)
                    actionHistory.trackActionResult(msg)
                    RetryToken.allItems
                }
            }
        }
    }

    /**
     * Fetch the [SoapCredential] for a given [Receiver].
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

    companion object {
        /** A default value for the timeouts to connect and send messages */
        private const val TIMEOUT = 50_000

        /** Our default Http Client */
        private fun createDefaultHttpClient(): HttpClient {
            return HttpClient(Apache) {
                // installs logging into the call to post to the server
                install(Logging) {
                    logger = io.ktor.client.plugins.logging.Logger.Companion.SIMPLE
                    level = LogLevel.INFO
                }
                // configures the Apache client with our specified timeouts
                engine {
                    followRedirects = true
                    socketTimeout = TIMEOUT
                    connectTimeout = TIMEOUT
                    connectionRequestTimeout = TIMEOUT
                    customizeClient {
                    }
                }
            }
        }
    }
}