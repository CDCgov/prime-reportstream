package gov.cdc.prime.router.tsgenerator

import com.google.common.reflect.ClassPath
import me.ntrrgc.tsGenerator.TypeScriptGenerator
import me.ntrrgc.tsGenerator.VoidType
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CompileClasspath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.reflect.full.hasAnnotation


@Suppress("UnstableApiUsage")
open class TypeScriptGeneratorTask : DefaultTask() {

    @OutputFile
    lateinit var outputPath: Path

    @CompileClasspath
    lateinit var classPath: FileCollection

    @Input
    lateinit var packageName: String

    @Input
    lateinit var typeMappings: Map<String, String>

    @Input
    lateinit var imports: List<String>

    @Input
    lateinit var intTypeName: String

    @Input
    lateinit var voidType: VoidType

    @TaskAction
    fun generateTypescriptDefinitions() {

        val urls = classPath.files.map { it.toURI().toURL() }

        val classLoader = URLClassLoader(urls.toTypedArray())

        val javaClasses = ClassPath.from(classLoader).getTopLevelClassesRecursive(packageName)

        val kotlinClasses = javaClasses
            .map { it.load() }
            .map { it.kotlin }

        val filteredKotlinClasses = kotlinClasses.filter { kotlinClass ->
            kotlinClass.hasAnnotation<TsExport>()
        }

        val mappings =
            typeMappings.entries.associate { (className, typescriptName) ->
                Class.forName(
                    className,
                    true,
                    classLoader
                ).kotlin to typescriptName
            }

        val generator = TypeScriptGenerator(
            rootClasses = filteredKotlinClasses,
            mappings = mappings,
            intTypeName = intTypeName,
            voidType = voidType
        )

        val result =
            if (imports.isEmpty())
                generator.definitionsText
            else
                imports.joinToString("\n") +
                    "\n\n" +
                    generator.definitionsText

        outputPath.toFile().writeText(result)

        println(outputPath)

    }
}