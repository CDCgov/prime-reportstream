package gov.cdc.prime.router.azure

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
        val sort: String,
        val sortColumn: String,
        val cursor: OffsetDateTime?,
        val endCursor: OffsetDateTime?,
        val pageSize: Int,
        val showFailed: Boolean
    ) {
        constructor(query: Map<String, String>) : this (
            extractSortOrder(query),
            extractSortCol(query),
            extractCursor(query, "cursor"),
            extractCursor(query, "endcursor"),
            extractPageSize(query),
            extractShowFailed(query)
        )

        companion object {
            fun extractSortOrder(query: Map<String, String>): String {
                return query.getOrDefault("sort", "DESC")
            }

            fun extractSortCol(query: Map<String, String>): String {
                return query.getOrDefault("sortcol", "default")
            }

            fun extractCursor(query: Map<String, String>, name: String): OffsetDateTime? {
                val cursor = query.get(name)
                return if (cursor != null) {
                    try {
                        OffsetDateTime.parse(cursor)
                    } catch (e: DateTimeParseException) {
                        throw IllegalArgumentException("\"$name\" must be a valid datetime")
                    }
                } else null
            }

            fun extractPageSize(query: Map<String, String>): Int {
                val size = query.getOrDefault("pagesize", "10").toIntOrNull()
                require(size != null) { "pageSize must be a positive integer" }
                return size
            }

            fun extractShowFailed(query: Map<String, String>): Boolean {
                return when (query.getOrDefault("showfailed", "true")) {
                    "false" -> false
                    else -> true
                }
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