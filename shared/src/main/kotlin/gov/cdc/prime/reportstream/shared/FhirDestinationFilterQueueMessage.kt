package gov.cdc.prime.reportstream.shared

import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.UUID

@JsonTypeName("destination-filter")
data class FhirDestinationFilterQueueMessage (
    override val reportId: UUID,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    val topic: Topic,
): QueueMessage, QueueMessage.ReportInformation {
    override val messageQueueName = QueueMessage.elrDestinationFilterQueueName
}