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
    private val settingErrorMessage = "Test CRUD of Setting API: "

    /**
     * Define the new dummy organization to be used for test of setting/create the new
     * dummy organization.
     */
    private val newDummyOrganization = """
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
     * Define the update dummy organization to be used for test of updating the
     * dummy organization.
     */
    private val updateDummyOrganization = """
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
            else -> {
                return bad(settingErrorMessage + "Failed on create new dummy organization.")
            }
        }

        /**
         * VERIFY the created dummy organization
         */
        echo("VERITY the new dummy organization was created...")
        val (_, responseNewDummy, resultNewDummy) = SettingsUtilities.get(path, dummyAccessToken)
        val (payloadNewDummy, errorNewDummy) = resultNewDummy
        if (errorNewDummy?.response?.statusCode == HttpStatus.SC_NOT_FOUND) {
            return bad(settingErrorMessage + responseNewDummy.responseMessage)
        }

        /**
         * The payload must contain the known "NEWDUMMYORG" defined
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
            else ->
                return bad(settingErrorMessage + "Failed on can't create update dummy organization.")
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
         * The payload must contain the known "UPDATEDUMMYORG" defined
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

        return good("Test passed: Test CRUD REST API ")
    }
}

class SettingsUpdateValidationTest : CoolTest() {
    override val name = "settings-validation"
    override val description = """
        Test Setting API: 
        1. Create Organization of FEDERAL jurisdiction, expect 200 OK
        2. Update it with state code, county name present, expect 400 Bad Request
        3. Add sender happy case, expect 200 OK
        4. Add receiver happy case, expect 200 OK
        5. Add receiver with inconsistency, expect 400 Bad Request
    """
    override val status = TestStatus.DRAFT

    /**
     * Define private local variables for use in the test.
     */
    private val dummyAccessToken = "dummy-token"
    private val orgName = "org-with-federal-jurisdiction"
    private val receiverName = "receiver01"
    private val invalidReceiverName = "receiver_invalid"
    private val senderName = "sender01"
    private val settingErrorMessage = "Error Test Setting API: "

    /**
     * Fabricated organization for testing.
     */
    private val newTestOrg = """
        {
            "name": "$orgName",
            "description": "A Federal jurisdiction organization with state code, county name.",
            "jurisdiction": "FEDERAL",
            "meta": {
                "version": 0,
                "createdBy": "local@test.com",
                "createdAt": "2024-02-18T00:00:00.0Z"
            }
        }
    """.trimIndent()

    private val updateTestOrg = """
        {
            "name": "$orgName",
            "description": "A Federal jurisdiction organization with state code, county name.",
            "jurisdiction": "FEDERAL",
            "stateCode": "CA",
            "countyName": "San Diego",
            "meta": {
                "version": 0,
                "createdBy": "local@test.com",
                "createdAt": "2024-02-18T00:00:00.0Z"
            }
        }
    """.trimIndent()

    private val addSender = """
        {
            "name" : "$senderName",
            "organizationName" : "$orgName",
            "topic" : "full-elr",
            "format" : "CSV",
            "customerStatus" : "active",
            "schemaName" : "waters/waters-covid-19",
            "processingType" : "sync",
            "allowDuplicates" : true,
            "senderType" : null,
            "primarySubmissionMethod" : null
        }
""".trimIndent()

    private val addReceiver = """
        {
            "name" : "$receiverName",
            "organizationName" : "$orgName",
            "topic" : "covid-19",
            "customerStatus" : "active",
            "translation" : {
                "schemaName" : "az/az-covid-19",
                "format" : "CSV",
                "useBatching" : false,
                "defaults" : { },
                "nameFormat" : "STANDARD",
                "receivingOrganization" : null,
                "type" : "CUSTOM"
            },
            "transport" : {
                "storageName" : "PartnerStorage",
                "containerName" : "hhsprotect",
                "type" : "BLOBSTORE"
            }
        }
        """.trimIndent()

    private val addReceiverInvalid = """
        {
            "name" : "$invalidReceiverName",
            "organizationName" : "$orgName",
            "topic" : "covid-19",
            "customerStatus" : "active",
            "translation" : {
                "schemaName" : "az/az-covid-19",
                "format" : "CSV",
                "useBatching" : false,
                "defaults" : { },
                "nameFormat" : "STANDARD",
                "receivingOrganization" : null,
                "type" : "CUSTOM"
            },
            "conditionFilter": ["%resource.livdTableLookup('Component').contains('coronavirus').not()"],
            "transport" : {
                "storageName" : "PartnerStorage",
                "containerName" : "hhsprotect",
                "type" : "BLOBSTORE"
            }
        }
        """.trimIndent()

    override suspend fun run(environment: Environment, options: CoolTestOptions): Boolean {
        ugly("Starting Create Organization ${environment.url}")

        val path = SettingCommand.formPath(
            Environment.LOCAL,
            SettingCommand.Operation.PUT,
            SettingCommand.SettingType.ORGANIZATION,
            orgName
        )

        val pathDelete = SettingCommand.formPath(
            Environment.LOCAL,
            SettingCommand.Operation.DELETE,
            SettingCommand.SettingType.ORGANIZATION,
            orgName
        )

        val pathReceiver = SettingCommand.formPath(
            Environment.LOCAL,
            SettingCommand.Operation.PUT,
            SettingCommand.SettingType.RECEIVER,
            "$orgName.$receiverName"
        )

        val pathReceiverInvalid = SettingCommand.formPath(
            Environment.LOCAL,
            SettingCommand.Operation.PUT,
            SettingCommand.SettingType.RECEIVER,
            "$orgName.$invalidReceiverName"
        )

        val pathReceiverList = SettingCommand.formPath(
            Environment.LOCAL,
            SettingCommand.Operation.LIST,
            SettingCommand.SettingType.RECEIVER,
            orgName
        )

        val pathSender = SettingCommand.formPath(
            Environment.LOCAL,
            SettingCommand.Operation.PUT,
            SettingCommand.SettingType.SENDER,
            "$orgName.$senderName"
        )

        val pathSenderList = SettingCommand.formPath(
            Environment.LOCAL,
            SettingCommand.Operation.LIST,
            SettingCommand.SettingType.SENDER,
            orgName
        )

        echo("Delete the test organization if already exists ...")

        val (_, _, result) = SettingsUtilities.get(path, dummyAccessToken)
        val (_, error) = result
        if (error?.response?.statusCode != HttpStatus.SC_NOT_FOUND) {
            val (_, responseDel, _) = SettingsUtilities.delete(path, dummyAccessToken)
            when (responseDel.statusCode) {
                HttpStatus.SC_OK -> Unit
                else ->
                    return bad(
                        settingErrorMessage +
                            "Failed delete residual test organization $orgName - ${responseDel.responseMessage}."
                    )
            }
        }

        /**
         * CREATE the test organization
         */
        echo("CREATE the test organization..., should succeed")
        var output = SettingsUtilities.put(path, dummyAccessToken, newTestOrg)
        val (_, responseCreateOrg, _) = output
        when (responseCreateOrg.statusCode) {
            HttpStatus.SC_CREATED -> Unit
            else -> {
                return bad(
                    settingErrorMessage +
                        "Failed on create new test organization: $orgName, error code: ${responseCreateOrg.statusCode}."
                )
            }
        }

        /**
         * VERIFY the test organization
         */
        echo("VERITY the new test organization was created...by getting it back.")
        val (_, responseNewOrg, resultNewOrg) = SettingsUtilities.get(path, dummyAccessToken)
        val (_, errorNewOrg) = resultNewOrg
        if (errorNewOrg?.response?.statusCode == HttpStatus.SC_NOT_FOUND) {
            return bad(settingErrorMessage + responseNewOrg.responseMessage)
        }

        /**
         * Next update the newly created organization
         * Update failure expect because the organization has invalid fields: stateCode and countyName
         */
        echo("UPDATE the new test organization with invalid fields, stateCode, countyName.")
        val (_, responseUpdateOrg, _) = SettingsUtilities.put(path, dummyAccessToken, updateTestOrg)
        when (responseUpdateOrg.statusCode) {
            HttpStatus.SC_BAD_REQUEST -> {
                // expect 400 status code with validation errors
                val respMsg = responseUpdateOrg.toString()
                if (!(
                    respMsg.contains(
                        "For FEDERAL jurisdiction, 'stateCode' must NOT be present"
                    ) &&
                            respMsg.contains(
                        "For FEDERAL jurisdiction, 'countyName' must NOT be present"
                            )
                )
                ) {
                            Unit
                        } else {
                            return bad(
                        settingErrorMessage +
                                "Expect validation error when updating $orgName response 400 code," +
                                " but got response: $respMsg"
                            )
                        }
            }
            else ->
                // got response code other than 400
                return bad(
                    settingErrorMessage +
                        "Expect validation error when updating $orgName response 400 code," +
                        " but got: ${responseUpdateOrg.statusCode}, " +
                            "response text: $responseUpdateOrg.toString()"
                )
        }

        /**
         * Add a receiver to the test organization
         */
        echo("Add a receiver to the test organization..., should succeed")
        val (_, responseAddReceiver, _) = SettingsUtilities.put(pathReceiver, dummyAccessToken, addReceiver)
        when (responseAddReceiver.statusCode) {
            HttpStatus.SC_CREATED -> Unit
            else -> {
                return bad(
                    settingErrorMessage +
                        "Failed on add receiver: $receiverName to test" +
                            " organization: $orgName, error code: ${responseAddReceiver.statusCode}."
                )
            }
        }

        /**
         * Verify by retrieve the receiver
         */
        echo("Get the receiver of the test organization..., should succeed")
        val (_, responseListReceiver, _) = SettingsUtilities.get(pathReceiverList, dummyAccessToken)
        when (responseListReceiver.statusCode) {
            HttpStatus.SC_OK -> Unit
            else -> {
                return bad(
                    settingErrorMessage +
                        "Can not find receiver: $receiverName of test " +
                            "organization: $orgName, error code: ${responseListReceiver.statusCode}."
                )
            }
        }

        /**
         * Add a receiver to the test organization
         */
        echo("Add a receiver (invalid) to the test organization..., should fail with 400 status code.")
        val (_, responseAddReceiverInvalid, _) =
            SettingsUtilities.put(pathReceiverInvalid, dummyAccessToken, addReceiverInvalid)
        when (responseAddReceiverInvalid.statusCode) {
            HttpStatus.SC_BAD_REQUEST -> {
                // expect 400 status code with validation errors
                val respMsg = responseUpdateOrg.toString()
                if (!(
                    respMsg.contains(
                        "conditionFilter not allowed for topic: 'covid-19', 'monkeypox', 'test'"
                    )
                )
                ) {
                            Unit
                        } else {
                            return bad(
                        settingErrorMessage +
                                "Expect validation error when updating $orgName response 400 code," +
                                " but got response: $respMsg"
                            )
                        }
            }
            else -> {
                return bad(
                    settingErrorMessage +
                        "Failed on add receiver (invalid): $invalidReceiverName " +
                        "to test organization: $orgName, " +
                            "error code: ${responseAddReceiverInvalid.statusCode}."
                )
            }
        }

        /**
         * Add a sender to the test organization
         */
        echo("Add a sender to the test organization..., should succeed")
        val (_, responseAddSender, _) = SettingsUtilities.put(pathSender, dummyAccessToken, addSender)
        when (responseAddSender.statusCode) {
            HttpStatus.SC_CREATED -> Unit
            else -> {
                return bad(
                    settingErrorMessage +
                        "Failed on add sender: $senderName to test organization: $orgName, " +
                            "error code: ${responseAddSender.statusCode}."
                )
            }
        }

        /**
         * Verify by retrieve the sender
         */
        echo("Get the sender of the test organization..., should succeed")
        val (_, responseListSender, _) = SettingsUtilities.get(pathSenderList, dummyAccessToken)
        when (responseListSender.statusCode) {
            HttpStatus.SC_OK -> Unit
            else -> {
                return bad(
                    settingErrorMessage +
                        "Can not find sender: $senderName of test organization: $orgName, " +
                            "error code: ${responseListSender.statusCode}."
                )
            }
        }

        /**
         * DELETE the test organization, post test clean up
         */
        echo("DELETE the test organization...")
        val (_, responseDelTestOrg, resultDelTestOrg) = SettingsUtilities.delete(pathDelete, dummyAccessToken)
        val (_, errorDelTestOrg) = resultDelTestOrg
        if (errorDelTestOrg?.response?.statusCode == HttpStatus.SC_NOT_FOUND) {
            return bad(settingErrorMessage + "Failed on delete (clean up) - " + responseDelTestOrg.responseMessage)
        }

        /**
         * VERIFY the dummy organization deleted
         */
        echo("VERIFY the test organization is deleted...")
        val (_, responseCleanUpTestOrg, resultCleanUpTestOrg) = SettingsUtilities.get(path, dummyAccessToken)
        val (_, errorDummy) = resultCleanUpTestOrg
        if (errorDummy?.response?.statusCode != HttpStatus.SC_NOT_FOUND) {
            return bad(settingErrorMessage + "Failed cleaned up - " + responseCleanUpTestOrg.responseMessage)
        }

        return good(
            """
                Test passed: Test Setting API:
                1. Create Org happy case, Passed
                2. Update Org with Invalid fields got expected 400 response, Passed
                3. Add Receiver happy case, Passed
                4. Add Receiver with inconsistency got expected 400 response, Passed
                5. Add Sender happy case, Passed
        """
        )
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
            emptySender.schemaName,
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