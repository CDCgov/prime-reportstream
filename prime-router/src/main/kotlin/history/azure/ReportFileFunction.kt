package gov.cdc.prime.router.history.azure

import gov.cdc.prime.router.azure.WorkflowEngine
import org.apache.logging.log4j.kotlin.Logging
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import java.util.UUID

/**
 * Submissions API
 * Returns a list of Actions from `public.action`.
 */
abstract class ReportFileFunction(
    internal val workflowEngine: WorkflowEngine = WorkflowEngine(),
) : Logging {
    data class Parameters(
        val sortDir: SubmissionAccess.SortOrder,
        val sortColumn: SubmissionAccess.SortColumn,
        val cursor: OffsetDateTime,
        val pageSize: Int,
        val showFailed: Boolean
    ) {
        constructor(query: Map<String, String>) : this (
            sortDir = extractSortOrder(query),
            sortColumn = extractSortCol(query),
            extractDateTime(query, "cursor"),
            extractPageSize(query),
            extractShowFailed(query)
        )

        companion object {
            fun extractSortOrder(query: Map<String, String>): SubmissionAccess.SortOrder {
                val sort = query["sort"]
                return if (sort == null)
                    SubmissionAccess.SortOrder.DESC
                else
                    SubmissionAccess.SortOrder.valueOf(sort)
            }

            fun extractSortCol(query: Map<String, String>): SubmissionAccess.SortColumn {
                val col = query["sortcol"]
                return if (col == null)
                    SubmissionAccess.SortColumn.CREATED_AT
                else
                    SubmissionAccess.SortColumn.valueOf(col)
            }

            fun extractDateTime(query: Map<String, String>, name: String): OffsetDateTime {
                val dt = query[name]
                return if (dt != null) {
                    try {
                        OffsetDateTime.parse(dt)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("\"$name\" must be a valid datetime")
                    }
                } else OffsetDateTime.now()
            }

            fun extractPageSize(query: Map<String, String>): Int {
                val size = query.getOrDefault("pagesize", "50").toInt()
                require(size > 0) { "Page size must be a positive integer" }
                return size
            }

            fun extractShowFailed(query: Map<String, String>): Boolean {
                return query["showfailed"]?.toBoolean() ?: false
            }
        }
    }

    /**
     * Utility function.  Mimic String.toLongOrNull()
     *
     * @param str
     * @return a valid UUID, or null if this [str] cannot be parsed into a valid UUID.
     */
    fun toUuidOrNull(str: String): UUID? {
        return try {
            UUID.fromString(str)
        } catch (e: IllegalArgumentException) {
            logger.debug("Invalid format for report ID: $str", e)
            null
        }
    }
}