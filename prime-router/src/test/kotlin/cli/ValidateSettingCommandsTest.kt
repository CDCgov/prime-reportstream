package gov.cdc.prime.router.cli

import kotlin.test.Test
import kotlin.test.assertTrue

class ValidateSettingCommandsTest {
    @Test
    fun `test validate settings organizations yml against organization schema`() {
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(listOf("-i", "settings/organizations.yml", "-s", "settings/schemas/settings.json"))
        val errors = validateCmd.validate()
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `test validate topic covid19 receiver with condition filter`() {
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(
            listOf(
                "-i", "./src/test/unit_test_files/invalid_setting_receiver_covid19_with_cond_filters.yml",
            "-s", "settings/schemas/settings.json"
            )
        )
        val result = validateCmd.validate()
        assertTrue(result.size == 2)
        result.forEach {
            assertTrue { it.message.contains("Filter not allowed for topic: 'covid-19', 'monkeypox', 'test'") }
            assertTrue {
                it.message.contains("conditionFilter") || it.message.contains("mappedConditionFilter")
            }
        }
    }

    @Test
    fun `test validate organizations with jurisdiction vs state code vs county name`() {
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(
            listOf(
                "-i", "./src/test/unit_test_files/invalid_setting_jurisdiction_vs_state_code_county_name.yml",
            "-s", "settings/schemas/settings.json"
            )
        )
        val result = validateCmd.validate()

        assertTrue(result.size == 8)
        assertTrue(
            result.elementAt(0).toString()
                .contains("[0]: For STATE jurisdiction, countyName must NOT present")
        )
        assertTrue(
            result.elementAt(1).toString()
            .contains("[1]: For STATE jurisdiction, stateCode must present")
        )
        assertTrue(
            result.elementAt(2).toString()
                .contains("[2]: For FEDERAL jurisdiction, stateCode must NOT present")
        )
        assertTrue(
            result.elementAt(3).toString()
                .contains("[3]: For FEDERAL jurisdiction, countyName must NOT present")
        )
        assertTrue(
            result.elementAt(4).toString()
            .contains("[4]: For COUNTY jurisdiction, both stateCode and countyName is required")
        )
        assertTrue(
            result.elementAt(5).toString()
            .contains("[5]: For COUNTY jurisdiction, both stateCode and countyName is required")
        )
        assertTrue(
            result.elementAt(6).toString()
            .contains("[5]: For COUNTY jurisdiction, both stateCode and countyName is required")
        )
        assertTrue(
            result.elementAt(7).toString()
            .contains("[6]: For COUNTY jurisdiction, both stateCode and countyName is required")
        )
    }

    @Test
    fun `test validate single sender with sub schema sender json`() {
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(
            listOf(
                "-i", "./src/test/unit_test_files/one_sender_waters.yml",
                "-s", "settings/schemas/sender.json"
            )
        )
        val result = validateCmd.validate()

        assertTrue(result.size == 1)
        assertTrue(result.elementAt(0).toString().contains("name: null found, string expected"))
    }

    @Test
    fun `test validate single receiver with sub schema receiver json`() {
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(
            listOf(
                "-i", "./src/test/unit_test_files/one_receiver_waters.yml",
                "-s", "settings/schemas/receiver.json"
            )
        )
        val result = validateCmd.validate()

        assertTrue(result.size == 1)
        assertTrue(
            result.elementAt(0).toString()
        .contains("conditionFilter not allowed for topic: 'covid-19', 'monkeypox', 'test'")
        )
    }

    @Test
    fun `test validate single organization with sub schema organization json`() {
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(
            listOf(
                "-i", "./src/test/unit_test_files/one_org_waters.yml",
                "-s", "settings/schemas/organization.json"
            )
        )
        val result = validateCmd.validate()

        assertTrue(result.size == 2)
        assertTrue(
            result.elementAt(0).toString()
            .contains("conditionFilter not allowed for topic: 'covid-19', 'monkeypox', 'test'")
        )
        assertTrue(
            result.elementAt(1).toString()
            .contains("For FEDERAL jurisdiction, stateCode must NOT present")
        )
    }
}