package gov.cdc.prime.router.fhirengine.engine

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import gov.cdc.prime.reportstream.shared.QueueMessage
import gov.cdc.prime.router.Options
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.Topic
import gov.cdc.prime.router.azure.Event
import gov.cdc.prime.router.azure.QueueAccess
import java.util.UUID

/**
 * An interface for Messages to be put on an Azure Queue
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(FhirConvertQueueMessage::class, name = "convert"),
    JsonSubTypes.Type(FhirDestinationFilterQueueMessage::class, name = "destination-filter"),
    JsonSubTypes.Type(FhirReceiverFilterQueueMessage::class, name = "receiver-filter"),
    JsonSubTypes.Type(FhirTranslateQueueMessage::class, name = "translate"),
    JsonSubTypes.Type(BatchEventQueueMessage::class, name = "batch"),
    JsonSubTypes.Type(ProcessEventQueueMessage::class, name = "process"),
    JsonSubTypes.Type(ReportEventQueueMessage::class, name = "report")
)
abstract class PrimeRouterQueueMessage : QueueMessage {
    fun send(queueAccess: QueueAccess) {
        if (this.messageQueueName.isNotEmpty()) {
            queueAccess.sendMessage(this.messageQueueName, serialize())
        }
    }
}

abstract class ReportPipelineMessage :
    QueueMessage.ReportInformation,
    PrimeRouterQueueMessage()

@JsonTypeName("receive")
data class FhirConvertSubmissionQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    override val headers: Map<String, String> = emptyMap(),
) : ReportPipelineMessage(), QueueMessage.ReceiveInformation {
    override val messageQueueName = QueueMessage.Companion.elrSubmissionConvertQueueName
}

@JsonTypeName("convert")
data class FhirConvertQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    var topic: Topic,
    var schemaName: String = "",
) : ReportPipelineMessage() {
    override val messageQueueName = QueueMessage.Companion.elrConvertQueueName
}

@JsonTypeName("destination-filter")
data class FhirDestinationFilterQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    val topic: Topic,
) : ReportPipelineMessage() {
    override val messageQueueName = QueueMessage.Companion.elrDestinationFilterQueueName
}

@JsonTypeName("receiver-filter")
data class FhirReceiverFilterQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    val topic: Topic,
    val receiverFullName: String,
) : ReportPipelineMessage() {
    override val messageQueueName = QueueMessage.Companion.elrReceiverFilterQueueName
}

@JsonTypeName("translate")
data class FhirTranslateQueueMessage(
    override val reportId: ReportId,
    override val blobURL: String,
    override val digest: String,
    override val blobSubFolderName: String,
    val topic: Topic,
    val receiverFullName: String,
) : ReportPipelineMessage() {
    override val messageQueueName = QueueMessage.Companion.elrTranslationQueueName
}

abstract class WithEventAction : PrimeRouterQueueMessage() {
    abstract val eventAction: Event.EventAction
}

@JsonTypeName("batch")
data class BatchEventQueueMessage(
    override val eventAction: Event.EventAction,
    val receiverName: String,
    val emptyBatch: Boolean,
    val at: String,
) : WithEventAction() {
    override val messageQueueName = ""
}

@JsonTypeName("report")
data class ReportEventQueueMessage(
    override val eventAction: Event.EventAction,
    val emptyBatch: Boolean,
    val reportId: UUID,
    val at: String,
) : WithEventAction() {
    override val messageQueueName = QueueMessage.Companion.elrSendQueueName
}

@JsonTypeName("process")
data class ProcessEventQueueMessage(
    override val eventAction: Event.EventAction,
    val reportId: UUID,
    val options: Options,
    val defaults: Map<String, String>,
    val routeTo: List<String>,
    val at: String,
) : WithEventAction() {
    override val messageQueueName = ""
}

// Register submodule subtypes
fun registerPrimeRouterQueueMessageSubtypes() {
    QueueMessage.ObjectMapperProvider.registerSubtypes(
        FhirConvertQueueMessage::class.java,
        FhirDestinationFilterQueueMessage::class.java,
        FhirReceiverFilterQueueMessage::class.java,
        FhirTranslateQueueMessage::class.java,
        BatchEventQueueMessage::class.java,
        ProcessEventQueueMessage::class.java,
        ReportEventQueueMessage::class.java
    )
}

// Call this function at the appropriate initialization point
fun initializeQueueMessages() {
    registerPrimeRouterQueueMessageSubtypes()
}