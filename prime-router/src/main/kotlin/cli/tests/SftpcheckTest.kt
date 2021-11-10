package gov.cdc.prime.router.cli.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.common.Environment
import gov.cdc.prime.router.cli.SettingCommand
import gov.cdc.prime.router.cli.SettingsUtilities
import org.apache.http.HttpStatus
import java.io.File

/**
 * Test SFTP receiver connections.  It checks the ignore.XYZ organization for the connection:
 *  For each sftp receiver listed in the ignore organization, call the sftpcheck API
 *  endpoint and verify you get back an OK response that we are able to connect to
 *  the SFTP server.
 *  This test needs to be able to run in staging and local, but not production.
 *  When the smoke tests are running in sequential mode then have this new test
 *  run after 'ping' and before 'end2end'.
 *
 * @returns:
 *  If SUCCESS:
 *      good("Test passed: SFTP")
 *  If ERROR:
 *      bad("Test SFTP receiver connections: Failed"
 *
 */

class SftpcheckTest : CoolTest() {
    override val name = "sftpcheck"
    override val description = "Test SFTP receiver connections"
    override val status = TestStatus.DRAFT

    /**
     * Define private local variables for use in the test.
     */
    private val receiverOrganizationPath = "./settings/organizations.yml"
    private val dummyAccessToken = "dummy"
    private val sftpcheckUri = "/api/check?sftpcheck="
    private val sftpcheckMessage = "Test SFTP receiver connections: "
    var testResult = true
    val yamlMapper: ObjectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
    val allowEnvironment = arrayListOf(Environment.LOCAL, Environment.STAGING)

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {

        /**
         * Check for allowable environment to run the test.
         */
        if (!allowEnvironment.contains(environment))
            return true

        ugly("Starting SFTPCHECK Receciver Connections test ${environment.url}")

        /**
         * Get ignore organizations.
         */
        val ignoreReceivers = getAllIgnoreRecivers()

        /**
         * Start check the connection for each organization
         */
        ignoreReceivers.forEach { receiver ->
            /**
             * Obtain the URL/path endpoint per environment (localhost or staging)
             */
            val path = environment.url.toString() + sftpcheckUri + receiver

            /**
             * Check the organization ignore receiver connections
             */
            echo("SFTPCHECK Organizatin: ${receiver}...")

            val (_, response, result) = SettingsUtilities.get(path, dummyAccessToken)
            val (_, error) = result
            if (response?.statusCode == HttpStatus.SC_OK) {
                good("----> " + sftpcheckMessage + "PASSED with response code: " +
                    " ${response?.statusCode} ")
            } else {
                testResult = bad("----> " + sftpcheckMessage + "FAILED with error code: : " +
                    "${response.statusCode} - ${error?.response?.responseMessage}...")
            }
        }

        return when (testResult) {
            true -> good("Test passed: SFTPCHECK ")
            false -> bad(sftpcheckMessage + "Failed")
        }
    }

    /**
     * getAllIgnoreReceivers - it extracts all the ignore organization from the organizations.yml file.
     *
     * @returns:
     *  sftpcheckReceiver - contains all the ignore organizations
     */
    fun getAllIgnoreRecivers(): List<String> {

        /**
         * Get all organizations from organizations.yml to the list
         */
        val file = File(receiverOrganizationPath)
        val input = String(file.readBytes())
        if (input.isBlank()) SettingCommand.abort("Blank input")
        val deepOrgs: List<DeepOrganization> = yamlMapper.readValue(input)
        val sftpcheckReceivers = mutableListOf<String>()

        /**
         * Extract ignore receivers from the list
         */
        deepOrgs.flatMap { it.receivers }.forEach { receiver ->
            if (receiver.organizationName.equals("ignore")) {
                sftpcheckReceivers += receiver.organizationName + "." + receiver.name
            }
        }
        return sftpcheckReceivers
    }
}