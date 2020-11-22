package gov.cdc.prime.router.transport

import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.DatabaseAccess

interface Transport {
    fun send(service: OrganizationService, header: DatabaseAccess.Header, contents: ByteArray): Boolean
}