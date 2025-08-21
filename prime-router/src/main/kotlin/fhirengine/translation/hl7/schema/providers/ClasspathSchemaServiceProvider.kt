package gov.cdc.prime.router.fhirengine.translation.hl7.schema.providers

import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import java.io.InputStream
import java.net.URI

class ClasspathSchemaServiceProvider : SchemaServiceProvider {
    override fun getProviderType(): String = "classpath"

    override fun getInputStream(schemaUri: URI): InputStream =
        javaClass.classLoader.getResourceAsStream(schemaUri.path.substring(1))
            ?: throw SchemaException("Cannot read $schemaUri")
}