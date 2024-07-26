package gov.cdc.prime.reportstream.submissions

 import gov.cdc.prime.reportstream.submissions.CustomMediaTypes.APPLICATION_FHIR_NDJSON
import gov.cdc.prime.reportstream.submissions.CustomMediaTypes.APPLICATION_HL7_V2
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.charset.StandardCharsets

 @Configuration
 class WebConfig : WebMvcConfigurer {

     override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
         val stringConverter = StringHttpMessageConverter(StandardCharsets.UTF_8)
         stringConverter.setSupportedMediaTypes(
             listOf(
             MediaType(APPLICATION_HL7_V2),
             MediaType(APPLICATION_FHIR_NDJSON)
         )
         )
         converters.add(stringConverter)
     }
 }