package gov.cdc.prime.router.transport

import com.sendgrid.*
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.DatabaseAccess

class EmailTransport : ITransport {

    override fun send(
        orgName: String,
        transport: OrganizationService.Transport,
        header: DatabaseAccess.Header,
        contents: ByteArray
    ): Boolean {

        val sg: SendGrid = SendGrid(System.getenv("SENDGRID_API_KEY"))
        val request = Request()
        request.setMethod(Method.POST)
        request.setEndpoint("mail/send")
        request.setBody("{to: 'matthew.young@bisonworks.com' }")
        /* 
            from: 'qtv1@cdc.gov',
            subject: 'Sending is fun',
            text: 'this is text',
            html: '<strong>this is html</strong>'
        }")*/
        val response = sg.api(request)
        System.out.println(response.getStatusCode())
        System.out.println(response.getBody())
        System.out.println(response.getHeaders())
        return true
    }
}