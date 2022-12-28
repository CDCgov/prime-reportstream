package gov.cdc.prime.tsGeneratePlugin

import me.ntrrgc.tsGenerator.VoidType
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import java.io.Serializable
import java.nio.file.Path

/**
 * Annotation to load
 */
class TsExportAnnotation(val fullyQualifiedName: String, val packageName: String) : Serializable

open class TypescriptGeneratorExtension(project: Project) {
    /**
     * The annotation to look for to automatically select classes in packages
     */
    val annotation: Property<TsExportAnnotation> = project.objects.property(TsExportAnnotation::class.java)

    /**
     * Manually select classes. Use their fully qualified names.
     */
    val manualClasses: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * Where to write output typescript declaration file
     */
    val outputPath: Property<Path?> = project.objects.property(Path::class.java)

    /**
     * Usually layout.files(project.sourceSets.main.get().runtimeClasspath)
     */
    val classPath: Property<FileCollection?> = project.objects.property(FileCollection::class.java)

    /**
     * [TypescriptGenerator][me.ntrrgc.tsGenerator.TypeScriptGenerator] param mappings
     */
    val typeMappings: MapProperty<String, String> = project.objects.mapProperty(String::class.java, String::class.java)
        .convention(mapOf("java.lang.Void" to "undefined"))
    val imports: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * [TypescriptGenerator][me.ntrrgc.tsGenerator.TypeScriptGenerator] param intTypeName
     */
    val intTypeName: Property<String> = project.objects.property(String::class.java).convention("number")

    /**
     * What typescript type to use for void
     * @see me.ntrrgc.tsGenerator.VoidType
     */
    val voidType: Property<VoidType> = project.objects.property(VoidType::class.java).convention(VoidType.UNDEFINED)
}
