<<<<<<< HEAD
package gov.cdc.prime.router.transport

import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.DatabaseAccess

class EmailTransport : ITransport {
=======
package gov.cdc.prime.router.azure

import gov.cdc.prime.router.OrganizationService

class EmailTransport : Transport {
>>>>>>> allow for multiple transports

    override fun send(orgName: String, transport: OrganizationService.Transport, header: DatabaseAccess.Header, contents: ByteArray): Boolean {
        return true
    }
}
