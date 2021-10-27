package gov.cdc.prime.router.cli.tests

import gov.cdc.prime.router.azure.ReportStreamEnv
import gov.cdc.prime.router.cli.SettingsUtilities

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

class Settings : CoolTest() {
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
     * Define private local environment to use for creating the path endpoint
     * to the dummy organization.
     */
    private val envlocal = SettingsUtilities.Environment(
        "local",
        (System.getenv("PRIME_RS_API_ENDPOINT_HOST") ?: "localhost") + ":7071",
        useHttp = true,
    )

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

    override suspend fun run(environment: ReportStreamEnv, options: CoolTestOptions): Boolean {
        ugly("Starting CRUD REST API ${environment.urlPrefix}")

        val path = SettingsUtilities.formPath(
            envlocal,
            SettingsUtilities.Operation.GET,
            SettingsUtilities.SettingType.ORG,
            settingName
        )

        // VERIFY the dummy organization existed or not
        echo("VERIFY the dummy organization existed or not...")
        var output = SettingsUtilities.get(path, dummyAccessToken, settingName)
        if (!output.contains("Error")) {
            output = SettingsUtilities.delete(path, dummyAccessToken, settingName)
            if (output.contains("Error")) {
                return bad(settingErrorMessage + output)
            }
        }

        // CREATE the dummy organization
        echo("CREATE the new dummy organization...")
        output = SettingsUtilities.put(path, dummyAccessToken, settingName, newDummyOrganization)
        if (output.contains("Error")) {
            return bad(settingErrorMessage + output)
        }

        // VERIFY the created dummy organization
        echo("VERITY the new dummy organization was created...")
        output = SettingsUtilities.get(path, dummyAccessToken, settingName)
        if (output.contains("Error")) {
            return bad(settingErrorMessage + output)
        }

        if (!output.contains("NEWDUMMYORG")) {
            return bad(settingErrorMessage + "It is not the created dummy organization.")
        }

        // UPDATE the dummy organization
        echo("UPDATE the dummy organization...")
        output = SettingsUtilities.put(path, dummyAccessToken, settingName, updateDummyOrganization)
        if (output.contains("Error")) {
            return bad(settingErrorMessage + output)
        }

        // VERIFY the updated dummy organization
        echo("VERIFY it is the new dummy organization is updated...")
        output = SettingsUtilities.get(path, dummyAccessToken, settingName)
        if (output.contains("Error")) {
            return bad(settingErrorMessage + output)
        }

        if (!output.contains("UPDATEDUMMYORG")) {
            return bad(settingErrorMessage + "It is not the updated dummy organization.")
        }

        // DELETE the updated dummy organization
        echo("DELETE the updated dummy organization...")
        output = SettingsUtilities.delete(path, dummyAccessToken, settingName)
        if (output.contains("Error")) {
            return bad(settingErrorMessage + output)
        }

        // VERIFY the dummy organization deleted
        echo("VERIFY it is the new dummy organization is deleted...")
        output = SettingsUtilities.get(path, dummyAccessToken, settingName)
        if (!output.contains("Error")) {
            return bad(settingErrorMessage + "The updated dummy organization not deleted.")
        }

        return good("Test passed: Test GRUD REST API ")
    }
}