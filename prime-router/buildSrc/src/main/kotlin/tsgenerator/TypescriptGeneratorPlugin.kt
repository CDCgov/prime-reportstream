package gov.cdc.prime.router.tsgenerator

import me.ntrrgc.tsGenerator.VoidType
import org.gradle.api.Plugin
import org.gradle.api.Project

open class TypeScriptGeneratorPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        project.extensions.create("typescriptGenerator", TypeScriptGeneratorExtension::class.java)

        project.afterEvaluate {

            val config = project.extensions.findByType(TypeScriptGeneratorExtension::class.java)!!

            project.tasks.create("generateTypescriptDefinitions", TypeScriptGeneratorTask::class.java).apply {

                description = "Generates Typescript definitions from Kotlin classes."

                outputPath = config.outputPath ?: throw IncompletePluginConfigurationException(
                    "outputPath"
                )

                classPath = config.classPath ?: throw IncompletePluginConfigurationException(
                    "classPath"
                )

                packageName = config.packageName ?: throw IncompletePluginConfigurationException(
                    "packageName"
                )

                typeMappings = config.typeMappings

                imports = config.imports

                intTypeName = config.intTypeName

                voidType = when (config.voidType) {
                    "UNDEFINED" -> VoidType.UNDEFINED
                    "NULL" -> VoidType.NULL
                    else -> throw InvalidPluginConfigurationException(
                        "voidType", "'NULL', or 'UNDEFINED'"
                    )
                }
            }

        }

    }

    class IncompletePluginConfigurationException(missing: String) : IllegalArgumentException(
        "Incomplete TypescriptGenerator plugin configuration: $missing is missing"
    )

    class InvalidPluginConfigurationException(input: String, expected: String) : IllegalArgumentException(
        "Incomplete TypescriptGenerator plugin configuration: $input is invalid. Expected: $expected."
    )

}