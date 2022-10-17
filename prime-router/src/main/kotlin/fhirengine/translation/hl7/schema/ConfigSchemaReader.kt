package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import gov.cdc.prime.router.fhirengine.translation.hl7.HL7ConversionException
import gov.cdc.prime.router.fhirengine.translation.hl7.SchemaException
import org.apache.commons.io.FilenameUtils
import org.apache.logging.log4j.kotlin.Logging
import java.io.File
import java.io.InputStream

/**
 * Read schema configuration.
 */
object ConfigSchemaReader : Logging {
    /**
     * Read a schema [schemaName] from a file given the root [folder].
     * @return the validated schema
     * @throws Exception if the schema is invalid
     */
    fun fromFile(schemaName: String, folder: String? = null): ConfigSchema {
        // Load a schema including any parent schemas.  Note that child schemas are loaded first and the parents last.
        val schemaList = mutableListOf<ConfigSchema>()
        schemaList.add(readSchemaTreeFromFile(schemaName, folder))
        while (!schemaList.last().extends.isNullOrBlank()) {
            // Make sure there are no circular dependencies
            if (schemaList.any { FilenameUtils.getName(schemaName) == FilenameUtils.getName(schemaList.last().extends) }
            ) {
                throw SchemaException("Schema circular dependency found while loading schema $schemaName")
            }
            val depSchemaFolder = "$folder/${FilenameUtils.getPath(schemaList.last().extends)}"
            val depSchemaName = FilenameUtils.getName(schemaList.last().extends)
            schemaList.add(readSchemaTreeFromFile(depSchemaName, depSchemaFolder))
        }

        // Now merge the parent with all the child schemas
        val mergedSchema = mergeSchemas(schemaList)

        if (!mergedSchema.isValid()) {
            throw SchemaException("Invalid schema $schemaName: \n${mergedSchema.errors.joinToString("\n")}")
        }
        return mergedSchema
    }

    /**
     * Merge the parent and child schemas provided in the [schemaList].  Note that [schemaList] MUST be ordered
     * from the lowest child to parent.
     * @return a merged schema
     */
    private fun mergeSchemas(schemaList: List<ConfigSchema>): ConfigSchema {
        val parentSchema = schemaList.last()
        for (i in (schemaList.size - 2) downTo 0) {
            parentSchema.merge(schemaList[i])
        }
        return parentSchema
    }

    /**
     * Read a complete schema tree from a file for [schemaName] in [folder].
     * Note this is a recursive function used to walk through all the schemas to load.
     * @return the validated schema
     */
    internal fun readSchemaTreeFromFile(
        schemaName: String,
        folder: String? = null,
        ancestry: List<String> = listOf()
    ): ConfigSchema {
        val file = File(folder, "$schemaName.yml")
        if (!file.canRead()) throw Exception("Cannot read ${file.absolutePath}")
        val rawSchema = try {
            readOneYamlSchema(file.inputStream())
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
            element.schemaRef = readSchemaTreeFromFile(element.schema!!, rootFolder, rawSchema.ancestry)
        }
        return rawSchema
    }

    /**
     * Read one YAML formatted schema from the given [inputStream].
     * @return the schema
     */
    internal fun readOneYamlSchema(inputStream: InputStream): ConfigSchema {
        val mapper = ObjectMapper(YAMLFactory())
        val rawSchema = mapper.readValue(inputStream, ConfigSchema::class.java)
        // Are there any null elements?  This may mean some unknown array value in the YAML
        if (rawSchema.elements.any { false }) {
            throw SchemaException("Invalid empty element found in schema. Check that all array items are elements.")
        }
        return rawSchema
    }
}