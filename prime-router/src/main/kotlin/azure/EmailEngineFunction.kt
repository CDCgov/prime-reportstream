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

data class EmailSchedule ( 
    val template: String,
    val type: String,
    val cronSchedule: String,
    val organizations: List<String>? = ArrayList<String>(),
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
     * TODO: Fixme!
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
        return listOf( "pima-az-phd" ); //ret;
    }

    /**
     * TODO: Fixme!
     */
    private fun getSchedules(): List<EmailSchedule>{
        return listOf( EmailSchedule( "d-415aa983fe064c02989bc7465d0c9ed8", "marketing", "25 19 * * *") )
    }

    /**
     * TODO: Fixme!
     */
    @Suppress( "UNUSED_PARAMETER" )
    private fun getEmails( org: String ): List<String> {

        @Suppress( "UNUSED_VARIABLE")
        var reponse = httpGet( url="", 
                               headers=mapOf(
                                "header1" to "1"
        ) );
        return listOf( "qtv1@cdc.gov" ); //,"qom6@cdc.gov","qop5@cdc.gov","qop4@cdc.gov","qva8@cdc.gov","rdz8@cdc.gov","qpu0@cdc.gov" )
    }

     /**
     * TODO: Fixme!
     */
    @Suppress( "UNRESOLVED_REFERENCE")
    private fun getReportsSinceLast( org: String, last: Date ): List<ReportFile> {

      val reportFiles = workflowEngine
                        .fetchDownloadableReportFiles( 
                                last.toInstant().atOffset(ZoneOffset.UTC), 
                                org )

        System.out.println( "Found ${reportFiles.size} reports since the last run ${last.toInstant().atOffset(ZoneOffset.UTC)}" );
        return ArrayList<ReportFile>();
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

        val sg:SendGrid = SendGrid("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");
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

