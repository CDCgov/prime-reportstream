package gov.cdc.prime.reportstream.shared

import com.fasterxml.jackson.annotation.JsonTypeName
import java.util.UUID

@JsonTypeName("convert")
data class FhirConvertQueueMessage (
    override val reportId: UUID,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    var topic: Topic,
    var schemaName: String = "",
) : QueueMessage, QueueMessage.ReportInformation {
    override val messageQueueName = QueueMessage.elrReceiveQueueName
}