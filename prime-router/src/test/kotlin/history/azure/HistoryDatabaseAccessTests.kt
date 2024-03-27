package gov.cdc.prime.router.history.azure

import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryDatabaseAccessTests {
    @Test
    fun `test organizationFilter`() {
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

        var conditionActual = DatabaseDeliveryAccess().organizationFilter("test", "test")
        assertEquals(conditionExpected, conditionActual.toString())
    }
}