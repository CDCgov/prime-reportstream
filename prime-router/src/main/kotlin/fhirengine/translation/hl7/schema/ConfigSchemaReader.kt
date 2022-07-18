package gov.cdc.prime.router.fhirengine.translation.hl7.schema

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.JacksonYAMLParseException
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
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
        val rawSchema = readSchemaTreeFromFile(schemaName, folder)

        if (!rawSchema.isValid()) {
            throw Exception("Invalid schema $schemaName: \n${rawSchema.errors.joinToString("\n")}")
        }
        return rawSchema
    }

    /**
     * Read a complete schema tree from a file for [schemaName] in [folder].
     * @return the validated schema
     */
    internal fun readSchemaTreeFromFile(schemaName: String, folder: String? = null): ConfigSchema {
        val file = File(folder, "$schemaName.yml")
        if (!file.canRead()) throw Exception("Cannot read ${file.absolutePath}")
        val rawSchema = try {
            readOneYamlSchema(file.inputStream())
        } catch (e: JacksonYAMLParseException) {
            logger.error("Error while reading schema configuration from file ${file.absolutePath}", e)
            throw e
        } catch (e: JsonMappingException) {
            logger.error("Error while reading schema configuration from file ${file.absolutePath}", e)
            throw e
        }

        // Process any schema references
        val rootFolder = file.parent
        rawSchema.elements.filter { !it.schema.isNullOrBlank() }.forEach { element ->
            element.schemaRef = readSchemaTreeFromFile(element.schema!!, rootFolder)
        }
        return rawSchema
    }

    /**
     * Read one YAML formatted schema from the given [inputStream].
     * @return the schema
     */
    internal fun readOneYamlSchema(inputStream: InputStream): ConfigSchema {
        val mapper = ObjectMapper(YAMLFactory())
        return mapper.readValue(inputStream, ConfigSchema::class.java)
    }
}