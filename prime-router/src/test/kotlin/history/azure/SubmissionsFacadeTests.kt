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
    fun `test checkActionAccessAuthorization`() {
        fun resetAction(): Action {
            val action = Action()
            action.actionId = 123
            action.sendingOrg = "myOrg"
            action.actionName = TaskAction.receive
            action.httpStatus = 201
            return action
        }
        val mockSubmissionAccess = mockk<DatabaseSubmissionsAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)

        // Regular user Happy path test.
        var action = resetAction()
        val userClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHSender_myOrg"),
            "sub" to "bob@bob.com"
        )
        var claims = AuthenticatedClaims(userClaims, AuthenticationType.Okta)
        val mockRequest = MockHttpRequestMessage()
        mockRequest.httpHeaders[HttpHeaders.AUTHORIZATION.lowercase()] = "Bearer dummy"
        assertThat(facade.checkAccessAuthorization(claims, action.sendingOrg, null, mockRequest)).isTrue()

        // Sysadmin happy path:   Sysadmin user ok to be in a different org.
        val adminClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHfoobar", "DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(adminClaims, AuthenticationType.Okta)
        assertThat(facade.checkAccessAuthorization(claims, action.sendingOrg, null, mockRequest)).isTrue()

        // Error: Regular user and Orgs don't match
        claims = AuthenticatedClaims(userClaims, AuthenticationType.Okta)
        action = resetAction()
        action.sendingOrg = "UnhappyOrg" // mismatch sendingOrg
        assertThat(facade.checkAccessAuthorization(claims, action.sendingOrg, null, mockRequest)).isFalse()
    }
}