package gov.cdc.prime.router.config.validation

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import gov.cdc.prime.router.DeepOrganization
import java.io.File
import kotlin.test.Test

class JsonSchemaValidationServiceTest {

    private val service: JsonSchemaValidationService = JsonSchemaValidationServiceImpl()

    @Test
    fun `validate yaml string`() {
        val yaml = """
            - name: unit-test-phd
              description: unit testing public health department
              jurisdiction: STATE
              filters:
                - topic: covid-19
                  jurisdictionalFilter: [ "orEquals(ordering_facility_state, UT, patient_state, UT)" ]
                - topic: full-elr
                  jurisdictionalFilter: [ "(Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state.exists() and Bundle.entry.resource.ofType(ServiceRequest)[0].requester.resolve().organization.resolve().address.state = 'UT') or (Bundle.entry.resource.ofType(Patient).address.state.exists() and Bundle.entry.resource.ofType(Patient).address.state = 'UT')" ]
              stateCode: UT
              receivers:
                - name: elr
                  organizationName: unit-test-phd
                  topic: covid-19
                  customerStatus: active
                  jurisdictionalFilter:
                    - orEquals(patient_state, UT, ordering_facility_state, UT)
                  translation:
                    type: HL7
                    useBatchHeaders: true
                  timing:
                    operation: MERGE
                    numberPerDay: 1440
                    initialTime: 00:00
                    timeZone: EASTERN
                  transport:
                    type: SFTP
                    host: sftp
                    port: 22
                    filePath: ./upload
                    credentialName: DEFAULT-SFTP
        """.trimIndent()

        val result = service.validateYAMLStructure(
            ConfigurationType.Organizations,
            yaml
        )

        assertThat(result).isInstanceOf<ConfigurationValidationSuccess<List<DeepOrganization>>>()
    }

    @Test
    fun `error parsing yaml`() {
        val badYaml = """
            This is not our 
            organizations yaml
            at all!"
        """.trimIndent()

        val result = service.validateYAMLStructure(
            ConfigurationType.Organizations,
            badYaml
        )

        assertThat(result)
            .isInstanceOf<ConfigurationValidationFailure<List<DeepOrganization>>>()
            .transform { it.errors }
            .isNotEmpty()
    }

    @Test
    fun organizations() {
        val result = service.validateYAMLStructure(
            ConfigurationType.Organizations,
            File("settings/organizations.yml")
        )

        assertThat(result).isInstanceOf<ConfigurationValidationSuccess<List<DeepOrganization>>>()
    }
}