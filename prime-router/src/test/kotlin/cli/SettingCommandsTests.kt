package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.rendering.AnsiLevel
import org.junit.jupiter.api.Assertions
import kotlin.test.Test

class SettingCommandsTests {
    private val multipleSettings = MultipleSettings()
    private val orgSettings = OrganizationSettings()
    private val senderSettings = SenderSettings()
    private val receiverSettings = ReceiverSettings()

    @Test
    fun `valid organization`() {
        val result = orgSettings.test(
            "set -i src/test/resources/yaml_validation/settings/org-success.yml",
            stdin = "y",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("org-success.yml is valid!")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `valid organization no validation`() {
        val result = orgSettings.test(
            "set -i src/test/resources/yaml_validation/settings/org-success.yml --no-validation",
            stdin = "y",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).doesNotContain("org-success.yml is valid!")
        assertThat(result.stdout).contains("org-success.yml will not be validated.")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `invalid organization`() {
        val result = orgSettings.test(
            "set -i src/test/resources/yaml_validation/failure/invalid.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stderr).contains("invalid.yml is invalid!")
        assertThat(result.stdout).doesNotContain("Success.")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `invalid organization no validation`() {
        Assertions.assertThrows(MismatchedInputException::class.java) {
            val result = orgSettings.test(
                "set -i src/test/resources/yaml_validation/failure/invalid.yml --no-validation",
                ansiLevel = AnsiLevel.TRUECOLOR
            )
            assertThat(result.stdout).contains("invalid.yml will not be validated.")
        }
    }

    @Test
    fun `organization json`() {
        val result = orgSettings.test(
            "set -i src/test/resources/yaml_validation/settings/org-success.json --json",
            stdin = "y\ny",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains(
            "JSON files cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stdout).contains("Success.")
        assertThat(result.stdout).contains("org-success.json will not be validated.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `organization json do not proceed`() {
        val result = orgSettings.test(
            "set -i src/test/resources/yaml_validation/settings/org-success.json --json",
            stdin = "n",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains(
            "JSON files cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stderr).contains("No change applied")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `organization json no validation`() {
        val result = orgSettings.test(
            "set -i src/test/resources/yaml_validation/settings/org-success.json --json --no-validation",
            stdin = "y",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).doesNotContain(
            "JSON files cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stdout).contains("org-success.json will not be validated.")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `sender`() {
        val result = senderSettings.test(
            "set -i src/test/resources/yaml_validation/settings/sender.yml",
            stdin = "y\ny",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains(
            "SENDERS cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stdout).contains("sender.yml will not be validated.")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `sender no validation`() {
        val result = senderSettings.test(
            "set -i src/test/resources/yaml_validation/settings/sender.yml --no-validation",
            stdin = "y\ny",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).doesNotContain(
            "SENDERS cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stdout).contains("sender.yml will not be validated.")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `sender do not proceed`() {
        val result = senderSettings.test(
            "set -i src/test/resources/yaml_validation/settings/sender.yml",
            stdin = "n",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains(
            "SENDERS cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stderr).contains("No change applied")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `receiver`() {
        val result = receiverSettings.test(
            "set -i src/test/resources/yaml_validation/settings/receiver.yml",
            stdin = "y\ny",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains(
            "RECEIVERS cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stdout).contains("receiver.yml will not be validated.")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `receiver no validation`() {
        val result = receiverSettings.test(
            "set -i src/test/resources/yaml_validation/settings/receiver.yml --no-validation",
            stdin = "y\ny",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).doesNotContain(
            "RECEIVERS cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stdout).contains("receiver.yml will not be validated.")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `receiver do not proceed`() {
        val result = receiverSettings.test(
            "set -i src/test/resources/yaml_validation/settings/receiver.yml",
            stdin = "n",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains(
            "RECEIVERS cannot be validated at this time! You can use the " +
                "--no-validation flag to avoid this prompt."
        )
        assertThat(result.stderr).contains("No change applied")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `valid multiple-settings`() {
        val result = multipleSettings.test(
            "set -i settings/organizations.yml",
            stdin = "y",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("organizations.yml is valid!")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `valid multiple-settings no validation`() {
        val result = multipleSettings.test(
            "set -i settings/organizations.yml --no-validation",
            stdin = "y",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).doesNotContain("organizations.yml is valid!")
        assertThat(result.stdout).contains("organizations.yml will not be validated.")
        assertThat(result.stdout).contains("Success.")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `invalid multiple-settings`() {
        val result = multipleSettings.test(
            "set -i src/test/resources/yaml_validation/failure/invalid.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stderr).contains("invalid.yml is invalid!")
        assertThat(result.stdout).doesNotContain("Success.")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `invalid multiple-settings no validation`() {
        Assertions.assertThrows(MismatchedInputException::class.java) {
            val result = multipleSettings.test(
                "set -i src/test/resources/yaml_validation/failure/invalid.yml --no-validation",
                ansiLevel = AnsiLevel.TRUECOLOR
            )
            assertThat(result.stdout).contains("invalid.yml will not be validated.")
        }
    }
}