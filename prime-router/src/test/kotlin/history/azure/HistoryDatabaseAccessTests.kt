package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.common.BaseEngine
import io.mockk.every
import io.mockk.mockkObject
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

// Note: These tests are testing DatabaseDeliveryAccess.kt and not HistoryDatabaseAccessTests.kt
class HistoryDatabaseAccessTests {
    @Test
    fun `test organizationFilter`() {
        var conditionExpected = """
            (
              "public"."report_file"."next_action" = 'send'
              and "public"."report_file"."receiving_org" = 'test'
              and "public"."report_file"."receiving_org_svc" = 'test'
            )
        """.trimIndent()
        mockkObject(BaseEngine)
        every { BaseEngine.settingsProviderSingleton.findOrganization(any()) } returns mockTestOrg
        var conditionActual = DatabaseDeliveryAccess().organizationFilter("test", "test")

        assertEquals(conditionExpected, conditionActual.toString())
    }

    @Test
    fun `test organizationFilter with feature flag`() {
        var conditionExpected = """
            (
              (
                (
                  "public"."report_file"."next_action" = 'send'
                  and lower("public"."report_file"."schema_topic") <> lower('elr-elims')
                )
                or (
                  "public"."report_file"."next_action" is null
                  and "public"."report_file"."transport_params" is not null
                  and "public"."report_file"."transport_result" not like '%downloadedBy%'
                  and lower("public"."report_file"."schema_topic") = lower('elr-elims')
                )
              )
              and "public"."report_file"."receiving_org" = 'test'
              and "public"."report_file"."receiving_org_svc" = 'test'
            )
        """.trimIndent()
        mockkObject(BaseEngine)
        every { BaseEngine.settingsProviderSingleton.findOrganization(any()) } returns mockElimsTestOrg
        var conditionActual = DatabaseDeliveryAccess().organizationFilter("test", "test")

        assertEquals(conditionExpected, conditionActual.toString())
    }
}