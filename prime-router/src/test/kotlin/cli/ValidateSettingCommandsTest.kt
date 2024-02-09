package gov.cdc.prime.router.cli

import kotlin.test.Test
import kotlin.test.assertTrue

class ValidateSettingCommandsTest {
    @Test
    fun `test validate settings organizations yml against organization schema`() {
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(listOf("-i", "settings/organizations.yml", "-s", "settings/organizations.schema.json"))
        val errors = validateCmd.validate()
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `test validate topic covid19 receiver with condition filter`() {
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(
            listOf(
                "-i", "./src/test/unit_test_files/invalid_setting_receiver_covid19_with_cond_filters.yml",
            "-s", "settings/organizations.schema.json"
            )
        )
        val result = validateCmd.validate()
        assertTrue(result.size == 2)
        result.forEach {
            assertTrue { it.message.contains("receiver") }
            assertTrue {
                it.message.contains("conditionFilter") || it.message.contains("mappedConditionFilter")
            }
        }
    }

    @Test
    fun `test validate organizations with jurisdiction vs state code vs county name`() {
        // TODO: map generic validator error message to report stream friendly messages
        //        validation completed: validation messages count: 8
        //        $[0]: must not be valid to the schema "$.items.$ref.else.then.allOf[1].not" : {"required":["countyName"]}
        //        $[1]: required property 'stateCode' not found
        //        $[2]: must not be valid to the schema "$.items.$ref.then.allOf[0].not" : {"required":["stateCode"]}
        //        $[3]: must not be valid to the schema "$.items.$ref.then.allOf[1].not" : {"required":["countyName"]}
        //        $[4]: required property 'stateCode' not found
        //        $[5]: required property 'stateCode' not found
        //        $[5]: required property 'countyName' not found
        //        $[6]: required property 'countyName' not found
        val validateCmd = ValidateSettingCommands()
        validateCmd.parse(
            listOf(
                "-i", "./src/test/unit_test_files/invalid_setting_jurisdiction_vs_state_code_county_name.yml",
            "-s", "settings/organizations.schema.json"
            )
        )
        val result = validateCmd.validate()

        assertTrue(result.size == 8)
        assertTrue(
            result.elementAt(0).toString().contains("countyName") &&
                result.elementAt(0).toString().contains("[0]: must not be valid to")
        )
        assertTrue(result.elementAt(1).toString().contains("[1]: required property 'stateCode' not found"))
        assertTrue(
            result.elementAt(2).toString().contains("stateCode") &&
                result.elementAt(2).toString().contains("[2]: must not be valid to")
        )
        assertTrue(
            result.elementAt(3).toString().contains("countyName") &&
                result.elementAt(3).toString().contains("[3]: must not be valid to")
        )
        assertTrue(result.elementAt(4).toString().contains("[4]: required property 'stateCode' not found"))
        assertTrue(result.elementAt(5).toString().contains("[5]: required property 'stateCode' not found"))
        assertTrue(result.elementAt(6).toString().contains("[5]: required property 'countyName' not found"))
        assertTrue(result.elementAt(7).toString().contains("[6]: required property 'countyName' not found"))
    }
}