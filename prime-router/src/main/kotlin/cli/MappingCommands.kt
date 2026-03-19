package gov.cdc.prime.router.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.io.IOException

/**
 * A CLI command class for handling data mapping tasks such as finding unreferenced files.
 * This command can be extended to include more functionality in the future.
 */
class MappingCommands :
    CliktCommand(
        name = "data-mapping",
    ) {
    override fun help(context: Context): String = "CLI tool for data mapping tasks"

    /**
     * The directory to scan for unreferenced files.
     * This option is required when the `--find-unreferenced` flag is provided.
     */
    private val inputDir by option("-d", "--directory", help = "Directory to scan for unreferenced files")
        .file(true, canBeDir = true, mustBeReadable = true)

    /**
     * Output file to write the list of unreferenced files.
     * If provided, the results will be written to this file instead of being printed to the console.
     */
    private val outputFile by option("-o", "--output-file", help = "Output file for unreferenced files")
        .file()

    /**
     * Flag to trigger the action of finding unreferenced files in the specified directory.
     * When this flag is set, the command will search for files that are not referenced by any other files in the directory.
     */
    private val findUnreferencedFiles by option(
        "--find-unreferenced",
        help = "Find unreferenced files in the directory"
    )
        .flag(default = false)

    /**
     * Main entry point for the command.
     * If the `--find-unreferenced` flag is set, the command will find and print/write the unreferenced files.
     * If no option is provided, a help message is displayed.
     */
    override fun run() {
        // If findUnreferencedFiles option is set, call the corresponding function
        if (findUnreferencedFiles) {
            if (inputDir == null) {
                throw CliktError("You must specify a directory with --directory to find unreferenced files.")
            }
            findUnreferencedFiles(inputDir!!)
        } else {
            echo("No command specified. Use --help to find available options.")
        }
    }

    /**
     * Main method to find unreferenced files in the given directory.
     */
    private fun findUnreferencedFiles(directory: File) {
        if (!directory.isDirectory) {
            throw CliktError("The specified path '${directory.absolutePath}' is not a directory.")
        }

        val allFiles = getAllFiles(directory)

        // Extract relative file paths without extensions
        val relativePathsWithoutExtension = allFiles.map {
            it.relativeTo(directory).path.removeSuffix(".${it.extension}")
        }

        // Set of relative file paths that are referenced in other files
        val referencedFiles = mutableSetOf<String>()

        // Search for each relative path in other files
        allFiles.forEach { file ->
            referencedFiles.addAll(searchFileForReferences(file, relativePathsWithoutExtension))
        }

        // Find unreferenced files by checking which relative paths weren't found
        val unreferencedFiles = allFiles.filter {
            it.relativeTo(directory).path.removeSuffix(".${it.extension}") !in referencedFiles
        }.sorted()

        // Output the result
        if (unreferencedFiles.isNotEmpty()) {
            if (outputFile != null) {
                outputFile!!.writeText(unreferencedFiles.joinToString("\n") { it.absolutePath })
                echo("Wrote unreferenced files list to ${outputFile!!.absolutePath}")
            } else {
                echo("The following files are not referenced by any other files:")
                unreferencedFiles.forEach { echo(it.absolutePath) }
                echo(
                    """
                        
                        Reminder: The paths are relative to the input directory. The input dir should be the top level of the mapping suite
                    """.trimIndent()
                )
            }
        } else {
            echo("All files are referenced.")
        }
    }

    /**
     * Searches the content of a file for references to relative file paths without the extension.
     *
     * @param file The file whose contents will be searched.
     * @param relativePathsWithoutExtension A list of relative file paths without extensions to search for.
     * @return A set of relative paths that were found as references within the file's content.
     */
    private fun searchFileForReferences(file: File, relativePathsWithoutExtension: List<String>): Set<String> {
        val referencedFiles = mutableSetOf<String>()
        try {
            val content = file.readText()

            // Search for relative paths without extensions
            relativePathsWithoutExtension.forEach { relativePath ->
                // Build regex: match the path if it's followed by a path delimiter (/) or end of word
                val regex = Regex(
                    "\\b$relativePath(\\.\\w+)?\\b" // Match with or without file extension
                )

                // If the pattern is found, add the relative path to referenced files
                if (regex.containsMatchIn(content)) {
                    referencedFiles.add(relativePath)
                }
            }
        } catch (e: IOException) {
            // Ignore unreadable files (e.g., binary files or permission issues)
        }
        return referencedFiles
    }

    /**
     * Recursively collects all file paths in a given directory.
     * It walks through the directory and gathers all regular files (ignores directories).
     *
     * @param directory The root directory to start searching from.
     * @return A list of all files found in the directory.
     */
    private fun getAllFiles(directory: File): List<File> = directory.walkTopDown().filter { it.isFile }.toList()
}