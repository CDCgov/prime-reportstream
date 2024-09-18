package gov.cdc.prime.reportstream.shared

abstract class WithEventAction : QueueMessage {
    abstract val eventAction: EventAction
}