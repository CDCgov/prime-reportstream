package gov.cdc.prime.router.azure

import gov.cdc.prime.router.azure.db.enums.TaskAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ActionHistoryTests {
    @Test
    fun `test constructor`() {
        val actionHistory = ActionHistory("batch", "any string is ok here")
        assertEquals(actionHistory.action.actionName, TaskAction.batch)
    }

    @Test
    fun `test constructor with bad data`() {
        assertFails { ActionHistory("foobar") }
    }
}