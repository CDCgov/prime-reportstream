package gov.cdc.prime.router.azure

import com.google.gson.Gson
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import gov.cdc.prime.router.secrets.SecretHelper
import java.io.IOException
import java.util.logging.Logger

const val NO_REPLY_EMAIL = "no-reply@cdc.gov"
const val REGISTER_EMAIL = "reportstream@cdc.gov"
const val REGISTER_SUBJECT = "Thank you for registering with ReportStream!"

class SenderTosRequest {
    data class SenderTosFormJSON(
        val title: String,
        val firstName: String,
        val lastName: String,
        val email: String,
        val territory: String,
        val organizationName: String,
        val operatesInMultipleStates: Boolean,
        val agreedToTermsOfService: Boolean
    )
}

class EmailSenderFunction {

    @FunctionName("emailRegisteredOrganization")
    @StorageAccount("AzureWebJobsStorage")
    fun emailRegisteredOrganization(
        @HttpTrigger(
            name = "emailRegisteredOrganization",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "email-registered"
        )
        request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        val logger: Logger = context.logger
        var ret = request.createResponseBuilder(HttpStatus.BAD_REQUEST)

        if (request.body !== null) {
            logger.info(request.body)
//            ret.status(sendRegistrationConfirmation(request.body!!, logger))
            ret.body(request.body)
        }

        return ret.build()
    }

    private fun sendRegistrationConfirmation(requestBody: String, logger: Logger): HttpStatus {
        val gson = Gson()
        val body = gson.fromJson<SenderTosRequest.SenderTosFormJSON>(
            requestBody,
            SenderTosRequest.SenderTosFormJSON::class.java
        )
        val title = body.title
        val firstName = body.firstName
        val lastName = body.lastName
        val email = body.email
        val territory = body.territory
        val organizationName = body.organizationName
        val operatesInMultipleStates = body.operatesInMultipleStates
        val agreedToTermsOfService = body.agreedToTermsOfService

        var status: HttpStatus = HttpStatus.NOT_FOUND
        val content: Content = Content()
        content.type = "plain/text"

        val p: Personalization = Personalization()
        p.addTo(Email(REGISTER_EMAIL))
        p.addCc(Email(email))

        val mail: Mail = Mail()
        mail.addPersonalization(p)
        mail.setSubject(REGISTER_SUBJECT)
        mail.setFrom(Email(NO_REPLY_EMAIL))
        mail.addContent(content)

        var sendgridId: String? = SecretHelper.getSecretService().fetchSecret("SENDGRID_ID")
        var response: Response = Response()

        if (sendgridId !== null) {

            val sg: SendGrid = SendGrid(sendgridId)
            val request: Request = Request()

            try {
                request.method = Method.POST
                request.endpoint = "mail/send"
                request.body = mail.build()
                response = sg.api(request)
            } catch (ex: IOException) {
                logger.warning("Can't contact sendgrid")
                status = HttpStatus.BAD_GATEWAY
            } finally {
                logger.info("sending to $email - result ${response.statusCode}")
                status = HttpStatus.valueOf(response.statusCode)
                if (!(200..299).contains(response.statusCode)) {
                    logger.severe("error - ${response.body}")
                }
            }
        } else {
            logger.info("Can't find SENDGRID_ID secret")
        }

        return status
    }
}