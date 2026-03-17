package gov.cdc.prime.router.transport

import com.microsoft.azure.functions.ExecutionContext
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import gov.cdc.prime.router.EmailTransportType
import gov.cdc.prime.router.ReportId
import gov.cdc.prime.router.TransportType
import gov.cdc.prime.router.azure.ActionHistory
import gov.cdc.prime.router.azure.WorkflowEngine
import gov.cdc.prime.router.azure.db.tables.pojos.ItemLineage
import gov.cdc.prime.router.azure.observability.event.IReportStreamEventService
import gov.cdc.prime.router.report.ReportService
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.StringTemplateResolver
import java.nio.file.Files
import java.nio.file.Path
import java.util.Calendar
import java.util.logging.Level

class EmailTransport : ITransport {

    override fun send(
        transportType: TransportType,
        header: WorkflowEngine.Header,
        sentReportId: ReportId,
        externalFileName: String,
        retryItems: RetryItems?,
        context: ExecutionContext,
        actionHistory: ActionHistory, // not used by emailer
        reportEventService: IReportStreamEventService,
        reportService: ReportService,
        lineages: List<ItemLineage>?,
        queueMessage: String,
    ): RetryItems? {
        val emailTransport = transportType as EmailTransportType
        val content = buildContent(header)
        val mail = buildMail(content, emailTransport)

        try {
            val sg = SendGrid(System.getenv("SENDGRID_API_KEY"))
            val request = Request()
            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail.build()
            sg.api(request)
        } catch (ex: Exception) {
            context.logger.log(Level.SEVERE, "Email/SendGrid exception", ex)
            return RetryToken.allItems
        }
        return null
    }

    fun getTemplateEngine(): TemplateEngine {
        val templateEngine = TemplateEngine()
        val stringTemplateResolver = StringTemplateResolver()
        templateEngine.setTemplateResolver(stringTemplateResolver)
        return templateEngine
    }

    fun getTemplateFromAttributes(htmlContent: String, attr: Map<String, Any>): String {
        val templateEngine = getTemplateEngine()
        val context = Context()
        attr.forEach { (k, v) -> context.setVariable(k, v) }
        return templateEngine.process(htmlContent, context)
    }

    fun buildContent(header: WorkflowEngine.Header): Content {
        val htmlTemplate = Files.readString(Path.of("./assets/email-templates/test-results-ready__inline.html"))

        val attr = mapOf(
            "today" to Calendar.getInstance(),
            "file" to header.reportFile.reportId
        )

        val html = getTemplateFromAttributes(htmlTemplate, attr)
        val content = Content("text/html", html)
        return content
    }

    fun buildMail(content: Content, emailTransport: EmailTransportType): Mail {
        val subject = "COVID-19 Reporting:  Your test results are ready"

        val mail = Mail()
        mail.setFrom(Email(emailTransport.from))
        mail.setSubject(subject)
        mail.addContent(content)
        mail.setReplyTo(Email("noreply@cdc.gov"))
        val personalization = Personalization()
        emailTransport.addresses.forEach {
            personalization.addTo(Email(it))
        }
        mail.addPersonalization(personalization)
        return mail
    }
}