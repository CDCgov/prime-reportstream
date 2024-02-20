package gov.cdc.prime.router.cli

import assertk.assertThat
import com.github.ajalt.clikt.testing.test
import kotlin.test.Test

class ValidateSettingCommandsTest {
    @Test
    fun `test validate settings organizations yml against organization schema`() {
        val validateCmd = ValidateSettingCommands()
        val result = validateCmd.test(
            listOf(
                "-i", "settings/organizations.yml",
                "-s", "./src/main/resources/settings/schemas/settings.json"
            )
        )
        assertThat(
            result.stdout.contains(
                "validation completed: validation messages count: 0"
            )
        )
    }

    @Test
    fun `test validate topic covid19 receiver with condition filter`() {
        val validateCmd = ValidateSettingCommands()
        val result = validateCmd.test(
            listOf(
                "-i", "./src/test/unit_test_files/invalid_setting_receiver_covid19_with_cond_filters.yml",
                "-s", "./src/main/resources/settings/schemas/settings.json"
            )
        )
        assertThat(
            result.stdout.contains(
                "validation completed: validation messages count: 2"
            )
        )
        assertThat(
            result.stdout.contains(
                "error: conditionFilter not allowed for topic: 'covid-19', 'monkeypox', 'test'"
            )
        )
        assertThat(
            result.stdout.contains(
                "error: mappedConditionFilter not allowed for topic: 'covid-19', 'monkeypox', 'test'"
            )
        )
    }

    @Test
    fun `test validate organizations with jurisdiction vs state code vs county name`() {
        val validateCmd = ValidateSettingCommands()
        val result = validateCmd.test(
            listOf(
                "-i", "./src/test/unit_test_files/invalid_setting_jurisdiction_vs_state_code_county_name.yml",
                "-s", "./src/main/resources/settings/schemas/settings.json"
            )
        )
        assertThat(
            result.stdout.contains(
            "validation completed: validation messages count: 7"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$[0]: For STATE jurisdiction, countyName must NOT be present"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$[1]: For STATE jurisdiction, stateCode must present"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$[2]: For FEDERAL jurisdiction, stateCode must NOT be present"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$[3]: For FEDERAL jurisdiction, countyName must NOT be present"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$[4]: For COUNTY jurisdiction, both stateCode and countyName is required"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$[5]: For COUNTY jurisdiction, both stateCode and countyName is required"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$[5]: For COUNTY jurisdiction, both stateCode and countyName is required"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$[6]: For COUNTY jurisdiction, both stateCode and countyName is required"
            )
        )
    }

    @Test
    fun `test validate single sender with sub schema sender json`() {
        val validateCmd = ValidateSettingCommands()
        val result = validateCmd.test(
            listOf(
                "-i", "./src/test/unit_test_files/one_sender_waters.yml",
                "-s", "./src/main/resources/settings/schemas/sender.json"
            )
        )
        assertThat(
            result.stdout.contains(
            "validation completed: validation messages count: 1"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$.name: null found, string expected"
            )
        )
    }

    @Test
    fun `test validate single receiver with sub schema receiver json`() {
        val validateCmd = ValidateSettingCommands()
        val result = validateCmd.test(
            listOf(
                "-i", "./src/test/unit_test_files/one_receiver_waters.yml",
                "-s", "./src/main/resources/settings/schemas/receiver.json"
            )
        )
        assertThat(
            result.stdout.contains(
            "validation completed: validation messages count: 1"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: conditionFilter not allowed for topic: 'covid-19', 'monkeypox', 'test'"
            )
        )
    }

    @Test
    fun `test validate single organization with sub schema organization json`() {
        val validateCmd = ValidateSettingCommands()
        val result = validateCmd.test(
            listOf(
                "-i", "./src/test/unit_test_files/one_org_waters.yml",
                "-s", "./src/main/resources/settings/schemas/organization.json"
            )
        )
        assertThat(
            result.stdout.contains(
            "validation completed: validation messages count: 2"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: conditionFilter not allowed for topic: 'covid-19', 'monkeypox', 'test'"
            )
        )
        assertThat(
            result.stdout.contains(
            "error: \$: For FEDERAL jurisdiction, stateCode must NOT be present"
            )
        )
    }
}