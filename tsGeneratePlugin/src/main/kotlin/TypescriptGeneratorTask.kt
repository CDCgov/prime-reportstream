package gov.cdc.prime.tsGeneratePlugin

import io.github.classgraph.ClassGraph
import me.ntrrgc.tsGenerator.TypeScriptGenerator
import me.ntrrgc.tsGenerator.VoidType
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.net.URLClassLoader
import java.nio.file.Path
import org.gradle.api.provider.*

@Suppress("UnstableApiUsage")
abstract class TypescriptGeneratorTask : DefaultTask() {
    @get:Input
    abstract val annotation: Property<TsExportAnnotation>

    @get:Input
    abstract val manualClasses: ListProperty<String>

    @get:CompileClasspath
    abstract val classPath: Property<FileCollection>

    @get:Input
    abstract val typeMappings: MapProperty<String, String>

    @get:Input
    abstract val imports: ListProperty<String>

    @get:Input
    abstract val intTypeName: Property<String>

    @get:Input
    abstract val voidType: Property<VoidType>

    @get:OutputFile
    abstract val outputPath: Property<Path>

    init {
        description = "Generates Typescript definitions from Kotlin classes."
    }

    fun useExtension(ext: TypescriptGeneratorExtension) {
        annotation.set(ext.annotation)
        manualClasses.set(ext.manualClasses)
        outputPath.set(ext.outputPath)
        classPath.set(ext.classPath)
        typeMappings.set(ext.typeMappings)
        imports.set(ext.imports)
        intTypeName.set(ext.intTypeName)
        voidType.set(ext.voidType)
    }

    @TaskAction
    fun generateTypescriptDefinitions() {
        if (!outputPath.isPresent) {
            throw IncompletePluginConfigurationException("outputPath")
        }
        if (!classPath.isPresent) {
            throw IncompletePluginConfigurationException("classPath")
        }
        if ( (!manualClasses.isPresent || manualClasses.get().isEmpty()) &&
            !annotation.isPresent) {
            throw IncompletePluginConfigurationException("manualClasses or tsExportAnnotation")
        }

        val urls = classPath.get().files.map { it.toURI().toURL() }
        val classLoader = URLClassLoader(urls.toTypedArray())
        val classGraph = ClassGraph().addClassLoader(classLoader).enableAllInfo()
            .acceptPackages(annotation.get().packageName).scan()

        val klasses = classGraph.allClasses.filter { klass ->
            !klass.hasAnnotation(JsonAnnotations.JSONIGNORETYPE.fullName) &&
            (annotation.isPresent && klass.hasAnnotation(annotation.get().fullyQualifiedName)) ||
                (manualClasses.isPresent && manualClasses.get().any { it == klass.name })
        }.map{ it.loadClass().kotlin }
        logger.info("Found ${klasses.size} exportable class(es)")

        val mappings =
            typeMappings.get().entries.associate { (className, typescriptName) ->
                Class.forName(
                    className,
                    true,
                    classLoader
                ).kotlin to typescriptName
            }

        val generator = TypeScriptGenerator(
            rootClasses = klasses,
            mappings = mappings,
            intTypeName = intTypeName.get(),
            voidType = voidType.get(),
            classTransformers = listOf(TypescriptClassTransformer())
        )

        val result =
            if (imports.get().isEmpty())
                generator.definitionsText
            else
                imports.get().joinToString("\n") +
                    "\n\n" +
                    generator.definitionsText

        outputPath.get().toFile().writeText(result)
        logger.info(outputPath.get().toString())
    }
}


class IncompletePluginConfigurationException(missing: String) : IllegalArgumentException(
    "Incomplete TypescriptGenerator plugin configuration: $missing is missing"
)

class InvalidPluginConfigurationException(input: String, expected: String) : IllegalArgumentException(
    "Incomplete TypescriptGenerator plugin configuration: $input is invalid. Expected: $expected."
)