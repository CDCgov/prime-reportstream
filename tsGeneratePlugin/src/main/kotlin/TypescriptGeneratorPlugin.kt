package gov.cdc.prime.tsGeneratePlugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.register

abstract class TypescriptGeneratorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val typescriptGenerator = project.extensions.create<TypescriptGeneratorExtension>("typescriptGenerator")

        project.tasks.register<TypescriptGeneratorTask>("generateTypescriptDefinitions") {
            useExtension(typescriptGenerator)
        }
    }
}