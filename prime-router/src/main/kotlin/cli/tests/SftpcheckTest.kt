package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.cli.CommandUtilities
import gov.cdc.prime.router.common.Environment
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

private const val jsonMimeType = "application/json"

/**
 * Test SFTP receiver connections.  It checks the ignore.XYZ organization for the connection:
 * For each sftp receiver listed in the ignore organization, call the sftpcheck API
 * endpoint and verify you get back an OK response that we are able to connect to
 * the SFTP server.
 *
 * This test needs to be able to run in staging and local, but not production.
 * When the smoke tests are running in sequential mode then have this new test
 * run after 'ping' and before 'end2end'.
 */

class SftpcheckTest : CoolTest() {
    override val name = "sftpcheck"
    override val description = "Test SFTP receiver connections"
    override val status = TestStatus.DRAFT

    /**
     * Define private local variables for use in the test.
     */
    private val sftpcheckUri = "/api/check?sftpcheck="
    private val ignoreReceiverNamesURI = "/api/settings/organizations/ignore/receivers"
    private val sftpcheckMessage = "----> Test SFTP receiver connections: "
    var sftpcheckTestResult = true

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting SFTPCHECK Receciver Connections test ${environment.url}")

        // Get receiver ignore organizations with transport.host=sftp
        val accessToken = OktaAuthTests.getOktaAccessToken(environment, name) // Get accessToken per environment.
        val ignoreReceiverNamePath = environment.formUrl(ignoreReceiverNamesURI).toString()
        val ignoreReceiversNameList = listReceiverNames(ignoreReceiverNamePath, accessToken)

        // If the Ignore receiver list is empty, prompt error message and skip the test.
        if (ignoreReceiversNameList.isEmpty()) {
            return bad(sftpcheckMessage + "FAILED: Ignore receiver list is empty.")
        }

        // Start check the connection for each organization
        ignoreReceiversNameList.forEach { receiver ->

            // Obtain the URL/path endpoint per environment (localhost or staging)
            val path = environment.formUrl(sftpcheckUri + receiver).toString()

            // Check the organization ignore receiver connections
            echo("SFTPCHECK Organizatin: $receiver...")

            val response = sftpReceiverIgnoreOrganizationCheck(path, accessToken)

            val respStr = runBlocking {
                response.body<String>()
            }

            if (response.status == HttpStatusCode.OK) {
                good(
                    sftpcheckMessage + "PASSED with response code: " +
                            " ${response.status.value} "
                )
            } else {
                sftpcheckTestResult = bad(
                    sftpcheckMessage + "FAILED with error code: : " +
                            "${response.status.value} - $respStr..."
                )
            }
        }

        return when (sftpcheckTestResult) {
            true -> good("Test passed: SFTPCHECK ")
            false -> bad("Test SFTP receiver connections: Failed")
        }
    }

    /**
     * listReceiverNames - it extracts all receiver ignore organization from the database setting table
     *  using setting API.
     *
     * @returns:
     *  receiverNames - contains all receiver ignore organizations
     *  emptyList - if there is any problem with getting ignore organization.
     */
    private fun listReceiverNames(
        path: String,
        accessToken: String,
    ): List<String> {
        val client = CommandUtilities.createDefaultHttpClient(
            BearerTokens(accessToken, refreshToken = "")
        )
        return runBlocking {
            val response =
                client.get(path) {
                    accept(ContentType.Application.Json)
                }

            val respStr = runBlocking {
                response.body<String>()
            }

            when {
                response.status != HttpStatusCode.OK -> emptyList()
                else -> {
                    val receiverJsonArray = JSONObject(respStr)
                    (0 until receiverJsonArray.length())
                        .map { receiverJsonArray.getJSONObject(it.toString()) }
                        .filter {
                            (
                                    !it.isNull("transport") &&
                                            !it.getJSONObject("transport").isNull("host") &&
                                            it.getJSONObject("transport").getString("host") == "sftp"
                                    )
                        }
                        .map { "${it.getString("organizationName")}.${it.getString("name")}" }
                }
            }
        }

//        val (_, _, result) = Fuel
//            .get(path)
//            .authentication()
//            .bearer(accessToken)
//            .header(Headers.CONTENT_TYPE to jsonMimeType)
//            .responseJson()
//        return when (result) {
//            is Result.Failure -> emptyList()
//            is Result.Success -> {
//                val receiverJsonArray = result.value.array()
//                (0 until receiverJsonArray.length())
//                    .map { receiverJsonArray.getJSONObject(it) }
//                    .filter {
//                        (
//                            !it.isNull("transport") &&
//                                !it.getJSONObject("transport").isNull("host") &&
//                                it.getJSONObject("transport").getString("host") == "sftp"
//                            )
//                    }
//                    .map { "${it.getString("organizationName")}.${it.getString("name")}" }
//            }
//        }
    }

    /**
     * SftpReceiverIgnoreOrganizationCheck - Makes the GET Fuel call the given endpoint.
     * @return: Triple
     *		ERROR: 		Error getting organization's name.
     *		SUCCESS: 	JSON payload body.
     */
    private fun sftpReceiverIgnoreOrganizationCheck(
        path: String,
        accessToken: String,
    ): HttpResponse {
        val client = CommandUtilities.createDefaultHttpClient(
            BearerTokens(accessToken, refreshToken = "")
        )
        return runBlocking {
            client.get(path) {
                accept(ContentType.Application.Json)
            }
//        return Fuel
//            .get(path)
//            .authentication()
//            .bearer(accessToken)
//            .header(Headers.CONTENT_TYPE to jsonMimeType)
//            .responseString()
        }
    }
}