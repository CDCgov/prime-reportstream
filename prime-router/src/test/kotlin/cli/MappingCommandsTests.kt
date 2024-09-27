package gov.cdc.prime.router.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.github.ajalt.clikt.testing.test
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.test.Test

class MappingCommandsTest {

    private val command = MappingCommands()

    @Test
    fun `should throw error when no directory is provided`() {
        // Simulate running the command without providing a directory
        val result = command.test(
            "--find-unreferenced"
        )
            // Ensure an error message is output
            assertThat(result.stderr).contains("You must specify a directory")
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `should list unreferenced files in a directory`() {
        val tempDir = createTempDirectory()

        // Create some test files
        val file1 = File(tempDir.toString(), "file1.yml").apply { writeText("file1 content") }
        val file2 = File(tempDir.toString(), "file2.yml").apply { writeText("Referencing file1: file1") }

        // Test the command
        val result = command.test(
            "--find-unreferenced", "--directory", tempDir.toString()
        )

        assertThat(result.stdout).contains(file2.absolutePath)
        assertThat(result.stdout).doesNotContain(file1.absolutePath)

        // Cleanup
        file1.delete()
        file2.delete()
        tempDir.deleteRecursively()
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun `should write output to a file`() {
        val tempDir = createTempDirectory()
        val outputFile = File(tempDir.toString(), "output.txt")

        // Create some test files
        val file1 = File(tempDir.toString(), "file1.yml").apply { writeText("file1 content") }
        val file2 = File(tempDir.toString(), "file2.yml").apply { writeText("Referencing file1: file1") }

        // Test the command with output file option
        command.test(
            "--find-unreferenced", "--directory", tempDir.toString(), "--output-file", outputFile.absolutePath
        )

        // Check that the output file contains the correct data
        val outputContent = outputFile.readText()
        assertThat(outputContent).contains(file2.absolutePath)
        assertThat(outputContent).doesNotContain(file1.absolutePath)

        // Cleanup
        file1.delete()
        file2.delete()
        outputFile.delete()
        tempDir.deleteRecursively()
    }
}