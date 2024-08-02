package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.rendering.AnsiLevel
import kotlin.test.Test

class ValidateYMLCommandTests {

    private val command = ValidateYAMLCommand()

    @Test
    fun `valid single file`() {
        val result = command.test(
            "--type organizations --file src/test/resources/yaml_validation/success/test1.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("test1.yml is valid!")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `valid single directory`() {
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
    fun `valid multiple file`() {
        val result = command.test(
            "--type organizations --file src/test/resources/yaml_validation/success/test1.yml " +
                "src/test/resources/yaml_validation/success/test2.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("test1.yml is valid!")
        assertThat(result.stdout).contains("test2.yml is valid!")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `valid multiple dirs`() {
        val result = command.test(
            "--type organizations --dir src/test/resources/yaml_validation/success " +
                "src/test/resources/yaml_validation/success2",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("test1.yml is valid!")
        assertThat(result.stdout).contains("test2.yml is valid!")
        assertThat(result.stdout).contains("test3.yml is valid!")
        assertThat(result.stdout).contains("test4.yml is valid!")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `valid dir exclude files`() {
        val result = command.test(
            "--type organizations --dir src/test/resources/yaml_validation/success --exclude-file " +
                "src/test/resources/yaml_validation/success/recursive/test3.yml",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("test1.yml is valid!")
        assertThat(result.stdout).contains("test2.yml is valid!")
        assertThat(result.stdout).doesNotContain("test3.yml")
        assertThat(result.statusCode).isEqualTo(0)
    }

    @Test
    fun `valid dir exclude dir`() {
        val result = command.test(
            "--type organizations --dir src/test/resources/yaml_validation/success --exclude-dir " +
                "src/test/resources/yaml_validation/success/recursive",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stdout).contains("test1.yml is valid!")
        assertThat(result.stdout).contains("test2.yml is valid!")
        assertThat(result.stdout).doesNotContain("test3.yml")
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

        assertThat(result.stdout).contains("not-yaml.txt is not a YAML file! It will be skipped during validation.")
        assertThat(result.stderr).contains("No YAML files being validated!")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `no YAML files in directory`() {
        val result = command.test(
            "--type organizations --dir src/test/resources/yaml_validation/failure/recursive",
            ansiLevel = AnsiLevel.TRUECOLOR
        )
        val outputNormalized = result.stdout.replace("\\", "/")

        assertThat(outputNormalized).contains(
            "src/test/resources/yaml_validation/failure/recursive contains no YAML files!"
        )
        assertThat(result.stderr).contains("No YAML files being validated!")
        assertThat(result.statusCode).isEqualTo(1)
    }

    @Test
    fun `missing directory or file option`() {
        val result = command.test(
            "--type organizations",
            ansiLevel = AnsiLevel.TRUECOLOR
        )

        assertThat(result.stderr).contains("No YAML files being validated!")
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