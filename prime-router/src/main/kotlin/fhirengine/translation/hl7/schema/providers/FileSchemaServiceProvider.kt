package gov.cdc.prime.router.fhirengine.translation.hl7.schema.providers

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import java.io.File
import java.io.InputStream
import java.net.URI

class FileSchemaServiceProvider : SchemaServiceProvider {
    override fun getProviderType(): String = "file"

    override fun getInputStream(schemaUri: URI): InputStream {
        val file = File(schemaUri)
        if (!file.canRead()) throw SchemaException("Cannot read ${file.absolutePath}")
        return file.inputStream()
    }
}