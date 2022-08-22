package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.azure.HttpUtilities
import gov.cdc.prime.router.cli.FileUtilities
import gov.cdc.prime.router.cli.SettingCommand
import gov.cdc.prime.router.cli.SettingsUtilities
import gov.cdc.prime.router.common.Environment
import org.apache.http.HttpStatus
import java.net.HttpURLConnection

/**
 * Test CRUD of the Setting API.  It is smoke test that does the following steps:
 *  1. Do a 'get' to confirm that no organization exists called 'dummy'.
 *  2. CREATE: Do a 'set' to submit an Organization called 'dummy'
 *  3. READ: Do a 'get' to confirm that 'dummy' now exists, and that it has correct fields.
 *  4. UPDATE: Do a 'set' to submit a modification of the 'dummy' organization, with some minor tweak
 *  5. Do another 'get' to confirm you get back the new/modified version of 'dummy'
 *  6. DELETE: Do a 'delete' to remove 'dummy'
 *  7. Re-run step 1, to confirm that it's gone.
 *
 * @returns:
 *  If SUCCESS:
 *      good("Test passed: Test GRUD REST API ")
 *  If ERROR:
 *      bad("Test GRUD of Setting API: $output" - Where $output is the specific error message from CRUD API
 *
 */

class SettingsTest : CoolTest() {
    override val name = "settings"
    override val description = "Test CRUD of the Setting API"
    override val status = TestStatus.DRAFT

    /**
     * Define private local variables for use in the test.
     */
    private val dummyAccessToken = "dummy"
    private val settingName = "dummy"
    private val settingErrorMessage = "Test GRUD of Setting API: "

    /**
     * Define the new dummy organization to be use for test of setting/create the new
     * dummy organization.
     */
    val newDummyOrganization = """
        {
            "name": "dummy",
            "description": "NEWDUMMYORG",
            "jurisdiction": "FEDERAL",
            "stateCode": null,
            "countyName": null,
            "meta": {
                "version": 0,
                "createdBy": "local@test.com",
                "createdAt": "2021-01-01T00:00:00.0Z"
            }
        }
    """

    /**
     * Define the update dummy organization to be use for test of updating the
     * dummy organization.
     */
    val updateDummyOrganization = """
        {
            "name": "dummy",
            "description": "UPDATEDUMMYORG",
            "jurisdiction": "FEDERAL",
            "stateCode": null,
            "countyName": null,
            "meta": {
                "version": 0,
                "createdBy": "local@test.com",
                "createdAt": "2021-01-01T00:00:00.0Z"
            }
        }
    """

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting CRUD REST API ${environment.url}")

        /**
         * Obtain the URL/path endpoint
         */
        val path = SettingCommand.formPath(
            Environment.LOCAL,
            SettingCommand.Operation.GET,
            SettingCommand.SettingType.ORGANIZATION,
            settingName
        )

        /**
         * VERIFY the dummy organization existed or not
         */
        echo("VERIFY the dummy organization existed or not...")

        val (_, _, result) = SettingsUtilities.get(path, dummyAccessToken)
        val (_, error) = result
        if (error?.response?.statusCode != HttpStatus.SC_NOT_FOUND) {
            val (_, responseDel, resultDel) = SettingsUtilities.delete(path, dummyAccessToken)
            val (_, errorDel) = resultDel
            when (errorDel?.response?.statusCode) {
                HttpStatus.SC_OK -> Unit
                else ->
                    return bad(settingErrorMessage + "Failed Dummy organization - ${responseDel.responseMessage}.")
            }
        }

        /**
         * CREATE the dummy organization
         */
        echo("CREATE the new dummy organization...")
        var output = SettingsUtilities.put(path, dummyAccessToken, newDummyOrganization)
        val (_, responseCreateNewDummy, _) = output
        when (responseCreateNewDummy.statusCode) {
            HttpStatus.SC_CREATED -> Unit
            else -> return bad(settingErrorMessage + "Failed on create new dummy organization.")
        }

        /**
         * VERIFY the created dummy organization
         */
        echo("VERITY the new dummy organization was created...")
        var (_, responseNewDummy, resultNewDummy) = SettingsUtilities.get(path, dummyAccessToken)
        var (payloadNewDummy, errorNewDummy) = resultNewDummy
        if (errorNewDummy?.response?.statusCode == HttpStatus.SC_NOT_FOUND) {
            return bad(settingErrorMessage + responseNewDummy.responseMessage)
        }

        /**
         * The payload must contains the known "NEWDUMMYORG" defined
         * in the newDummyOrganization resource above.
         */
        if (!payloadNewDummy?.contains("NEWDUMMYORG")!!) {
            return bad(settingErrorMessage + "It is not the created dummy organization.")
        }

        /**
         * UPDATE the dummy organization
         */
        output = SettingsUtilities.put(path, dummyAccessToken, updateDummyOrganization)
        val (_, responseCreateUpdateDummy, _) = output
        when (responseCreateUpdateDummy.statusCode) {
            HttpStatus.SC_OK -> Unit
            else -> return bad(settingErrorMessage + "Failed on can't create update dummy organization.")
        }

        /**
         * VERIFY the updated dummy organization
         */
        echo("VERIFY it is the new dummy organization is updated...")
        val (_, _, resultUpdateOrg) = SettingsUtilities.get(path, dummyAccessToken)
        val (payload, errorUpdateDummy) = resultUpdateOrg
        if (errorUpdateDummy?.response?.statusCode == HttpStatus.SC_NOT_FOUND) {
            return bad(settingErrorMessage + "Failed on verify the new dummy organization.")
        }

        /**
         * The payload must contains the known "UPDATEDUMMYORG" defined
         * in the updateDummyOrganization resource above.
         */
        if (!payload?.contains("UPDATEDUMMYORG")!!) {
            return bad(settingErrorMessage + "It is not the updated dummy organization.")
        }

        /**
         * DELETE the updated dummy organization
         */
        echo("DELETE the updated dummy organization...")
        val (_, responseDelUpdateOrg, resultDelUpdateOrg) = SettingsUtilities.delete(path, dummyAccessToken)
        val (_, errorDelUpdateOrg) = resultDelUpdateOrg
        if (errorDelUpdateOrg?.response?.statusCode == HttpStatus.SC_NOT_FOUND) {
            return bad(settingErrorMessage + "Failed on delete - " + responseDelUpdateOrg.responseMessage)
        }

        /**
         * VERIFY the dummy organization deleted
         */
        echo("VERIFY it is the new dummy organization is deleted...")
        val (_, responseCleanUpDummyOrg, resultCleanUpDummyOrg) = SettingsUtilities.get(path, dummyAccessToken)
        val (_, errorDummy) = resultCleanUpDummyOrg
        if (errorDummy?.response?.statusCode != HttpStatus.SC_NOT_FOUND) {
            return bad(settingErrorMessage + "Failed cleaned up - " + responseCleanUpDummyOrg.responseMessage)
        }

        return good("Test passed: Test GRUD REST API ")
    }
}

/**
 * The key to this [SenderSettings] smoke test is in the SETTINGS_TEST receiver in organizations.yml.
 * SETTINGS_TEST has specific filters such that if the sender settings were not properly extracted
 * from the `ignore.ignore-empty` sender, the SETTINGS_TEST receiver's data will get filtered out,
 * and the test will fail.
 */
class SenderSettings : CoolTest() {
    override val name = "sender-settings"
    override val description = "Test that we can extract values from Sender settings and put into our data "
    override val status = TestStatus.SMOKE

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting $name: $description")
        val file = FileUtilities.createFakeCovidFile(
            metadata,
            settings,
            emptySender,
            options.items,
            receivingStates,
            settingsTestReceiver.name,
            options.dir,
        )
        echo("Created datafile $file")
        val (responseCode, json) =
            HttpUtilities.postReportFile(
                environment,
                file,
                emptySender,
                options.asyncProcessMode,
                options.key,
                payloadName = "$name ${status.description}",
            )
        echo("Response to POST: $responseCode")
        if (!options.muted) echo(json)
        if (responseCode != HttpURLConnection.HTTP_CREATED) {
            return bad("***$name Test FAILED***:  response code $responseCode")
        }
        val reportId = getReportIdFromResponse(json)
            ?: return bad("***$name Test FAILED***: A report ID came back as null")
        echo("Id of submitted report: $reportId")
        return pollForLineageResults(
            reportId = reportId,
            receivers = listOf(settingsTestReceiver),
            totalItems = options.items,
            asyncProcessMode = options.asyncProcessMode
        )
    }
}