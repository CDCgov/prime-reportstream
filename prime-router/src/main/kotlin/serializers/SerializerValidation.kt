package gov.cdc.prime.router.serializers

import gov.cdc.prime.router.ActionLogger
import gov.cdc.prime.router.InvalidReportMessage
import gov.cdc.prime.router.REPORT_MAX_ITEMS
import gov.cdc.prime.router.REPORT_MAX_ITEM_COLUMNS

class SerializerValidation {
    companion object {

        @JvmStatic
        fun throwMaxItemsError(
            size: Int,
            actionLogs:
                ActionLogger,
            message: String = "",
            maxSize: Int = REPORT_MAX_ITEMS
        ) {
            val defaultMaxItemsMessage = "Your file's row size of $size exceeds the maximum of " +
                "$maxSize rows per file. Reduce the amount of rows in this file."

            val errorMessage = message.ifEmpty { defaultMaxItemsMessage }

            throwActionLogException(size, maxSize, actionLogs, errorMessage)
        }

        @JvmStatic
        fun throwMaxItemColumnsError(
            size: Int,
            actionLogs: ActionLogger,
            message: String = "",
            maxSize: Int = REPORT_MAX_ITEM_COLUMNS
        ) {
            val defaultMaxItemColumnsMessage = "Number of columns in your report exceeds the maximum of " +
                "$maxSize allowed. Adjust the excess columnar data in your report."

            val errorMessage = message.ifEmpty { defaultMaxItemColumnsMessage }

            throwActionLogException(size, maxSize, actionLogs, errorMessage)
        }

        private fun throwActionLogException(
            size: Int,
            maxSize: Int,
            actionLogs: ActionLogger,
            message: String
        ) {
            if (size > maxSize) {
                actionLogs.error(
                    InvalidReportMessage(
                        message
                    )
                )
                throw actionLogs.exception
            }
        }
    }
}