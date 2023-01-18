package gov.cdc.prime.router.azure

import com.microsoft.azure.functions.HttpMethod
import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpResponseMessage
import com.microsoft.azure.functions.HttpStatus
import com.microsoft.azure.functions.HttpStatusType
import gov.cdc.prime.router.Organization
import gov.cdc.prime.router.Receiver
import gov.cdc.prime.router.Sender
import gov.cdc.prime.router.SettingsProvider
import java.net.URI
import kotlin.collections.Map

class MockHttpResponseMessage : HttpResponseMessage.Builder, HttpResponseMessage {
    var httpStatus: HttpStatusType = HttpStatus.OK
    var content: Any? = null

    override fun getStatus(): HttpStatusType {
        return this.httpStatus
    }

    override fun getHeader(var1: String): String {
        return "world"
    }

    override fun getBody(): Any? {
        return this.content
    }

    override fun status(status: HttpStatusType): HttpResponseMessage.Builder {
        this.httpStatus = status
        return this
    }

    override fun header(key: String, value: String): HttpResponseMessage.Builder {
        return this
    }

    override fun body(body: Any?): HttpResponseMessage.Builder {
        this.content = body
        return this
    }

    override fun build(): HttpResponseMessage {
        return this
    }
}

class MockHttpRequestMessage(
    val content: String? = null,
    val method: HttpMethod = HttpMethod.GET
) : HttpRequestMessage<String?> {
    val httpHeaders = mutableMapOf<String, String>()
    val parameters = mutableMapOf<String, String>()

    override fun getUri(): URI {
        return URI.create("http://localhost/")
    }

    override fun getHttpMethod(): HttpMethod {
        return method
    }

    override fun getHeaders(): Map<String, String> {
        return this.httpHeaders
    }

    override fun getQueryParameters(): MutableMap<String, String> {
        return this.parameters
    }

    override fun getBody(): String? {
        return content
    }

    override fun createResponseBuilder(var1: HttpStatus): HttpResponseMessage.Builder {
        return MockHttpResponseMessage().status(var1)
    }

    override fun createResponseBuilder(var1: HttpStatusType): HttpResponseMessage.Builder {
        return MockHttpResponseMessage().status(var1)
    }
}

class MockSettings : SettingsProvider {
    var organizationStore: MutableMap<String, Organization> = mutableMapOf()
    var receiverStore: MutableMap<String, Receiver> = mutableMapOf()
    var senderStore: MutableMap<String, Sender> = mutableMapOf()

    override val organizations get() = this.organizationStore.values
    override val senders get() = this.senderStore.values
    override val receivers get() = this.receiverStore.values

    override fun findOrganization(name: String): Organization? {
        return organizationStore[name]
    }

    override fun findReceiver(fullName: String): Receiver? {
        return receiverStore[fullName]
    }

    override fun findSender(fullName: String): Sender? {
        return senderStore[Sender.canonicalizeFullName(fullName)]
    }

    override fun findOrganizationAndReceiver(fullName: String): Pair<Organization, Receiver>? {
        val (organizationName, _) = Receiver.parseFullName(fullName)
        val organization = organizationStore[organizationName] ?: return null
        val receiver = receiverStore[fullName] ?: return null
        return Pair(organization, receiver)
    }
}