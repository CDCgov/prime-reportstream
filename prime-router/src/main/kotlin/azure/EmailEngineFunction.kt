package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.TimerTrigger
import com.microsoft.azure.functions.annotation.StorageAccount

import gov.cdc.prime.router.azure.db.tables.pojos.ReportFile
import gov.cdc.prime.router.azure.WorkflowEngine

import java.util.Date;
import java.time.ZoneOffset;

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.annotation.JsonProperty

import java.time.ZonedDateTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.CronType;

import com.sendgrid.helpers.mail.objects.*;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;

import gov.cdc.prime.router.azure.db.enums.SettingType;
import org.jooq.Configuration

import java.io.IOException;

import khttp.get as httpGet
import org.json.JSONObject
import org.json.JSONArray

import gov.cdc.prime.router.secrets.SecretHelper;

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.BindingName
import com.microsoft.azure.functions.annotation.HttpTrigger

import com.okta.jwt.JwtVerifiers
import java.util.logging.Level
import java.util.logging.Logger

const val SCHEDULE = "*/5 * * * *" // every 5 minutes
const val OKTA_ISSUER = "https://hhs-prime.okta.com/oauth2/default"
const val OKTA_GROUPS_API = "https://hhs-prime-admin.okta.com/api/v1/groups"
const val FROM_EMAIL = "reportstream@cdc.gov"
const val SUBJECT_EMAIL = "ReportStream Daily Email"



data class EmailSchedule ( 
    val template: String,
    val type: String,
    val cronSchedule: String,
    val organizations: List<String> = ArrayList<String>(),
    val emails: List<String> = ArrayList<String>(),
    val parameters: Map<String,String> = HashMap<String,String>()
) {}
 
class EmailScheduleEngine  {

    val workflowEngine = WorkflowEngine()

    /**
     * Create Email Schedule
     */
    @FunctionName("createEmailSchedule")
    @StorageAccount("AzureWebJobsStorage")
    fun createEmailSchedule(
        @HttpTrigger(
            name = "createEmailSchedule",
            methods = [HttpMethod.POST],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "email-schedule"
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        var user:String? = validateUser( request, context.logger );
        var ret = request.createResponseBuilder(HttpStatus.UNAUTHORIZED);

        if( user !== null ){
            val id = WorkflowEngine.databaseAccess.insertEmailSchedule(request.body, user );
            ret.status(HttpStatus.CREATED)
            ret.body( "created id $id")
        }
        return ret.build();
    }

    /**
     * Delete (inactivate) Email Schedule
     */
    @FunctionName("deleteEmailSchedule")
    @StorageAccount("AzureWebJobsStorage")
    fun deleteEmailSchedule(
        @HttpTrigger(
            name = "deleteEmailSchedule",
            methods = [HttpMethod.DELETE],
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "email-schedule/{scheduleId}"
        ) request: HttpRequestMessage<String?>,
        @BindingName("scheduleId") scheduleId: Int,
        context: ExecutionContext,
    ): HttpResponseMessage {
        var user:String? = validateUser( request, context.logger );
        var ret = request.createResponseBuilder(HttpStatus.UNAUTHORIZED);

        if( user !== null ){
            val id = WorkflowEngine.databaseAccess.deleteEmailSchedule(scheduleId);
            ret.status(HttpStatus.CREATED)
            ret.body( "deactivated id $id")
        }
        return ret.build();
    }

    /**
     * Timer Trigger to fire when processing schedules
     *   (fires every 5 minutes)
     */
    @FunctionName("emailScheduleEngine")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @TimerTrigger( name = "emailScheduleEngine", schedule = SCHEDULE ) timerInfo : String,
        context: ExecutionContext
    ){
        val logger : Logger = context.logger;
        val mapper = ObjectMapper().registerModule(KotlinModule());

        // get the schedules to fire
        val schedulesToFire : List<EmailSchedule> = WorkflowEngine.databaseAccess.fetchEmailSchedules()
            .map{ mapper.readValue<EmailSchedule>( it ) }
            .filter{ shouldFire( it ) }

        schedulesToFire.forEach { schedule -> 
            val orgs: Iterable<String> = getOrgs( schedule );

            logger.info( "processing ${schedule.template}" )

            // get the orgs to fire for
            orgs.forEach{ org ->
                    val emails: List<String> = if ( schedule.emails.size > 0) schedule.emails else getEmails(org, logger)
                    logger.info( "processing ${org}" )
                    emails.forEach{ email -> 
                        logger.info( "sending email to ${email}" )
                        dispatchToSendGrid( schedule.template, listOf(email), logger );
                    }
            }
        }
    }

    /**
     * Determine if a schedule should fire
     * 
     * @param schedule to check if it should fire
     * 
     * @returns true if the given schedule should fire; false otherwise
     */
    private fun shouldFire( schedule: EmailSchedule ): Boolean{

        val parser = CronParser( CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX) );
        // Get date for last execution
        val now = ZonedDateTime.now();
        val executionTime = ExecutionTime.forCron(parser.parse(schedule.cronSchedule));
        val timeFromLastExecution = executionTime.timeFromLastExecution(now);

        return ( timeFromLastExecution.get().toSeconds() <= 4*60 /* 4 minutes */);
    }

    /**
     * Retreives the organization to send the emailschedule to
     * 
     * @param schedule the schedule to check organizations against
     * 
     * @returns List of organizations for the schedule
     */
    private fun getOrgs( schedule: EmailSchedule ): Iterable<String> {
        return (
            if (schedule.organizations.size > 0) 
                schedule.organizations 
            else 
                fetchAllOrgs() 
        )
    }

    /**
     * Retrieves the list of all organization supported
     * 
     * @returns List of all organizations supported
     */
    private fun fetchAllOrgs(): List<String>{    
        @Suppress( "NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER" )    
        return workflowEngine.db.transactReturning {  tx -> 
            @Suppress( "UNRESOLVED_REFERENCE")
            val settings = workflowEngine.db.fetchSettings( SettingType.ORGANIZATION, tx )
            @Suppress( "NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER" )
            settings.map{ it.getName() }
        }
    }

    /**
     * Validates the JWT Token supplied in the authorization header
     * 
     * @param request the HTTPRequest object
     * @param logger logger
     * 
     * @returns user from the token; otherwise null
     */
    fun validateUser( request: HttpRequestMessage<String?>, logger: Logger): String? {
        var jwtToken = request.headers["authorization"] ?: ""

        jwtToken = if (jwtToken.length > 7) jwtToken.substring(7) else ""

        var user: String? = null;

        if (jwtToken.isNotBlank()) {
            try {
                // get the access token verifier
                val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                    .setIssuer(OKTA_ISSUER)
                    .build()
                // get it to decode the token from the header
                val jwt = jwtVerifier.decode(jwtToken)
                    ?: throw Throwable("Error in validation of jwt token")
                // get the user name
                user = jwt.claims["sub"].toString();
            }
            catch (ex: Throwable) {
                logger.log(Level.WARNING, "Error in verification of token", ex)
            }
        }
        return user;
    }


    /**
     * Converts an organization name to an OKTA group name
     * 
     * @param org organization name (ex. pima-az-phd )
     * 
     * @returns encoded org name (ex. DHpima_az_phd )
     */
    private fun encodeOrg( org: String ): String {
        return "DH"+org.replace( "-","_" )
    }

    /**
     * Retrieve a list of emails within an organization
     * 
     * @params org The organization to fetch emails for
     * 
     * @returns List of emails to send to
     */
    private fun getEmails( org: String, logger: Logger): List<String> {

        var emails :MutableList<String> = mutableListOf()
    
        try{

            var ssws: String? = SecretHelper.getSecretService().fetchSecret("SSWS_OKTA")

            if( ssws !== null ){

                var grp = encodeOrg( org );

                // get the OKTA Group Id
                var response1 = httpGet( url="${OKTA_GROUPS_API}?q=${grp}", 
                                    headers=mapOf( "Authorization" to "SSWS ${ssws}" ) );
                var grpId = ((response1.jsonArray).get( 0 ) as JSONObject).getString("id");

                // get the users within that OKTA group
                var response = httpGet( url="${OKTA_GROUPS_API}/${grpId}/users", 
                                    headers=mapOf( "Authorization" to "SSWS ${ssws}" ) );
            
                for( user in response.jsonArray )
                    emails.add( (user as JSONObject).getJSONObject("profile").getString("email") );
            }
        }
        catch( ex: Throwable ){
            logger.warning( "Error in fetching emails" )
            emails = mutableListOf()
        }

        return emails;
    }

     /**
     * Dispatches the select emails (using the desired template) to SENDGRID
     * 
     * @param template the name of the template (in sendgrid) to use
     * @param emails list of emails to send to
     * @param logger context logger
     */
    private fun dispatchToSendGrid( 
        template: String,
        emails: Iterable<String>,
        logger: Logger
    ){
        val p:Personalization = Personalization();
        emails.forEach{ to -> p.addTo( Email(to) ) };
        p.setSubject(SUBJECT_EMAIL);

        val mail : Mail = Mail()
        mail.addPersonalization( p );
        mail.setSubject(SUBJECT_EMAIL);
        mail.setFrom( Email(FROM_EMAIL) );
        mail.setTemplateId( template )

        var sendgridId: String? = SecretHelper.getSecretService().fetchSecret("SENDGRID_ID")
        
        if( sendgridId !== null ){
        
            val sg:SendGrid = SendGrid(sendgridId);
            val request:Request = Request();
            try {
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());
                sg.api(request);
            } catch (ex:IOException) {
                logger.warning("Can't contact sendgrid")
            }
        }
        else{
            logger.info("Can't find SENDGRID_ID secret")
        }
    
    }
}

