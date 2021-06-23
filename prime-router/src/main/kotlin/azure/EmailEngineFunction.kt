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

data class EmailSchedule ( 
    val template: String,
    val type: String,
    val cronSchedule: String,
    val organizations: List<String>? = ArrayList<String>(),
    val emails: List<String>? = ArrayList<String>(),
    val parameters: Map<String,String>? = HashMap<String,String>()
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
        // get the schedules to fire
        val schedulesToFire : List<EmailSchedule> = getSchedules().filter{ shouldFire( it ) };
        schedulesToFire.forEach { schedule -> 
            val last: Date = getLastTimeFired( schedule );
            val orgs: List<String> = getOrgs( schedule );

            System.out.println( "processing ${schedule.template}" )

            // get the orgs to fire for
            orgs.forEach{ org ->
                    val emails: List<String> = getEmails(org);
                    @Suppress( "UNRESOLVED_REFERENCE")
                    val reportsSinceLast: List<ReportFile> = getReportsSinceLast(org,last);

                    System.out.println( "processing ${org}")
                    dispatchToSendGrid( schedule.template, emails, reportsSinceLast );
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
        if( timeFromLastExecution.get().toSeconds() <= 4*60) 
            System.out.println( "Firing ${schedule.template}; timeFromLastExecution= ${timeFromLastExecution}")

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
            if (schedule.organizations !== null && schedule.organizations.size > 0) 
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

    /**
     * TODO: Fixme!
     */
    private fun getSchedules(): List<EmailSchedule>{
        return listOf( EmailSchedule( template="d-415aa983fe064c02989bc7465d0c9ed8", 
                                      type="daily", 
                                      cronSchedule="02 14 * * *",
                                      organizations=listOf( "pima-az-phd"),
                                      emails=listOf( "qtv1@cdc.gov","qom6@cdc.gov","qop5@cdc.gov","qop4@cdc.gov","qva8@cdc.gov","rdz8@cdc.gov","qpu0@cdc.gov" )) )
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
    private fun getEmails( org: String ): List<String> {

        var ssws: String = System.getenv("SSWS-OKTA") ?: "00KPnlSG2vpP3VtKDlv5lsrYXhGEpnXmP1VABopqIX"
        var grp = convertOrgToGroup( org );

        // get the OKTA Group Id
        @Suppress( "UNUSED_VARIABLE")
        var response1 = httpGet( url="https://hhs-prime-admin.okta.com/api/v1/groups?q=${grp}", 
                               headers=mapOf( "Authorization" to "SSWS ${ssws}" ) );
        var grpId = ((response1.jsonArray).get( 0 ) as JSONObject).getString("id");

        // get the users within that OKTA group
        @Suppress( "UNUSED_VARIABLE")
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

        System.out.println( "Found ${reportFiles.size} reports since the last run ${last.toInstant().atOffset(ZoneOffset.UTC)}" );
        return reportFiles;
    }

     /**
     * TODO: Fixme!
     */
    @Suppress( "UNRESOLVED_REFERENCE")
    private fun dispatchToSendGrid( 
        template: String,
        emails: List<String>,
        reportsSinceLast: List<ReportFile>
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

        var sendgridId: String = System.getenv("SENDGRID-ID") ?: "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"
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
          System.out.println( ex )
        }
    
    }

    


}

