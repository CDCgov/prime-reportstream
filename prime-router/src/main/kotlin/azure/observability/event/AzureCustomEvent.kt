package gov.cdc.prime.router.azure.observability.event

import gov.cdc.prime.router.azure.observability.AzureCustomDimensionsSerializable

/**
 * Top level event interface for all custom events to be emitted to the
 * Azure AppInsights customEvents table
 *
 * An event's class name is used as the event name in the customEvents table
 *
 * The properties of an event will be serialized and will be able to be queried
 * against in the Azure Log Explorer
 */
interface AzureCustomEvent : AzureCustomDimensionsSerializable