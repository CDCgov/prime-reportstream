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

    @FunctionName("emailScheduleEngine")
    @StorageAccount("AzureWebJobsStorage")
    @Suppress( "UNUSED_PARAMETER" )
    fun run(
        @TimerTrigger( name = "emailScheduleEngine", schedule = "*/5 * * * *") timerInfo : String,
        context: ExecutionContext
    ){
        val mapper = ObjectMapper().registerModule(KotlinModule());

        // get the schedules to fire
        val schedulesToFire : List<EmailSchedule> = getSchedules()
            .map{ mapper.readValue<EmailSchedule>( it ) }
            .filter{ shouldFire( it ) };
        schedulesToFire.forEach { schedule -> 
            val last: Date = getLastTimeFired( schedule );
            val orgs: List<String> = getOrgs( schedule );

            context.logger.info( "processing ${schedule.template}" )

            // get the orgs to fire for
            orgs.forEach{ org ->
                    val emails: List<String> = if ( schedule.emails.size > 0) schedule.emails else getEmails(org, context)
                    val reportsSinceLast: List<ReportFile> = getReportsSinceLast(org,last);
                    context.logger.info( "processing ${org}" )
                    emails.forEach{ email -> 
                        context.logger.info( "sending email to ${email}" )
                        dispatchToSendGrid( schedule.template, listOf(email), reportsSinceLast, context );
                    }
            }
        }
    }

    /**
     * Determine if a schedule should fire
     */
    private fun shouldFire( schedule: EmailSchedule ): Boolean{

        val parser = CronParser( CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX) );
        // Get date for last execution
        val now = ZonedDateTime.now();
        val executionTime = ExecutionTime.forCron(parser.parse(schedule.cronSchedule));
        val timeFromLastExecution = executionTime.timeFromLastExecution(now);

        return ( timeFromLastExecution.get().toSeconds() <= 4*60);
    }

    /**
     * Reports the last time that the timer has been fired for this schedule
     * 
     * @param schedule the schedule to check the last time fired against
     * @returns Date of the last time fired
     */
    private fun getLastTimeFired( schedule: EmailSchedule ): Date {
        val parser = CronParser( CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX) );
        // Get date for last execution
        val now = ZonedDateTime.now();
        val executionTime = ExecutionTime.forCron(parser.parse(schedule.cronSchedule));

        return Date.from(executionTime.lastExecution(now).get().toInstant());
    }

    /**
     * Retreives the organization to send the emailschedule to
     * 
     * @param schedule the schedule to check organizations against
     * @returns List of organizations for the schedule
     */
    private fun getOrgs( schedule: EmailSchedule ): List<String> {
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
        var ret = workflowEngine.db.transactReturning {  tx -> 
            @Suppress( "UNRESOLVED_REFERENCE")
            val settings = workflowEngine.db.fetchSettings( SettingType.ORGANIZATION, tx )
            @Suppress( "NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER" )
            settings.map{ it.getName() }
        }
       
        @Suppress("OVERLOAD_RESOLUTION_AMBIGUITY")
        System.out.println( ret );
        return ret;
    }


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
        var user:String? = validateUser( request, context );
        var ret = request.createResponseBuilder(HttpStatus.CREATED);

        if( user !== null ){
            WorkflowEngine.databaseAccess.insertEmailSchedule(request.body, user );
        }
        else{
            ret.status(HttpStatus.UNAUTHORIZED )
        }
        return ret.build();
    }


    fun validateUser( request: HttpRequestMessage<String?>, context: ExecutionContext ): String? {
        var jwtToken = request.headers["authorization"] ?: ""

        jwtToken = if (jwtToken.length > 7) jwtToken.substring(7) else ""

        if (jwtToken.isNotBlank()) {
            try {
                // get the access token verifier
                val jwtVerifier = JwtVerifiers.accessTokenVerifierBuilder()
                    .setIssuer("https://hhs-prime.okta.com/oauth2/default")
                    .build()
                // get it to decode the token from the header
                val jwt = jwtVerifier.decode(jwtToken)
                    ?: throw Throwable("Error in validation of jwt token")
                // get the user name and org
                return jwt.claims["sub"].toString();
            }
            catch (ex: Throwable) {
                context.logger.log(Level.WARNING, "Error in verification of token", ex)
                return null
            }
        }
        return null;
    }
    

    /**
     * TODO: Fixme!
     */
    private fun getSchedules(): List<String>{
        return WorkflowEngine.databaseAccess.fetchEmailSchedules();
    }

    /**
     * Converts an organization name to an OKTA group name
     */
    private fun convertOrgToGroup( org: String ): String {
        return "DH"+org.replace( "-","_" )
    }

    /**
     * Retrieve a list of emails within an organization
     * 
     * @params org The organization to fetch emails for
     * @returns List of emails to send to
     */
    private fun getEmails( org: String, context: ExecutionContext ): List<String> {

        var ssws: String? = SecretHelper.getSecretService().fetchSecret("SSWS_OKTA")

        if( ssws == null ){
            context.logger.warning("Can't find SSWS_OKTA secret")
            return listOf();
        }
        
        var grp = convertOrgToGroup( org );

        // get the OKTA Group Id
        var response1 = httpGet( url="https://hhs-prime-admin.okta.com/api/v1/groups?q=${grp}", 
                               headers=mapOf( "Authorization" to "SSWS ${ssws}" ) );
        var grpId = ((response1.jsonArray).get( 0 ) as JSONObject).getString("id");

        // get the users within that OKTA group
        var response = httpGet( url="https://hhs-prime-admin.okta.com/api/v1/groups/${grpId}/users", 
                               headers=mapOf( "Authorization" to "SSWS ${ssws}" ) );
    
       var emails :MutableList<String> = mutableListOf()

        for( user in response.jsonArray ){
            emails.add( (user as JSONObject).getJSONObject("profile").getString("email") );
        } 

        return emails;
    }

     /**
     * Get the reports that have been generated since a given date
     * 
     * @param org
     * 
     */
    private fun getReportsSinceLast( org: String, last: Date ): List<ReportFile> {

      val reportFiles = workflowEngine
                        .fetchDownloadableReportFiles( 
                                last.toInstant().atOffset(ZoneOffset.UTC), 
                                org )

        return reportFiles;
    }

     /**
     * TODO: Fixme!
     */
    @Suppress( "UNRESOLVED_REFERENCE")
    private fun dispatchToSendGrid( 
        template: String,
        emails: List<String>,
        reportsSinceLast: List<ReportFile>,
        context: ExecutionContext
    ){
        val from: Email = Email("reportstream@cdc.gov");
        val subject = "ReportStream Daily Email";
        val mail : Mail = Mail()
        val p:Personalization = Personalization();
        emails.forEach{ to -> p.addTo( Email(to) ) };
        p.setSubject(subject);
        p.addDynamicTemplateData("reportsSinceLast", reportsSinceLast)

        mail.addPersonalization( p );
        mail.setSubject(subject);
        mail.setFrom( from );
        mail.setTemplateId( template )

        var sendgridId: String? = SecretHelper.getSecretService().fetchSecret("SENDGRID_ID")
        
        if( sendgridId !== null ){
        
            val sg:SendGrid = SendGrid(sendgridId);
            val request:Request = Request();
            try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            val response:Response = sg.api(request);
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
            System.out.println(response.getHeaders());
            } catch (ex:IOException) {
                context.logger.warning("Can't contact sendgrid")
            }
        }
        else{
            context.logger.warning("Can't find SENDGRID_ID secret")
        }
    
    }
}

