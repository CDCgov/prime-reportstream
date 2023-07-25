package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import assertk.assertions.isTrue
import com.google.common.net.HttpHeaders
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.history.DeliveryFacility
import gov.cdc.prime.router.history.DeliveryHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
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
            Topic.COVID_19,
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
            Topic.COVID_19,
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
            Topic.COVID_19,
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
    fun `test checkAccessAuthorizationForOrg`() {
        val mockDeliveryAccess = mockk<DatabaseDeliveryAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = DeliveryFacade(mockDeliveryAccess, mockDbAccess)
        val mockRequest = MockHttpRequestMessage()

        // Regular user Happy path test.
        val userClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHmyReceivingOrg"),
            "sub" to "bob@bob.com"
        )
        var claims = AuthenticatedClaims(userClaims, AuthenticationType.Okta)
//        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"
        val org1 = "myReceivingOrg"
        assertThat(facade.checkAccessAuthorizationForOrg(claims, org1, null, mockRequest)).isTrue()
        // User has right to see any sender channel within that org.
        assertThat(facade.checkAccessAuthorizationForOrg(claims, org1, "quux", mockRequest)).isTrue()

        // PrimeAdmin happy path:   PrimeAdmin user ok to be in a different org.
        val adminClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHfoobar", "DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(adminClaims, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorizationForOrg(claims, org1, null, mockRequest)).isTrue()
        // PrimeAdmin has right to see any sender channel within that org.
        assertThat(facade.checkAccessAuthorizationForOrg(claims, org1, "quux", mockRequest)).isTrue()

        // Error: Regular user and Orgs don't match
        claims = AuthenticatedClaims(userClaims, AuthenticationType.Okta)
        val badOrg = "UnhappyOrg" // mismatch sendingOrg
        assertThat(facade.checkAccessAuthorizationForOrg(claims, badOrg, null, mockRequest)).isFalse()
        // This auth also denied, regardless of the sender channel.
        assertThat(facade.checkAccessAuthorizationForOrg(claims, badOrg, "quux", mockRequest)).isFalse()
    }

    @Test
    fun `test checkAccessAuthorizationForAction`() {
        val mockDeliveryAccess = mockk<DatabaseDeliveryAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = DeliveryFacade(mockDeliveryAccess, mockDbAccess)
        val mockRequest = MockHttpRequestMessage()

        val action = Action()
        action.actionId = 123
        action.sendingOrg = "mySendingOrg"
        action.sendingOrgClient = "mySendingOrgClient"
        action.receivingOrg = "myReceivingOrg"
        action.receivingOrgSvc = "myReceivingOrgSvc"

        // Regular user Happy path test.
        val userClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHmyReceivingOrg"),
            "sub" to "bob@bob.com"
        )
        var claims = AuthenticatedClaims(userClaims, AuthenticationType.Okta)
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"
        assertThat(facade.checkAccessAuthorizationForAction(claims, action, mockRequest)).isTrue()

        // PrimeAdmin happy path:   PrimeAdmin user ok to be in a different org.
        val adminClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHfoobar", "DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(adminClaims, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorizationForAction(claims, action, mockRequest)).isTrue()

        // Error: Regular user and Orgs don't match
        val mismatchedClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHfoobar"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(mismatchedClaims, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorizationForAction(claims, action, mockRequest)).isFalse()

        // This is a delivery query.  So, we sure better not be looking up the sendingOrg.
        val mismatchedClaims2: Map<String, Any> = mapOf(
            "organization" to listOf("DHSender_mySendingOrg"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(mismatchedClaims2, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorizationForAction(claims, action, mockRequest)).isFalse()

        // This is a delivery query.  So, we sure better not be looking up the sendingOrg, even without the
        // annoying "Sender" string in the claims, which is actually not needed any more.
        val mismatchedClaims3: Map<String, Any> = mapOf(
            "organization" to listOf("DHmySendingOrg"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(mismatchedClaims3, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorizationForAction(claims, action, mockRequest)).isFalse()
    }

    @Test
    fun `test companion instance`() {
        val facade = DeliveryFacade.instance
        assertThat(facade).isNotNull()
    }
}