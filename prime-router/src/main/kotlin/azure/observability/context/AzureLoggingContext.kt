package gov.cdc.prime.router.azure.observability.context

import gov.cdc.prime.router.azure.observability.AzureCustomDimensionsSerializable

/**
 * Top level context interface for adding complex objects to the MDC
 */
interface AzureLoggingContext : AzureCustomDimensionsSerializable