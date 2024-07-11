@file:Suppress("ktlint:standard:no-empty-file")

package gov.cdc.prime.reportstream.submissions
//
// import gov.cdc.prime.reportstream.submissions.CustomMediaTypes.APPLICATION_HL7_V2
// import java.io.IOException
// import org.springframework.http.HttpInputMessage
// import org.springframework.http.HttpOutputMessage
// import org.springframework.http.MediaType
// import org.springframework.http.converter.HttpMessageConverter
//
// class CustomMessageConverter : HttpMessageConverter<Any> {
//    override fun getSupportedMediaTypes(): List<MediaType> {
//        return listOf(APPLICATION_HL7_V2)
//    }
//
//    @Throws(IOException::class)
//    override fun read(clazz: Class<out Any>, inputMessage: HttpInputMessage): Any {
//        val inputStream = inputMessage.body
//        val hl7Data = inputStream.readBytes().toString(Charsets.UTF_8)
//        return mapOf("hl7Data" to hl7Data)
//    }
//
//    @Throws(IOException::class)
//    override fun write(obj: Any, contentType: MediaType?, outputMessage: HttpOutputMessage) {
//        val outputStream = outputMessage.body
//        val hl7Data = (obj as Map<*, *>)["hl7Data"].toString()
//        outputStream.write(hl7Data.toByteArray(Charsets.UTF_8))
//    }
//
//    override fun canRead(clazz: Class<*>, mediaType: MediaType?): Boolean {
//        return APPLICATION_HL7_V2 == mediaType && clazz == Map::class.java
//    }
//
//    override fun canWrite(clazz: Class<*>, mediaType: MediaType?): Boolean {
//        return APPLICATION_HL7_V2 == mediaType && clazz == Map::class.java
//    }
// }