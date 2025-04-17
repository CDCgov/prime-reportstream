package gov.cdc.prime.router.azure.observability.event

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.common.ReportNodeBuilder.Companion.reportGraph
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.version.Version
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test as KotlinTest

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class ReportEventServiceTest {

    @Test
    fun `test getReportEventData`() {
        val submittedReport = reportGraph {
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
                            action(TaskAction.receiver_filter)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.translate)
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                                reportGraphNode {
                                    action(TaskAction.send)
                                    transportResult("Success")
                                    receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                                }
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.receiver_filter)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            reportGraphNode {
                                action(TaskAction.translate)
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                                reportGraphNode {
                                    action(TaskAction.send)
                                    transportResult("Success")
                                    receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                                }
                            }
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val translateNode = submittedReport.children[0].children[0].children[0].children[0]
        val sentNode = translateNode.children[0]

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getReportEventData(
            sentNode.node.reportId,
            "",
            translateNode.node.reportId,
            TaskAction.send,
            Topic.FULL_ELR,
            emptyList(),
        )

        assertThat(data).isEqualToIgnoringGivenProperties(
            ReportEventData(
                sentNode.node.reportId,
                translateNode.node.reportId,
                listOf(submittedReport.node.reportId),
                Topic.FULL_ELR,
                "",
                TaskAction.send,
                OffsetDateTime.now(),
                Version.commitId
            ),
            ReportEventData::timestamp
        )
    }

    @Test
    fun `test getReportEventData no parent`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getReportEventData(
            submittedReport.node.reportId,
            "",
            null,
            TaskAction.send,
            Topic.FULL_ELR,
            emptyList()
        )

        assertThat(data).isEqualToIgnoringGivenProperties(
            ReportEventData(
                submittedReport.node.reportId,
                null,
                listOf(),
                Topic.FULL_ELR,
                "",
                TaskAction.send,
                OffsetDateTime.now(),
                Version.commitId
            ),
            ReportEventData::timestamp
        )
    }

    @Test
    fun `test getItemEventData`() {
        val submittedReport = reportGraph {
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
                            action(TaskAction.receiver_filter)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.translate)
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                                reportGraphNode {
                                    action(TaskAction.send)
                                    transportResult("Success")
                                    receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                                }
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.receiver_filter)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            reportGraphNode {
                                action(TaskAction.translate)
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                                reportGraphNode {
                                    action(TaskAction.send)
                                    transportResult("Success")
                                    receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                                }
                            }
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val translateNode = submittedReport.children[0].children[0].children[0].children[0]

        val reportEventService = ReportStreamEventService(
            ReportStreamTestDatabaseContainer.testDatabaseAccess,
            InMemoryAzureEventService(),
            ReportService(
                ReportGraph(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            )
        )

        val data = reportEventService.getItemEventData(
            1,
            translateNode.node.reportId,
            1,
            "",
            null
        )

        assertThat(data).isEqualTo(
            ItemEventData(
                1,
                1,
                1,
                "",
                "phd.elr-hl7-sender"
            )
        )
    }
}

class ReportEventStreamUnitTests {

    @AfterEach
    fun afterEach() {
        unmockkAll()
    }

    @KotlinTest
    fun `test getReportEventData does query db when rootReports is empty`() {
        val reportId = UUID.randomUUID()
        val parentReportId = UUID.randomUUID()
        val reportFile = ReportFile()
        reportFile.reportId = reportId

        val mockReportService = mockk<ReportService>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val mockAzureEventService = mockk<AzureEventService>()
        val eventService = ReportStreamEventService(mockDbAccess, mockAzureEventService, mockReportService)

        every { mockReportService.getRootReports(any()) } returns listOf(reportFile)

        eventService.getReportEventData(
            childReportId = UUID.randomUUID(),
            childBodyUrl = "",
            parentReportId = parentReportId,
            pipelineStepName = TaskAction.convert,
            topic = Topic.FULL_ELR,
            rootReports = emptyList(),
        )

        verify(exactly = 1) {
            mockReportService.getRootReports(parentReportId)
        }
    }

    @KotlinTest
    fun `test getReportEventData does not query db when rootReports is not empty`() {
        val parentReportId = UUID.randomUUID()
        val reportId = UUID.randomUUID()

        val reportFile = ReportFile()
        reportFile.reportId = reportId
        reportFile.sendingOrg = "test-sending-org"
        reportFile.sendingOrgClient = "test-sending-org-client"

        val mockReportService = mockk<ReportService>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val mockAzureEventService = mockk<AzureEventService>()
        val eventService = ReportStreamEventService(mockDbAccess, mockAzureEventService, mockReportService)

        eventService.getReportEventData(
            childReportId = UUID.randomUUID(),
            childBodyUrl = "",
            parentReportId = parentReportId,
            pipelineStepName = TaskAction.convert,
            topic = Topic.FULL_ELR,
            rootReports = listOf(reportFile),
        )

        verify(exactly = 0) {
            mockReportService.getRootReports(any())
        }
    }

    @KotlinTest
    fun `test getItemEventData does query db when rootReport is null`() {
        val reportId = UUID.randomUUID()

        val reportFile = ReportFile()
        reportFile.reportId = reportId
        reportFile.sendingOrg = "test-sending-org"
        reportFile.sendingOrgClient = "test-sending-org-client"

        val mockReportService = mockk<ReportService>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val mockAzureEventService = mockk<AzureEventService>()
        val eventService = ReportStreamEventService(mockDbAccess, mockAzureEventService, mockReportService)

        every {
            mockReportService.getRootItemIndex(any(), any())
        } returns 0

        every { mockReportService.getRootReports(any()) } returns listOf(reportFile)

        eventService.getItemEventData(
            childItemIndex = 0,
            parentReportId = UUID.randomUUID(),
            parentItemIndex = 0,
            trackingId = "test-tracking-id",
            rootReport = null
        )

        verify(exactly = 1) {
            mockReportService.getRootReports(any())
        }
    }

    @KotlinTest
    fun `test getItemEventData does not query db when rootReport is not null`() {
        val parentReportId = UUID.randomUUID()
        val reportId = UUID.randomUUID()

        val reportFile = ReportFile()
        reportFile.reportId = reportId
        reportFile.sendingOrg = "test-sending-org"
        reportFile.sendingOrgClient = "test-sending-org-client"

        val mockReportService = mockk<ReportService>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val mockAzureEventService = mockk<AzureEventService>()

        val eventService = ReportStreamEventService(mockDbAccess, mockAzureEventService, mockReportService)

        every {
            mockReportService.getRootItemIndex(any(), any())
        } returns 0

        eventService.getItemEventData(
            childItemIndex = 0,
            parentReportId = parentReportId,
            parentItemIndex = 0,
            trackingId = "test-tracking-id",
            rootReport = reportFile
        )

        verify(exactly = 0) {
            mockReportService.getRootReports(any())
        }
    }
}