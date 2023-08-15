package gov.cdc.prime.router.cli.tests

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.common.Environment
import org.apache.http.HttpStatus

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

            val (_, response, _) = sftpReceiverIgnoreOrganizationCheck(path, accessToken)
            if (response.statusCode == HttpStatus.SC_OK) {
                good(
                    sftpcheckMessage + "PASSED with response code: " +
                        " ${response.statusCode} "
                )
            } else {
                sftpcheckTestResult = bad(
                    sftpcheckMessage + "FAILED with error code: : " +
                        "${response.statusCode} - ${response.responseMessage}..."
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
        accessToken: String
    ): List<String> {
        val (_, _, result) = Fuel
            .get(path)
            .authentication()
            .bearer(accessToken)
            .header(Headers.CONTENT_TYPE to jsonMimeType)
            .responseJson()
        return when (result) {
            is Result.Failure -> emptyList()
            is Result.Success -> {
                val receiverJsonArray = result.value.array()
                (0 until receiverJsonArray.length())
                    .map { receiverJsonArray.getJSONObject(it) }
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
     * SftpReceiverIgnoreOrganizationCheck - Makes the GET Fuel call the given endpoint.
     * @return: Triple
     *		ERROR: 		Error getting organization's name.
     *		SUCCESS: 	JSON payload body.
     */
    private fun sftpReceiverIgnoreOrganizationCheck(
        path: String,
        accessToken: String,
    ): Triple<Request, Response, Result<String, FuelError>> {
        return Fuel
            .get(path)
            .authentication()
            .bearer(accessToken)
            .header(Headers.CONTENT_TYPE to jsonMimeType)
            .responseString()
    }
}