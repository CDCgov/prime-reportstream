package gov.cdc.prime.reportstream.shared.queue_message

import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.UUID

@JsonTypeName("receive")
data class FhirReceiveQueueMessage (
    override val reportId: UUID,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val headers: Map<String, String> = emptyMap()
) : QueueMessage, QueueMessage.ReportInformation, QueueMessage.ReceiveInformation {
    override val messageQueueName = QueueMessage.elrReceiveQueueName
}