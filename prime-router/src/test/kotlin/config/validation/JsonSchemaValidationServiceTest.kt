package gov.cdc.prime.router.config.validation

import assertk.assertThat
import assertk.assertions.isEmpty
import java.io.File
import kotlin.test.Test

class JsonSchemaValidationServiceTest {

    private val service = JsonSchemaValidationService()

    @Test
    fun organizations() {
        val errors = service.validateYAMLFileStructure(
            ConfigurationType.ORGANIZATIONS,
            File("settings/organizations.yml")
        )

        assertThat(errors).isEmpty()
    }
}