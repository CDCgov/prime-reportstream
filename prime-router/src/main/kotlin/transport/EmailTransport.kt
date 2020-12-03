package gov.cdc.prime.router.azure

import gov.cdc.prime.router.OrganizationService

class EmailTransport : Transport {

    override fun send(orgName: String, transport: OrganizationService.Transport, header: DatabaseAccess.Header, contents: ByteArray): Boolean {
        return true
    }
}
