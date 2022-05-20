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

        val goodReturn = listOf(
            DeliveryHistory(
                922,
                OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
                "ca-dph",
                "elr-secondary",
                201,
                null,
                "b9f63105-bbed-4b41-b1ad-002a90f07e62",
                "covid-19",
                14,
                ".../simple_report.default%2Fpdi-covid-19-b9f63105-bbed-4b41-b1ad-002a90f07e62-20220505140424.csv",
                "primedatainput/pdi-covid-19",
                "CSV"
            ),
            DeliveryHistory(
                284,
                OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
                "ca-dph",
                "elr-secondary",
                201,
                null,
                "c3c8e304-8eff-4882-9000-3645054a30b7",
                "covid-19",
                1,
                ".../ca-dph.elr-secondary%2Fcovid-19-c3c8e304-8eff-4882-9000-3645054a30b7-20220412130611.hl7",
                "covid-19",
                "HL7_BATCH"
            )
        )

        every {
            mockDeliveryAccess.fetchActions(
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

        assertThat(
            facade.findDeliveries(
                "ca-dph",
                ReportFileAccess.SortOrder.ASC,
                ReportFileAccess.SortColumn.CREATED_AT,
                null,
                null,
                10
            )
        ).isEqualTo(goodReturn)
    }
}