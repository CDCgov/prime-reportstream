package gov.cdc.prime.reportstream.auth.service

import gov.cdc.prime.reportstream.auth.config.ApplicationConfig
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.net.URI

/**
 * Implementations are ways to decide the ultimate destination of an incoming request
 */
interface ProxyURIStrategy {
    fun getTargetURI(incomingUri: URI): URI
}

/**
 * This implementation decides via the path prefix. Currently used locally for when all services are
 * running on different ports of localhost.
 *
 * Configured under proxyConfig.pathMappings
 *
 * http://localhost:9000/submissions/health -> http://localhost:8880/health
 */
@Component
@Profile("local")
class PathPrefixProxyURIStrategy(
    private val applicationConfig: ApplicationConfig,
) : ProxyURIStrategy {
    override fun getTargetURI(incomingUri: URI): URI {
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
            throw IllegalStateException("no configured proxy target in path mappings for path=${incomingUri.path}")
        }
    }
}

@Component
@Profile("deployed")
class HostProxyPathURIStrategy : ProxyURIStrategy {
    override fun getTargetURI(incomingUri: URI): URI {
        TODO("Not yet implemented")
    }
}