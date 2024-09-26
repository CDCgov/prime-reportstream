package gov.cdc.prime.reportstream.shared.queuemessage

import com.fasterxml.jackson.annotation.JsonTypeName
import gov.cdc.prime.reportstream.shared.EventAction
import gov.cdc.prime.reportstream.shared.ReportOptions
import java.util.UUID

@JsonTypeName("process")
class ProcessEventQueueMessage(
    override val eventAction: EventAction,
    val reportId: UUID,
    val options: ReportOptions,
    val defaults: Map<String, String>,
    val routeTo: List<String>,
    val at: String,
) : WithEventAction() {
    override val messageQueueName = ""
}