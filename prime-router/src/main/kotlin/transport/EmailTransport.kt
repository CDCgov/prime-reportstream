package gov.cdc.prime.router.transport

import com.sendgrid.*
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.*
import gov.cdc.prime.router.OrganizationService
import gov.cdc.prime.router.azure.DatabaseAccess
import java.nio.file.Files
import java.nio.file.Path
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.StringTemplateResolver
import org.thymeleaf.context.Context
import java.util.Calendar


class EmailTransport : ITransport {

    override fun send(
        orgName: String,
        transport: OrganizationService.Transport,
        header: DatabaseAccess.Header,
        contents: ByteArray
    ): Boolean {

        val emailTransport = transport as OrganizationService.Email

        val from = Email("qtv1@cdc.gov")
        val subject = "COVID-19 Reporting:  Your test results are ready"
        

        val htmlTemplate = Files.readString(Path.of("./assets/email-templates/test-results-ready__inline.html"))

        val attr = mapOf( 
            "countyName" to "Pima",
            "stateName" to "AZ",
            "today" to Calendar.getInstance(),
            "file" to "123"
        )

        val html = getTemplateFromAttributes(htmlTemplate, attr)

        val content = Content("text/html", html)
        val mail = Mail();
        mail.setFrom( from )
        mail.setSubject( subject );
        mail.addContent( content );
        mail.setReplyTo( Email("noreply@cdc.gov"))
        val personalization = Personalization();
        emailTransport.addresses.forEach{
            personalization.addTo( Email( it ) )
        } 
        mail.addPersonalization(personalization);

        val sg = SendGrid(System.getenv("SENDGRID_API_KEY"))
        val request = Request()
        request.setMethod(Method.POST)
        request.setEndpoint("mail/send")
        request.setBody(mail.build())
        val response = sg.api(request)
        return response.statusCode == 202
    }

    fun getTemplateEngine(): TemplateEngine {
        val templateEngine = TemplateEngine();
        val stringTemplateResolver = StringTemplateResolver();
        templateEngine.setTemplateResolver(stringTemplateResolver);
        return templateEngine;
    }   
    
    fun getTemplateFromAttributes(htmlContent:String, attr: Map<String, Any>) : String
     {
            val templateEngine = getTemplateEngine();
            val context = Context();
            attr.forEach{ (k,v) -> context.setVariable(k, v) };
            return templateEngine.process(htmlContent, context);        
    }


}