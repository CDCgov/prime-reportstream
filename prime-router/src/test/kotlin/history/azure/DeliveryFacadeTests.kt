package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.history.DeliveryHistory
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import kotlin.test.Test

class DeliveryFacadeTests {
    @Test
    fun `test findDeliveries`() {
        val mockDeliveryAccess = mockk<DatabaseDeliveryAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = DeliveryFacade(mockDeliveryAccess, mockDbAccess)

        val delivery1 = DeliveryHistory(
            284,
            OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
            "ca-dph",
            "elr-secondary",
            201,
            null,
            "c3c8e304-8eff-4882-9000-3645054a30b7",
            "covid-19",
            1,
            "",
            "covid-19",
            "HL7_BATCH"
        )

        val delivery2 = DeliveryHistory(
            922,
            OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
            "ca-dph",
            "elr-secondary",
            201,
            null,
            "b9f63105-bbed-4b41-b1ad-002a90f07e62",
            "covid-19",
            14,
            "",
            "primedatainput/pdi-covid-19",
            "CSV"
        )

        val goodReturn = listOf(delivery1, delivery2)

        every {
            mockDeliveryAccess.fetchActions(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                DeliveryHistory::class.java
            )
        } returns goodReturn

        val deliveries = facade.findDeliveries(
            "ca-dph",
            "elr",
            ReportFileAccess.SortDir.ASC,
            ReportFileAccess.SortColumn.CREATED_AT,
            null,
            null,
            null,
            10
        )

        assertThat(deliveries.first().reportId).isEqualTo(delivery1.reportId)
        assertThat(deliveries.last().reportId).isEqualTo(delivery2.reportId)
    }
}