package gov.cdc.prime.router.serializers

import gov.cdc.prime.router.ActionError
import gov.cdc.prime.router.ActionLogger
import kotlin.test.Test
import kotlin.test.assertFailsWith

class SerializerValidationTests {

    @Test
    fun `test throwMaxItemsError with specified message`() {
        val actionLogs = ActionLogger()
        assertFailsWith<ActionError>() {
            SerializerValidation.throwMaxItemsError(
                10001,
                actionLogs,
                "This is an error message"
            )
        }
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
    }

    @Test
    fun `test throwMaxItemColumnsError with specified message`() {
        val actionLogs = ActionLogger()
        assertFailsWith<ActionError>() {
            SerializerValidation.throwMaxItemColumnsError(
                2001,
                actionLogs,
                "This is an error message"
            )
        }
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
    }
}