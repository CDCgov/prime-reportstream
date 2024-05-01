package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.engine.LookupTableValueSet
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.File
import java.io.InputStream
import java.net.URI

/**
 * Read schema configuration.
 */
object ConfigSchemaReader : Logging {
    /**
     * Read a schema [schemaName] of type [schemaClass] from a file given the root [folder].
     * @property Original The type that this schema expects as input
     * @property Converted The type that this schema will produce as output
     * @property Schema Reference to this schema's type
     * @property SchemaElement The type of the schema elements that make up the schema
     * @return the validated schema
     * @throws Exception if the schema is invalid
     */
    fun <
        Original,
        Converted,
        Schema : ConfigSchema<Original, Converted, Schema, SchemaElement>,
        SchemaElement : ConfigSchemaElement<Original, Converted, SchemaElement, Schema>,
        > fromFile(
        schemaName: String,
        schemaClass: Class<out Schema>,
        blobConnectionInfo: BlobAccess.BlobContainerMetadata,
    ): Schema {
        // Load a schema including any parent schemas.  Note that child schemas are loaded first and the parents last.
        val schemaList = fromUri(URI(schemaName), schemaClass, blobConnectionInfo)

        // Now merge the parent with all the child schemas
        val mergedSchema = mergeSchemas(schemaList)

        if (!mergedSchema.isValid()) {
            throw SchemaException("Invalid schema $schemaName: \n${mergedSchema.errors.joinToString("\n")}")
        }
        return mergedSchema
    }

    /**
     * Read a schema from a [URI] using the scheme to determine how to read the schema
     * @property Original The type that this schema expects as input
     * @property Converted The type that this schema will produce as output
     * @property Schema Reference to this schema's type
     * @property SchemaElement The type of the schema elements that make up the schema
     */
    private fun <
        Original,
        Converted,
        Schema : ConfigSchema<Original, Converted, Schema, SchemaElement>,
        SchemaElement : ConfigSchemaElement<Original, Converted, SchemaElement, Schema>,
        > fromUri(
        schemaUri: URI,
        schemaClass: Class<out Schema>,
        blobConnectionInfo: BlobAccess.BlobContainerMetadata,
    ): List<Schema> {
        val schemaList = mutableListOf(
            readSchemaTreeUri(
                schemaUri, schemaClass = schemaClass, blobConnectionInfo = blobConnectionInfo
            )
        )
        while (!schemaList.last().extends.isNullOrBlank()) {
            // Make sure there are no circular dependencies
            if (schemaList.any {
                    FilenameUtils.getName(schemaUri.path) == FilenameUtils.getName(schemaList.last().extends)
                }
            ) {
                throw SchemaException("Schema circular dependency found while loading schema ${schemaUri.path}")
            }
            schemaList.add(
                readSchemaTreeUri(
                    URI(schemaList.last().extends!!), schemaClass = schemaClass, blobConnectionInfo = blobConnectionInfo
                )
            )
        }

        return schemaList
    }

    /**
     * Merge the parent and child schemas provided in the [schemaList].  Note that [schemaList] MUST be ordered
     * from the lowest child to parent.
     *
     * @property Original The type that this schema expects as input
     * @property Converted The type that this schema will produce as output
     * @property Schema Reference to this schema's type
     * @property SchemaElement The type of the schema elements that make up the schema
     * @return a merged schema
     */
    private fun <
        Original,
        Converted,
        Schema : ConfigSchema<Original, Converted, Schema, SchemaElement>,
        SchemaElement : ConfigSchemaElement<Original, Converted, SchemaElement, Schema>,
        > mergeSchemas(
        schemaList: List<Schema>,
    ): Schema {
        val parentSchema = schemaList.last()
        for (i in (schemaList.size - 2) downTo 0) {
            val childSchema = schemaList[i]
            parentSchema.override(childSchema)
        }
        return parentSchema
    }

    /**
     * Reads a schema using the scheme to determine how to read it.  Currently supports:
     * - azure
     * - classpath
     * - file system
     *
     * @property Original The type that this schema expects as input
     * @property Converted The type that this schema will produce as output
     * @property Schema Reference to this schema's type
     * @property SchemaElement The type of the schema elements that make up the schema
     */
    internal fun <
        Original,
        Converted,
        Schema : ConfigSchema<Original, Converted, Schema, SchemaElement>,
        SchemaElement : ConfigSchemaElement<Original, Converted, SchemaElement, Schema>,
        > readSchemaTreeUri(
        schemaUri: URI,
        ancestry: List<String> = listOf(),
        schemaClass: Class<out Schema>,
        blobConnectionInfo: BlobAccess.BlobContainerMetadata,
    ): Schema {
        val rawSchema = when (schemaUri.scheme) {
            "file" -> {
                val file = File(schemaUri)
                if (!file.canRead()) throw SchemaException("Cannot read ${file.absolutePath}")
                file.inputStream().use { fis ->
                    readOneYamlSchema(fis, schemaClass)
                }
            }
            "classpath" -> {
                (
                    javaClass.classLoader.getResourceAsStream(schemaUri.path.substring(1))
                    ?: throw SchemaException("Cannot read $schemaUri")
                ).use { ips ->
                    readOneYamlSchema(
                        ips,
                        schemaClass
                    )
                }
            }
            "azure" -> {
                // Note: the schema URIs will not include the container name i.e.
                // azure:/hl7_mapping/receivers/STLTs/CA/CA.yml
                val blob = BlobAccess.downloadBlobAsByteArray(
                    "${blobConnectionInfo.getBlobEndpoint()}${schemaUri.path}",
                    blobConnectionInfo
                )
                blob.inputStream().use { bis ->
                    readOneYamlSchema(bis, schemaClass)
                }
            }
            else -> throw SchemaException("Unexpected scheme: ${schemaUri.scheme}")
        }
        rawSchema.name = schemaUri.path

        if (ancestry.contains(rawSchema.name)) {
            throw HL7ConversionException("Circular reference detected for schema ${rawSchema.name}")
        }
        rawSchema.ancestry = ancestry + rawSchema.name!!

        // Process any schema references
        rawSchema.elements.filter { !it.schema.isNullOrBlank() }.forEach { element ->
            element.schemaRef =
                readSchemaTreeUri(URI(element.schema!!), rawSchema.ancestry, schemaClass, blobConnectionInfo)
        }
        return rawSchema
    }

    /**
     * Read a complete schema tree of type [schemaClass] from a file for [schemaName] in [folder].
     * Note this is a recursive function used to walk through all the schemas to load.
     * @property Original The type that this schema expects as input
     * @property Converted The type that this schema will produce as output
     * @property Schema Reference to this schema's type
     * @property SchemaElement The type of the schema elements that make up the schema
     * @return the validated schema
     */
    internal fun <
        Original,
        Converted,
        Schema : ConfigSchema<Original, Converted, Schema, SchemaElement>,
        SchemaElement : ConfigSchemaElement<Original, Converted, SchemaElement, Schema>,
        > readSchemaTreeRelative(
        schemaName: String,
        folder: String? = null,
        ancestry: List<String> = listOf(),
        schemaClass: Class<out Schema>,
    ): Schema {
        val file = File(folder, "$schemaName.yml")
        if (!file.canRead()) throw SchemaException("Cannot read ${file.absolutePath}")
        val rawSchema = try {
            readOneYamlSchema(file.inputStream(), schemaClass)
        } catch (e: Exception) {
            val msg = "Error while reading schema configuration from file ${file.absolutePath}"
            logger.error(msg, e)
            throw SchemaException(msg, e)
        }

        // set schema name to match the filename
        rawSchema.name = schemaName

        if (ancestry.contains(rawSchema.name)) {
            throw HL7ConversionException("Circular reference detected for schema ${rawSchema.name}")
        }
        rawSchema.ancestry = ancestry + rawSchema.name!!

        // Process any schema references
        val rootFolder = file.parent
        rawSchema.elements.filter { !it.schema.isNullOrBlank() }.forEach { element ->
            element.schemaRef = readSchemaTreeRelative(element.schema!!, rootFolder, rawSchema.ancestry, schemaClass)
        }
        return rawSchema
    }

    /**
     * Read one YAML formatted schema of type [schemaClass] from the given [inputStream].
     * @property Original The type that this schema expects as input
     * @property Converted The type that this schema will produce as output
     * @property Schema Reference to this schema's type
     * @property SchemaElement The type of the schema elements that make up the schema
     * @return the schema
     */
    internal fun <
        Original,
        Converted,
        Schema : ConfigSchema<Original, Converted, Schema, SchemaElement>,
        SchemaElement : ConfigSchemaElement<Original, Converted, SchemaElement, Schema>,
        > readOneYamlSchema(
        inputStream: InputStream,
        schemaClass: Class<out Schema>,
    ): Schema {
        val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()
        mapper.registerSubtypes(LookupTableValueSet::class.java)
        val rawSchema = mapper.readValue(inputStream, schemaClass)
        // Are there any null elements?  This may mean some unknown array value in the YAML
        if (rawSchema.elements.any { false }) {
            throw SchemaException("Invalid empty element found in schema. Check that all array items are elements.")
        }
        return rawSchema
    }
}