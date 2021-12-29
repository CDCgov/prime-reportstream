package gov.cdc.prime.router.common

import com.helger.commons.io.stream.StringInputStream
import org.apache.commons.lang3.RandomStringUtils
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.mail.BodyPart
import javax.mail.MultipartDataSource
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart

/**
 * Class to serialize/deserialize into the "multipart/mixed" format.
 *
 * see https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html for information on the format.
 */
class MixedMultiPart(val parts: List<Part>, val boundary: String) : MultipartDataSource {

    /**
     * A part of the stream. Represents a file.
     */
    data class Part(
        val contentType: String,
        val fileName: String,
        val body: String
    )

    /**
     * Serialize in MIME Multipart Mixed format and return a string.
     */
    fun serialize(): String {
        val outputStream = ByteArrayOutputStream()
        val mimeMultipart = MimeMultipart(this)
        mimeMultipart.writeTo(outputStream)
        return String(outputStream.toByteArray())
    }

    override fun getInputStream(): InputStream {
        error("Not implemented")
    }

    override fun getOutputStream(): OutputStream {
        error("Not implemented")
    }

    override fun getContentType(): String {
        return "$MIME_MIXED_MULTIPART; boundary=$boundary"
    }

    override fun getName(): String {
        error("Not implemented")
    }

    override fun getCount(): Int {
        return parts.size
    }

    override fun getBodyPart(index: Int): BodyPart {
        val part = parts[index]
        val headerText = """
            Content-Type: ${part.contentType}
            Content-Disposition: attachment; filename*="${part.fileName}"
        """.trimIndent()
        val headers = InternetHeaders(StringInputStream.utf8(headerText))
        return MimeBodyPart(headers, part.body.toByteArray())
    }

    companion object {
        /**
         * Deserialize an [input] string with a [boundary] string. Return a MixedMultiPart object.
         */
        fun deserialize(input: String, boundary: String): MixedMultiPart {
            val partList = mutableListOf<Part>()
            val contentType = "$MIME_MIXED_MULTIPART; boundary=$boundary"
            val mimeMultipart = MimeMultipart(StringDataSource(input, contentType))
            val count = mimeMultipart.count
            for (index in 0 until count) {
                val mimePart = mimeMultipart.getBodyPart(index)
                val content = mimePart.content as? InputStream ?: error("")
                val body = String(content.readAllBytes())
                val part = Part(mimePart.contentType, mimePart.fileName, body)
                partList.add(part)
            }
            return MixedMultiPart(partList, boundary)
        }

        /**
         * Generate a random string that can be used for boundary string
         */
        fun generateBoundary(): String {
            return RandomStringUtils.randomAlphanumeric(20)
        }

        const val MIME_MIXED_MULTIPART = "multipart/mixed"

        private class StringDataSource(val input: String, val contentTypeWithBoundary: String) :
            javax.activation.DataSource {
            override fun getInputStream(): InputStream {
                return input.byteInputStream()
            }

            override fun getOutputStream(): OutputStream {
                error("Not implemented")
            }

            override fun getContentType(): String {
                return contentTypeWithBoundary
            }

            override fun getName(): String {
                error("Not implemented")
            }
        }
    }
}