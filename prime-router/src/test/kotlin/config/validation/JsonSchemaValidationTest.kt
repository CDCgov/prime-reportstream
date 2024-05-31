package gov.cdc.prime.router.config.validation

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.first
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import com.networknt.schema.JsonSchema
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import com.networknt.schema.ValidationMessage
import gov.cdc.prime.router.common.JacksonMapperUtilities
import org.apache.logging.log4j.kotlin.Logging
import java.io.File
import kotlin.test.Test

class JsonSchemaValidationTest : Logging {

    private val schema: JsonSchema = run {
        val rawSchema = File("./metadata/json_schema/testing/person.json").inputStream()
        val factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012)
        factory.getSchema(rawSchema)
    }

    @Test
    fun `Valid yaml file`() {
        val results = validateYaml("yaml_validation/valid-person.yml")

        assertThat(results).isEmpty()
    }

    @Test
    fun `Invalid yaml file with missing property`() {
        val results = validateYaml("yaml_validation/missing-property.yml")

        assertThat(results).hasSize(1)
        assertThat(results)
            .first()
            .transform { it.message }
            .contains("age")
    }

    @Test
    fun `Invalid yaml file with extra property`() {
        val results = validateYaml("yaml_validation/extra-property.yml")

        assertThat(results).hasSize(1)
        assertThat(results)
            .first()
            .transform { it.message }
            .contains("extra")
    }

    private fun validateYaml(path: String): Set<ValidationMessage> {
        val rawYml = javaClass.classLoader.getResourceAsStream(path)
        val yaml = JacksonMapperUtilities.yamlMapper.readTree(rawYml)
        val results = schema.validate(yaml)

        return results
    }
}