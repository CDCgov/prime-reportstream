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
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import gov.cdc.prime.router.secrets.SecretHelper
import java.io.IOException
import java.util.logging.Logger

const val NO_REPLY_EMAIL = "no-reply@cdc.gov"
const val REPORT_STREAM_EMAIL = "reportstream@cdc.gov"
const val TOS_SUBJECT_BASE = "TOS Agreement for "

/*INFO:
*  a TemplateID can be found by navigating to our SendGrid dashboard,
*  expanding the Email API nav list on the left and clicking
*  Dynamic Templates. The list will show templates with IDs
*/
const val TOS_AGREEMENT_TEMPLATE_ID = "d-472779cf554f418a9209acb62d2a48da"

class TosAgreement {
    data class TosAgreementFormData(
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
            ret.status(sendMail(request.body!!, logger))
        }

        return ret.build()
    }

    private fun trimStrings(body: TosAgreement.TosAgreementFormData) {
        
    }

    private fun parseBody(requestBody: String): TosAgreement.TosAgreementFormData? {
        val gson = Gson()
        val tosAgreement = TosAgreement.TosAgreementFormData::class.java

        /*TODO:
        *  This should turn into something that's returned with a dynamic class (second
        *  param) to parse many types of request bodies.
        */
        return gson.fromJson<TosAgreement.TosAgreementFormData>(
            requestBody,
            tosAgreement
        )
    }

    private fun createMail(requestBody: String): String? {
        val body = parseBody(requestBody)
        val mail: Mail = Mail()
        val p: Personalization = Personalization()

        /*TODO:
        *  I want to turn this block into something that is dynamically set via some
        *  param we pass in. For now, though, this will handle the TOS mail construction.
        */
        mail.setTemplateId(TOS_AGREEMENT_TEMPLATE_ID)
        mail.setFrom(Email(NO_REPLY_EMAIL))
        mail.setSubject(TOS_SUBJECT_BASE + body?.organizationName)
        p.addTo(Email(REPORT_STREAM_EMAIL))
        p.addCc(Email(body?.email))
        p.addDynamicTemplateData("formData", body)
        mail.addPersonalization(p)

        return mail.build()
    }

    private fun sendMail(requestBody: String, logger: Logger): HttpStatus {
        var response: Response = Response()
        var status: HttpStatus = HttpStatus.NOT_FOUND
        val mail = createMail(requestBody)
        val sendgridId: String? = SecretHelper.getSecretService().fetchSecret("SENDGRID_ID")

        if (sendgridId !== null) {
            val sg: SendGrid = SendGrid(sendgridId)
            val request: Request = Request()

            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail

            try {
                response = sg.api(request)
            } catch (ex: IOException) {
                logger.warning("Can't contact sendgrid")
                status = HttpStatus.BAD_GATEWAY
            } finally {
                logger.info("sending email - result ${response.statusCode}")
                status = HttpStatus.valueOf(response.statusCode)
                if (!(200..299).contains(response.statusCode)) {
                    logger.severe("error - ${response.body}")
                }
            }
        } else {
            logger.info("Can't find SENDGRID_ID secret")
            logger.info(mail)
        }

        return status
    }
}