package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import gov.cdc.prime.router.DetailActionLog
import gov.cdc.prime.router.DetailReport
import gov.cdc.prime.router.DetailedSubmissionHistory
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import kotlin.test.Test

class SubmissionsFacadeTests {
    @Test
    fun `test findDetailedSubmissionHistory`() {
        val mockSubmissionAccess = mockk<SubmissionAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)
        // Good return
        val goodReturn = DetailedSubmissionHistory(
            550, TaskAction.receive, OffsetDateTime.now(),
            null, null, null, null, null, null
        )
        every {
            mockSubmissionAccess.fetchAction(
                any(), any(), DetailedSubmissionHistory::class.java,
                DetailReport::class.java,
                DetailActionLog::class.java
            )
        } returns goodReturn
        every {
            mockSubmissionAccess.fetchRelatedActions(
                550, DetailedSubmissionHistory::class.java,
                DetailReport::class.java,
                DetailActionLog::class.java
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
        val mockSubmissionAccess = mockk<SubmissionAccess>()
        val mockDbAccess = mockk<DatabaseAccess>()
        val facade = SubmissionsFacade(mockSubmissionAccess, mockDbAccess)

        // Regular user Happy path test.
        var action = resetAction()
        var claims = AuthenticatedClaims("Mary", PrincipalLevel.USER, "myOrg", emptyMap())
        assertThat(facade.checkActionAccessAuthorization(action, claims)).isTrue()

        // Sysadmin happy path:   Sysadmin user ok to be in a different org.
        claims = AuthenticatedClaims("Mo", PrincipalLevel.SYSTEM_ADMIN, "different", emptyMap())
        assertThat(facade.checkActionAccessAuthorization(action, claims)).isTrue()

        // Error tests for regular users:
        claims = AuthenticatedClaims("Frank", PrincipalLevel.USER, "myOrg", emptyMap())

        action = resetAction()
        action.sendingOrg = "UnhappyOrg" // mismatch sendingOrg
        assertThat(facade.checkActionAccessAuthorization(action, claims)).isFalse()

        action = resetAction()
        action.actionName = TaskAction.process // Not a 'receive' action
        assertThat(facade.checkActionAccessAuthorization(action, claims)).isFalse()
    }
}