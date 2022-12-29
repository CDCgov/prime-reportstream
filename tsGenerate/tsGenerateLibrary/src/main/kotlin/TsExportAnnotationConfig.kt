package gov.cdc.prime.tsGenerateLibrary

import java.io.Serializable

/**
 * Annotation to load
 */
open class TsExportAnnotationConfig(
    val packageName: String,
    val fullyQualifiedName: String = TsExport::class.qualifiedName!!) : Serializable