package gov.cdc.prime.router.history.db

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isNotNull
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.OrganizationAPI
import gov.cdc.prime.router.azure.ReceiverAPI
import gov.cdc.prime.router.azure.SettingsFacade
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.common.ReportNodeBuilder
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.metadata.LookupTable
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class DeliveryHistoryDatabaseAccessTests {

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @Test
    fun `test it should return the correct filename`() {
        val submittedReport = ReportNodeBuilder.reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.batch)
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                                reportGraphNode {
                                    action(TaskAction.send)
                                    transportResult("Success")
                                    receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                                }
                            }
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val metadata = UnitTestUtils.simpleMetadata
        val settings = SettingsFacade(
            metadata, ReportStreamTestDatabaseContainer.testDatabaseAccess
        )
        settings.putSetting(
            UniversalPipelineTestUtils.universalPipelineOrganization.name,
            JacksonMapperUtilities
                .defaultMapper.writeValueAsString(
                    UniversalPipelineTestUtils.universalPipelineOrganization
                ),
            mockk<AuthenticatedClaims>(relaxed = true),
            OrganizationAPI::class.java
        )
        settings.putSetting(
            UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0].name,
            JacksonMapperUtilities.defaultMapper.writeValueAsString(
                UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0]
            ),
            mockk<AuthenticatedClaims>(relaxed = true),
            ReceiverAPI::class.java,
            UniversalPipelineTestUtils.universalPipelineOrganization.name
        )
        mockkObject(BaseEngine)
        every { BaseEngine.settingsProviderSingleton } returns settings

        metadata.lookupTableStore += mapOf(
            "observation-mapping" to LookupTable("observation-mapping", emptyList())
        )
        val workflowEngine = WorkflowEngine
            .Builder()
            .metadata(metadata)
            .settingsProvider(settings)
            .databaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess)
            .build()

        val historyDatabaseAccess =
            DeliveryHistoryDatabaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess, workflowEngine)

        val deliveries = historyDatabaseAccess.getDeliveries(
            DeliveryHistoryApiSearch(
                emptyList(),
                DeliveryHistoryTable.DELIVERY_HISTORY.CREATED_AT
            ),
            UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0].organizationName,
            UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0].name,
            null,
            null,
            null
        )

        val fileName = submittedReport.children[0].children[0].children[0].children[0].node.externalName

        assertThat(deliveries.results).hasSize(1)
        assertThat(deliveries.results[0].fileName).isNotNull().all {
            equals(fileName)
        }
    }
}