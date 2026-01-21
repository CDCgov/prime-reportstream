package gov.cdc.prime.router.transport

import com.google.common.base.Preconditions
import com.microsoft.azure.functions.ExecutionContext
import gov.cdc.prime.router.Cryptography
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.RESTTransportType
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.credentials.CredentialHelper
import gov.cdc.prime.router.credentials.CredentialRequestReason
import gov.cdc.prime.router.credentials.RestCredential
import gov.cdc.prime.router.credentials.UserApiKeyCredential
import gov.cdc.prime.router.credentials.UserAssertionCredential
import gov.cdc.prime.router.credentials.UserJksCredential
import gov.cdc.prime.router.credentials.UserPassCredential
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.tokens.AuthUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
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
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
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
        externalFileName: String,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory,
        reportEventService: IReportStreamEventService,
        reportService: ReportService,
        lineages: List<ItemLineage>?,
        queueMessage: String,
    ): RetryItems? {
        val logger: Logger = context.logger

        val restTransportInfo = transportType as RESTTransportType
        val reportId = "${header.reportFile.reportId}"
        val receiver = header.receiver ?: error("No receiver defined for report $reportId")
        var reportContent: ByteArray = header.content ?: error("No content for report $reportId")

        val (credential, jksCredential) = getCredential(restTransportInfo, receiver)

        val client: HttpClient = HttpClientFactory.getClient(jksCredential)

        return try {
            // run our call to the endpoint in a blocking fashion
            runBlocking {
                launch {
                    try {
                        val httpHeaders = getHeaders(restTransportInfo, reportId)

                        val accessToken = getAccessToken(
                            restTransportInfo,
                            jksCredential,
                            credential,
                            httpHeaders,
                            logger
                        )

                        // only add a Bearer header for OAuth/JWT flows, not raw API-key flows
                        if (restTransportInfo.authType != "apiKey") {
                            accessToken?.let {
                                httpHeaders["Authorization"] = "${getAuthorizationHeader(restTransportInfo)} $it"
                            }
                        }

                        // If encryption is needed.
                        if (restTransportInfo.encryptionKeyUrl.isNotEmpty()) {
                            reportContent = encryptTheReport(
                                reportContent,
                                restTransportInfo.encryptionKeyUrl,
                                httpHeaders,
                                httpClient ?: client
                            )
                        }

                        // post the report
                        val response = postReport(
                            reportContent,
                            externalFileName,
                            restTransportInfo.reportUrl,
                            httpHeaders,
                            logger,
                            httpClient ?: client,
                            header.reportFile.createdAt
                        )
                        val responseBody = response.bodyAsText()
                        // update the action history
                        val msg = "Success: REST transport of $externalFileName to $restTransportInfo:\n$responseBody"
                        logger.info("Message successfully sent!")
                        actionHistory.trackActionResult(response.status, msg)
                        actionHistory.trackSentReport(
                            receiver,
                            sentReportId,
                            externalFileName,
                            restTransportInfo.toString(),
                            msg,
                            header,
                            reportEventService,
                            reportService,
                            this::class.java.simpleName,
                            lineages,
                            queueMessage
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
            // probably don't want to retry unless it is a 429 and the issue is due to overzealous rate-limiting.
            when (t) {
                // do not retry on any response exception that is a client exception unless it is a 429
                is ResponseException -> {
                    if (t.response.status.value in 400..499 && t.response.status.value != 429) {
                        (t).let {
                            logger.severe(
                                "Received ${it.response.status.value}: ${it.response.status.description} " +
                                    "requesting ${it.response.request.url}. This is not recoverable. Will not retry."
                            )
                        }
                        actionHistory.setActionType(TaskAction.send_error)
                        actionHistory.trackActionResult(t.response.status, msg)
                        null
                    } else {
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
     * Get credential either two-legged or default.
     * @param transport the transport type of two-legged or default
     * @param receiver the receiver setting
     */
    fun getCredential(transport: RESTTransportType, receiver: Receiver): Pair<RestCredential, UserJksCredential?> {
        val jwtParams = transport.jwtParams
        val credential: RestCredential = if (transport.authType == "two-legged") {
            lookupTwoLeggedCredential(receiver, jwtParams)
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
            ?: error("Unable to find credentials for $receiverFullName using $credentialLabel")
    }

    /**
     * Get a credential using PrivateKey to generate signed JWT authentication token (senderToken)
     * @param receiver the fullName of the receiver is the label of the credential
     */
    fun lookupTwoLeggedCredential(receiver: Receiver, jwtParams: Map<String, String> = emptyMap()): RestCredential {
        val credential = lookupDefaultCredential(receiver)
        val privateKey = AuthUtils.readPrivateKeyPem((credential as UserApiKeyCredential).apiKey)
        val senderToken = if (jwtParams.isEmpty()) {
            AuthUtils.generateOrganizationToken(
                Organization(
                    receiver.name, receiver.fullName,
                    Organization.Jurisdiction.FEDERAL, null, null, null, null, null
                ),
                "", privateKey, ""
            )
        } else {
            val issuer = jwtParams["iss"] ?: receiver.name
            val audience = jwtParams["aud"] ?: ""
            AuthUtils.generateToken(issuer, audience, privateKey, "", UUID.randomUUID().toString())
        }
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
     * Generate headers
     * @param [restTransportInfo] holds receiver-specific Rest parameters
     * @param [reportId] report id to be added as header
     */
    fun getHeaders(
        restTransportInfo: RESTTransportType,
        reportId: String,
    ): MutableMap<String, String> = restTransportInfo.headers.mapValues {
            if (it.value == "header.reportFile.reportId") {
                reportId
            } else {
                it.value
            }
        }.toMutableMap()

    /**
     * Get the Accesstoken based on authType given in Restransport header
     *
     * @param restTransportInfo - Transport setting
     * @param jksCredential The jks credential
     */
    suspend fun getAccessToken(
        restTransportInfo: RESTTransportType,
        jksCredential: UserJksCredential?,
        credential: RestCredential,
        httpHeaders: MutableMap<String, String>,
        logger: Logger,
    ): String? {
        var accessToken: String? = null

        if (restTransportInfo.authType == "apiKey") {
            val apiKeyCredential = credential as UserApiKeyCredential
            httpHeaders["shared-api-key"] = apiKeyCredential.apiKey
            httpHeaders["System_ID"] = apiKeyCredential.user
            httpHeaders["Key"] = apiKeyCredential.apiKey
            accessToken = apiKeyCredential.apiKey
        }

        if (restTransportInfo.authType == "two-legged" || restTransportInfo.authType == null) {
            // parse headers for any dynamic values, OK needs the report ID
            accessToken = getOAuthToken(
                restTransportInfo,
                jksCredential,
                credential,
                logger
            )
            logger.info("Token successfully added!")
        }

        return accessToken
    }

    /**
     * Get the OAuth token based on credential type
     *
     * @param restTransportInfo - Transport setting
     * @param jksCredential The jks credential
     */
    suspend fun getOAuthToken(
        restTransportInfo: RESTTransportType,
        jksCredential: UserJksCredential?,
        credential: RestCredential,
        logger: Logger,
    ): String {
        val tokenClient = HttpClientFactory.getClient(jksCredential)
        // get the credential and use it to request an OAuth token
        // Usually credential is a UserApiKey, with an apiKey field (NY)
        // if not, try as UserPass with pass field (OK)
        val tokenInfo: TokenInfo
        when (credential) {
            is UserApiKeyCredential -> {
                tokenInfo = getAuthTokenWithUserApiKey(
                    restTransportInfo,
                    credential,
                    logger,
                    tokenClient
                )
            }

            is UserPassCredential -> {
                tokenInfo = getAuthTokenWithUserPass(
                    restTransportInfo,
                    credential,
                    logger,
                    tokenClient
                )
            }

            is UserAssertionCredential -> {
                tokenInfo = getAuthTokenWithAssertion(
                    restTransportInfo,
                    credential,
                    logger,
                    tokenClient
                )
            }

            else -> error("UserApiKey or UserPass credential required")
        }
        return tokenInfo.accessToken
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
        restTransportInfo: RESTTransportType,
        credential: UserAssertionCredential,
        logger: Logger,
        httpClient: HttpClient,
    ): TokenInfo {
        val tokenInfo: TokenInfo = httpClient.submitForm(
            restTransportInfo.authTokenUrl,
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
        val tokenInfo: TokenInfo = httpClient.submitForm(
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
        restTransportInfo: RESTTransportType,
        credential: UserPassCredential,
        logger: Logger,
        httpClient: HttpClient,
    ): TokenInfo {
        val restUrl = restTransportInfo.authTokenUrl
        val idTokenInfoString: String = httpClient.post(restUrl) {
            expectSuccess = true // throw an exception if not successful
            buildHeaders(
                if (restTransportInfo.authHeaders[AUTH_TYPE_HEADER] == BASIC) {
                    // Authorization-Type: "Basic Auth" requires the following:
                    // Header:
                    //  Authorization : "Basic + base64(username+password)"
                    // Body:
                    //  None
                    restTransportInfo.authHeaders.map { (key, value) ->
                        if (key == AUTH_TYPE_HEADER && value == BASIC) {
                            val credentialString = credential.user + ":" + credential.pass
                            Pair(
                                "Authorization",
                                "Basic " + Base64.getEncoder().encodeToString(credentialString.encodeToByteArray())
                            )
                        } else {
                            Pair(key, value)
                        }
                    }.toMap()
                } else {
                    restTransportInfo.authHeaders
                }
            )

            // Authorization-Type: username/password requires the following:
            // Header:
            //  Content-Type: application/json
            //  Authorization: username/password
            // Body:
            // { "username": "<username>", "password": "<password>"
            if (restTransportInfo.authHeaders[AUTH_TYPE_HEADER] == USERPASS) {
                setBody(mapOf("username" to credential.user, "password" to credential.pass))
            } else if (restTransportInfo.authHeaders[AUTH_TYPE_HEADER] == EMAILPASS) {
                // Authorization-Type: email/password requires the following:
                // Header:
                //  Content-Type: application/json
                //  Authorization: username/password
                // Body:
                // { "email": "<email@domain.com>", "password": "<password>"
                setBody(mapOf("email" to credential.user, "password" to credential.pass))
            }
        }.body()

        logger.info("Got Token with UserPass")

        val idTokenInfoAccessToken: String = try {
            Json.decodeFromString<IdToken>(idTokenInfoString).idToken
        } catch (e: Exception) {
            idTokenInfoString
        }

        return TokenInfo(accessToken = idTokenInfoAccessToken, expiresIn = 3600)
    }

    /**
     * getEncryptionKey extracts the encryption key from provided URL.  It is to be used to encrypt message
     * sending over the wire.
     *
     * @param encryptionKeyUrl - Url to extract the key from
     * @param headers - headers
     * @param httpClient - given http client engine
     *
     */
    suspend fun getEncryptionKey(
        encryptionKeyUrl: String,
        headers: Map<String, String>,
        httpClient: HttpClient,
    ): String = httpClient.get(encryptionKeyUrl) {
            buildHeaders(
                headers.map { (key, value) -> Pair(key, value) }.toMap()
            )
        }.body<String>()

    /**
     * Encrypt the report to the REST service. This is a suspend function, meaning it can get called as an
     * async method, though we call it a blocking way.
     *
     * @param message The report we want to send as a ByteArray
     * @param encryptionKeyUrl The URL to extract the encryption key
     * @param context Really just here to get logging injected
     */
    suspend fun encryptTheReport(
        message: ByteArray,
        encryptionKeyUrl: String,
        headers: Map<String, String>,
        httpClient: HttpClient,
    ): ByteArray {
        val encryptionKey = getEncryptionKey(encryptionKeyUrl, headers, httpClient)
        val jsonEncryptionKey = JSONObject(encryptionKey)
        val aesKey = Base64.getDecoder().decode(jsonEncryptionKey.get("aesKey").toString())
        val aesIV = Base64.getDecoder().decode(jsonEncryptionKey.get("aesIV").toString())

        val iv = IvParameterSpec(aesIV)
        val crypto = Cryptography()

        // Note: I discussed with CDC security expert, Mr. Stephen Nesman, and he said, it is ok to use the
        // AES/CBC/PKCS5Padding algorithm. Since, this is the internal, double, message encryption requested by
        // Natus. RS uses the TLS1.3 for external encryption SSL cert on the wire to communicate with they server.
        val algorithm = "AES/CBC/PKCS5Padding"

        val enKey: SecretKey = SecretKeySpec(aesKey, "AES")
        val encryptedMsg = crypto.encrypt(algorithm, message, enKey, iv)
        return encryptedMsg
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
        reportCreateDate: OffsetDateTime,
    ): HttpResponse {
        logger.info(fileName)
        val boundary = "WebAppBoundary"
            val response: HttpResponse = if (headers["method"] == "PUT") {
                httpClient.put(restUrl) {
                    build(logger, headers, message, fileName, boundary, reportCreateDate)
                }
            } else {
                httpClient.post(restUrl) {
                    build(logger, headers, message, fileName, boundary, reportCreateDate)
                }
            }

            return response
    }

    object HttpClientFactory {
        private const val TIMEOUT = 120_000
        private val cache = ConcurrentHashMap<String, HttpClient>()

        fun getClient(jksCredential: UserJksCredential?): HttpClient {
            val key = jksCredential?.jks ?: "DEFAULT"
            return cache.computeIfAbsent(key) {
                HttpClient(Apache) {
                    install(Logging) {
                        logger = io.ktor.client.plugins.logging.Logger.Companion.SIMPLE
                        level = LogLevel.INFO
                    }
                    install(ContentNegotiation) {
                        json(
                            Json {
                                prettyPrint = true
                                isLenient = true
                                ignoreUnknownKeys = true
                            }
                        )
                    }
                    engine {
                        jksCredential?.let { sslContext = getSslContext(it) }
                        followRedirects = true
                        socketTimeout = TIMEOUT
                        connectTimeout = TIMEOUT
                        connectionRequestTimeout = TIMEOUT
                    }
                }
            }
        }
    }

    companion object {

        private const val AUTH_TYPE_HEADER = "Authorization-Type"
        private const val BASIC = "Basic Auth"
        private const val USERPASS = "username/password"
        private const val EMAILPASS = "email/password"

        /** A default value for the timeouts to connect and send messages */
        private const val TIMEOUT = 120_000

        /** Get Authentication header.  It is hear to ease Unit Test */
        fun getAuthorizationHeader(
            restTransportInfo: RESTTransportType?,
        ): String = restTransportInfo?.headers?.get("BearerToken") ?: "Bearer"

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
        fun HttpMessageBuilder.buildHeaders(header: Map<String, String>): Unit =
            header.forEach { entry ->
                headers.append(entry.key, entry.value)
            }

        /**
         * Builds an HTTP request based on a [message] and the receiver's [headers] settings
         */
        fun HttpRequestBuilder.build(
            logger: Logger,
            headers: Map<String, String>,
            message: ByteArray,
            fileName: String,
            boundary: String,
            reportCreateDate: OffsetDateTime,
        ) {
            logger.info("sending report to rest API")
            expectSuccess = true // throw an exception if not successful

            // Calculate Content-Length if needed.
            buildHeaders(
                // Calculate Content-Length if needed.
                headers.map { (key, value) ->
                    if (key == "Content-Length" && value == "<calculated when request is sent>") {
                        Pair(key, message.size.toString())
                    } else {
                        Pair(key, value)
                    }
                }.toMap()
            )

            setBody(
                when (headers["Content-Type"]) {
                    // OK or NBS Content-Type: text/plain
                    "text/plain" -> {
                        TextContent(message.toString(Charsets.UTF_8), ContentType.Text.Plain)
                    }
                    // Flexion Content-Type: text/fhir+ndjson
                    "text/fhir+ndjson" -> {
                        TextContent(message.toString(Charsets.UTF_8), ContentType.Application.Json)
                    }
                    // WA Content-Type: application/json
                    "application/json" -> {
                        contentType(ContentType.Application.Json)
                        // create JSON object for the BODY. This encodes "/" character as "//", needed for WA to accept as valid JSON
                        JSONObject().put("body", message.toString(Charsets.UTF_8)).toString()
                    }
                    "application/hl7-v2" -> {
                        val filteredMsg = message.toString(Charsets.UTF_8).replace("\n", "\r").dropLast(1) + "\r"
                        TextContent(filteredMsg, ContentType("application", "hl7-v2"))
                    }
                    // NY Content-Type: multipart/form-data
                    "multipart/form-data" -> {
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    headers["Key"] ?: "payload", message,
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"${fileName}\"")
                                    }
                                )
                            },
                            boundary
                        )
                    }
                    "elims/json" -> {
                        contentType(ContentType.Application.Json)
                        val body = JSONObject()
                        body.put("System_ID", headers["System_ID"] ?: "")
                        body.put("Key", headers["Key"] ?: "")
                        body.put("DateReceived", reportCreateDate.toString())
                        body.put("FileName", fileName)
                        // This encodes "\" character as "\\", needed for Hl7 to be read as valid JSON
                        body.put("Message", message.toString(Charsets.UTF_8))
                        body.put("Comment", "")
                        body.toString()
                    }
                    else -> {
                        // Note: It is here for default content-type.  It is used for integration test
                        contentType(ContentType.Text.Plain)
                        val headerContentType = headers["Content-Type"]
                        logger.warning(
                            "Unsupported Content-Type: " +
                                "$headerContentType - please check your REST Transport setting"
                        )
                    }
                }
            )
            accept(ContentType.Application.Json)
        }
    }
}