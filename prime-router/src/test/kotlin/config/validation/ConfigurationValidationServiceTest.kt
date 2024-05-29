package gov.cdc.prime.router.config.validation

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import gov.cdc.prime.router.DeepOrganization
import gov.cdc.prime.router.Organization
import io.mockk.every
import io.mockk.mockk
import org.apache.logging.log4j.kotlin.Logging
import java.io.File
import java.io.InputStream
import kotlin.test.Test

class ConfigurationValidationServiceTest : Logging {

    private inner class Fixture {
        val service: ConfigurationValidationService =
            ConfigurationValidationServiceImpl()

        val mockedJsonSchemaValidationService = mockk<JsonSchemaValidationService>()
        val mockedConfigurationValueValidationService = mockk<ConfigurationValueValidationService>()
        val mockedService: ConfigurationValidationService = ConfigurationValidationServiceImpl(
            mockedJsonSchemaValidationService,
            mockedConfigurationValueValidationService
        )

        val orgs =
            listOf(
                DeepOrganization(
                    name = "unit test",
                    description = "description",
                    jurisdiction = Organization.Jurisdiction.FEDERAL
                )
            )
    }

    @Test
    fun `successful validation`() {
        val f = Fixture()

        every {
            f.mockedJsonSchemaValidationService
                .validateYAMLStructure(ConfigurationType.Organizations, any<InputStream>())
        } returns ConfigurationValidationSuccess(f.orgs)
        every {
            f.mockedConfigurationValueValidationService.validate(ConfigurationType.Organizations, any())
        } returns ConfigurationValidationSuccess(f.orgs)

        val result = f.mockedService.validateYAML(ConfigurationType.Organizations, "valid YAML")

        assertThat(result)
            .isInstanceOf<ConfigurationValidationSuccess<List<DeepOrganization>>>()
            .transform { it.parsed }
            .isEqualTo(f.orgs)
    }

    @Test
    fun `failed validation on JSON schema`() {
        val f = Fixture()

        every {
            f.mockedJsonSchemaValidationService
                .validateYAMLStructure(ConfigurationType.Organizations, any<InputStream>())
        } returns ConfigurationValidationFailure(listOf("schema validation failed"))

        val result = f.mockedService.validateYAML(ConfigurationType.Organizations, "invalid YAML")

        assertThat(result)
            .isInstanceOf<ConfigurationValidationFailure<List<DeepOrganization>>>()
            .transform { it.errors.first() }
            .isEqualTo("schema validation failed")
    }

    @Test
    fun `failed validation on value validation`() {
        val f = Fixture()

        every {
            f.mockedJsonSchemaValidationService
                .validateYAMLStructure(ConfigurationType.Organizations, any<InputStream>())
        } returns ConfigurationValidationSuccess(f.orgs)
        every {
            f.mockedConfigurationValueValidationService.validate(ConfigurationType.Organizations, any())
        } returns ConfigurationValidationFailure(listOf("Value validation failed"))

        val result = f.mockedService.validateYAML(ConfigurationType.Organizations, "invalid YAML")

        assertThat(result)
            .isInstanceOf<ConfigurationValidationFailure<List<DeepOrganization>>>()
            .transform { it.errors.first() }
            .isEqualTo("Value validation failed")
    }

    @Test
    fun organizations() {
        val f = Fixture()

        val result = f.service.validateYAML(
            ConfigurationType.Organizations,
            File("settings/organizations.yml")
        )

        // print out issues to be helpful
        if (result is ConfigurationValidationFailure) {
            logger.warn("Error validating organizations.yml")
            result.errors.forEach { logger.warn(it) }
            result.cause?.let { logger.warn(it) }
        }

        assertThat(result).isInstanceOf<ConfigurationValidationSuccess<List<Organization>>>()
    }
}