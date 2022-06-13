package gov.cdc.prime.router.history.azure

import java.time.OffsetDateTime

interface ReportFileAccess {
    /**
     * Values that results can be sorted by.
     */
    enum class SortDir {
        DESC,
        ASC,
    }

    /* As sorting Submission / Delivery results expands, we can add
    * column names to this enum. Make sure the column you
    * wish to sort by is indexed. */
    enum class SortColumn {
        CREATED_AT
    }

    /**
     * Get multiple results based on a particular organization.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param orgService is a specifier for an organization, such as the client or service used to send/receive
     * @param sortDir sort the table in ASC or DESC order.
     * @param sortColumn sort the table by specific column; default created_at.
     * @param cursor is the OffsetDateTime of the last result in the previous list.
     * @param since is the OffsetDateTime that dictates how far back returned results date.
     * @param until is the OffsetDateTime that dictates how recently returned results date.
     * @param pageSize is an Integer used for setting the number of results per page.
     * @param showFailed whether to include actions that failed to be sent.
     * @param klass the class that the found data will be converted to.
     * @return a list of results matching the SQL Query.
     */
    fun <T> fetchActions(
        organization: String,
        orgService: String?,
        sortDir: SortDir,
        sortColumn: SortColumn,
        cursor: OffsetDateTime?,
        since: OffsetDateTime?,
        until: OffsetDateTime?,
        pageSize: Int,
        showFailed: Boolean,
        klass: Class<T>
    ): List<T>

    /**
     * Fetch a single (usually detailed) action of a specific type.
     *
     * @param organization is the Organization Name returned from the Okta JWT Claim.
     * @param actionId the action id attached to this submission.
     * @param klass the class that the found data will be converted to.
     * @return the submission matching the given query parameters, or null.
     */
    fun <T> fetchAction(
        organization: String,
        actionId: Long,
        klass: Class<T>
    ): T?

    /**
     * Fetch the details of an action's relations (descendants).
     *
     * @param actionId the action id attached to the action to find relations for.
     * @param klass the class that the found data will be converted to.
     * @return a list of descendants for the given action id.
     */
    fun <T> fetchRelatedActions(
        actionId: Long,
        klass: Class<T>
    ): List<T>
}