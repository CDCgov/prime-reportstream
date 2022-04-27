package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.ajalt.clikt.core.PrintMessage
import gov.cdc.prime.router.CovidSender
import gov.cdc.prime.router.CustomerStatus
import gov.cdc.prime.router.FullELRSender
import gov.cdc.prime.router.Report
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
    override val description = "Test FHIR Auth"
    override val status = TestStatus.DRAFT // Not SMOKE because it requires login to do settings stuff.  Can't automate
    // create sender in 'ignore' organization
    private val accessTokenDummy = "dummy"
    val organization = "ignore"
    val senderName = "temporary_sender_auth_test"
    private val senderFullName = "$organization.$senderName"
    val scope = "$senderFullName.report"

    private lateinit var settingAccessTok: String // Token for this test code to access the Settings APIs
    private lateinit var savedSender: Sender
    private lateinit var fakeReportFile: File
    private lateinit var settingsEnv: Environment

    fun abort(message: String): Nothing {
        throw PrintMessage(message, error = true)
    }

    /**
     * Basically this populates the lateinit vars above.
     *
     * 1. Put a new sender in the settings table, but without any auth sender key stuff attached to it.
     * 2. Pull it back out again to confirm it worked, and prepare to attach keys to it later.
     * 3. Create a fake report.
     */
    fun setup(environment: Environment) {
        val newSender = FullELRSender(
            name = senderName,
            organizationName = organization,
            format = Sender.Format.CSV,
            customerStatus = CustomerStatus.INACTIVE
        )

        // Convert from ReportStreamEnv to Settings.Environment.  todo consolidate these!
        settingsEnv = environment

        settingAccessTok = if (settingsEnv.oktaApp == null) accessTokenDummy else {
            OktaCommand.fetchAccessToken(settingsEnv.oktaApp!!)
                ?: abort(
                    "Test needs a valid access token for settings API." +
                        " Run ./prime login to fetch/refresh your access token."
                )
        }

        // save the new sender
        PutSenderSetting()
            .put(
                settingsEnv,
                settingAccessTok,
                SettingCommand
                    .SettingType.SENDER,
                senderFullName,
                jacksonObjectMapper().writeValueAsString(newSender)
            )

        // get the sender previously written
        val savedSenderJson = GetSenderSetting().get(
            settingsEnv,
            settingAccessTok,
            SettingCommand.SettingType.SENDER,
            senderFullName
        )

        // deserialize the written sender
        savedSender = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(savedSenderJson, FullELRSender::class.java)
            ?: error("Unable to save sender")

        // create a fake report
        fakeReportFile = FileUtilities.createFakeCovidFile(
            metadata,
            settings,
            sender = savedSender as CovidSender,
            count = 1,
            format = Report.Format.CSV,
            directory = System.getProperty("java.io.tmpdir"),
            targetStates = null,
            targetCounties = null
        )
    }

    /**
     * Associate a public key to the sender
     * and store in on the database
     */
    private fun saveSendersKey(key: String, kid: String) {
        // associate a public key to the sender
        val publicKeyStr = SenderUtils.readPublicKeyPem(key)
        publicKeyStr.kid = kid
        savedSender = savedSender.makeCopyWithNewScopeAndJwk(scope, publicKeyStr)

        // save the sender with the new key
        PutSenderSetting()
            .put(
                settingsEnv,
                settingAccessTok,
                SettingCommand.SettingType.SENDER,
                senderFullName,
                jacksonObjectMapper().writeValueAsString(savedSender)
            )
    }

    /**
     * Given a private key and the kid tries to retrieve an access token
     */
    private fun getWatersAccessTok(
        environment: Environment,
        privateKeyStr: String,
        kid: String
    ): Pair<Int, String> {
        val baseUrl = environment.url.toString() + HttpUtilities.tokenApi
        val privateKey = SenderUtils.readPrivateKeyPem(privateKeyStr)
        val senderSignedJWT = SenderUtils.generateSenderToken(savedSender, baseUrl, privateKey, kid)
        val senderTokenUrl =
            SenderUtils.generateSenderUrl(settingsEnv, senderSignedJWT, scope)

        return HttpUtilities.postHttp(senderTokenUrl.toString(), "".toByteArray())
    }

    private fun teardown() {
        // delete the sender
        DeleteSenderSetting()
            .delete(
                settingsEnv,
                settingAccessTok,
                SettingCommand.SettingType.SENDER,
                senderFullName
            )
    }

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        var passed = true
        ugly("Starting $name test of server-server authentication using keypairs:")

        setup(environment)

        // first, try to send a report without having assigned a public key
        val (responseCode, _) = HttpUtilities.postReportFileFhir(environment, fakeReportFile, savedSender)

        if (responseCode == 401) {
            good("Attempt to send token with no auth rightly failed.")
        } else {
            bad("Should get a 401 response while sending report without a token. Instead got $responseCode")
            passed = false
        }

        // EC tests
        "testing-kid-ec".let { kid ->
            // associate a key to the sender
            saveSendersKey(end2EndExampleECPublicKeyStr, kid)

            // attempt to get an access token with an invalid private key
            val (responseCode2, _) = getWatersAccessTok(environment, end2EndExampleECPrivateInvalidKeyStr, kid)
            if (responseCode2 == 401) {
                good("EC key: Attempt to get a token with invalid private key rightly failed.")
            } else {
                bad("EC key: Should get a 401 response to invalid private key but instead got $responseCode2")
                passed = false
            }

            // get a valid private key
            val (httpStatusGetToken, responseGetToken) =
                getWatersAccessTok(environment, end2EndExampleECPrivateKeyStr, kid)
            val watersAccessTok = jacksonObjectMapper().readTree(responseGetToken).get("access_token").textValue()

            if (httpStatusGetToken == 200) {
                good("EC key: Attempt to get a token with valid sender key succeeded.")
            } else {
                bad("EC key: Should get a 200 response to getToken instead got $httpStatusGetToken")
                passed = false
            }

            // but now try to send a report with a tampered access token
            val (responseCode3, json3) =
                HttpUtilities.postReportFileFhir(
                    environment,
                    fakeReportFile,
                    savedSender,
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
            val (responseCode4) =
                HttpUtilities.postReportFileFhir(environment, fakeReportFile, savedSender, watersAccessTok)

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
            saveSendersKey(end2EndExampleRSAPublicKeyStr, kid)

            // try to get an access token with an invalid private key
            val (responseCode2, _) = getWatersAccessTok(environment, end2EndExampleRSAPrivateInvalidKeyStr, kid)
            if (responseCode2 == 401) {
                good("RSA key: Attempt to get a token with invalid private key rightly failed.")
            } else {
                bad("RSA key: Should get a 401 response to invalid private key but instead got $responseCode2")
                passed = false
            }

            // get an access token with a valid private key
            val (httpStatusGetToken, responseGetToken) =
                getWatersAccessTok(environment, end2EndExampleRSAPrivateKeyStr, kid)
            val watersAccessTok = jacksonObjectMapper().readTree(responseGetToken).get("access_token").textValue()

            if (httpStatusGetToken == 200) {
                good("RSA key: Attempt to get a token with valid sender key succeeded.")
            } else {
                bad("RSA key: Should get a 200 response to getToken instead got $httpStatusGetToken")
                passed = false
            }

            // try to send a report with a tampered access token
            val (responseCode3, json3) =
                HttpUtilities.postReportFileFhir(
                    environment,
                    fakeReportFile,
                    savedSender,
                    watersAccessTok.reversed()
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

            // try to send a report with a valid access token
            val (responseCode4) =
                HttpUtilities.postReportFileFhir(environment, fakeReportFile, savedSender, watersAccessTok)

            if (responseCode4 == 201) {
                good("RSA key: Got a 201 back from post with valid token.")
            } else {
                bad("RSA key: Should get a 201 response to valid token, but but instead got $responseCode4")
                passed = false
            }
        }

        teardown()
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