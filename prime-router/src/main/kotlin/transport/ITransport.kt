package gov.cdc.prime.router.transport

import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.DatabaseAccess

interface ITransport {
    fun send(
        orgName: String,
        transportType: TransportType,
        header: DatabaseAccess.Header,
        contents: ByteArray
    ): Boolean
}