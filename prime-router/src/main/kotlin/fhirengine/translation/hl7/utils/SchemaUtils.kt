package gov.cdc.prime.router.fhirengine.translation.hl7.utils

import java.util.*

enum class URIScheme {
    FILE,
    CLASSPATH,
    AZURE,
    ;

    override fun toString(): String {
        return name.lowercase(Locale.getDefault())
    }
}

/**
 * helper
 */
fun getURI(scheme: URIScheme, folder: String?, schemaName: String): String {
    var path = if (folder.isNullOrBlank()) schemaName else "$folder/$schemaName"
    if (!path.startsWith("$scheme:/")) {
        path = if (path.startsWith("/")) "$scheme:$path" else "$scheme:/$path"
    }
    if (!path.endsWith(".yml")) {
        path = "$path.yml"
    }
    return path
}