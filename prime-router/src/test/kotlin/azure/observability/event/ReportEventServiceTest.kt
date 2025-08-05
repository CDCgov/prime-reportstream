package gov.cdc.prime.router.azure.observability.event

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isEqualToIgnoringGivenProperties
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.ReportNodeBuilder.Companion.reportGraph
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.history.db.ReportGraph
import gov.cdc.prime.router.report.ReportService
import gov.cdc.prime.router.version.Version
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.OffsetDateTime

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
            Topic.FULL_ELR
        )

        assertThat(data).isEqualToIgnoringGivenProperties(
            ReportEventData(
                sentNode.node.reportId,
                translateNode.node.reportId,
                Topic.FULL_ELR,
                "",
                TaskAction.send,
                OffsetDateTime.now(),
                Version.commitId,
                ""
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
            Topic.FULL_ELR
        )

        assertThat(data).isEqualToIgnoringGivenProperties(
            ReportEventData(
                submittedReport.node.reportId,
                null,
                Topic.FULL_ELR,
                "",
                TaskAction.send,
                OffsetDateTime.now(),
                Version.commitId,
                ""
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
            ""
            )

        assertThat(data).isEqualTo(
            ItemEventData(
                1,
                1,
                1,
                ""
            )
        )
    }
}