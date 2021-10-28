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

        val (_, _, result) = SettingsUtilities.get(path, dummyAccessToken)
        val (_, error) = result
        if (error?.response?.statusCode != 404) {
            val (_, _, result) = SettingsUtilities.delete(path, dummyAccessToken)
            if (result == null)
                return bad(settingErrorMessage)
        }

        // CREATE the dummy organization
        echo("CREATE the new dummy organization...")
        var output = SettingsUtilities.put(path, dummyAccessToken, newDummyOrganization)
        if (output == null) {
            return bad(settingErrorMessage + output)
        }

        // VERIFY the created dummy organization
        echo("VERITY the new dummy organization was created...")
        var (_, _, resultNewDummy) = SettingsUtilities.get(path, dummyAccessToken)
        var (payloadNewDummy, errorNewDummy) = resultNewDummy
        if (errorNewDummy != null) {
            return bad(settingErrorMessage)
        }

        if (!payloadNewDummy?.contains("NEWDUMMYORG")!!) {
            return bad(settingErrorMessage + "It is not the created dummy organization.")
        }

        // UPDATE the dummy organization
        output = SettingsUtilities.put(path, dummyAccessToken, updateDummyOrganization)
        if (output == null) {
            return bad(settingErrorMessage + "can't update the organization")
        }

        // VERIFY the updated dummy organization
        echo("VERIFY it is the new dummy organization is updated...")
        val (_, _, resultUpdateOrg) = SettingsUtilities.get(path, dummyAccessToken)
        val (payload, errorUpdateDummy) = resultUpdateOrg
        if (errorUpdateDummy != null) {
            return bad(settingErrorMessage + "can't get the updated organization")
        }

        if (!payload?.contains("UPDATEDUMMYORG")!!) {
            return bad(settingErrorMessage + "It is not the updated dummy organization.")
        }

        // DELETE the updated dummy organization
        echo("DELETE the updated dummy organization...")
        val (_, _, resultDel) = SettingsUtilities.delete(path, dummyAccessToken)
        if (resultDel == null) {
            return bad(settingErrorMessage + "can't delete organization.")
        }

        // VERIFY the dummy organization deleted
        echo("VERIFY it is the new dummy organization is deleted...")
        val (_, _, resultDummy) = SettingsUtilities.get(path, dummyAccessToken)
        val (_, errorDummy) = resultDummy
        if (errorDummy == null) {
            return bad(settingErrorMessage + "The updated dummy organization not deleted.")
        }

        return good("Test passed: Test GRUD REST API ")
    }
}