package gov.cdc.prime.reportstream.shared.queue_message

import com.fasterxml.jackson.annotation.JsonTypeName
import gov.cdc.prime.reportstream.shared.Topic
import java.util.UUID

@JsonTypeName("receiver-filter")
data class FhirReceiverFilterQueueMessage (
    override val reportId: UUID,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    val topic: Topic,
    val receiverFullName: String,
) : QueueMessage, QueueMessage.ReportInformation {
    override val messageQueueName = QueueMessage.elrReceiverFilterQueueName
}