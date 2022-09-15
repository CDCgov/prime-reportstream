package gov.cdc.prime.router.serializers

import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLogger
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SerializerValidationTests {
    private val testDefaultMaxItemsErrorMessage = "Reduce the amount of rows in this file."
    private val testDefaultMaxItemColumnsErrorMessage = "Number of columns in your report exceeds the maximum of"
    private val testCustomErrorMessage = "This is an error message"

    private fun assertDefaultMaxItemsErrorMessage(actionLogs: ActionLogger) {
        assert(actionLogs.errors[0].detail.message.contains(testDefaultMaxItemsErrorMessage))
    }

    private fun assertDefaultMaxItemColumnsErrorMessage(actionLogs: ActionLogger) {
        assert(actionLogs.errors[0].detail.message.contains(testDefaultMaxItemColumnsErrorMessage))
    }

    private fun assertCustomErrorMessage(actionLogs: ActionLogger) {
        assert(actionLogs.errors[0].detail.message.contains(testCustomErrorMessage))
    }

    @Test
    fun `test throwMaxItemsError with specified message`() {
        val actionLogs = ActionLogger()
        assertFailsWith<ActionError>() {
            SerializerValidation.throwMaxItemsError(
                10001,
                actionLogs,
                testCustomErrorMessage
            )
        }

        assertCustomErrorMessage(actionLogs)
    }

    @Test
    fun `test throwMaxItemsError with default message`() {
        val actionLogs = ActionLogger()
        assertFailsWith<ActionError>() {
            SerializerValidation.throwMaxItemsError(
                10001,
                actionLogs
            )
        }
        assertDefaultMaxItemsErrorMessage(actionLogs)
    }

    @Test
    fun `test throwMaxItemsError with specific maxSize`() {
        val actionLogs = ActionLogger()
        assertFailsWith<ActionError>() {
            SerializerValidation.throwMaxItemsError(
                101,
                actionLogs,
                "",
                100
            )
        }
        assertDefaultMaxItemsErrorMessage(actionLogs)
    }

    @Test
    fun `test throwMaxItemColumnsError with specified message`() {
        val actionLogs = ActionLogger()
        assertFailsWith<ActionError>() {
            SerializerValidation.throwMaxItemColumnsError(
                2001,
                actionLogs,
                testCustomErrorMessage
            )
        }
        assertCustomErrorMessage(actionLogs)
    }

    @Test
    fun `test throwMaxItemColumnsError with default message`() {
        val actionLogs = ActionLogger()
        assertFailsWith<ActionError>() {
            SerializerValidation.throwMaxItemColumnsError(
                2001,
                actionLogs
            )
        }

        assertDefaultMaxItemColumnsErrorMessage(actionLogs)
    }

    @Test
    fun `test throwMaxItemColumnsError with specific maxSize`() {
        val actionLogs = ActionLogger()
        assertFailsWith<ActionError>() {
            SerializerValidation.throwMaxItemColumnsError(
                101,
                actionLogs,
                "",
                100
            )
        }
        assertDefaultMaxItemColumnsErrorMessage(actionLogs)
    }
}