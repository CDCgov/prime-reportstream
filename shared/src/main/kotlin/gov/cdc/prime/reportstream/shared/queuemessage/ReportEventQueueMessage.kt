package gov.cdc.prime.reportstream.shared.queuemessage

import com.fasterxml.jackson.annotation.JsonTypeName
import gov.cdc.prime.reportstream.shared.EventAction
import java.util.UUID

@JsonTypeName("report")
data class ReportEventQueueMessage(
    override val eventAction: EventAction,
    val emptyBatch: Boolean,
    val reportId: UUID,
    val at: String,
) : WithEventAction() {
    override val messageQueueName = QueueMessage.elrSendQueueName
}