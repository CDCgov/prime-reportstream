package gov.cdc.prime.router.transport
<<<<<<< HEAD

=======
>>>>>>> 714c9675eaeaa2acab01e8495c93a41cf601c373
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.DatabaseAccess

interface ITransport {
    fun send(
        orgName: String,
        transport: OrganizationService.Transport,
        header: DatabaseAccess.Header,
        contents: ByteArray
    ): Boolean
}