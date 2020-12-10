package gov.cdc.prime.router.azure

import com.google.common.net.HttpHeaders
import com.microsoft.azure.functions.ExecutionContext
import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.annotation.AuthorizationLevel
import com.microsoft.azure.functions.annotation.FunctionName
import com.microsoft.azure.functions.annotation.HttpTrigger
import com.microsoft.azure.functions.annotation.StorageAccount
import java.nio.file.Files
import java.nio.file.Path
import org.thymeleaf.TemplateEngine
import org.thymeleaf.templateresolver.StringTemplateResolver
import org.thymeleaf.context.Context
import java.util.Calendar
import java.util.GregorianCalendar

class DownloadFunction {
    private val clientName = "client"
    private val csvMimeType = "text/csv"

    data class TestResult(
        val date: Calendar,
        val expires: Int,
        val total: Int,
        val positive: Int,
        val file: String
    );

    @FunctionName("download")
    @StorageAccount("AzureWebJobsStorage")
    fun run(
        @HttpTrigger(
            name = "req",
            methods = [HttpMethod.GET],
            authLevel = AuthorizationLevel.FUNCTION
        ) request: HttpRequestMessage<String?>,
        context: ExecutionContext,
    ): HttpResponseMessage {
        val file: String = request.queryParameters["file"] ?: "";
        if( file.isBlank() ) 
            return responsePage(request);
        else 
            return responseFile(request,file);
    }

    private fun responseFile( request: HttpRequestMessage<String?>, fileName: String ) : HttpResponseMessage {
        var response = request.createResponseBuilder(HttpStatus.OK)
        .body("aaa,aaaa")
        .header("Content-Type", "text/csv")
        .build();

        return response;    
    }

    private fun responsePage(request: HttpRequestMessage<String?>)  : HttpResponseMessage  {
        val htmlTemplate = Files.readString(Path.of("./assets/csv-download-site/index__inline.html"))

        val todaysTestResults = listOf<TestResult>(
            TestResult( Calendar.getInstance(), 7, 10, 3, "123111111"),
        );

        val previousTestResults = listOf<TestResult>(
            TestResult( GregorianCalendar(2020,12,9), 7, 8, 2, "123111111"),
            TestResult( GregorianCalendar(2020,12,8), 7, 11, 2, "123111111"),
            TestResult( GregorianCalendar(2020,12,7), 7, 12, 3, "123111111"),
            TestResult( GregorianCalendar(2020,12,6), 7, 9, 1, "123111111"),
            TestResult( GregorianCalendar(2020,12,5), 7, 9, 1, "123111111"),
            TestResult( GregorianCalendar(2020,12,4), 7, 10, 3, "123111111"),
            TestResult( GregorianCalendar(2020,12,3), 7, 12, 4, "123111111")            
        )
        
        val attr = mapOf( 
            "countyName" to "Pima",
            "stateName" to "AZ",
            "user" to "qtv1@cdc.gov",
            "today" to Calendar.getInstance(),
            "todays" to todaysTestResults,
            "previous" to previousTestResults
        )

        val html = getTemplateFromAttributes( htmlTemplate, attr );

        var response = request.createResponseBuilder(HttpStatus.OK)
                        .body(html)
                        .header("Content-Type", "text/html")
                        .build();
        
        return response;
    }

    private fun getTemplateEngine(): TemplateEngine {
        val templateEngine = TemplateEngine();
        val stringTemplateResolver = StringTemplateResolver();
        templateEngine.setTemplateResolver(stringTemplateResolver);
        return templateEngine;
    }   
    
    private fun getTemplateFromAttributes(htmlContent:String, attr: Map<String, Any>) : String
     {
            val templateEngine = getTemplateEngine();
            val context = Context();
            if( !attr.isEmpty() )
                attr.forEach{ (k,v) -> context.setVariable(k, v) };
            return templateEngine.process(htmlContent, context);        
    }
}