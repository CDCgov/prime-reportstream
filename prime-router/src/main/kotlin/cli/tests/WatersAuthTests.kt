package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.PrintMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.cli.DeleteSenderSetting
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.cli.GetSenderSetting
import gov.cdc.prime.router.cli.OktaCommand
import gov.cdc.prime.router.cli.PutSenderSetting
import gov.cdc.prime.router.cli.SettingCommand
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.SenderUtils
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

/**
 *
 */
class WatersAuthTests : CoolTest() {
    override val name = "watersauth"
    override val description = "Test Auth of various waters endpoints"
    override val status = TestStatus.DRAFT // Not SMOKE because it requires login to do settings stuff.  Can't automate

    private val accessTokenDummy = "dummy"
    private lateinit var settingsEnv: Environment

    fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }

    /**
     * Create a fake report file using the schema expected by [sender].  Creates a file locally.  Does not submit it.
     */
    fun createFakeReport(sender: Sender): File {
        assert(sender.format == Sender.Format.CSV)
        return FileUtilities.createFakeFile(
            metadata,
            settings,
            sender = sender,
            count = 1,
            format = Report.Format.CSV,
            directory = System.getProperty("java.io.tmpdir"),
            targetStates = null,
            targetCounties = null
        )
    }

    fun getOktaSettingsAccessTok(): String {
        val oktaSettingAccessTok = if (settingsEnv.oktaApp == null) accessTokenDummy else {
            OktaCommand.fetchAccessToken(settingsEnv.oktaApp!!)
                ?: abort(
                    "Test needs a valid okta access token for the settings API." +
                        " Run ./prime login to fetch/refresh your access token."
                )
        }
        return oktaSettingAccessTok
    }

    /**
     * Utility function to attach a new sender to an existing organization.
     */
    fun createNewSenderForExistingOrg(senderName: String, orgName: String): Sender {
        val newSender = Sender(
            name = senderName,
            organizationName = orgName,
            format = Sender.Format.CSV,
            topic = "covid-19",
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "primedatainput/pdi-covid-19"
        )

        val oktaSettingAccessTok = getOktaSettingsAccessTok()

        // save the new sender to the Settings.
        PutSenderSetting()
            .put(
                settingsEnv,
                oktaSettingAccessTok,
                SettingCommand
                    .SettingType.SENDER,
                newSender.fullName,
                jacksonObjectMapper().writeValueAsString(newSender)
            )

        // query the API get the sender previously written
        val savedSenderJson = GetSenderSetting().get(
            settingsEnv,
            oktaSettingAccessTok,
            SettingCommand.SettingType.SENDER,
            newSender.fullName
        )

        // deserialize the sender obj we got back from the API
        return jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(savedSenderJson, Sender::class.java)
            ?: error("Unable to save sender")
    }

    /**
     * Utility function to associate a public [key] (named with key id [kid])
     * to an existing [sender] and store in on the database.
     * The [key] gives permission to an authenticated sender requesting [scope].
     * @returns a new copy of the old sender object, now with the key added to it.
     */
    private fun saveSendersKey(sender: Sender, key: String, kid: String, scope: String): Sender {
        // associate a public key to the sender
        val publicKeyStr = SenderUtils.readPublicKeyPem(key)
        publicKeyStr.kid = kid
        val senderPlusNewKey = Sender(sender, scope, publicKeyStr)

        // save the sender with the new key
        PutSenderSetting()
            .put(
                settingsEnv,
                getOktaSettingsAccessTok(),
                SettingCommand.SettingType.SENDER,
                senderPlusNewKey.fullName,
                jacksonObjectMapper().writeValueAsString(senderPlusNewKey)
            )
        return senderPlusNewKey
    }

    /**
     * Given a [privateKeyStr], key id [kid] and requested [scope], coming from [sender],
     * this tries to retrieve a 5 minute access token
     * @returns the Pair (http response code, json bod of the response)
     */
    private fun getWatersAccessTok(
        sender: Sender,
        environment: Environment,
        privateKeyStr: String,
        kid: String,
        scope: String
    ): Pair<Int, String> {
        val baseUrl = environment.url.toString() + HttpUtilities.tokenApi
        val privateKey = SenderUtils.readPrivateKeyPem(privateKeyStr)
        val senderSignedJWT = SenderUtils.generateSenderToken(sender, baseUrl, privateKey, kid)
        val senderTokenUrl =
            SenderUtils.generateSenderUrl(settingsEnv, senderSignedJWT, scope)

        return HttpUtilities.postHttp(senderTokenUrl.toString(), "".toByteArray())
    }

    /**
     * Delete a sender from the settings.  Used at the end of a test to teardown.
     */
    private fun deleteSender(sender: Sender) {
        DeleteSenderSetting()
            .delete(
                settingsEnv,
                getOktaSettingsAccessTok(),
                SettingCommand.SettingType.SENDER,
                sender.fullName
            )
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        var passed = true
        settingsEnv = environment
        passed = passed and doEcAndRsaEcKeyTests(environment)
        passed = passed and doAllSubmissionAuthTests(environment)
        return passed
    }

    private fun doEcAndRsaEcKeyTests(environment: Environment): Boolean {
        var passed = true
        val myOrg = "ignore"
        val mySenderName = "temporary_sender_auth_test"
        val mySenderFullName = "$myOrg.$mySenderName"
        val myScope = "$mySenderFullName.report"
        val mySender = createNewSenderForExistingOrg(mySenderName, myOrg)

        try {
            val myFakeReportFile = createFakeReport(mySender)

            // first, try to send a report with a bogus token
            ugly("Starting $name test of server-server authentication using keypairs:")
            val (responseCode, _) = HttpUtilities.postReportFileToWatersApi(
                environment,
                myFakeReportFile,
                mySender,
                "a.b.c"
            )

            if (responseCode == 401) {
                good("Attempt to send bogus token with no auth rightly failed.")
            } else {
                bad("Should get a 401 response while sending report with bogus token. Instead got $responseCode")
                passed = false
            }

            // EC tests
            "testing-kid-ec".let { kid ->
                // associate a key to the sender
                saveSendersKey(mySender, end2EndExampleECPublicKeyStr, kid, myScope)

                // attempt to get an access token with an invalid private key
                val (responseCode2, _) =
                    getWatersAccessTok(mySender, environment, end2EndExampleECPrivateInvalidKeyStr, kid, myScope)
                if (responseCode2 == 401) {
                    good("EC key: Attempt to get a token with invalid private key rightly failed.")
                } else {
                    bad("EC key: Should get a 401 response to invalid private key but instead got $responseCode2")
                    passed = false
                }

                // get a valid private key
                val (httpStatusGetToken, responseGetToken) =
                    getWatersAccessTok(mySender, environment, end2EndExampleECPrivateKeyStr, kid, myScope)
                val watersAccessTok = jacksonObjectMapper().readTree(responseGetToken).get("access_token").textValue()

                if (httpStatusGetToken == 200) {
                    good("EC key: Attempt to get a token with valid sender key succeeded.")
                } else {
                    bad("EC key: Should get a 200 response to getToken instead got $httpStatusGetToken")
                    passed = false
                }

                // but now try to send a report with a tampered access token
                val (responseCode3, json3) =
                    HttpUtilities.postReportFileToWatersApi(
                        environment,
                        myFakeReportFile,
                        mySender,
                        watersAccessTok.reversed()
                    )

                if (responseCode3 == 401) {
                    good("EC key: Attempt to send a report with tampered token rightly failed.")
                } else {
                    bad(
                        "EC key: " +
                            "Should get a 401 response to tampered token but instead got $responseCode3  " + json3
                    )
                    passed = false
                }

                // finally, now try to send a report with a valid access token
                val (responseCode4, _) =
                    HttpUtilities.postReportFileToWatersApi(environment, myFakeReportFile, mySender, watersAccessTok)

                if (responseCode4 == 201) {
                    good("EC key: Got a 201 back from post with valid token.")
                } else {
                    bad("EC key: Should get a 201 response to valid token, but but instead got $responseCode4")
                    passed = false
                }
            }

            // RSA Tests
            "testing-kid-rsa".let { kid ->
                // associate a key to the sender
                saveSendersKey(mySender, end2EndExampleRSAPublicKeyStr, kid, myScope)

                // try to get an access token with an invalid private key
                val (responseCode2, _) =
                    getWatersAccessTok(mySender, environment, end2EndExampleRSAPrivateInvalidKeyStr, kid, myScope)
                if (responseCode2 == 401) {
                    good("RSA key: Attempt to get a token with invalid private key rightly failed.")
                } else {
                    bad("RSA key: Should get a 401 response to invalid private key but instead got $responseCode2")
                    passed = false
                }

                // get an access token with a valid private key
                val (httpStatusGetToken, responseGetToken) =
                    getWatersAccessTok(mySender, environment, end2EndExampleRSAPrivateKeyStr, kid, myScope)
                val server2ServerAccessTok =
                    jacksonObjectMapper().readTree(responseGetToken).get("access_token").textValue()

                if (httpStatusGetToken == 200) {
                    good("RSA key: Attempt to get a token with valid sender key succeeded.")
                } else {
                    bad("RSA key: Should get a 200 response to getToken instead got $httpStatusGetToken")
                    passed = false
                }

                // try to send a report with a tampered access token
                val (responseCode3, json3) =
                    HttpUtilities.postReportFileToWatersApi(
                        environment,
                        myFakeReportFile,
                        mySender,
                        server2ServerAccessTok.reversed()
                    )

                if (responseCode3 == 401) {
                    good("RSA key: Attempt to send a report with a tampered token rightly failed.")
                } else {
                    bad(
                        "RSA key: " +
                            "Should get a 401 response to tampered token but instead got $responseCode3  " + json3
                    )
                    passed = false
                }

                // try to send a report with valid access token
                val (responseCode4, _) =
                    HttpUtilities.postReportFileToWatersApi(
                        environment,
                        myFakeReportFile,
                        mySender,
                        server2ServerAccessTok
                    )

                if (responseCode4 == 201) {
                    good("RSA key: Got a 201 back from post with valid token.")
                } else {
                    bad("RSA key: Should get a 201 response to valid token, but but instead got $responseCode4")
                    passed = false
                }
            }
        } finally {
            deleteSender(mySender)
        }
        return passed
    }

    /**
     * Run a wide variety of tests of auth of the submissions API endpoints.
     * This assumes settingsAccessTok has a valid token and that the 5 minutes has not expired.
     * Use it to perform a series of auth tests on our submission endpoints.
     */
    private fun doAllSubmissionAuthTests(
        environment: Environment,
    ): Boolean {
        var passed = true

        // First, quite a lot of setup for the testing:
        // Make sure the setting has a key with a scope that enables submission history calls.
        // Then get a 5-minute token, and submit a single file.

        val mySenderName = "temporary_submission_auth_test"
        val kid = "submission-testing-kid"

        // We need to submit reports to two separate orgs to do this testing.

        if (environment == Environment.PROD) error("Can't create simple_report test data in PROD")
        val org1 = "simple_report"
        val sender1 = createNewSenderForExistingOrg(mySenderName, org1)
        // Right now in order to access submission history you must have the default scope
        // This attaches the ignore.default.report scope claim to the ignore.temporary_sender_auth_test.report Sender.
        val scope1 = "$org1.default.report"
        val uploadReportScope1 = "$org1.$mySenderName.report"
        // Submit this new scope and public key to the Settings store, associated with this Sender.
        saveSendersKey(sender1, end2EndExampleRSAPublicKeyStr, kid, scope1)
        saveSendersKey(sender1, end2EndExampleRSAPublicKeyStr, kid, uploadReportScope1)

        val org2 = "ignore"
        val sender2 = createNewSenderForExistingOrg(mySenderName, org2)
        // Right now in order to access submission history you must have the default scope
        // This attaches the ignore.default.report scope claim to the ignore.temporary_sender_auth_test.report Sender.
        val scope2 = "$org2.default.report"
        val uploadReportScope2 = "$org2.$mySenderName.report"
        // Submit this new scope and public key to the Settings store, associated with this Sender.
        saveSendersKey(sender2, end2EndExampleRSAPublicKeyStr, kid, scope2)
        saveSendersKey(sender2, end2EndExampleRSAPublicKeyStr, kid, uploadReportScope2)

        try {
            val myFakeReportFile = createFakeReport(sender1)

            // Now request 5-minute tokens for the first org
            val (httpStatus1, responseToken1) =
                getWatersAccessTok(sender1, environment, end2EndExampleRSAPrivateKeyStr, kid, scope1)
            if (httpStatus1 != 200) {
                return bad("Should get a 200 response to getToken instead got $httpStatus1")
            }
            val token1 = jacksonObjectMapper().readTree(responseToken1).get("access_token").textValue()
            val (httpStatus1a, responseToken1a) =
                getWatersAccessTok(sender1, environment, end2EndExampleRSAPrivateKeyStr, kid, uploadReportScope1)
            if (httpStatus1a != 200) {
                return bad("Should get a 200 response to getToken instead got $httpStatus1a")
            }
            val uploadToken1 = jacksonObjectMapper().readTree(responseToken1a).get("access_token").textValue()

            // And a 5-minute token from the second org2
            val (httpStatus2, responseToken2) =
                getWatersAccessTok(sender2, environment, end2EndExampleRSAPrivateKeyStr, kid, scope2)
            if (httpStatus2 != 200) {
                return bad("Should get a 200 response to getToken instead got $httpStatus2")
            }
            val token2 = jacksonObjectMapper().readTree(responseToken2).get("access_token").textValue()
            val (httpStatus2a, responseToken2a) =
                getWatersAccessTok(sender2, environment, end2EndExampleRSAPrivateKeyStr, kid, uploadReportScope2)
            if (httpStatus2a != 200) {
                return bad("Should get a 200 response to getToken instead got $httpStatus2a")
            }
            val uploadToken2 = jacksonObjectMapper().readTree(responseToken2a).get("access_token").textValue()

            // Now submit a report to org1 and get its reportId1
            val (responseCode1, json1) = HttpUtilities.postReportFileToWatersApi(
                environment, myFakeReportFile, sender1, uploadToken1
            )
            val reportId1 = if (responseCode1 == 201) {
                getReportIdFromResponse(json1)
            } else {
                bad("Should get a 201 response to valid token, but but instead got $responseCode1")
                passed = false
                null
            }
            good("Test sending report $reportId1 to $org1 successful")

            // And submit a report to org2 and get its reportId2
            val (responseCode2, json2) = HttpUtilities.postReportFileToWatersApi(
                environment, myFakeReportFile, sender2, uploadToken2
            )
            val reportId2 = if (responseCode2 == 201) {
                getReportIdFromResponse(json2)
            } else {
                bad("Should get a 201 response to valid token, but but instead got $responseCode2")
                passed = false
                null
            }

            // Setup is done!   Ready to run some tests.
            passed = passed and oktaPrimeAdminSubmissionListAuthTests(environment, org1, org2)
            passed = passed and server2ServerSubmissionListAuthTests(environment, org1, org2, token1, token2)
            passed = passed and server2ServerSubmissionDetailsAuthTests(environment, reportId2, token2)
        } finally {
            deleteSender(sender1)
            deleteSender(sender2)
        }
        return passed
    }

    /**
     * Run a suite of tests against the submission list endpoint, with the assumption that the
     * user has already created a PrimeAdmin token.
     */
    private fun oktaPrimeAdminSubmissionListAuthTests(
        environment: Environment,
        orgName1: String,
        orgName2: String,
    ): Boolean {
        val advice = "Run   ./prime login --env staging    " +
            "to fetch/refresh a **PrimeAdmin** access token for the Staging environment."
        val oktaToken = OktaCommand.fetchAccessToken(OktaCommand.OktaApp.DH_STAGE) ?: abort(
            "The Okta PrimeAdmin tests use a Staging Okta token, even locally, which is not available. $advice"
        )
        val testCases = listOf(
            HistoryApiTestCase(
                "Get submissions to $orgName1",
                "${environment.url}/api/waters/org/$orgName1/submissions",
                mapOf("authentication-type" to "okta"),
                listOf("pagesize" to 1),
                oktaToken,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get submissions to $orgName1.default",
                "${environment.url}/api/waters/org/$orgName1.default/submissions",
                mapOf("authentication-type" to "okta"),
                listOf("pagesize" to 1),
                oktaToken,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get submissions to $orgName2",
                "${environment.url}/api/waters/org/$orgName2/submissions",
                mapOf("authentication-type" to "okta"),
                listOf("pagesize" to 1),
                oktaToken,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get submissions to $orgName2.default",
                "${environment.url}/api/waters/org/$orgName2.default/submissions",
                mapOf("authentication-type" to "okta"),
                listOf("pagesize" to 1),
                oktaToken,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
        )
        val historyApiTest = HistoryApiTest()
        historyApiTest.outputToConsole = this.outputToConsole
        val passed = historyApiTest.runHistoryTestCases(testCases)
        if (!passed) {
            bad("One or more Okta PrimeAdmin tests failed. You might need to:  $advice")
        }
        return passed
    }
    /**
     * Run a suite of tests against the submission list endpoint.
     */
    private fun server2ServerSubmissionListAuthTests(
        environment: Environment,
        orgName1: String,
        orgName2: String,
        token1: String,
        token2: String,
    ): Boolean {
        ugly("Starting $name Test: submission list queries using server2server auth.")
        val testCases = mutableListOf(
            // A variety of auth test cases, against the submission list queries
            // curl -H "Authorization: Bearer $TOK" "http://localhost:7071/api/waters/org/ignore/submissions?pagesize=1"
            HistoryApiTestCase(
                "Get submissions to $orgName1 with correct tok",
                "${environment.url}/api/waters/org/$orgName1/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                token1,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get submissions to $orgName1.default with correct auth",
                "${environment.url}/api/waters/org/$orgName1.default/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                token1,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get submissions to a bogus organization",
                "${environment.url}/api/waters/org/BOGOSITY/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                token1,
                HttpStatus.UNAUTHORIZED,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get submissions to $orgName2 with $orgName1 auth",
                "${environment.url}/api/waters/org/$orgName2/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                token1, // using token for org1 to access org2.  Not allowed.
                HttpStatus.UNAUTHORIZED,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get submissions to oh-doh receiver with $orgName1 auth",
                "${environment.url}/api/waters/org/oh-doh/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                token1,
                HttpStatus.UNAUTHORIZED,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get submissions to $orgName1 with $orgName2 auth",
                "${environment.url}/api/waters/org/$orgName1/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                token2, // using token for org2 to access org1.  Not allowed.
                HttpStatus.UNAUTHORIZED,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
        )
        val historyApiTest = HistoryApiTest()
        historyApiTest.outputToConsole = this.outputToConsole
        return historyApiTest.runHistoryTestCases(testCases)
    }

    private fun server2ServerSubmissionDetailsAuthTests(
        environment: Environment,
        reportId: ReportId?,
        server2ServerAccessTok: String
    ): Boolean {
        ugly("Starting $name Test: submission DETAILS using server2server auth. Querying for report $reportId.")
        if (reportId == null) {
            return bad("Unable to do server2ServerSubmissionDetailsAuthTests:  no reportId to test with")
        }
        val testCases = mutableListOf(
            // Example
            // curl -H "Authorization: Bearer $TOK" "http://localhost:7071/api/waters/report/<UUID>/history"
            HistoryApiTestCase(
                "Get submission details for a report: Happy path",
                "${environment.url}/api/waters/report/$reportId/history",
                emptyMap(),
                emptyList(),
                server2ServerAccessTok,
                HttpStatus.OK,
                expectedReports = setOf(reportId),
                SubmissionDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get submission details for a nonexistent report:",
                "${environment.url}/api/waters/report/87a02e0c-5b77-4595-a039-e143fbaadda2/history",
                emptyMap(),
                emptyList(),
                server2ServerAccessTok,
                HttpStatus.NOT_FOUND,
                expectedReports = setOf(UUID.fromString("87a02e0c-5b77-4595-a039-e143fbaadda2")),
                SubmissionDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get submission details for a bogus report:",
                "${environment.url}/api/waters/report/BOGOSITY/history",
                emptyMap(),
                emptyList(),
                server2ServerAccessTok,
                HttpStatus.NOT_FOUND,
                expectedReports = setOf(reportId),
                SubmissionDetailsChecker(this),
                doMinimalChecking = false,
            ),
        )
        val historyApiTest = HistoryApiTest()
        historyApiTest.outputToConsole = this.outputToConsole
        return historyApiTest.runHistoryTestCases(testCases)
    }
}

/**
 * Exercise the database jticache
 */
class Jti : CoolTest() {
    override val name = "jti"
    override val description = "Test the JTI Cache"
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting jti Test: $description")
        val db = WorkflowEngine().db
        val jtiCache = DatabaseJtiCache(db)
        var passed = true
        val uuid1 = UUID.randomUUID().toString()
        if (!jtiCache.isJTIOk(uuid1, OffsetDateTime.now())) {
            echo("JTI-1 $uuid1 has never been seen before.   It should have been OK, but was not.")
            passed = false
        }
        val uuid2 = UUID.randomUUID().toString()
        if (!jtiCache.isJTIOk(uuid2, OffsetDateTime.now().plusMinutes(10))) {
            echo("JTI-2 $uuid2 has never been seen before.   It should have been OK, but was not.")
            passed = false
        }
        val uuid3 = UUID.randomUUID().toString()
        if (!jtiCache.isJTIOk(uuid3, OffsetDateTime.now().minusMinutes(10))) {
            echo("JTI-3 $uuid3 has never been seen before.   It should have been OK, but was not.")
            passed = false
        }
        // Now send them all again.  All should return false
        if (jtiCache.isJTIOk(uuid1, OffsetDateTime.now())) {
            echo("JTI-1 $uuid1 has been seen before.   It should have failed, but it passed.")
            passed = false
        }
        if (jtiCache.isJTIOk(uuid2, OffsetDateTime.now())) {
            echo("JTI-2 $uuid2 has been seen before.   It should have failed, but it passed.")
            passed = false
        }
        if (jtiCache.isJTIOk(uuid3, OffsetDateTime.now())) {
            echo("JTI-3 $uuid3 has been seen before.   It should have failed, but it passed.")
            passed = false
        }
        if (passed) {
            good("JTI Database Cache test passed")
        } else {
            bad("JTI Database Cache test ****FAILED***")
        }
        return passed
    }
}