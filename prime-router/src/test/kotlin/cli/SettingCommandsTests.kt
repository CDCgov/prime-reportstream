package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.rendering.AnsiLevel
import kotlin.test.Test

class SettingCommandsTests {
    private val putMultipleSettings = PutMultipleSettings()
    private val putOrgSettings = PutOrganizationSetting()
    private val putSenderSettings = PutSenderSetting()
    private val putReceiverSettings = PutReceiverSetting()

    @Test
    fun `valid`() {
        val result = putOrgSettings.test(
            "--input src/test/resources/yaml_validation/success/test1.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("test1.yml is valid!")
        assertThat(result.statusCode).isEqualTo(0)
    }
}