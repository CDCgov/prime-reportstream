package gov.cdc.prime.router.fhirengine.translation.hl7.utils

/**
 * helper
 */
fun getURI(folder: String?, schemaName: String): String {
    var path = if (folder.isNullOrBlank()) schemaName else "$folder/$schemaName"
    if (!path.startsWith("classpath:/")) {
        path = if (path.startsWith("/")) "classpath:$path" else "classpath:/$path"
    }
    if (!path.endsWith(".yml")) {
        path = "$path.yml"
    }
    return path
}