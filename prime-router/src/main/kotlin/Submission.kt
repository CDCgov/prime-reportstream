package gov.cdc.prime.router

/**
 * A `Submission` represents one submission of a message from a sender.
 *
 * @param actionId of the submission
 */
open class Submission(
    val actionId: Long
) {
    constructor(copy: Submission) : this(
        copy.actionId,
    )
}