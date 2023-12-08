package gov.cdc.prime.router.transport

import com.google.common.base.Preconditions
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Organization
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
import gov.cdc.prime.router.credentials.UserAssertionCredential
import gov.cdc.prime.router.credentials.UserJksCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.tokens.AuthUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMessageBuilder
import io.ktor.http.Parameters
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.InputStream
import java.security.KeyStore
import java.util.Base64
import java.util.logging.Logger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

/**
 * A REST transport that will get an authentication token from the authTokenUrl
 * and POST HL7 to the reportUrl
 */
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
        val logger: java.util.logging.Logger = context.logger

        val restTransportInfo = transportType as RESTTransportType
        val reportId = "${header.reportFile.reportId}"
        val receiver = header.receiver ?: error("No receiver defined for report $reportId")
        val reportContent: ByteArray = header.content ?: error("No content for report $reportId")
        // get the file name, or create one from the report ID, NY requires a file name in the POST
        val fileName = header.reportFile.externalName ?: "$reportId.hl7"
        // get the username/password to authenticate with OAuth
        val (credential, jksCredential) = getCredential(restTransportInfo, receiver)

//         restTransportInfo.headers = restTransportInfo.headers + Pair("Content-Length", header.content.size.toLong())

        // get the TLS/SSL cert in a JKS if needed, NY uses a specific one
//        val jksCredential = restTransportInfo.tlsKeystore?.let { lookupJksCredentials(it) }

        return try {
            // run our call to the endpoint in a blocking fashion
            runBlocking {
                launch {
                    try {
                        // parse headers for any dynamic values, OK needs the report ID
                        var (httpHeaders, bearerTokens: BearerTokens?) = getOAuthToken(
                            restTransportInfo,
                            reportId,
                            jksCredential,
                            credential,
                            logger
                        )
                        logger.info("Token successfully added!")

                        // post the report
                        val reportClient = httpClient ?: createDefaultHttpClient(jksCredential, bearerTokens)
                        val response = postReport(
                            reportContent,
                            fileName,
                            restTransportInfo.reportUrl,
                            httpHeaders,
                            logger,
                            reportClient
                        )
                        val responseBody = response.bodyAsText()
                        // update the action history
                        val msg = "Success: REST transport of $fileName to $restTransportInfo:\n$responseBody"
                        logger.info("Message successfully sent!")
                        actionHistory.trackActionResult(response.status, msg)
                        actionHistory.trackSentReport(
                            receiver,
                            sentReportId,
                            fileName,
                            restTransportInfo.toString(),
                            msg,
                            header
                        )
                        actionHistory.trackItemLineages(Report.createItemLineagesFromDb(header, sentReportId))
                    } catch (t: Throwable) {
                        throw t
                    }
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
            logger.severe(msg)
            logger.severe(t.stackTraceToString())
            // do some additional handling of the error here. if we are dealing with a 400 error, we
            // probably don't want to retry, and we need to stop now
            // if the error is a 500 we can do a retry, but both should probably throw a pager duty notification
            when (t) {
                is ClientRequestException -> {
                    (t).let {
                        logger.severe(
                            "Received ${it.response.status.value}: ${it.response.status.description} " +
                                    "requesting ${it.response.request.url}. This is not recoverable. Will not retry."
                        )
                    }
                    actionHistory.setActionType(TaskAction.send_error)
                    actionHistory.trackActionResult(t.response.status, msg)
                    null
                }

                is ServerResponseException -> {
                    // this is largely duplicated code as below, but we may want to add additional
                    // instrumentation based on the specific error type we're getting. One benefit
                    // we can use now is getting the specific response information from the
                    // ServerResponseException
                    (t).let {
                        logger.severe(
                            "Received ${it.response.status.value}: ${it.response.status.description} " +
                                    "from the server ${it.response.request.url}, ${it.response.version}." +
                                    " This may be recoverable. Will retry."
                        )
                    }
                    actionHistory.setActionType(TaskAction.send_warning)
                    actionHistory.trackActionResult(t.response.status, msg)
                    RetryToken.allItems
                }

                else -> {
                    // this is an unknown exception, and maybe not one related to ktor, so we should
                    // track, but try again
                    actionHistory.setActionType(TaskAction.send_warning)
                    if (t is ResponseException) {
                        actionHistory.trackActionResult(t.response.status, msg)
                    } else {
                        actionHistory.trackActionResult(msg)
                    }
                    RetryToken.allItems
                }
            }
        }
    }

    /**
     * Get credential either two-legged or default.
     * @param transportType the transport type of two-legged or default
     * @param receiver the receiver setting
     */
    fun getCredential(transport: RESTTransportType, receiver: Receiver): Pair<RestCredential, UserJksCredential?> {
        val credential: RestCredential = if (transport.authType == "two-legged") {
            lookupTwoLeggedCredential(receiver)
        } else {
            lookupDefaultCredential(receiver)
        }

        val jksCredential: UserJksCredential? = transport.tlsKeystore?.let { lookupJksCredentials(it) }

        return Pair(credential, jksCredential)
    }

    /**
     * Get a credential from the credential service
     * @param receiver the fullName of the receiver is the label of the credential
     */
    fun lookupDefaultCredential(receiver: Receiver): RestCredential {
        Preconditions.checkNotNull(receiver.transport)
        Preconditions.checkArgument(receiver.transport is RESTTransportType)
        val receiverFullName = receiver.fullName
        val credentialLabel = CredentialHelper.formCredentialLabel(fromReceiverName = receiverFullName)
        return CredentialHelper.getCredentialService().fetchCredential(
            credentialLabel,
            "RESTTransport",
            CredentialRequestReason.REST_UPLOAD
        ) as? RestCredential?
            ?: error("Unable to find OAuth credentials for $receiverFullName using $credentialLabel")
    }

    /**
     * Get a credential using PrivateKey to generate signed JWT authentication token (senderToken)
     * @param receiver the fullName of the receiver is the label of the credential
     */
    fun lookupTwoLeggedCredential(receiver: Receiver): RestCredential {
        val credential = lookupDefaultCredential(receiver)
        val privateKey = AuthUtils.readPrivateKeyPem((credential as UserApiKeyCredential).apiKey)
        val senderToken = AuthUtils.generateOrganizationToken(
            Organization(
                receiver.name, receiver.fullName,
                Organization.Jurisdiction.FEDERAL, null, null, null, null, null
            ),
            "", privateKey, ""
        )
        return UserApiKeyCredential(credential.user, senderToken)
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

    /**
     * Get the OAuth token based on credential type
     *
     * @param restTransportInfo The URL to post to get the OAuth token
     * @param reportId The Assertion credential
     * @param jksCredential The jks credential
     * @param context Really just here to get logging injected
     */
    suspend fun getOAuthToken(
        restTransportInfo: RESTTransportType,
        reportId: String,
        jksCredential: UserJksCredential?,
        credential: RestCredential,
        logger: Logger,
    ): Pair<Map<String, String>, BearerTokens?> {
        var httpHeaders = restTransportInfo.headers.mapValues {
            if (it.value == "header.reportFile.reportId") {
                reportId
            } else {
                it.value
            }
        }
        val tokenClient = httpClient ?: createDefaultHttpClient(jksCredential, null)
        // get the credential and use it to request an OAuth token
        // Usually credential is a UserApiKey, with an apiKey field (NY)
        // if not, try as UserPass with pass field (OK)
        val tokenInfo: TokenInfo
        var bearerTokens: BearerTokens? = null
        when (credential) {
            is UserApiKeyCredential -> {
                tokenInfo = getAuthTokenWithUserApiKey(
                    restTransportInfo,
                    credential,
                    logger,
                    tokenClient
                )
                // if successful, add the token returned to the token storage
                bearerTokens = BearerTokens(tokenInfo.accessToken, tokenInfo.refreshToken ?: "")
            }

            is UserPassCredential -> {
                tokenInfo = getAuthTokenWithUserPass(
                    restTransportInfo.authTokenUrl,
                    credential,
                    logger,
                    tokenClient
                )
                // if successful, add token as "Authorization:" header
                httpHeaders = httpHeaders + Pair("Authorization", tokenInfo.accessToken)
            }

            is UserAssertionCredential -> {
                tokenInfo = getAuthTokenWithAssertion(
                    restTransportInfo.authTokenUrl,
                    credential,
                    logger,
                    tokenClient
                )
                // if successful, add token as "Authorization:" header
                bearerTokens = BearerTokens(tokenInfo.accessToken, tokenInfo.refreshToken ?: "")
            }

            else -> error("UserApiKey or UserPass credential required")
        }
        return Pair(httpHeaders, bearerTokens)
    }

    /**
     * Get the OAuth token by submitting Assertion credential
     * as OAuth 2.0 Client Credentials Grant Type, used by WA
     *
     * @param restUrl The URL to post to get the OAuth token
     * @param credential The Assertion credential
     * @param context Really just here to get logging injected
     * @param httpClient the HTTP client to make the call
     */
    suspend fun getAuthTokenWithAssertion(
        restUrl: String,
        credential: UserAssertionCredential,
        logger: Logger,
        httpClient: HttpClient,
    ): TokenInfo {
        httpClient.use { client ->
            val tokenInfo: TokenInfo = client.submitForm(
                restUrl,
                formParameters = Parameters.build {
                    append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer") // as specified by WA
                    append("assertion", credential.assertion)
                }

            ) {
                expectSuccess = true // throw an exception if not successful
            }.body()
            logger.info("Got Token with UserAssertion")
            return tokenInfo
        }
    }

    /**
     * Get the OAuth token by submitting UserApiKey credentials
     * as OAuth 2.0 Client Credentials Grant Type, used by NY
     *
     * @param restUrl The URL to post to get the OAuth token
     * @param credential The API key credential with user for clientID and apiKey for clientSecret
     * @param context Really just here to get logging injected
     * @param httpClient the HTTP client to make the call
     */
    suspend fun getAuthTokenWithUserApiKey(
        restTransportInfo: RESTTransportType,
        credential: UserApiKeyCredential,
        logger: Logger,
        httpClient: HttpClient,
    ): TokenInfo {
        httpClient.use { client ->
            val tokenInfo: TokenInfo = client.submitForm(
                restTransportInfo.authTokenUrl,
                formParameters = Parameters.build {
                    if (restTransportInfo.parameters.isEmpty()) {
                        // This block is for backward compatible since old
                        // REST Transport doesn't have parameters.
                        append("grant_type", "client_credentials")
                        append("client_id", credential.user)
                        append("client_secret", credential.apiKey)
                    } else {
                        restTransportInfo.parameters.forEach { param ->
                            when (param.value) {
                                "client_id" -> append(param.key, credential.user)
                                "client_secret" -> append(param.key, credential.apiKey)
                                else -> append(param.key, param.value)
                            }
                        }
                    }
                }
            ) {
                expectSuccess = true // throw an exception if not successful
                accept(ContentType.Application.Json)
            }.body()
            logger.info("Got Token with UserApiKey")
            return tokenInfo
        }
    }

    /**
     * Get the OAuth token by submitting UserPass credentials
     * as id-token, used by OK, and CDC NBS
     *
     * @param restUrl The URL to post to get the OAuth token
     * @param credential The UserPass credential with user for email and pass for password
     * @param context Really just here to get logging injected
     * @param httpClient the HTTP client to make the call
     */
    suspend fun getAuthTokenWithUserPass(
        restUrl: String,
        credential: UserPassCredential,
        logger: Logger,
        httpClient: HttpClient,
    ): TokenInfo {
        httpClient.use { client ->
            if (restUrl.contains("dataingestion.test.nbspreview.com")) {
                val idTokenInfoString: String = client.post(restUrl) {
                    val credentialString = credential.user + ":" + credential.pass
                    val basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentialString.encodeToByteArray())
                    expectSuccess = true // throw an exception if not successful
                    postHeaders(
                        mapOf(
                            "Authorization" to basicAuth,
                            "Host" to "dataingestion.test.nbspreview.com"
                        )
                    )
                }.body()
                logger.info("Got Token with UserPass")
                return TokenInfo(accessToken = "Bearer " + idTokenInfoString, expiresIn = 3600)
            } else {
                val idTokenInfoString: String = client.post(restUrl) {
                    expectSuccess = true // throw an exception if not successful
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("email" to credential.user, "password" to credential.pass))
                }.body()
                logger.info("Got Token with UserPass")
                val idTokenInfo = Json.decodeFromString<IdToken>(idTokenInfoString)
                return TokenInfo(accessToken = idTokenInfo.idToken, expiresIn = 3600)
            }
        }
    }

    /**
     * Post the report as HL7 to the REST service. This is based on SoapTransport connectToSoapService
     * This is a suspend function, meaning it can get called as an async method, though we call it
     * a blocking way.
     *
     * @param message The report we want to send as a ByteArray
     * @param restUrl The URL to post the report to
     * @param context Really just here to get logging injected
     */
    suspend fun postReport(
        message: ByteArray,
        fileName: String,
        restUrl: String,
        headers: Map<String, String>,
        logger: Logger,
        httpClient: HttpClient,
    ): HttpResponse {
        logger.info(fileName)
        val boundary = "WebAppBoundary"
        httpClient.use { client ->
            val theResponse: HttpResponse = client.post(restUrl) {
                logger.info("posting report to rest API")
                expectSuccess = true // throw an exception if not successful

                // NBS requires the Content-Length to be set
                val newHeaders = if (restUrl.contains("dataingestion.test.nbspreview.com")) {
                    headers.plus(mapOf("Content-Length" to message.size.toString()))
                } else {
                    headers
                }

                postHeaders(
                    newHeaders
                )
                setBody(
                    when (restUrl.substringAfterLast('/')) {
                        // OK or NBS
                        "hl7", "reports" -> {
                            TextContent(message.toString(Charsets.UTF_8), ContentType.Text.Plain)
                        }
                        // Flexion
                        "demographics" -> {
                            TextContent(message.toString(Charsets.UTF_8), ContentType.Application.Json)
                        }

                        "orders" -> {
                            TextContent(message.toString(Charsets.UTF_8), ContentType.Application.Json)
                        }
                        // WA
                        "elr" -> {
                            contentType(ContentType.Application.Json)
                            // create JSON object for the BODY. This encodes "/" character as "//", needed for WA to accept as valid JSON
                            JSONObject().put("body", message.toString(Charsets.UTF_8)).toString()
                        }

                        else -> {
                            // NY
                            MultiPartFormDataContent(
                                formData {
                                    append(
                                        "payload", message,
                                        Headers.build {
                                            append(HttpHeaders.ContentType, "text/plain")
                                            append(HttpHeaders.ContentDisposition, "filename=\"${fileName}\"")
                                        }
                                    )
                                },
                                boundary
                            )
                        }
                    }
                )
                accept(ContentType.Application.Json)
            }
            return theResponse
        }
    }

    companion object {
        /** A default value for the timeouts to connect and send messages */
        private const val TIMEOUT = 120_000

        /** Our default Http Client, with an optional SSL context, and optional auth token */
        private fun createDefaultHttpClient(jks: UserJksCredential?, bearerTokens: BearerTokens?): HttpClient {
            return HttpClient(Apache) {
                // installs logging into the call to post to the server
                install(Logging) {
                    logger = io.ktor.client.plugins.logging.Logger.Companion.SIMPLE
                    level = LogLevel.INFO // LogLevel.INFO for prod, LogLevel.ALL to view full request
                }
                // if we have a token, install it
                bearerTokens?.let {
                    install(Auth) {
                        bearer {
                            loadTokens {
                                bearerTokens
                            }
                        }
                    }
                }
                // install contentNegotiation to handle json response
                install(ContentNegotiation) {
                    json(
                        Json {
                            prettyPrint = true
                            isLenient = true
                            ignoreUnknownKeys = true
                        }
                    )
                }
                // configures the Apache client with our specified timeouts
                // if we have a JKS, create an SSL context with it
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
            // Open the keystore in the UserJksCredential, it's a PKCS12 type
            val jksDecoded = Base64.getDecoder().decode(jksCredential.jks)
            val inStream: InputStream = jksDecoded.inputStream()
            val jksPasscode = jksCredential.jksPasscode.toCharArray()
            val keyStore: KeyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(inStream, jksPasscode)
            // create keyManager with the keystore, use default SunX509 algorithm
            val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()) // "SunX509")
            keyManagerFactory.init(keyStore, jksPasscode)
            // create the sslContext, type TLS, with the keyManager
            // note the ktor sample code uses a TrustManger instead, but that fails with the NY cert file
            val sslContext = SSLContext.getInstance("TLSv1.3")
            sslContext.init(keyManagerFactory.keyManagers, null, null)
            return sslContext
        }

        /***
         * Builds header unit from map
         * @param [header] is a map of all values provided in receiver setting
         * @return Unit with all headers appended
         */
        private fun HttpMessageBuilder.postHeaders(header: Map<String, String>): Unit =
            header.forEach { entry ->
                headers.append(entry.key, entry.value)
            }
    }
}