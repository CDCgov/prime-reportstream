package gov.cdc.prime.router.cli.tests

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import gov.cdc.prime.router.cli.OktaCommand
import gov.cdc.prime.router.cli.SettingCommand
import gov.cdc.prime.router.common.Environment
import org.apache.http.HttpStatus

/**
 * Test SFTP receiver connections.  It checks the ignore.XYZ organization for the connection:
 *  For each sftp receiver listed in the ignore organization, call the sftpcheck API
 *  endpoint and verify you get back an OK response that we are able to connect to
 *  the SFTP server.
 *  This test needs to be able to run in staging and local, but not production.
 *  When the smoke tests are running in sequential mode then have this new test
 *  run after 'ping' and before 'end2end'.
 */

private const val jsonMimeType = "application/json"

class SftpcheckTest : CoolTest() {
    override val name = "sftpcheck"
    override val description = "Test SFTP receiver connections"
    override val status = TestStatus.DRAFT

    enum class SettingType { ORG, SENDER, RECEIVER }

    /**
     * Define private local variables for use in the test.
     */
    private val dummyAccessToken = "dummy"
    private val sftpcheckUri = "/api/check?sftpcheck="
    private val ignoreReceiverNamesURI = "/api/settings/organizations/ignore/receivers"
    private val sftpcheckMessage = "Test SFTP receiver connections: "
    var sftpcheckTestResult = true

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting SFTPCHECK Receciver Connections test ${environment.url}")

        /**
         * Get receiver ignore organizations with transport.host=sftp
         */
        val accessToken = getAccessToken(environment) // Get accessToken per environment.
        val ignoreReceiverNamePath = environment.formUrl(ignoreReceiverNamesURI).toString()
        val ignoreReceiversNameList = listReceiverNames(
            ignoreReceiverNamePath,
            SettingType.RECEIVER,
            accessToken
        )

        /**
         * If the Ignore receiver list is empty, prompt error message and skip the test.
         */
        if (ignoreReceiversNameList.isEmpty()) {
            return bad("----> " + sftpcheckMessage + "FAILED: Ignore receiver list is empty.")
        }

        /**
         * Start check the connection for each organization
         */
        ignoreReceiversNameList.forEach { receiver ->
            /**
             * Obtain the URL/path endpoint per environment (localhost or staging)
             */
            val path = environment.formUrl(sftpcheckUri + receiver).toString()

            /**
             * Check the organization ignore receiver connections
             */
            echo("SFTPCHECK Organizatin: $receiver...")

            val (_, response, result) = sftpReceiverIgnoreOrganizationCheck(path, accessToken)
            val (_, error) = result
            if (response.statusCode == HttpStatus.SC_OK) {
                good(
                    "----> " + sftpcheckMessage + "PASSED with response code: " +
                        " ${response.statusCode} "
                )
            } else {
                sftpcheckTestResult = bad(
                    "----> " + sftpcheckMessage + "FAILED with error code: : " +
                        "${response.statusCode} - ${error?.response?.responseMessage}..."
                )
            }
        }

        return when (sftpcheckTestResult) {
            true -> good("Test passed: SFTPCHECK ")
            false -> bad(sftpcheckMessage + "Failed")
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
    fun listReceiverNames(
        path: String,
        settingType: SettingType,
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
                val resultObjs = result.value.array()
                val receiverNames = if (settingType == SettingType.ORG) {
                    (0 until resultObjs.length())
                        .map { resultObjs.getJSONObject(it) }
                        .map { it.getString("name") }
                } else {
                    (0 until resultObjs.length())
                        .map { resultObjs.getJSONObject(it) }
                        .filter {
                            (
                                !it.isNull("transport") &&
                                    !it.getJSONObject("transport").isNull("host") &&
                                    it.getJSONObject("transport").getString("host").equals("sftp")
                                )
                        }
                        .map { "${it.getString("organizationName")}.${it.getString("name")}" }
                }
                receiverNames.sorted()
            }
        }
    }

    /**
     * SftpReceiverIgnoreOrganizationCheck - Makes the GET Fuel call the given endpoint.
     * @return: Triple
     *		ERROR: 		Error getting organization's name.
     *		SUCCESS: 	JSON payload body.
     */
    fun sftpReceiverIgnoreOrganizationCheck(
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

    /**
     * Get accessToken from Okta if available.
     */
    fun getAccessToken(environment: Environment): String {
        if (environment.oktaApp == null) return dummyAccessToken
        return OktaCommand.fetchAccessToken(environment.oktaApp)
            ?: SettingCommand.abort("Invalid access token. Run ./prime login to fetch/refresh your access token.")
    }
}