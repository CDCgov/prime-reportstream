package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.rendering.AnsiLevel
import kotlin.test.Test

class ValidateYMLCommandTests {

    private val command = ValidateYAMLCommand()

    @Test
    fun `valid file`() {
        val result = command.test(
            "--type organizations --file src/test/resources/yaml_validation/success/test1.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("test1.yml is valid!")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `valid directory`() {
        val result = command.test(
            "--type organizations --dir src/test/resources/yaml_validation/success",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("test1.yml is valid!")
        assertThat(result.stdout).contains("test2.yml is valid!")
        assertThat(result.stdout).contains("test3.yml is valid!")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `invalid file`() {
        val result = command.test(
            "--type organizations --file src/test/resources/yaml_validation/failure/invalid.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stderr).contains("invalid.yml is invalid!")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `invalid directory`() {
        val result = command.test(
            "--type organizations --dir src/test/resources/yaml_validation/failure",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        // 1 file is valid and the other is invalid therefore the command fails
        assertThat(result.stdout).contains("valid.yml is valid!")
        assertThat(result.stderr).contains("invalid.yml is invalid!")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `not YAML file`() {
        val result = command.test(
            "--type organizations --file src/test/resources/yaml_validation/failure/recursive/not-yaml.txt",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stderr).contains("Supplied file is not a YAML file")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `no YAML files in directory`() {
        val result = command.test(
            "--type organizations --dir src/test/resources/yaml_validation/failure/recursive",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stderr).contains("Supplied directory contains no YAML files")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `missing directory or file option`() {
        val result = command.test(
            "--type organizations",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stderr).contains("Specify either a directory or a file")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun organizations() {
        val result = command.test(
            "--type organizations --file settings/organizations.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("organizations.yml is valid!")
        assertThat(result.statusCode).isEqualTo(0)
    }
}