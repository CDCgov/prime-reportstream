package gov.cdc.prime.router.azure

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.json.responseJson
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import com.microsoft.azure.functions.annotation.TimerTrigger
import com.okta.jwt.JwtVerifiers
import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.Response
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import gov.cdc.prime.router.azure.db.enums.SettingType
import gov.cdc.prime.router.common.BaseEngine
import gov.cdc.prime.router.secrets.SecretHelper
import gov.cdc.prime.router.tokens.oktaMembershipClaim
import gov.cdc.prime.router.tokens.oktaSystemAdminGroup
import gov.cdc.prime.router.tokens.subjectClaim
import org.json.JSONObject
import java.io.IOException
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.logging.Level
import java.util.logging.Logger

const val SCHEDULE = "*/5 * * * *" // every 5 minutes
const val OKTA_ISSUER = "https://hhs-prime.okta.com/oauth2/default"
const val OKTA_GROUPS_API = "https://hhs-prime-admin.okta.com/api/v1/groups"
const val FROM_EMAIL = "reportstream@cdc.gov"
const val SUBJECT_EMAIL = "ReportStream Daily Email"
const val FIVE_MINUTES_IN_SECONDS = 5 * 60
const val AUTH_KEY = "Bearer "

data class EmailSchedule(
    val template: String,
    val type: String,
    val cronSchedule: String,
    val organizations: List<String> = ArrayList<String>(),
    val emails: List<String> = ArrayList<String>(),
    val parameters: Map<String, String> = HashMap<String, String>()
)

class EmailScheduleEngine {

    val workflowEngine = WorkflowEngine()

    /** Create Email Schedule */
    @FunctionName("createEmailSchedule")
    @StorageAccount("AzureWebJobsStorage")
    fun createEmailSchedule(
        @HttpTrigger(
            name = "createEmailSchedule",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "email-schedule"
        )
        request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        var user: String? = validateUser(request.headers["authorization"] ?: "", context.logger)
        var ret = request.createResponseBuilder(HttpStatus.UNAUTHORIZED)

        if (!user.isNullOrEmpty()) {
            val id = BaseEngine.databaseAccessSingleton.insertEmailSchedule(request.body, user)
            ret.status(HttpStatus.CREATED)
            ret.body("$id")
        }
        return ret.build()
    }

    /** Delete (inactivate) Email Schedule */
    @FunctionName("deleteEmailSchedule")
    @StorageAccount("AzureWebJobsStorage")
    fun deleteEmailSchedule(
        @HttpTrigger(
            name = "deleteEmailSchedule",
            methods = [HttpMethod.DELETE],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "email-schedule/{scheduleId}"
        )
        request: HttpRequestMessage<String?>,
        @BindingName("scheduleId") scheduleId: Int,
        context: ExecutionContext,
    ): HttpResponseMessage {
        var user: String? = validateUser(request.headers["authorization"] ?: "", context.logger)
        var ret = request.createResponseBuilder(HttpStatus.UNAUTHORIZED)

        if (!user.isNullOrEmpty()) {
            val id = BaseEngine.databaseAccessSingleton.deleteEmailSchedule(scheduleId)
            ret.status(HttpStatus.OK)
            ret.body("$id")
        }
        return ret.build()
    }

    /** Timer Trigger to fire when processing schedules (fires every 5 minutes) */
    @FunctionName("emailScheduleEngine")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @Suppress("UNUSED_PARAMETER")
        @TimerTrigger(name = "emailScheduleEngine", schedule = SCHEDULE)
        timerInfo: String,
        context: ExecutionContext
    ) {
        val logger: Logger = context.logger
        val mapper = ObjectMapper().registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )

        // get the schedules to fire
        val schedulesToFire: Iterable<EmailSchedule> =
            BaseEngine.databaseAccessSingleton
                .fetchEmailSchedules()
                .map { mapper.readValue<EmailSchedule>(it) }
                .filter { shouldFire(it) }

        schedulesToFire.forEach { schedule ->
            val orgs: Iterable<String> = getOrgs(schedule)

            logger.info("EmailEngineFunction:: processing schedule ${schedule.template}")

            // get the orgs to fire for
            orgs.forEach { org ->
                val countOfRecords =
                    workflowEngine.db.fetchDownloadableReportFiles(
                        OffsetDateTime.now().minusDays(1L),
                        org
                    )
                        .size
                val template = getTemplate(schedule.template, countOfRecords, logger)
                val emails: Iterable<String> =
                    if (schedule.emails.size > 0) schedule.emails else getEmails(org, logger)
                logger.info("EmailEngineFunction:: processing organization $org within $template")
                emails.forEach { email ->
                    logger.info(
                        "EmailEngineFunction:: sending email template $template to $email"
                    )
                    dispatchToSendGrid(template, listOf(email), logger)
                }
            }
        }
    }

    /**
     * Get the template to use. A template comes in 2 forms, either a straight UUID or a pair of
     * UUIDs separated by a ':' (the part on the left representing the 'we have results', and the
     * part on the right representing the 'we dont have results')
     *
     * @param template the template string from the database (i.e d-12345678 or
     * d-12345678:d-87654321)
     * @param count of records found in the last 24hrs
     *
     * @returns template name to use
     */
    private fun getTemplate(template: String, count: Int, logger: Logger): String {
        var retValue = template
        val split = template.split(":")

        if (split.size > 2) logger.warning("More than one :, taking only the first 2 templates")

        if (split.size > 1) retValue = if (count == 0) split[1].trim() else split[0]
        return retValue
    }

    /**
     * Determine if a schedule should fire
     *
     * @param schedule to check if it should fire
     *
     * @returns true if the given schedule should fire; false otherwise
     */
    private fun shouldFire(schedule: EmailSchedule): Boolean {

        val parser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))
        // Get date for last execution
        val now = ZonedDateTime.now()
        val executionTime = ExecutionTime.forCron(parser.parse(schedule.cronSchedule))
        val timeFromLastExecution = executionTime.timeFromLastExecution(now)

        /*
        So, the timer function doesn't fire on exactly every 5 minutes, so this is the "catch" at assure that anytime within
        less than 5 minutes of the last timer execution is valid (i.e. timer trigger fires at 11:59 but the cron for the
        schedule is at noon, without this, they don't "line up" and the schedule is always reported as shouldFire = false)
        This accounts for a difference in cron timers (Azure's and the one referenced here)
        */
        return (timeFromLastExecution.get().toSeconds() < FIVE_MINUTES_IN_SECONDS)
    }

    /**
     * Retreives the organization to send the emailschedule to
     *
     * @param schedule the schedule to check organizations against
     *
     * @returns List of organizations for the schedule
     */
    private fun getOrgs(schedule: EmailSchedule): Iterable<String> {
        return (if (schedule.organizations.size > 0) schedule.organizations else fetchAllOrgs())
    }

    /**
     * Retrieves the list of all organization supported
     *
     * @returns List of all organizations supported
     */
    private fun fetchAllOrgs(): Iterable<String> {
        @Suppress("NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER")
        return workflowEngine.db.transactReturning { tx ->
            @Suppress("UNRESOLVED_REFERENCE")
            val settings = workflowEngine.db.fetchSettings(SettingType.ORGANIZATION, tx)
            @Suppress("NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER") settings.map { it.getName() }
        }
    }

    /**
     * Validates the JWT Token supplied in the authorization header. To be valid, the token must be
     * from okta and must be part of the DHPrimeAdmins group
     *
     * @param request the HTTPRequest object
     * @param logger logger
     *
     * @returns user from the token; otherwise null
     *
     * @todo Consolidate Authentication and claims processing #1594
     */
    fun validateUser(requestToken: String, logger: Logger): String? {

        val jwtToken =
            if (requestToken.length > AUTH_KEY.length) requestToken.substring(AUTH_KEY.length)
            else ""

        var user: String? = null

        if (jwtToken.isNotBlank()) {
            try {
                // get the access token verifier
                val jwtVerifier =
                    JwtVerifiers.accessTokenVerifierBuilder().setIssuer(OKTA_ISSUER).build()
                // get it to decode the token from the header
                val jwt =
                    jwtVerifier.decode(jwtToken)
                        ?: throw Throwable("Error in validation of jwt token")

                // get the user name
                @Suppress("UNCHECKED_CAST")
                user =
                    if ((jwt.claims[oktaMembershipClaim] as List<String>).contains(oktaSystemAdminGroup))
                        jwt.claims[subjectClaim].toString()
                    else null
            } catch (ex: Throwable) {
                logger.log(Level.WARNING, "Error in verification of token", ex)
            }
        }
        return user
    }

    /**
     * Converts an organization name to an OKTA group name
     *
     * @param org organization name (ex. pima-az-phd )
     *
     * @returns encoded org name (ex. DHpima_az_phd )
     */
    private fun encodeOrg(org: String): String {
        return "DH" + org.replace("-", "_")
    }

    /**
     * Retrieve a list of emails within an organization
     *
     * @params org The organization to fetch emails for
     *
     * @returns List of emails to send to
     */
    private fun getEmails(org: String, logger: Logger): List<String> {

        var emails: MutableList<String> = mutableListOf()

        try {

            var ssws: String? = SecretHelper.getSecretService().fetchSecret("SSWS_OKTA")

            if (ssws !== null) {

                var grp = encodeOrg(org)

                // get the OKTA Group Id
                var (_, _, response1) = Fuel.get("$OKTA_GROUPS_API?q=$grp")
                    .header(mapOf("Authorization" to "SSWS $ssws")).responseJson()
                var grpId = ((response1.get().array()).get(0) as JSONObject).getString("id")

                // get the users within that OKTA group
                var (_, _, response) = Fuel.get("$OKTA_GROUPS_API/$grpId/users")
                    .header(mapOf("Authorization" to "SSWS $ssws")).responseJson()

                for (user in response.get().array()) emails.add(
                    (user as JSONObject).getJSONObject("profile").getString("email")
                )
            }
        } catch (ex: Throwable) {
            logger.warning("Error in fetching emails")
            emails = mutableListOf()
        }

        return emails
    }

    /**
     * Dispatches the select emails (using the desired template) to SENDGRID
     *
     * @param template the name of the template (in sendgrid) to use
     * @param emails list of emails to send to
     * @param logger context logger
     */
    private fun dispatchToSendGrid(template: String, emails: Iterable<String>, logger: Logger) {
        val p: Personalization = Personalization()
        emails.forEach { to -> p.addTo(Email(to)) }
        p.setSubject(SUBJECT_EMAIL)

        val mail: Mail = Mail()
        mail.addPersonalization(p)
        mail.setSubject(SUBJECT_EMAIL)
        mail.setFrom(Email(FROM_EMAIL))
        mail.setTemplateId(template)

        var sendgridId: String? = SecretHelper.getSecretService().fetchSecret("SENDGRID_ID")
        var response: Response = Response()

        if (sendgridId !== null) {

            val sg: SendGrid = SendGrid(sendgridId)
            val request: Request = Request()

            try {
                request.setMethod(Method.POST)
                request.setEndpoint("mail/send")
                request.setBody(mail.build())
                response = sg.api(request)
            } catch (ex: IOException) {
                logger.warning("Can't contact sendgrid")
            } finally {
                logger.info("sending to $emails - result ${response.getStatusCode()}")
                if (!(200..299).contains(response.getStatusCode()))
                    logger.severe("error - ${response.getBody()}")
            }
        } else {
            logger.info("Can't find SENDGRID_ID secret")
        }
    }
}