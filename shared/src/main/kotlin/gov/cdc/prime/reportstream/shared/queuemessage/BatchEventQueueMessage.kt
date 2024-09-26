package gov.cdc.prime.reportstream.shared.queuemessage

import com.fasterxml.jackson.annotation.JsonTypeName
import gov.cdc.prime.reportstream.shared.EventAction

@JsonTypeName("batch")
data class BatchEventQueueMessage(
    override val eventAction: EventAction,
    val receiverName: String,
    val emptyBatch: Boolean,
    val at: String,
) : WithEventAction() {
    override val messageQueueName = ""
}