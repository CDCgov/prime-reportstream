package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import gov.cdc.prime.router.azure.BlobAccess
import gov.cdc.prime.router.fhirengine.engine.LookupTableValueSet
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.converter.ConverterSchema
import gov.cdc.prime.router.fhirengine.translation.hl7.schema.fhirTransform.FhirTransformSchema
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
     * @return the validated schema
     * @throws Exception if the schema is invalid
     */
    fun fromFile(
        schemaName: String,
        folder: String? = null,
        schemaClass: Class<out ConfigSchema<out ConfigSchemaElement>>
    ): ConfigSchema<*> {
        // Load a schema including any parent schemas.  Note that child schemas are loaded first and the parents last.
        val schemaList = when (URI(schemaName).scheme) {
            null -> fromRelative(schemaName, folder, schemaClass)
            else -> fromUri(URI(schemaName), schemaClass)
        }

        // Now merge the parent with all the child schemas
        val mergedSchema = mergeSchemas(schemaList)

        if (!mergedSchema.isValid()) {
            throw SchemaException("Invalid schema $schemaName: \n${mergedSchema.errors.joinToString("\n")}")
        }
        return mergedSchema
    }

    /**
     * Reads a schema from the file directory via relative pathing.  This is the deprecated way of reading schemas
     * and continues to exist while the transition is executed
     */
    private fun fromRelative(
        schemaName: String,
        folder: String? = null,
        schemaClass: Class<out ConfigSchema<out ConfigSchemaElement>>
    ): List<ConfigSchema<*>> {
        val schemaList = mutableListOf<ConfigSchema<*>>()
        schemaList.add(readSchemaTreeRelative(schemaName, folder, schemaClass = schemaClass))
        while (!schemaList.last().extends.isNullOrBlank()) {
            // Make sure there are no circular dependencies
            if (schemaList.any { FilenameUtils.getName(schemaName) == FilenameUtils.getName(schemaList.last().extends) }
            ) {
                throw SchemaException("Schema circular dependency found while loading schema $schemaName")
            }
            val depSchemaFolder = "$folder/${FilenameUtils.getPath(schemaList.last().extends)}"
            val depSchemaName = FilenameUtils.getName(schemaList.last().extends)
            schemaList.add(readSchemaTreeRelative(depSchemaName, depSchemaFolder, schemaClass = schemaClass))
        }
        return schemaList
    }

    /**
     * Read a schema from a [URI] using the scheme to determine how to read the schema
     *
     */
    private fun fromUri(
        schemaUri: URI,
        schemaClass: Class<out ConfigSchema<out ConfigSchemaElement>>
    ): List<ConfigSchema<*>> {
        val schemaList = mutableListOf<ConfigSchema<*>>()
        schemaList.add(readSchemaTreeUri(schemaUri, schemaClass = schemaClass))
        while (!schemaList.last().extends.isNullOrBlank()) {
            // Make sure there are no circular dependencies
            if (schemaList.any {
                FilenameUtils.getName(schemaUri.path) == FilenameUtils.getName(schemaList.last().extends)
            }
            ) {
                throw SchemaException("Schema circular dependency found while loading schema ${schemaUri.path}")
            }
            schemaList.add(readSchemaTreeUri(URI(schemaList.last().extends!!), schemaClass = schemaClass))
        }

        return schemaList
    }

    /**
     * Merge the parent and child schemas provided in the [schemaList].  Note that [schemaList] MUST be ordered
     * from the lowest child to parent.
     * @return a merged schema
     */
    private fun mergeSchemas(schemaList: List<ConfigSchema<*>>): ConfigSchema<*> {
        val parentSchema = schemaList.last()
        for (i in (schemaList.size - 2) downTo 0) {
            val childSchema = schemaList[i]
            when {
                // Need to smart cast so the compiler knows which merge is being called
                parentSchema is ConverterSchema && childSchema is ConverterSchema ->
                    parentSchema.override(childSchema)
                parentSchema is FhirTransformSchema && childSchema is FhirTransformSchema ->
                    parentSchema.override(childSchema)
                else ->
                    throw SchemaException(
                        "Parent schema ${parentSchema.name} and child schema ${childSchema.name} of incompatible types"
                    )
            }
        }
        return parentSchema
    }

    /**
     * Reads a schema using the scheme to determine how to read it.  Currently supports:
     * - azure
     * - classpath
     * - file system
     */
    internal fun readSchemaTreeUri(
        schemaUri: URI,
        ancestry: List<String> = listOf(),
        schemaClass: Class<out ConfigSchema<out ConfigSchemaElement>> = ConverterSchema::class.java
    ): ConfigSchema<*> {
        val rawSchema = when (schemaUri.scheme) {
            "file" -> {
                val file = File(schemaUri)
                if (!file.canRead()) throw SchemaException("Cannot read ${file.absolutePath}")
                readOneYamlSchema(file.inputStream(), schemaClass)
            }
            "classpath" -> {
                val input = javaClass.classLoader.getResourceAsStream(schemaUri.path.substring(1))
                    ?: throw SchemaException("Cannot read $schemaUri")
                readOneYamlSchema(input, schemaClass)
            }
            "azure" -> {
                // TODO: #10487 will add a new function to download schemas, so this is just a temporary placeholder
                val blob = BlobAccess.downloadBlob(schemaUri.toString())
                readOneYamlSchema(blob.inputStream(), schemaClass)
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
                readSchemaTreeUri(URI(element.schema!!), rawSchema.ancestry, schemaClass)
        }
        return rawSchema
    }

    /**
     * Read a complete schema tree of type [schemaClass] from a file for [schemaName] in [folder].
     * Note this is a recursive function used to walk through all the schemas to load.
     * @return the validated schema
     */
    internal fun readSchemaTreeRelative(
        schemaName: String,
        folder: String? = null,
        ancestry: List<String> = listOf(),
        schemaClass: Class<out ConfigSchema<out ConfigSchemaElement>> = ConverterSchema::class.java
    ): ConfigSchema<*> {
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
            element.schemaRef =
                readSchemaTreeRelative(element.schema!!, rootFolder, rawSchema.ancestry, schemaClass)
        }
        return rawSchema
    }

    /**
     * Read one YAML formatted schema of type [schemaClass] from the given [inputStream].
     * @return the schema
     */
    internal fun readOneYamlSchema(
        inputStream: InputStream,
        schemaClass: Class<out ConfigSchema<out ConfigSchemaElement>> = ConverterSchema::class.java
    ): ConfigSchema<*> {
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