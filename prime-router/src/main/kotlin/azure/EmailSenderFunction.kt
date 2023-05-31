package gov.cdc.prime.router.azure

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

var NO_REPLY_EMAIL = "no-reply@reportstream.gov"
var REPORT_STREAM_EMAIL = "reportstream@cdc.gov"
const val TOS_SUBJECT_BASE = "TOS Agreement for "

/*INFO:
*  A TemplateID can be found by navigating to our SendGrid dashboard,
*  expanding the Email API nav list on the left and clicking
*  Dynamic Templates. The list will show templates with IDs
*/
const val TOS_AGREEMENT_TEMPLATE_ID = "d-472779cf554f418a9209acb62d2a48da"

data class TosAgreementForm(
    val title: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val territory: String,
    val organizationName: String,
    val operatesInMultipleStates: Boolean,
    val agreedToTermsOfService: Boolean
) {
    fun validate(logger: Logger): Boolean {
        for (key in TosAgreementForm::class.memberProperties) {
            val value = key.get(this)
            when (key.name) {
                "title" -> if (!verifyNotExceededLimit(key, value as String, logger)) return false
                "firstName" -> if (!verifyBoth(key, value as String, logger)) return false
                "lastName" -> if (!verifyBoth(key, value as String, logger)) return false
                "email" -> if (!verifyBoth(key, value as String, logger)) return false
                "territory" -> if (!verifyBoth(key, value as String, logger)) return false
                "organizationName" -> if (!verifyBoth(key, value as String, logger)) return false
                "agreedToTermsOfService" -> if (!verifyAgreed(logger)) return false
            }
        }
        return verifyAgreed(logger)
    }

    private fun verifyAgreed(logger: Logger): Boolean {
        if (!this.agreedToTermsOfService) logger.info("Uh oh, your agreement to the Terms of Service is marked false")
        return this.agreedToTermsOfService
    }

    private fun verifyBoth(key: KProperty1<TosAgreementForm, *>, value: String, logger: Logger): Boolean {
        if (!verifyIsNotBlank(key, value, logger) ||
            !verifyNotExceededLimit(key, value, logger)
        ) return false
        return true
    }

    private fun verifyIsNotBlank(key: KProperty1<TosAgreementForm, *>, value: String, logger: Logger): Boolean {
        if (value.isBlank()) logger.info("Uh oh, \"${key.name}\" in your request body is Null")
        return value.isNotBlank()
    }

    private fun verifyNotExceededLimit(key: KProperty1<TosAgreementForm, *>, value: String, logger: Logger): Boolean {
        if (value.length > 255) logger.info("Uh oh, \"${key.name}\" has exceeded the character limit")
        return value.length <= 255
    }
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
        val ret = request.createResponseBuilder(HttpStatus.UNAUTHORIZED)

        /* Body existence check */
        if (request.body === null) return ret.status(HttpStatus.BAD_REQUEST).build()

        /* Body shape check */
        val body: TosAgreementForm = parseBody(request.body!!, logger)
            ?: return ret.status(HttpStatus.BAD_REQUEST).build()

        /* Body content check */
        if (!body.validate(logger)) return ret.status(HttpStatus.BAD_REQUEST).build()
        logger.info(request.body)

        /* Body exists and has all required properties; sendMail's response decides the outcome of this */
        val sendgridId: String? = SecretHelper.getSecretService().fetchSecret("SENDGRID_ID")
        val mail: String = createMail(body)
        ret.status(sendMail(mail, sendgridId, logger)) /* Status becomes whatever SendGrid returns */
        return ret.build()
    }

    /*TODO:
    *  This should turn into something that's returned with a dynamic class (second
    *  param) to parse many types of request bodies. Currently it only takes a single
    *  class, TosAgreementForm.
    */
    private fun parseBody(requestBody: String, logger: Logger): TosAgreementForm? {
        return try {
            jacksonObjectMapper().readValue<TosAgreementForm>(requestBody, TosAgreementForm::class.java)
        } catch (ex: MismatchedInputException) {
            logger.info("There was an exception thrown when parsing your JSON")
            null
        }
    }

    private fun createMail(body: TosAgreementForm): String {
        val mail: Mail = Mail()
        val p: Personalization = Personalization()

        /*TODO:
        *  To be a generalized function, we'd have to dictate the build sequence based on
        *  the type of body we get. In this case, we're building for TosAgreementForm.
        */
        mail.setTemplateId(TOS_AGREEMENT_TEMPLATE_ID)
        mail.setFrom(Email(NO_REPLY_EMAIL))
        mail.setReplyTo(Email(REPORT_STREAM_EMAIL))
        mail.setSubject(TOS_SUBJECT_BASE + body.organizationName)
        p.addTo(Email(REPORT_STREAM_EMAIL))
        p.addCc(Email(body.email))
        p.addDynamicTemplateData("formData", body)
        mail.addPersonalization(p)

        return mail.build()
    }

    private fun sendMail(mail: String?, sendgridId: String?, logger: Logger): HttpStatus {
        var status: HttpStatus = HttpStatus.NOT_FOUND

        if (!sendgridId.isNullOrBlank() && !mail.isNullOrBlank()) {
            var response: Response = Response()
            val sg: SendGrid = SendGrid(sendgridId)
            val request: Request = Request()

            request.method = Method.POST
            request.endpoint = "mail/send"
            request.body = mail

            try {
                response = sg.api(request)
            } catch (ex: IOException) {
                logger.warning("Can't contact sendgrid")
                return HttpStatus.BAD_GATEWAY
            } finally {
                logger.info("sending email - result ${response.statusCode}")
                status = HttpStatus.valueOf(response.statusCode)
                if (!(200..299).contains(response.statusCode)) {
                    logger.severe("error - ${response.body}")
                }
            }
        } else if (mail.isNullOrBlank()) {
            logger.info("Error in the createMail() function")
            return HttpStatus.BAD_REQUEST
        } else if (sendgridId.isNullOrBlank()) {
            logger.warning("Can't find SENDGRID_ID secret")
            logger.info(mail)
            return HttpStatus.NOT_FOUND
        }

        return status
    }
}