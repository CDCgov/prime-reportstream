@file:Suppress("ktlint:standard:no-empty-file")

package gov.cdc.prime.reportstream.submissions

// import org.springframework.context.annotation.Configuration
// import org.springframework.http.MediaType
// import org.springframework.http.converter.HttpMessageConverter
// import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
// import org.springframework.web.servlet.config.annotation.EnableWebMvc
// import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
//
// val CUSTOM_MEDIA_TYPE = MediaType("application", "hl7-v2")
//
// @Configuration
// class WebConfig: WebMvcConfigurer {
//    override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
//        configurer.mediaType("application/hl7-v2", CUSTOM_MEDIA_TYPE)
//    }
//
//    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
//        converters.add(CustomMessageConverter())
//    }
// }