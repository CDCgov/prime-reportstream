package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.google.common.net.HttpHeaders
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.MockHttpRequestMessage
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import gov.cdc.prime.router.tokens.AuthenticationType
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test

class SubmissionsFacadeTests {
    @Test
    fun `test organization validation`() {
        val mockSubmissionAccess = mockk<HistoryDatabaseAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)

        assertThat {
            facade.findSubmissionsAsJson(
                "",
                null,
                HistoryDatabaseAccess.SortDir.ASC,
                HistoryDatabaseAccess.SortColumn.CREATED_AT,
                null,
                null,
                null,
                10,
                true
            )
        }.isFailure().hasMessage("Invalid organization.")

        assertThat {
            facade.findSubmissionsAsJson(
                "  \t\n",
                null,
                HistoryDatabaseAccess.SortDir.ASC,
                HistoryDatabaseAccess.SortColumn.CREATED_AT,
                null,
                null,
                null,
                10,
                true
            )
        }.isFailure().hasMessage("Invalid organization.")
    }

    @Test
    fun `test findDetailedSubmissionHistory`() {
        val mockSubmissionAccess = mockk<DatabaseSubmissionsAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)
        // Good return
        val goodReturn = DetailedSubmissionHistory(
            550, TaskAction.receive, OffsetDateTime.now(),
            null, null, emptyList()
        )
        every {
            mockSubmissionAccess.fetchAction(
                any(),
                any(),
                DetailedSubmissionHistory::class.java
            )
        } returns goodReturn

        // No lineage since we have no report ID
        val action1 = Action()
        action1.actionId = 550
        action1.sendingOrg = "myOrg"
        action1.actionName = TaskAction.receive
        assertThat(facade.findDetailedSubmissionHistory(action1)).isEqualTo(goodReturn)

        // Happy path
        goodReturn.reportId = UUID.randomUUID().toString()
        every {
            mockSubmissionAccess.fetchRelatedActions(
                UUID.fromString(goodReturn.reportId), DetailedSubmissionHistory::class.java
            )
        } returns emptyList()
        assertThat(facade.findDetailedSubmissionHistory(action1)).isEqualTo(goodReturn)
        // Failures
        val action2 = Action()
        action2.actionId = 550
        action2.sendingOrg = "myOrg" // good
        action2.actionName = TaskAction.process // bad. Submission queries only work on receive actions.
        assertThat { facade.findDetailedSubmissionHistory(action2) }.isFailure() // not a receive action
        action2.actionName = TaskAction.receive // good
        action2.sendingOrg = null // bad
        assertThat { facade.findDetailedSubmissionHistory(action2) }.isFailure() // missing sendingOrg
    }

    @Test
    fun `test checkAccessAuthorizationForOrg`() {
        val mockSubmissionAccess = mockk<DatabaseSubmissionsAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)
        val mockRequest = MockHttpRequestMessage()

        // Regular user Happy path test.
        val userClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHSender_myOrg"),
            "sub" to "bob@bob.com"
        )
        var claims = AuthenticatedClaims(userClaims, AuthenticationType.Okta)
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"
        val org1 = "myOrg"
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
        val mockSubmissionAccess = mockk<DatabaseSubmissionsAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)
        val mockRequest = MockHttpRequestMessage()

        val action = Action()
        action.actionId = 123
        action.sendingOrg = "mySendingOrg"
        action.sendingOrgClient = "mySendingOrgClient"
        action.receivingOrg = "myReceivingOrg"
        action.receivingOrgSvc = "myReceivingOrgSvc"

        // Regular user Happy path test.
        val userClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHSender_mySendingOrg"),
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
            "organization" to listOf("DHSender_foobar"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(mismatchedClaims, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorizationForAction(claims, action, mockRequest)).isFalse()

        // This is a submissions query.  So, we sure better not be looking up the receivingOrg.
        val mismatchedClaims2: Map<String, Any> = mapOf(
            "organization" to listOf("DHmyReceivingOrg"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(mismatchedClaims2, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorizationForAction(claims, action, mockRequest)).isFalse()

        // The auth should work, even without the
        // annoying "Sender_" string in the claims, which is actually not needed any more.
        val mismatchedClaims3: Map<String, Any> = mapOf(
            "organization" to listOf("DHmySendingOrg"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(mismatchedClaims3, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorizationForAction(claims, action, mockRequest)).isTrue()
    }
}