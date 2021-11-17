package gov.cdc.prime.router.azure

import assertk.assertThat
import assertk.assertions.isGreaterThan
import gov.cdc.prime.router.azure.db.enums.TaskAction
import gov.cdc.prime.router.azure.db.tables.pojos.Action
import io.mockk.every
import io.mockk.spyk
import org.jooq.tools.jdbc.MockConnection
import org.jooq.tools.jdbc.MockDataProvider
import org.jooq.tools.jdbc.MockResult
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.OffsetDateTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubmissionFacadeTests {
    private val dataProvider = MockDataProvider { emptyArray<MockResult>() }
    private val connection = MockConnection(dataProvider)
    private val accessSpy = spyk(DatabaseSubmissionsAccess(DatabaseAccess(connection)))

    private val actions = listOf(
        Action(
            1,
            TaskAction.receive,
            null,
            null,
            OffsetDateTime.now(),
            null,
            201,
            null,
            null,
            "simple_report",
            null
        ),
        Action(
            2,
            TaskAction.receive,
            null,
            null,
            OffsetDateTime.now(),
            null,
            201,
            null,
            null,
            "simple_report",
            null
        ),
        Action(
            3,
            TaskAction.receive,
            null,
            null,
            OffsetDateTime.now(),
            null,
            201,
            null,
            null,
            "simple_report",
            null
        ),
        Action(
            4,
            TaskAction.receive,
            null,
            null,
            OffsetDateTime.now(),
            null,
            201,
            null,
            null,
            "not_simple_report",
            null
        )
    )

    private fun setupSubmissionActionDatabaseAccess() {
        every {
            accessSpy.fetchActions("simple_report", any())
        }.returns(actions)
    }

    @Test
    fun `test findSubmissionsAsJson`() {
        setupSubmissionActionDatabaseAccess()
        val submissionsFacade = SubmissionsFacade(accessSpy)
        val ret1 = submissionsFacade.findSubmissionsAsJson("simple_report", 10)
        assertThat(ret1.length).isGreaterThan(2)
    }
}