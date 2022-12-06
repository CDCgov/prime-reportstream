package gov.cdc.prime.router.messageTracker

import gov.cdc.prime.router.history.DetailedActionLog
import java.time.LocalDateTime

/**
 * A generic object used to retrieve messages from the
 * covid_result_metadata table or elr_result_metadata table (or any other table that may pop up).
 *
 * @property id the primary key for this message object
 * @property messageId the full message id
 * @property sender the name of the sender account
 * @property submittedDate when the original report that contains the message was submitted to ReportStream
 * @property reportId the uuid of the submitted report that a message belongs to
 * @property fileName the name of the file of the submitted report that a message belongs to
 * @property fileUrl the blobstore url of the file the message belongs to
 * @property warnings a list of warnings in the report
 * @property errors a list of errors in the report
 * @property receiverData a list of the message's receivers
 * */
data class Message(
    val id: Long,
    val messageId: String,
    val sender: String,
    val submittedDate: LocalDateTime,
    val reportId: String,
    val fileName: String?,
    val fileUrl: String?,
    val warnings: List<DetailedActionLog> = emptyList(),
    val errors: List<DetailedActionLog> = emptyList(),
    val receiverData: List<MessageReceiver>? = emptyList()
)

/**
 * Data about a Receiver of a Message
 *
 * @property reportId the uuid of the receiver report that a message belongs to
 * @property receivingOrg the name of the org receiving the message
 * @property receivingOrgSvc the name of the receiver's service receiving the message
 * @property transportResult the transport result between ReportStream and the receiving service
 * @property fileName the name of the file the message belongs to
 * @property fileUrl the blobstore url the of the file the message belongs to
 * @property createdAt the time of which the file/report was sent or created
 * @property qualityFilters the quality filters of the report that the message may have been filtered out from
 */
data class MessageReceiver(
    val reportId: String,
    val receivingOrg: String,
    val receivingOrgSvc: String,
    val transportResult: String?,
    val fileName: String?,
    val fileUrl: String?,
    val createdAt: LocalDateTime,
    val qualityFilters: List<MessageActionLog>? = emptyList()
)