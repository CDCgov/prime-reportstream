package gov.cdc.prime.reportstream.auth.filter

import gov.cdc.prime.reportstream.auth.AuthApplicationConstants
import gov.cdc.prime.reportstream.auth.service.OktaGroupsService
import gov.cdc.prime.reportstream.shared.auth.jwt.OktaGroupsJWTConstants
import kotlinx.coroutines.reactor.mono
import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * This filter defines how the Okta-Groups header is added to requests. It follows the conventions
 * defined in spring-cloud-gateway and is instantiated via configuration under a route's filters.
 */
@Component
class AppendOktaGroupsGatewayFilterFactory(
    private val oktaGroupsService: OktaGroupsService,
) : AbstractGatewayFilterFactory<Any>() {

    /**
     * function used only in testing to create our filter without any configuration
     */
    fun apply(): GatewayFilter {
        return apply { _: Any? -> }
    }

    override fun apply(config: Any?): GatewayFilter {
        return GatewayFilter { exchange, chain ->
            exchange
                .getPrincipal<BearerTokenAuthentication>()
                .flatMap { oktaAccessTokenJWT ->
                    val appId = oktaAccessTokenJWT
                        .tokenAttributes[AuthApplicationConstants.Scopes.SUBJECT_SCOPE] as String
                    val organizations = oktaAccessTokenJWT
                        .tokenAttributes[AuthApplicationConstants.Scopes.ORGANIZATION_SCOPE] as List<*>?

                    // If there is no organization claim present, then we have an application user and
                    // require appending our custom header
                    if (organizations == null) {
                        mono { oktaGroupsService.generateOktaGroupsJWT(appId) }
                    } else {
                        Mono.empty()
                    }
                }
                .map { oktaGroupsJWT: String ->
                    exchange.request
                        .mutate()
                        .headers {
                            it.add(OktaGroupsJWTConstants.OKTA_GROUPS_HEADER, oktaGroupsJWT)
                        }
                        .build()
                }
                .switchIfEmpty(Mono.just(exchange.request)) // drop back in original unmodified request if not an app
                .flatMap { request ->
                    chain.filter(exchange.mutate().request(request).build())
                }
        }
    }
}