package gov.cdc.prime.reportstream.shared.queuemessage

import gov.cdc.prime.reportstream.shared.EventAction

abstract class WithEventAction : QueueMessage {
    abstract val eventAction: EventAction
}