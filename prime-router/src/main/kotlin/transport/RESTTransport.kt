package gov.cdc.prime.router.transport

import com.google.common.base.Preconditions
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.RestCredential
import gov.cdc.prime.router.credentials.UserApiKeyCredential
import gov.cdc.prime.router.credentials.UserJksCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.accept
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.Parameters
import io.ktor.http.content.TextContent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class RESTTransport(private val httpClient: HttpClient? = null) : ITransport {
    /**
     * Send the content on the specific transport. Return retry information, if needed. Null, if not.
     *
     * @param transportType the type of the transport (should always match the class)
     * @param header container of all info needed about report being sent.
     * @param sentReportId ID representing the report as sent externally.
     * @param retryItems the retry items from the last effort, if it was unsuccessful
     * @return null, if successful. RetryItems if not successful.
     */
    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
    ): RetryItems? {
        val restTransportInfo = transportType as RESTTransportType
        val fileName = header.reportFile.externalName
        val receiver = header.receiver ?: error("No receiver defined for report ${header.reportFile.reportId}")
        val credential = lookupCredentials(receiver)
        val jksCredential = restTransportInfo.tlsKeystore?.let { lookupJksCredentials(it) }
        return try {
            // run our call to the endpoint in a blocking fashion
            runBlocking {
                launch {
                    val restClient = httpClient ?: createDefaultHttpClient(jksCredential)
                    // get the OAuth token, used to authenticate
                    val tokenInfo = getAuthToken(
                        restTransportInfo.authTokenUrl,
                        credential,
                        receiver,
                        context,
                        restClient
                    )
                    bearerTokenStorage.add(BearerTokens(tokenInfo.access_token, tokenInfo.refresh_token!!))
                    context.logger.info("Token successfully added!")
                    // post the report
                    val responseBody = postReport(
                        header.content.toString(),
                        restTransportInfo.reportUrl,
                        restTransportInfo.headers,
                        context,
                        restClient
                    )
                    // update the action history
                    val msg = "Success: REST transport of $fileName to $restTransportInfo:\n$responseBody"
                    context.logger.info("Message successfully sent!")
                    actionHistory.trackActionResult(msg)
                    actionHistory.trackSentReport(
                        receiver,
                        sentReportId,
                        fileName,
                        restTransportInfo.toString(),
                        msg,
                        header.reportFile.itemCount
                    )
                    actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
                }
            }
            // nothing to retry, return null
            null
        } catch (t: Throwable) {
            // If Ktor fails to connect, or the server returns an error code, it is thrown
            // as an exception higher up, which we catch and then track here. We do not need
            // to worry about capturing and parsing out the return value from the response
            // because Ktor treats errors as exceptions
            val msg = "FAILED POST of inputReportId ${header.reportFile.reportId} to " +
                "$restTransportInfo (orgService = ${header.receiver.fullName})" +
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
     * Get a user ApiKey credential from the credential service
     * @param receiver the fullName of the receiver is the label of the credential
     */
    fun lookupCredentials(receiver: Receiver): RestCredential {
        Preconditions.checkNotNull(receiver.transport)
        Preconditions.checkArgument(receiver.transport is RESTTransportType)
        val receiverFullName = receiver.fullName
        val credentialLabel = CredentialHelper.formCredentialLabel(fromReceiverName = receiverFullName)
        return CredentialHelper.getCredentialService().fetchCredential(
            credentialLabel,
            "RESTTransport",
            CredentialRequestReason.REST_UPLOAD
        ) as? UserApiKeyCredential?
            ?: error("Unable to find OAuth credentials for $receiverFullName using $credentialLabel")
    }

    /**
     * Get a JKS from the credential service
     * @param [tlsKeystore] is the label of the credential
     */
    fun lookupJksCredentials(tlsKeystore: String): UserJksCredential {
        val credentialLabel = CredentialHelper.formCredentialLabel(tlsKeystore)
        return CredentialHelper.getCredentialService().fetchCredential(
            credentialLabel, "RESTTransport", CredentialRequestReason.REST_UPLOAD
        ) as? UserJksCredential?
            ?: error("Unable to find JKS credentials for $tlsKeystore connectionId($credentialLabel)")
    }

    suspend fun getAuthParameters(
        receiverOrg: String,
        credential: RestCredential
    ): Parameters {
        when (receiverOrg) {
            "ny-phd" -> {
                credential as UserApiKeyCredential
                return Parameters.build {
                    append("grant_type", "client_credentials")
                    append("client_id", credential.user)
                    append("client_secret", credential.apiKey)
                }
            }
            "ok-phd" -> {
                credential as UserPassCredential
                return Parameters.build {
                    append("email", credential.user)
                    append("password", credential.pass)
                }
            }
            else -> error("Unable to find build parameters for $receiverOrg.")
        }
    }

    /**
     * Get the OAuth token by submitting the credentials
     * This is a suspend function, meaning it can get called as an async method, though we call it
     * a blocking way.
     *
     * @param restUrl The URL to post to get the OAuth token
     * @param credential The API key credential with clientID as user and clientSecret as apiKey
     * @param context Really just here to get logging injected
     */
    suspend fun getAuthToken(
        restUrl: String,
        credential: RestCredential,
        receiver: Receiver,
        context: ExecutionContext,
        httpClient: HttpClient
    ): TokenInfo {
        val authParameters = getAuthParameters(receiver.organizationName, credential)
        httpClient.use { client ->
            val tokenInfo: TokenInfo = client.submitForm(
                restUrl,
                formParameters = authParameters

            ) {
                expectSuccess = true
                accept(ContentType.Application.Json)
            }.body()

            val showMeThatToken = tokenInfo.toString()
            context.logger.info(showMeThatToken)
            return tokenInfo
        }
    }

    /**
     * Post the report as to the REST service. This is based on SoapTransport connectToSoapService
     * This is a suspend function, meaning it can get called as an async method, though we call it
     * a blocking way.
     *
     * @param message The report we want to send as text
     * @param restUrl The URL to post the report to
     * @param context Really just here to get logging injected
     */
    suspend fun postReport(
        message: String,
        restUrl: String,
        headers: Map<String, String>,
        context: ExecutionContext,
        httpClient: HttpClient
    ): String {
        httpClient.use { client ->
            val theResponse: String = client.post(restUrl) {
                context.logger.info("posting report to rest API")
                expectSuccess = true
                postHeaders(
                    headers
                )
//                header(
//                    // we want to pass a specific header for NY, expect to refactor this as other receivers are added
//                    """UPHN-INFOMAP""", """{"properties":"labClia=10DRPTSTRM,target=NYS,content=L,format=HL7"}"""
//                )
                // we want to pass text in the body of our request. You need to do it this
                // way because the TextContent object sets other values like the charset, etc.
                // Ktor will balk if you try to set it some other way
                setBody(
                    TextContent(
                        message, ContentType.Text.Plain,
                    )
                )
                accept(ContentType.Application.Json)
            }.bodyAsText()
            return theResponse.toString()
        }
    }

    companion object {
        /** A default value for the timeouts to connect and send messages */
        private const val TIMEOUT = 50_000
        /** storage for the OAuth tokens */
        val bearerTokenStorage = mutableListOf<BearerTokens>()

        /** Our default Http Client, with an optional  */
        private fun createDefaultHttpClient(jks: UserJksCredential?): HttpClient {
            return HttpClient(Apache) {
                // installs logging into the call to post to the server
                install(Logging) {
                    logger = io.ktor.client.plugins.logging.Logger.Companion.SIMPLE
                    level = LogLevel.INFO
                }
                install(Auth) {
                    bearer {
                        loadTokens {
                            bearerTokenStorage.last()
                        }
                    }
                }
                // configures the Apache client with our specified timeouts
                engine {
                    jks?.let { sslContext = getSslContext(jks) }
                    followRedirects = true
                    socketTimeout = TIMEOUT
                    connectTimeout = TIMEOUT
                    connectionRequestTimeout = TIMEOUT
                    customizeClient {
                    }
                }
            }
        }

        /***
         * Create an SSL context with the provided cert
         * @param jksCredential containing the cert to use
         */
        private fun getSslContext(jksCredential: UserJksCredential): SSLContext? {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, getTrustManagerFactory(jksCredential)?.trustManagers, null)
            return sslContext
        }

        /***
         * Create a trust manager, so we can load the provided cert
         * @param jksCredential containing the cert to use
         */
        private fun getTrustManagerFactory(jksCredential: UserJksCredential): TrustManagerFactory? {
            val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(getKeyStore(jksCredential))
            return trustManagerFactory
        }

        /***
         * Open a key store and load the provided cert
         * @param jksCredential containing the cert to use
         */
        private fun getKeyStore(jksCredential: UserJksCredential): KeyStore {
            val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(jksCredential.jks.byteInputStream(), jksCredential.jksPasscode.toCharArray())
            return keyStore
        }

        private fun HttpMessageBuilder.postHeaders(header: Map<String, String>): Unit =
            header.forEach { entry ->
                headers.append(entry.key, entry.value)
            }
    }
}