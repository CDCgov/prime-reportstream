package gov.cdc.prime.router.azure

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
import kotlin.reflect.full.memberProperties

const val NO_REPLY_EMAIL = "no-reply@cdc.gov"
const val REPORT_STREAM_EMAIL = "reportstream@cdc.gov"
const val TOS_SUBJECT_BASE = "TOS Agreement for "

/*INFO:
*  a TemplateID can be found by navigating to our SendGrid dashboard,
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
            val value: Any? = key.get(this)
            if (
                value is String &&
                !key.toString().contains("title") /* Title is not required */
            ) {
                if (!verifyIsNotBlank(key.toString(), value, logger) ||
                    !verifyNotExceededLimit(key.toString(), value, logger)) return false
            } else if (value !is String && value !is Boolean) {
                logger.info("Uh oh, \"$value\" is an invalid value at: \"$key\"")
                return false
            }
        }
        return verifyAgreed(logger)
    }

    private fun verifyAgreed(logger: Logger): Boolean {
        if (!this.agreedToTermsOfService) logger.info("Uh oh, your agreement to the Terms of Service is marked false")
        return this.agreedToTermsOfService
    }

    private fun verifyIsNotBlank(key: String, value: String, logger: Logger): Boolean {
        if (value.isBlank()) logger.info("Uh oh, \"$key\" in your request body is Null")
        return value.isNotBlank()
    }

    private fun verifyNotExceededLimit(key: String, value: String, logger: Logger): Boolean {
        if (value.length > 255) logger.info("Uh oh, \"$key\" has exceeded the character limit")
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
        val ret = request.createResponseBuilder(HttpStatus.BAD_REQUEST)

        if (request.body !== null) {
            logger.info(request.body)
            val body: TosAgreementForm? = parseBody(request.body!!, TosAgreementForm::class.java, logger)
            val mail: String? = createMail(body, logger)
            if (!mail.isNullOrBlank()) {
                ret.status(sendMail(mail, logger)) /* Status becomes whatever SendGrid returns */
            }
        }

        return ret.build()
    }

    /*TODO:
    *  This should turn into something that's returned with a dynamic class (second
    *  param) to parse many types of request bodies. Currently it only takes a single
    *  class, TosAgreementForm.
    */
    private fun parseBody(requestBody: String, type: Class<TosAgreementForm>, logger: Logger): TosAgreementForm? {
        val gson = Gson()

        return try {
            gson.fromJson(
                requestBody,
                type
            )
        } catch (ex: JsonSyntaxException) {
            /*TODO:
            *  For some reason, malformed requests were not throwing this exception when mapped
            *  and it lead to a bunch of errors when cleansing everything. This should be debugged
            *  but until then, returning null and checking against a null type is okay.
            */
            logger.info("There was an exception thrown when parsing your JSON")
            null
        }
    }

    private fun createMail(body: TosAgreementForm?, logger: Logger): String? {
        if (body === null) return null
        val mail: Mail = Mail()
        val p: Personalization = Personalization()

        /*TODO:
        *  To be a generalized function, we'd have to dictate the build sequence based on
        *  the type of body we get. In this case, we're building for TosAgreementForm.
        */
        if (body.validate(logger)) {
            mail.setTemplateId(TOS_AGREEMENT_TEMPLATE_ID)
            mail.setFrom(Email(NO_REPLY_EMAIL))
            mail.setSubject(TOS_SUBJECT_BASE + body.organizationName)
            p.addTo(Email(REPORT_STREAM_EMAIL))
            p.addCc(Email(body.email))
            p.addDynamicTemplateData("formData", body)
            mail.addPersonalization(p)

            return mail.build()
        }
        logger.info("Your body was not validated")
        return null
    }

    private fun sendMail(mail: String?, logger: Logger): HttpStatus {
        var status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
        val sendgridId: String? = SecretHelper.getSecretService().fetchSecret("SENDGRID_ID")

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
                status = HttpStatus.BAD_GATEWAY
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
            logger.info("Can't find SENDGRID_ID secret")
            logger.info(mail)
            return HttpStatus.NOT_FOUND
        }

        return status
    }
}