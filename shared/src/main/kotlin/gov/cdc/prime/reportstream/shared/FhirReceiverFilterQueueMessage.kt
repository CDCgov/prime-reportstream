package gov.cdc.prime.reportstream.shared

import com.fasterxml.jackson.annotation.JsonTypeName
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
    override val messageQueueName = QueueMessage.Companion.elrReceiverFilterQueueName
}