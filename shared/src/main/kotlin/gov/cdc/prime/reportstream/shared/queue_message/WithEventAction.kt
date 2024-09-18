package gov.cdc.prime.reportstream.shared.queue_message

import gov.cdc.prime.reportstream.shared.EventAction

abstract class WithEventAction : QueueMessage {
    abstract val eventAction: EventAction
}