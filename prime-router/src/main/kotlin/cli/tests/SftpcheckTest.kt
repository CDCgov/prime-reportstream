package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.common.HttpClientUtils
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
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
            // Check the organization ignore receiver connections
            echo("SFTPCHECK Organizatin: $receiver...")

            val (response, respStr) = sftpReceiverIgnoreOrganizationCheck(
                environment.formUrl(sftpcheckUri + receiver).toString(), accessToken
            )

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
        val (response, respStr) = HttpClientUtils.getWithStringResponse(
            url = path,
            accessToken = accessToken
        )
        return when {
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

    /**
     * Makes the GET call the given endpoint.
     * @return: HttpResponse
     */
    private fun sftpReceiverIgnoreOrganizationCheck(
        path: String,
        accessToken: String,
    ): Pair<HttpResponse, String> = HttpClientUtils.getWithStringResponse(
            url = path,
            accessToken = accessToken
        )
}