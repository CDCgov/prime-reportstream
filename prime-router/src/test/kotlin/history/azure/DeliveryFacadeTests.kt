package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.history.DeliveryFacility
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
            null,
            "c3c8e304-8eff-4882-9000-3645054a30b7",
            "covid-19",
            1,
            "ca-dph",
            null,
            "",
            "covid-19",
            "HL7_BATCH"
        )

        val delivery2 = DeliveryHistory(
            922,
            OffsetDateTime.parse("2022-04-19T18:04:26.534Z"),
            null,
            "b9f63105-bbed-4b41-b1ad-002a90f07e62",
            "covid-19",
            14,
            "ca-dph",
            "elr-secondary",
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
            null,
            HistoryDatabaseAccess.SortDir.ASC,
            HistoryDatabaseAccess.SortColumn.CREATED_AT,
            null,
            null,
            null,
            10
        )

        assertThat(deliveries.first().reportId).isEqualTo(delivery1.reportId)
        assertThat(deliveries.last().reportId).isEqualTo(delivery2.reportId)

        // Unhappy path: "Invalid organization."
        assertThat {
            facade.findDeliveries(
                "",
                "elr",
                HistoryDatabaseAccess.SortDir.ASC,
                HistoryDatabaseAccess.SortColumn.CREATED_AT,
                null,
                null,
                null,
                10
            )
        }.isFailure().hasMessage("Invalid organization.")

        // Unhappy path: "pageSize must be a positive integer."
        assertThat {
            facade.findDeliveries(
                "ca-dph",
                "elr",
                HistoryDatabaseAccess.SortDir.ASC,
                HistoryDatabaseAccess.SortColumn.CREATED_AT,
                null,
                null,
                null,
                -10
            )
        }.isFailure().hasMessage("pageSize must be a positive integer.")

        // Unhappy path: "End date must be after start date."
        assertThat {
            facade.findDeliveries(
                "ca-dph",
                "elr",
                HistoryDatabaseAccess.SortDir.ASC,
                HistoryDatabaseAccess.SortColumn.CREATED_AT,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now().minusDays(1),
                10
            )
        }.isFailure().hasMessage("End date must be after start date.")

        // Happy Path: date window coverage for since
        assertThat {
            facade.findDeliveries(
                "ca-dph",
                "elr",
                HistoryDatabaseAccess.SortDir.ASC,
                HistoryDatabaseAccess.SortColumn.CREATED_AT,
                null,
                OffsetDateTime.now(),
                null,
                10
            )
        }.isSuccess()

        // Happy Path: date window coverage for until
        assertThat {
            facade.findDeliveries(
                "ca-dph",
                null,
                HistoryDatabaseAccess.SortDir.ASC,
                HistoryDatabaseAccess.SortColumn.CREATED_AT,
                null,
                OffsetDateTime.now(),
                null,
                10
            )
        }.isSuccess()

        // Happy Path: date window coverage for both
        assertThat {
            facade.findDeliveries(
                "ca-dph",
                "elr",
                HistoryDatabaseAccess.SortDir.ASC,
                HistoryDatabaseAccess.SortColumn.CREATED_AT,
                null,
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now(),
                10
            )
        }.isSuccess()
    }

    @Test
    fun `test findDetailedDeliveryHistory`() {
        val mockDeliveryAccess = mockk<DatabaseDeliveryAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = DeliveryFacade(mockDeliveryAccess, mockDbAccess)

        val delivery = DeliveryHistory(
            284,
            OffsetDateTime.parse("2022-04-12T17:06:10.534Z"),
            null,
            "c3c8e304-8eff-4882-9000-3645054a30b7",
            "covid-19",
            1,
            "ca-dph",
            "elr-secondary",
            "",
            "covid-19",
            "HL7_BATCH"
        )

        every {
            mockDeliveryAccess.fetchAction(
                any(),
                any(),
                DeliveryHistory::class.java
            )
        } returns delivery

        val result = facade.findDetailedDeliveryHistory(
            delivery.actionId,
        )

        assertThat(delivery.reportId).isEqualTo(result?.reportId)
    }

    @Test
    fun `test findDeliveryFacilities`() {
        val mockDeliveryAccess = mockk<DatabaseDeliveryAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = DeliveryFacade(mockDeliveryAccess, mockDbAccess)

        val reportId = "c3c8e304-8eff-4882-9000-3645054a30b7"

        val facilities = listOf<DeliveryFacility>(
            DeliveryFacility(
                "Any lab USA",
                "Kurtistown",
                "HI",
                "43D1961163",
                0,
                1,
            ),
            DeliveryFacility(
                "Any lab USA",
                "Paauilo",
                "HI",
                "52D0993791",
                0,
                14,
            ),
            DeliveryFacility(
                "Any lab USA",
                "Kekaha",
                "HI",
                "90D6609198",
                0,
                1,
            ),
        )

        every {
            mockDeliveryAccess.fetchFacilityList(
                any(),
                any(),
                any(),
            )
        } returns facilities

        // Happy path
        val result = facade.findDeliveryFacilities(
            ReportId.fromString(reportId),
            HistoryDatabaseAccess.SortDir.DESC,
            DatabaseDeliveryAccess.FacilitySortColumn.NAME,
        )

        assertThat(facilities.first().testingLabName).isEqualTo(result.first().testingLabName)
        assertThat(facilities.first().location).isEqualTo(result.first().location)
    }

    @Test
    fun `test companion instance`() {
        val facade = DeliveryFacade.instance
        assertThat(facade).isNotNull()
    }
}