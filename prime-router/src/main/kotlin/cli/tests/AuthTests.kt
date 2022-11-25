package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.PrintMessage
import com.microsoft.azure.functions.HttpStatus
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.Report
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.cli.DeleteSenderSetting
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.cli.GetSenderSetting
import gov.cdc.prime.router.cli.LookupTableEndpointUtilities
import gov.cdc.prime.router.cli.OktaCommand
import gov.cdc.prime.router.cli.PutSenderSetting
import gov.cdc.prime.router.cli.SettingCommand
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.tokens.DatabaseJtiCache
import gov.cdc.prime.router.tokens.Scope
import gov.cdc.prime.router.tokens.SenderUtils
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime
import java.util.UUID

/**
 *  Test a variety of waters endpoints across all our various authorization techniques
 */
class OktaAuthTests : CoolTest() {
    override val name = "oktaauth"
    override val description = "Test Okta Authorization and Authentication of various waters endpoints"
    // Not SMOKE because it requires login to do settings stuff.  Can't automate.  Doesn't work on Staging.
    override val status = TestStatus.DRAFT

    companion object {
        private const val accessTokenDummy = "dummy"

        fun abort(message: String): Nothing {
            throw PrintMessage(message, error = true)
        }

        /**
         * Create a fake report file using the schema expected by [sender].  Creates a file locally.  Does not submit it.
         */
        fun createFakeReport(sender: Sender): File {
            assert(sender.format == Sender.Format.CSV)
            assert(sender is CovidSender)
            return FileUtilities.createFakeCovidFile(
                metadata,
                settings,
                sender = sender as CovidSender,
                count = 1,
                format = Report.Format.CSV,
                directory = System.getProperty("java.io.tmpdir"),
                targetStates = null,
                targetCounties = null
            )
        }

        fun getOktaAccessTok(environment: Environment): String {
            return OktaCommand.fetchAccessToken(environment.oktaApp)
                ?: abort(
                    "Test needs a valid okta access token for the settings API." +
                        " Run ./prime login to fetch/refresh your access token."
                )
        }

        fun getOktaAccessTokOrLocal(environment: Environment): String {
            return if (environment.oktaApp == null) {
                accessTokenDummy
            } else {
                getOktaAccessTok(environment)
            }
        }
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        var passed = true
        val org1 = historyTestOrgName
        val sender1 = historyTestSender

        val org2 = orgName
        val sender2 = defaultIgnoreSender

        val myFakeReportFile = createFakeReport(sender1)
        val oktaToken = getOktaAccessTokOrLocal(environment)

        // Now submit a report to org1 and get its reportId1
        val (responseCode1, json1) = HttpUtilities.postReportFileToWatersApi(
            environment, myFakeReportFile, sender1, oktaToken
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
            environment, myFakeReportFile, sender2, oktaToken
        )
        val reportId2 = if (responseCode2 == 201) {
            getReportIdFromResponse(json2)
        } else {
            bad("Should get a 201 response to valid token, but but instead got $responseCode2")
            passed = false
            null
        }

        // Setup is done!   Ready to run some Okta Auth tests.
        // (Confusing:   If running smokes from your laptop, pointed at, say, Staging,
        // `Environment.isLocal()` will be true but `environment` will be Environment.STAGING.)
        if (environment == Environment.LOCAL) {
            //  This test fails on STAGING - there is no 'local' there.
            passed = passed and oktaLocalAuthTests(environment, myFakeReportFile, sender1, org1, reportId1)
        }
        passed = passed and oktaBadTokenAuthTests(environment, myFakeReportFile, sender1, org1, reportId1)
        passed = passed and oktaPrimeAdminSubmissionListAuthTests(environment, org1, org2)
        passed = passed and oktaPrimeAdminReportDetailsAuthTests(environment, org1, org2, reportId1, reportId2)
        passed = passed and oktaLookupTableSmokeTests(environment)
        return passed
    }

    /**
     * Run a suite of tests using local auth.
     * Note these tests will always fail on Staging, since there's no such thing as local auth there.
     */
    private fun oktaLocalAuthTests(
        environment: Environment,
        fakeReportFile: File,
        sender1: Sender,
        orgName1: String,
        reportId1: ReportId?,
    ): Boolean {
        ugly("Starting $name Test: try various API paths using local auth only")
        var passed = true
        // First, test submitting a report using local auth.
        val (responseCode, json) = HttpUtilities.postReportFileToWatersApi(
            environment, fakeReportFile, sender1, token = "dummy"
        )
        if (responseCode != 201) {
            passed = passed and bad("Local auth: Should get a 201 response, but but instead got $responseCode")
        } else {
            if (getReportIdFromResponse(json) == null)
                passed = passed and bad("Local auth:  got a 201 json, but no reportId. json is $json")
        }
        val testCases = listOf(
            HistoryApiTestCase(
                "Local server2server auth: get list-of-submissions to $orgName1",
                "${environment.url}/api/waters/org/$orgName1/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                bearer = "",
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Local server2server auth: Get report-details-history for one report",
                "${environment.url}/api/waters/report/$reportId1/history",
                emptyMap(),
                emptyList(),
                bearer = "x",
                HttpStatus.OK,
                expectedReports = setOf(reportId1!!),
                ReportDetailsChecker(this),
                doMinimalChecking = false
            ),
            HistoryApiTestCase(
                "Local okta auth: get list-of-submissions to $orgName1",
                "${environment.url}/api/waters/org/$orgName1/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                bearer = "",
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Local okta auth: Get report-details-history for one report",
                "${environment.url}/api/waters/report/$reportId1/history",
                emptyMap(),
                emptyList(),
                bearer = "x",
                HttpStatus.OK,
                expectedReports = setOf(reportId1),
                ReportDetailsChecker(this),
                doMinimalChecking = false
            ),
        )
        val historyApiTest = HistoryApiTest()
        historyApiTest.outputToConsole = this.outputToConsole
        passed = passed and historyApiTest.runHistoryTestCases(testCases)
        this.outputMsgs.addAll(historyApiTest.outputMsgs)
        return passed
    }

    /**
     * Run a suite of tests using a bad token
     */
    private fun oktaBadTokenAuthTests(
        environment: Environment,
        fakeReportFile: File,
        sender1: Sender,
        orgName1: String,
        reportId1: ReportId?,
    ): Boolean {
        ugly("Starting $name Test: try various API paths using a bad token")
        var passed = true
        val advice = "Run   ./prime login --env staging    " +
            "to fetch/refresh a **PrimeAdmin** access token for the Staging environment."
        val oktaToken = OktaCommand.fetchAccessToken(OktaCommand.OktaApp.DH_STAGE) ?: abort(
            "The Okta PrimeAdmin tests use a Staging Okta token, even locally, which is not available. $advice"
        )
        val badToken = oktaToken.reversed()
        // First, test submitting a report using local auth.
        val (responseCode, _) = HttpUtilities.postReportFileToWatersApi(
            environment, fakeReportFile, sender1, token = badToken
        )
        if (responseCode != HttpStatus.UNAUTHORIZED.value()) {
            passed = passed and bad("Local auth: Should get a 401 response, but but instead got $responseCode")
        }
        val testCases = listOf(
            HistoryApiTestCase(
                "Badtoken server2server: get list-of-submissions to $orgName1",
                "${environment.url}/api/waters/org/$orgName1/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                bearer = badToken,
                HttpStatus.UNAUTHORIZED,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Badtoken server2server: Get report-details-history for one report",
                "${environment.url}/api/waters/report/$reportId1/history",
                emptyMap(),
                emptyList(),
                bearer = badToken,
                HttpStatus.UNAUTHORIZED,
                expectedReports = setOf(reportId1!!),
                ReportDetailsChecker(this),
                doMinimalChecking = false
            ),
            HistoryApiTestCase(
                "Badtoken okta: get list-of-submissions to $orgName1",
                "${environment.url}/api/waters/org/$orgName1/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                bearer = badToken,
                HttpStatus.UNAUTHORIZED,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Badtoken okta: Get report-details-history for one report",
                "${environment.url}/api/waters/report/$reportId1/history",
                emptyMap(),
                emptyList(),
                bearer = badToken,
                HttpStatus.UNAUTHORIZED,
                expectedReports = setOf(reportId1),
                ReportDetailsChecker(this),
                doMinimalChecking = false
            ),
        )
        val historyApiTest = HistoryApiTest()
        historyApiTest.outputToConsole = this.outputToConsole
        passed = passed and historyApiTest.runHistoryTestCases(testCases)
        this.outputMsgs.addAll(historyApiTest.outputMsgs)
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
        ugly("Starting $name Test: test list-of-submissions queries using Okta PrimeAdmin token")
        val advice = "Run   ./prime login --env staging    " +
            "to fetch/refresh a **PrimeAdmin** access token for the Staging environment."
        val oktaToken = OktaCommand.fetchAccessToken(OktaCommand.OktaApp.DH_STAGE) ?: abort(
            "The Okta PrimeAdmin tests use a Staging Okta token, even locally, which is not available. $advice"
        )
        val testCases = listOf(
            HistoryApiTestCase(
                "Okta: get list-of-submissions to $orgName1",
                "${environment.url}/api/waters/org/$orgName1/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                oktaToken,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Okta: Get list-of-submissions to $orgName1.default",
                "${environment.url}/api/waters/org/$orgName1.default/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                oktaToken,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Okta: Get list-of-submissions to $orgName2",
                "${environment.url}/api/waters/org/$orgName2/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                oktaToken,
                HttpStatus.OK,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Okta: Get list-of-submissions to $orgName2.default",
                "${environment.url}/api/waters/org/$orgName2.default/submissions",
                emptyMap(),
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
        this.outputMsgs.addAll(historyApiTest.outputMsgs)
        if (!passed) {
            bad("One or more Okta PrimeAdmin tests failed. You might need to:  $advice")
        }
        return passed
    }

    private fun oktaPrimeAdminReportDetailsAuthTests(
        environment: Environment,
        orgName1: String,
        orgName2: String,
        reportId1: ReportId?,
        reportId2: ReportId?,
    ): Boolean {
        ugly("Starting $name Test: test report-details-history queries using Okta auth.")
        val advice = "Run   ./prime login --env staging    " +
            "to fetch/refresh a **PrimeAdmin** access token for the Staging environment."
        val oktaToken = OktaCommand.fetchAccessToken(OktaCommand.OktaApp.DH_STAGE) ?: abort(
            "The Okta PrimeAdmin tests use a Staging Okta token, even locally, which is not available. $advice"
        )

        if (reportId1 == null || reportId2 == null) {
            return bad("Unable to do oktaPrimeAdminReportDetailsAuthTests:  no reportId's to test with")
        }
        val testCases = mutableListOf(
            HistoryApiTestCase(
                "Get report-details-history for an $orgName1 report using an Okta PrimeAdmin token: Happy path",
                "${environment.url}/api/waters/report/$reportId1/history",
                emptyMap(),
                emptyList(),
                oktaToken,
                HttpStatus.OK,
                expectedReports = setOf(reportId1),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get report-details-history for an $orgName2 report using an Okta PrimeAdmin token: Happy path",
                "${environment.url}/api/waters/report/$reportId2/history",
                emptyMap(),
                emptyList(),
                oktaToken,
                HttpStatus.OK,
                expectedReports = setOf(reportId2),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get report-details-history for a nonexistent report using an Okta PrimeAdmin token: should fail",
                "${environment.url}/api/waters/report/87a02e0c-5b77-4595-a039-e143fbaadda2/history",
                emptyMap(),
                emptyList(),
                oktaToken,
                HttpStatus.NOT_FOUND,
                expectedReports = setOf(UUID.fromString("87a02e0c-5b77-4595-a039-e143fbaadda2")),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get report-details-history for a bogus report using an Okta PrimeAdmin token: should fail",
                "${environment.url}/api/waters/report/BOGOSITY/history",
                emptyMap(),
                emptyList(),
                oktaToken,
                HttpStatus.NOT_FOUND,
                expectedReports = setOf(reportId1),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
        )
        val historyApiTest = HistoryApiTest()
        historyApiTest.outputToConsole = this.outputToConsole
        val passed = historyApiTest.runHistoryTestCases(testCases)
        this.outputMsgs.addAll(historyApiTest.outputMsgs)
        if (!passed) {
            bad("One or more Okta PrimeAdmin tests failed. You might need to:  $advice")
        }
        return passed
    }

    /**
     * Test basic functionality of the lookuptoable API, using okta auth, in [environment].
     * This assumes you have a primeadmin auth, so you can read and write the lookup tables.
     */
    private fun oktaLookupTableSmokeTests(environment: Environment): Boolean {
        ugly("Starting LookupTable tests using Okta auth")
        val (passed, failedMessages) = lookupTableReadAndWriteSmokeTests(environment)
        if (passed) {
            good("Test passed:   Reading and Writing LookupTables using Okta auth.")
        } else {
            failedMessages.forEach { bad(it) }
        }
        return passed
    }
}

/**************************
 *  Test server2server auth (aka two-legged auth, FHIR auth) against a variety of waters endpoints
 **************************/
class Server2ServerAuthTests : CoolTest() {
    override val name = "server2serverauth"
    override val description = "Use server2server (two-legged) auth against various waters endpoints"
    override val status = TestStatus.SMOKE

    private lateinit var settingsEnv: Environment

    /**
     * Utility function to attach a new sender to an existing organization.
     */
    private fun createNewSenderForExistingOrg(senderName: String, orgName: String): Sender {
        val newSender = CovidSender(
            name = senderName,
            organizationName = orgName,
            format = Sender.Format.CSV,
            customerStatus = CustomerStatus.INACTIVE,
            schemaName = "primedatainput/pdi-covid-19"
        )

        val oktaSettingAccessTok = OktaAuthTests.getOktaAccessTokOrLocal(settingsEnv) // ironic: still need okta

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
        return JacksonMapperUtilities.allowUnknownsMapper.readValue(savedSenderJson, CovidSender::class.java)
    }

    /**
     * Utility function to associate a public [key] (named with key id [kid])
     * to an existing [sender] and store in on the database.
     * The [key] gives permission to an authenticated sender requesting [scope].
     * @returns a new copy of the old sender object, now with the key added to it.
     */
    private fun saveServer2ServerKey(sender: Sender, key: String, kid: String, scope: String): Sender {
        // associate a public key to the sender
        val publicKeyStr = SenderUtils.readPublicKeyPem(key)
        publicKeyStr.kid = kid
        val senderPlusNewKey = sender.makeCopyWithNewScopeAndJwk(scope, publicKeyStr)

        // save the sender with the new key
        PutSenderSetting()
            .put(
                settingsEnv,
                OktaAuthTests.getOktaAccessTokOrLocal(settingsEnv),
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
    private fun getServer2ServerAccessTok(
        sender: Sender,
        environment: Environment,
        privateKeyStr: String,
        kid: String,
        scope: String
    ): Pair<Int, String> {
        val baseUrl = environment.url.toString() + HttpUtilities.tokenApi
        val privateKey = SenderUtils.readPrivateKeyPem(privateKeyStr)
        val senderSignedJWT = SenderUtils.generateSenderToken(sender, baseUrl, privateKey, kid)
        val senderTokenUrl = settingsEnv.formUrl("api/token").toString()
        val body = SenderUtils.generateSenderUrlParameterString(senderSignedJWT, scope)
        return HttpUtilities.postHttp(senderTokenUrl, body.toByteArray())
    }

    /**
     * Delete a sender from the settings.  Used at the end of a test to teardown.
     */
    private fun deleteSender(sender: Sender) {
        DeleteSenderSetting()
            .delete(
                settingsEnv,
                OktaAuthTests.getOktaAccessTokOrLocal(settingsEnv),
                SettingCommand.SettingType.SENDER,
                sender.fullName
            )
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        var passed = true
        settingsEnv = environment
        passed = passed and doEcAndRsaEcKeyTests(environment)
        passed = passed and doServer2ServerEndpointAuthTests(environment)
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
            val myFakeReportFile = OktaAuthTests.createFakeReport(mySender)

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
                saveServer2ServerKey(mySender, end2EndExampleECPublicKeyStr, kid, myScope)

                // attempt to get an access token with an invalid private key
                val (responseCode2, _) =
                    getServer2ServerAccessTok(mySender, environment, end2EndExampleECPrivateInvalidKeyStr, kid, myScope)
                if (responseCode2 == 401) {
                    good("EC key: Attempt to get a token with invalid private key rightly failed.")
                } else {
                    bad("EC key: Should get a 401 response to invalid private key but instead got $responseCode2")
                    passed = false
                }

                // get a valid private key
                val (httpStatusGetToken, responseGetToken) =
                    getServer2ServerAccessTok(mySender, environment, end2EndExampleECPrivateKeyStr, kid, myScope)
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
                saveServer2ServerKey(mySender, end2EndExampleRSAPublicKeyStr, kid, myScope)

                // try to get an access token with an invalid private key
                val (responseCode2, _) =
                    getServer2ServerAccessTok(
                        mySender,
                        environment,
                        end2EndExampleRSAPrivateInvalidKeyStr,
                        kid,
                        myScope
                    )
                if (responseCode2 == 401) {
                    good("RSA key: Attempt to get a token with invalid private key rightly failed.")
                } else {
                    bad("RSA key: Should get a 401 response to invalid private key but instead got $responseCode2")
                    passed = false
                }

                // get an access token with a valid private key
                val (httpStatusGetToken, responseGetToken) =
                    getServer2ServerAccessTok(mySender, environment, end2EndExampleRSAPrivateKeyStr, kid, myScope)
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
     * Run a wide variety of tests of auth of waters API endpoints.
     */
    private fun doServer2ServerEndpointAuthTests(
        environment: Environment,
    ): Boolean {
        var passed = true
        // There is quite a lot of setup needed here.
        // To truly test auth, we need tokens for two orgs, so we can test that org1 can't see org2, etc.
        // So you'll see everything done twice here.

        val mySenderName = "temporary_submission_auth_test"
        val kid = "submission-testing-kid"

        if (environment == Environment.PROD) error("Can't create simple_report test data in PROD")
        val org1 = "simple_report"
        var sender1 = createNewSenderForExistingOrg(mySenderName, org1)
        // Test various functionality using the general <orgname>.*.user role.
        val scope1 = "$org1.*.user"
        // Scope <org>.<sender>.report only gives access to submit to that org only.  Doesn't work for history GETs.
        val uploadReportScope1 = "$org1.$mySenderName.report"
        // Submit this new scope and public key to the Settings store, associated with this Sender.
        sender1 = saveServer2ServerKey(sender1, end2EndExampleRSAPublicKeyStr, kid, scope1)
        sender1 = saveServer2ServerKey(sender1, end2EndExampleRSAPublicKeyStr, kid, uploadReportScope1)

        val org2 = "ignore"
        var sender2 = createNewSenderForExistingOrg(mySenderName, org2)
        // Test various functionality using the general <orgname>.*.admin role.
        val scope2 = "$org2.*.admin"
        // Submit this new scope and public key to the Settings store, associated with this Sender.
        sender2 = saveServer2ServerKey(sender2, end2EndExampleRSAPublicKeyStr, kid, scope2)

        try {
            val myFakeReportFile = OktaAuthTests.createFakeReport(sender1)

            // 1) Now request 5-minute token for the first org, USING THE UPLOAD-ONLY SCOPE
            val (submitHttpStatus1, submitResponseToken1) =
                getServer2ServerAccessTok(sender1, environment, end2EndExampleRSAPrivateKeyStr, kid, uploadReportScope1)
            if (submitHttpStatus1 != 200) {
                return bad("Should get a 200 response to getToken instead got $submitHttpStatus1")
            }
            val submitToken1 = jacksonObjectMapper().readTree(submitResponseToken1).get("access_token").textValue()

            // 1a) Now request 5-minute token for the first org, USING THE GENERAL READ/WRITE Submission SCOPE
            val (httpStatus1, responseToken1) =
                getServer2ServerAccessTok(sender1, environment, end2EndExampleRSAPrivateKeyStr, kid, scope1)
            if (httpStatus1 != 200) {
                return bad("Should get a 200 response to getToken instead got $httpStatus1")
            }
            val token1 = jacksonObjectMapper().readTree(responseToken1).get("access_token").textValue()

            // 2) And a 5-minute token from the second org2
            val (httpStatus2, responseToken2) =
                getServer2ServerAccessTok(sender2, environment, end2EndExampleRSAPrivateKeyStr, kid, scope2)
            if (httpStatus2 != 200) {
                return bad("Should get a 200 response to getToken instead got $httpStatus2")
            }
            val token2 = jacksonObjectMapper().readTree(responseToken2).get("access_token").textValue()

            // Since we're getting tokens, test getting a primeadmin token, which we have no rights to get.
            val (httpStatusBad1, _) =
                getServer2ServerAccessTok(
                    sender1, environment, end2EndExampleRSAPrivateKeyStr, kid, Scope.primeAdminScope
                )
            if (httpStatusBad1 == 401) {
                good("Test upgrading my scope to primeadmin failed, as it should.")
            } else {
                bad("Should get a 401 response to getToken primeadmin scope. Instead got $httpStatusBad1")
                passed = false
            }

            // Another token test.  sender1 does not have org admin scope.   This should fail also
            val (httpStatusBad2, _) =
                getServer2ServerAccessTok(
                    sender1, environment, end2EndExampleRSAPrivateKeyStr, kid, "$org1.*.admin"
                )
            if (httpStatusBad1 == 401) {
                good("Test upgrading my scope to org admin failed, as it should.")
            } else {
                bad("Should get a 401 response to getToken org admin scope. Instead got $httpStatusBad2")
                passed = false
            }

            // Now submit a report to org1 and get its reportId1, testing the submit-only token, to make sure it works
            val (responseCode1, json1) = HttpUtilities.postReportFileToWatersApi(
                environment, myFakeReportFile, sender1, submitToken1
            )
            val reportId1 = if (responseCode1 == 201) {
                getReportIdFromResponse(json1)
            } else {
                bad(
                    "Org1: should get a 201 response to valid token, but but instead got $responseCode1"
                )
                passed = false
                null
            }
            good("Test sending report $reportId1 to $org1 successful")

            // Now ATTEMPT to submit a report to org1 using the org2 token.  Should fail.
            val (responseCode1fail, _) = HttpUtilities.postReportFileToWatersApi(
                environment, myFakeReportFile, sender1, token2 // wrong token!
            )
            if (responseCode1fail == 401) {
                good("Test sending report to $org1 with WRONG TOKEN failed, as it should.")
            } else {
                bad(
                    "Org1: should get a 401 response to wrong token, but but instead got $responseCode1fail"
                )
                passed = false
            }

            // And submit a report to org2 and get its reportId2, testing using the broader-permission'ed token.
            val (responseCode2, json2) = HttpUtilities.postReportFileToWatersApi(
                environment, myFakeReportFile, sender2, token2
            )
            val reportId2 = if (responseCode2 == 201) {
                getReportIdFromResponse(json2)
            } else {
                bad("Org2: Should get a 201 response to valid token, but but instead got $responseCode2")
                passed = false
                null
            }

            // Setup is done!   Ready to run some tests.
            passed = passed and server2ServerSubmissionListAuthTests(environment, org1, org2, token1, token2)
            passed = passed and server2ServerReportDetailsAuthTests(
                environment, org1, org2, reportId1, reportId2, token1, token2
            )
            passed = passed and server2ServerLookupTableSmokeTests(environment, token1)
            passed = passed and server2ServerLookupTableSmokeTests(environment, token2)
        } finally {
            deleteSender(sender1)
            deleteSender(sender2)
        }
        return passed
    }

    /**
     * Test basic functionality of the lookuptoable API, using server2server auth, in [environment].
     * This assumes you do not have primeadmin auth, so you can only read the lookup tables.
     */
    private fun server2ServerLookupTableSmokeTests(environment: Environment, token: String): Boolean {
        ugly("Starting LookupTable tests using Server2Server auth")
        val (passed, failedMessages) = lookupTableReadOnlySmokeTests(environment, token)
        if (passed) {
            good("Test passed:   Lookuptable functions tests using server2server auth.")
        } else {
            failedMessages.forEach { bad(it) }
        }
        return passed
    }

    /**
     * Run a suite of tests against the list-of-submissions endpoint.
     */
    private fun server2ServerSubmissionListAuthTests(
        environment: Environment,
        orgName1: String,
        orgName2: String,
        token1: String,
        token2: String,
    ): Boolean {
        ugly("Starting $name Test: test list-of-submissions queries using server2server tokens")
        val testCases = mutableListOf(
            // A variety of auth test cases, against the submission list queries
            // curl -H "Authorization: Bearer $TOK" "http://localhost:7071/api/waters/org/ignore/submissions?pagesize=1"
            HistoryApiTestCase(
                "Get list-of-submissions to $orgName1 with correct tok",
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
                "Get list-of-submissions to $orgName1.default with correct auth",
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
                "Get list-of-submissions to a bogus organization",
                "${environment.url}/api/waters/org/BOGOSITY/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                token1,
                HttpStatus.NOT_FOUND,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get list-of-submissions to $orgName2 with $orgName1 auth",
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
                "Get list-of-submissions to oh-doh receiver with $orgName1 auth",
                "${environment.url}/api/waters/org/oh-doh/submissions",
                emptyMap(),
                listOf("pagesize" to 1),
                token1,
                HttpStatus.NOT_FOUND,
                expectedReports = emptySet(),
                SubmissionListChecker(this),
                doMinimalChecking = true
            ),
            HistoryApiTestCase(
                "Get list-of-submissions to $orgName1 with $orgName2 auth",
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
        val passed = historyApiTest.runHistoryTestCases(testCases)
        this.outputMsgs.addAll(historyApiTest.outputMsgs)
        return passed
    }

    private fun server2ServerReportDetailsAuthTests(
        environment: Environment,
        orgName1: String,
        orgName2: String,
        reportId1: ReportId?,
        reportId2: ReportId?,
        token1: String,
        token2: String,
    ): Boolean {
        ugly("Starting $name Test: test report-details-history queries using server2server auth.")
        if (reportId1 == null || reportId2 == null) {
            return bad("Unable to do server2ServerReportDetailsAuthTests:  no reportId's to test with")
        }
        val testCases = mutableListOf(
            // Example
            // curl -H "Authorization: Bearer $TOK" "http://localhost:7071/api/waters/report/<UUID>/history"
            HistoryApiTestCase(
                "Get report-details-history for an $orgName1 report using an $orgName1 token: Happy path",
                "${environment.url}/api/waters/report/$reportId1/history",
                emptyMap(),
                emptyList(),
                token1,
                HttpStatus.OK,
                expectedReports = setOf(reportId1),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get report-details-history for an $orgName2 report using an $orgName2 token: Happy path",
                "${environment.url}/api/waters/report/$reportId2/history",
                emptyMap(),
                emptyList(),
                token2,
                HttpStatus.OK,
                expectedReports = setOf(reportId2),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get report-details-history for an $orgName1 report using an $orgName2 token: Should fail",
                "${environment.url}/api/waters/report/$reportId1/history",
                emptyMap(),
                emptyList(),
                token2,
                HttpStatus.UNAUTHORIZED,
                expectedReports = setOf(reportId1),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get report-details-history for an $orgName2 report using an $orgName1 token: Should fail",
                "${environment.url}/api/waters/report/$reportId2/history",
                emptyMap(),
                emptyList(),
                token1,
                HttpStatus.UNAUTHORIZED,
                expectedReports = setOf(reportId2),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get report-details-history for a nonexistent report:",
                "${environment.url}/api/waters/report/87a02e0c-5b77-4595-a039-e143fbaadda2/history",
                emptyMap(),
                emptyList(),
                token1,
                HttpStatus.NOT_FOUND,
                expectedReports = setOf(UUID.fromString("87a02e0c-5b77-4595-a039-e143fbaadda2")),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
            HistoryApiTestCase(
                "Get report-details-history for a bogus report:",
                "${environment.url}/api/waters/report/BOGOSITY/history",
                emptyMap(),
                emptyList(),
                token1,
                HttpStatus.NOT_FOUND,
                expectedReports = setOf(reportId1),
                ReportDetailsChecker(this),
                doMinimalChecking = false,
            ),
        )
        val historyApiTest = HistoryApiTest()
        historyApiTest.outputToConsole = this.outputToConsole
        val passed = historyApiTest.runHistoryTestCases(testCases)
        this.outputMsgs.addAll(historyApiTest.outputMsgs)
        return passed
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

/**
 * Test ability, as a non-PrimeAdmin, to read a lookup table. Also test that you cannot write a lookup table.
 * This test must be run as a regular user, not PrimeAdmin.
 *
 * Pass in a bearer [accessToken] for a non-PrimeAdmin user, or if none is passed, this will attempt to use Okta.
 * @return Pair(true if tests passed / false if failed, Failure messages are in the List<String>)
 */
fun lookupTableReadOnlySmokeTests(
    environment: Environment,
    accessToken: String? = null,
): Pair<Boolean, List<String>> {
    val failedMessages = mutableListOf<String>()
    var passed = true
    val lookupTableUtils = LookupTableEndpointUtilities(environment, accessToken)

    // Can we do a lookup?
    val lookupList = try {
        lookupTableUtils.fetchList(false)
    } catch (e: Exception) {
        passed = false
        failedMessages += "Got Exception ({e.message }) on call to fetchList."
        null
    }

    // These next few tests can only be run if there's actually a table in the LookupTables
    if (!lookupList.isNullOrEmpty()) {
        val myTable = try {
            // try to test using a relatively fast/small table, if its there:
            val niceSmallTableName = "sender_valuesets"
            lookupList.first { it.tableName == niceSmallTableName && it.isActive == true }
        } catch (e: NoSuchElementException) {
            // Sigh. That didn't work.  Just use any old table for this test.
            lookupList.first()
        }
        // contents Test
        val contents = lookupTableUtils.fetchTableContent(myTable.tableName, myTable.tableVersion)
        if (contents.isEmpty()) {
            passed = false
            failedMessages += "Test FAILED:  expected some contents to be in table ${myTable.tableName}, but its empty."
        }
        // info Test
        val info = lookupTableUtils.fetchTableInfo(myTable.tableName, myTable.tableVersion)
        if (info.tableName != myTable.tableName || info.createdBy != myTable.createdBy) {
            passed = false
            failedMessages += "Test FAILED:  fetchTableInfo returns different object from ${myTable.tableName} in List."
        }
        // Should NOT be able to activate the Tables because we don't have write access.
        try {
            lookupTableUtils.activateTable(myTable.tableName, myTable.tableVersion)
            passed = false // should never reach here.
            failedMessages += "Test FAILED:  should NOT have been able to activate a table."
        } catch (_: IOException) { // passed
        }
    }

    val tableName2 = "bogus-test-table2"
    // Should NOT be able to create tables because we don't have write access
    try {
        val tableData = listOf(mapOf("to be or not" to "be"), mapOf("Shall I compare thee " to "a summer's day?"))
        lookupTableUtils.createTable(tableName2, tableData, forceTableToCreate = true)
        passed = false // should never reach here.
        failedMessages += "Test FAILED:  should NOT have been able to create a table."
    } catch (_: IOException) { // passed
    }

    return Pair(passed, failedMessages)
}

/**
 * Test ability of a PrimeAdmin to both read and write Lookup Tables.
 *
 * Pass in a bearer [accessToken] for a PrimeAdmin user, or if none is passed, this will attempt to use Okta.
 * @return Pair(true if tests passed / false if failed, Failure messages are in the List<String>)
 */
fun lookupTableReadAndWriteSmokeTests(
    environment: Environment,
    accessToken: String? = null,
): Pair<Boolean, List<String>> {
    val failedMessages = mutableListOf<String>()
    var passed = true
    val lookupTableUtils = LookupTableEndpointUtilities(environment, accessToken)

    val tableName = "bogus-test-table"

    // createTable test
    val tableData = listOf(mapOf("a" to "11", "b" to "21"), mapOf("a" to "12", "b" to "22"))
    val createInfo = lookupTableUtils.createTable(tableName, tableData, forceTableToCreate = true)
    if (createInfo.tableName != tableName || createInfo.tableVersion < 1) {
        passed = false
        failedMessages += "Test FAILED:  unable to create lookup table $tableName"
    }

    // activateTable test
    val activationInfo = lookupTableUtils.activateTable(tableName, createInfo.tableVersion)
    if (activationInfo.tableName != tableName || activationInfo.tableVersion != createInfo.tableVersion) {
        passed = false
        failedMessages += "Test FAILED:  unable to activate lookup table $tableName"
    }

    // contents Test
    val contents = lookupTableUtils.fetchTableContent(tableName, createInfo.tableVersion)
    if (contents.size != tableData.size) {
        passed = false
        failedMessages += "Test FAILED:  expected list of size ${tableData.size} but got ${contents.size}"
    }

    if (!contents[1].containsKey("b") || contents[1]["a"] != "12") {
        passed = false
        failedMessages += "Test FAILED:  retrieved list had wrong contents"
    }

    val lookupList = lookupTableUtils.fetchList(false)
    if (lookupList.isEmpty()) {
        passed = false
        failedMessages += "Got empty LookupTable List."
    }
    return Pair(passed, failedMessages)
}