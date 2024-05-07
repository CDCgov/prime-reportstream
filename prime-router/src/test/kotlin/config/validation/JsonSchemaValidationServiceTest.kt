package gov.cdc.prime.router.config.validation

import assertk.assertThat
import assertk.assertions.isEmpty
import java.io.File
import kotlin.test.Test

class JsonSchemaValidationServiceTest {

    private val service = JsonSchemaValidationService()

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
                  jurisdictionalFilter: [ "(%performerState.exists() and %performerState = 'UT') or (%patientState.exists() and %patientState = 'UT')" ]
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

        val errors = service.validateYAMLStructure(
            ConfigurationType.ORGANIZATIONS,
            yaml
        )

        assertThat(errors).isEmpty()
    }

    @Test
    fun organizations() {
        val errors = service.validateYAMLStructure(
            ConfigurationType.ORGANIZATIONS,
            File("settings/organizations.yml")
        )

        assertThat(errors).isEmpty()
    }
}