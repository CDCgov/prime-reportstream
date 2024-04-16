package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.WorkflowEngine
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import kotlin.test.Test
import kotlin.test.assertEquals

val mockTestOrg = Organization(
    "phd",
    "test",
    Organization.Jurisdiction.FEDERAL,
    "",
    "",
    null,
    null,
)

val mockElimsTestOrg = Organization(
    "phd",
    "test",
    Organization.Jurisdiction.FEDERAL,
    "",
    "",
    null,
    listOf("ELIMS_DATA"),
)
class HistoryDatabaseAccessTests {
    @Test
    fun `test organizationFilter`() {
        var conditionExpected = """
            (
              "public"."action"."action_name" = 'batch'
              and "public"."report_file"."receiving_org" = 'test'
              and "public"."report_file"."receiving_org_svc" = 'test'
            )
        """.trimIndent()
        val dataProvider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(dataProvider)
        val accessSpy = spyk(DatabaseAccess(connection))
        val engine = mockkClass(WorkflowEngine::class)
        every { engine.settings.findOrganization(any()) } returns mockTestOrg
        var conditionActual = DatabaseDeliveryAccess(accessSpy, engine).organizationFilter("test", "test")

        assertEquals(conditionExpected, conditionActual.toString())
    }

    @Test
    fun `test organizationFilter with feature flag`() {
        var conditionExpected = """
            (
              (
                "public"."action"."action_name" = 'batch'
                or (
                  "public"."action"."action_name" = 'send'
                  and "public"."report_file"."schema_topic" = 'ELR_ELIMS'
                )
              )
              and "public"."report_file"."receiving_org" = 'test'
              and "public"."report_file"."receiving_org_svc" = 'test'
            )
        """.trimIndent()
        val dataProvider = MockDataProvider { emptyArray<MockResult>() }
        val connection = MockConnection(dataProvider)
        val accessSpy = spyk(DatabaseAccess(connection))
        val engine = mockkClass(WorkflowEngine::class)
        every { engine.settings.findOrganization(any()) } returns mockElimsTestOrg
        var conditionActual = DatabaseDeliveryAccess(accessSpy, engine).organizationFilter("test", "test")

        assertEquals(conditionExpected, conditionActual.toString())
    }
}