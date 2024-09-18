package gov.cdc.prime.reportstream.shared

import com.fasterxml.jackson.annotation.JsonTypeName

@JsonTypeName("batch")
data class BatchEventQueueMessage(
    override val eventAction: EventAction,
    val receiverName: String,
    val emptyBatch: Boolean,
    val at: String,
) : WithEventAction() {
    override val messageQueueName = ""
}
