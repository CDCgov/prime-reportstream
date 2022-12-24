
import me.ntrrgc.tsGenerator.VoidType
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

interface TypeScriptGeneratorExtension {
    /**
     * The annotation to look for to automatically select classes in packages
     */
    abstract val annotation: Property<TsExportAnnotation>

    /**
     * Manually select classes. Use either their fully qualified names or use globs (*).
     */
    abstract val manualClasses: ListProperty<String>

    /**
     * Where to write output typescript declaration file
     */
    abstract val outputPath: Property<Path?>

    /**
     * Usually layout.files(project.sourceSets.main.get().runtimeClasspath)
     */
    abstract val classPath: Property<FileCollection?>

    /**
     * [TypescriptGenerator][me.ntrrgc.tsGenerator.TypeScriptGenerator] param mappings
     */
    abstract val typeMappings: MapProperty<String, String>
    abstract val imports: ListProperty<String>

    /**
     * [TypescriptGenerator][me.ntrrgc.tsGenerator.TypeScriptGenerator] param intTypeName
     */
    abstract val intTypeName: Property<String>

    /**
     * What typescript type to use for void
     * @see me.ntrrgc.tsGenerator.VoidType
     */
    abstract val voidType: Property<VoidType>
}
