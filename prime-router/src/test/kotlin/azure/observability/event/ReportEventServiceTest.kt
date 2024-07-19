package gov.cdc.prime.router.azure.observability.event

import assertk.assertions.contains
import gov.cdc.prime.router.db.ReportStreamTestDatabaseSetupExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@ExtendWith(ReportStreamTestDatabaseSetupExtension::class)
class ReportEventServiceTest