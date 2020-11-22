package gov.cdc.prime.router.transport

import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.ReportQueue

interface Transport {
    fun send(service: OrganizationService, header: ReportQueue.Header, contents: ByteArray): Boolean
}