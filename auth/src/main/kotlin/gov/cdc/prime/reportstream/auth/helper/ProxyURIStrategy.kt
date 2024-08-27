package gov.cdc.prime.reportstream.auth.helper

import gov.cdc.prime.reportstream.auth.config.ApplicationConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI

interface ProxyURIStrategy {
    fun getURL(incomingUri: URI): URI
}

@Component
@Profile("local")
class PathPrefixProxyURIStrategy @Autowired constructor(
    private val applicationConfig: ApplicationConfig,
) : ProxyURIStrategy {
    override fun getURL(incomingUri: URI): URI {
        val proxyPathMappings = applicationConfig.proxyConfig.pathMappings
        val maybePathMapping = proxyPathMappings.find { incomingUri.path.startsWith(it.pathPrefix) }
        return if (maybePathMapping != null) {
            val baseUri = URI(maybePathMapping.baseUrl)
            val path = incomingUri.path.removePrefix(maybePathMapping.pathPrefix)
            URI(
                baseUri.scheme,
                baseUri.userInfo,
                baseUri.host,
                baseUri.port,
                path,
                incomingUri.query,
                incomingUri.fragment
            )
        } else {
            throw IllegalStateException("no configured proxy target in path mappings") // TODO: handle with Either
        }
    }
}

@Component
@Profile("deployed")
class HostProxyPathURIStrategy : ProxyURIStrategy {
    override fun getURL(incomingUri: URI): URI {
        TODO("Not yet implemented")
    }
}