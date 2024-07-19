package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import gov.cdc.prime.router.ActionLog
import gov.cdc.prime.router.ActionLogLevel
import gov.cdc.prime.router.FileSettings
import gov.cdc.prime.router.InvalidParamMessage
import gov.cdc.prime.router.MimeFormat
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.common.JacksonMapperUtilities
import gov.cdc.prime.router.common.ReportNodeBuilder.Companion.reportGraph
import gov.cdc.prime.router.common.UniversalPipelineTestUtils
import gov.cdc.prime.router.db.ReportStreamTestDatabaseContainer
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import gov.cdc.prime.router.unittest.UnitTestUtils
import io.mockk.every
import io.mockk.mockkObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class SubmissionFunctionIntegrationTests {

    @Test
    fun `it should return a history for partially delivered submission`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            itemCount(0)
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            itemCount(0)
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.PARTIALLY_DELIVERED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `it should return a history for partially delivered submission (for legacy route step)`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.route)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            itemCount(0)
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.route)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                            itemCount(0)
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.PARTIALLY_DELIVERED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    @Test
    fun `it should return a history that a submission has been received`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.RECEIVED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(0)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(0)
    }

    @Test
    fun `it should return a history that indicates the report is not going to be delivered`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(0)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `it should return a history that indicates the report is not going to be delivered (legacy route step)`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.route)
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.NOT_DELIVERING.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(0)
        assertThat(historyNode.get("errors").size()).isEqualTo(0)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    @Test
    fun `it should return a history that indicates waiting to deliver`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(1)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `it should return a history that indicates waiting to deliver (legacy route step)`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.route)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                        }
                    }
                    reportGraphNode {
                        action(TaskAction.route)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[1])
                        }
                    }
                }
            }
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.WAITING_TO_DELIVER.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(1)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
    }

    @Test
    fun `it should return history of a submission that is delivered`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
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
                    reportGraphNode {
                        action(TaskAction.destination_filter)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
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
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.DELIVERED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(1)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
        assertThat(historyNode.get("sender").asText()).isEqualTo("phd.elr-hl7-sender")
        assertThat(historyNode.get("actualCompletionAt").asText()).isNotNull()
    }

    // TODO: remove after route queue empty (see https://github.com/CDCgov/prime-reportstream/issues/15039)
    @Test
    fun `it should return history of a submission that is delivered (legacy route step)`() {
        val submittedReport = reportGraph {
            topic(Topic.FULL_ELR)
            format(MimeFormat.HL7)
            sender(UniversalPipelineTestUtils.hl7Sender)

            submission {
                action(TaskAction.receive)
                reportGraphNode {
                    action(TaskAction.convert)
                    log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.warning))
                    reportGraphNode {
                        action(TaskAction.route)
                        log(ActionLog(InvalidParamMessage("log"), type = ActionLogLevel.error))
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
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
                    reportGraphNode {
                        action(TaskAction.route)
                        reportGraphNode {
                            action(TaskAction.translate)
                            receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            reportGraphNode {
                                action(TaskAction.send)
                                transportResult("Success")
                                receiver(UniversalPipelineTestUtils.universalPipelineOrganization.receivers[0])
                            }
                        }
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
        }.generate(ReportStreamTestDatabaseContainer.testDatabaseAccess)

        val httpRequestMessage = MockHttpRequestMessage()

        val func = setupSubmissionFunction()

        val history = func
            .getReportDetailedHistory(httpRequestMessage, submittedReport.node.reportId.toString())
        assertThat(history).isNotNull()
        val historyNode = JacksonMapperUtilities.defaultMapper.readTree(history.body.toString())
        assertThat(
            historyNode.get("overallStatus").asText()
        ).isEqualTo(DetailedSubmissionHistory.Status.DELIVERED.toString())
        assertThat(historyNode.get("destinations").size()).isEqualTo(2)
        assertThat(historyNode.get("errors").size()).isEqualTo(1)
        assertThat(historyNode.get("warnings").size()).isEqualTo(1)
        assertThat(historyNode.get("sender").asText()).isEqualTo("phd.elr-hl7-sender")
        assertThat(historyNode.get("actualCompletionAt").asText()).isNotNull()
    }

    @BeforeEach
    fun setupAuth() {
        val jwt = mapOf("organization" to listOf(oktaSystemAdminGroup), "sub" to "test@cdc.gov")
        val claims = AuthenticatedClaims(jwt, AuthenticationType.Okta)
        mockkObject(AuthenticatedClaims)
        every { AuthenticatedClaims.authenticate(any()) } returns claims
    }

    private fun setupSubmissionFunction(): SubmissionFunction {
        val workflowEngine = WorkflowEngine
            .Builder()
            .metadata(UnitTestUtils.simpleMetadata)
            .settingsProvider(
                FileSettings().loadOrganizations(UniversalPipelineTestUtils.universalPipelineOrganization)
            )
            .databaseAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess)
            .build()
        return SubmissionFunction(
            SubmissionsFacade(
                DatabaseSubmissionsAccess(ReportStreamTestDatabaseContainer.testDatabaseAccess),
                ReportStreamTestDatabaseContainer.testDatabaseAccess
            ),
            workflowEngine,

            )
    }
}