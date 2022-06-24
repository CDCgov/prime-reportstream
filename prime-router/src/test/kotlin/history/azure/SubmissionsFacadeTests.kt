package gov.cdc.prime.router.history.azure

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import gov.cdc.prime.router.azure.DatabaseAccess
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import gov.cdc.prime.router.history.DetailedSubmissionHistory
import gov.cdc.prime.router.tokens.AuthenticatedClaims
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import kotlin.test.Test

class SubmissionsFacadeTests {
    @Test
    fun `test organization validation`() {
        val mockSubmissionAccess = mockk<ReportFileAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)

        assertThat {
            facade.findSubmissionsAsJson(
                "",
                null,
                ReportFileAccess.SortDir.ASC,
                ReportFileAccess.SortColumn.CREATED_AT,
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
                ReportFileAccess.SortDir.ASC,
                ReportFileAccess.SortColumn.CREATED_AT,
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
                any(), any(), DetailedSubmissionHistory::class.java
            )
        } returns goodReturn
        every {
            mockSubmissionAccess.fetchRelatedActions(
                550, DetailedSubmissionHistory::class.java
            )
        } returns emptyList()
        assertThat(facade.findDetailedSubmissionHistory("org", 550)).isEqualTo(goodReturn)
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
        var claims = AuthenticatedClaims(userClaims)
        assertThat(facade.checkSenderAccessAuthorization(action, claims)).isTrue()

        // Sysadmin happy path:   Sysadmin user ok to be in a different org.
        val adminClaims: Map<String, Any> = mapOf(
            "organization" to listOf("DHfoobar", "DHPrimeAdmins"),
            "sub" to "bob@bob.com"
        )
        claims = AuthenticatedClaims(adminClaims)
        assertThat(facade.checkSenderAccessAuthorization(action, claims)).isTrue()

        // Error: Regular user and Orgs don't match
        claims = AuthenticatedClaims(userClaims)
        action = resetAction()
        action.sendingOrg = "UnhappyOrg" // mismatch sendingOrg
        assertThat(facade.checkSenderAccessAuthorization(action, claims)).isFalse()
    }
}