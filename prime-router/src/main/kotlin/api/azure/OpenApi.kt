package gov.cdc.prime.router.api.azure

import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.security.OAuthFlow
import io.swagger.v3.oas.annotations.security.OAuthFlows
import io.swagger.v3.oas.annotations.security.OAuthScope
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.security.SecuritySchemes
import io.swagger.v3.oas.annotations.servers.Server

const val PRIME_ADMIN_PATTERN = "*.*.primeadmin"

@OpenAPIDefinition(
    info = Info(
        title = "Prime ReportStream",
        description = "A router of public health data",
        contact = Contact(
            name = "USDS at Centers for Disease Control and Prevention",
            url = "https://reportstream.cdc.gov",
            email = "reportstream@cdc.gov"
        ),
        version = "0.2.0-oas3"
    ),
    externalDocs = ExternalDocumentation(
        description = "ReportStream API Programmer Guide",
        url = "https://staging.reportstream.cdc.gov/resources/api/getting-started"
    ),
    security = [
        SecurityRequirement(name = "OAuth2"),
        SecurityRequirement(name = "ServerToServer")
    ],
    servers = [
        Server(
            url = "https://prime.cdc.gov/api/",
            description = "Production Server"
        ),
        Server(
            url = "https://staging.prime.cdc.gov/api/",
            description = "Staging Server"
        )
    ]
)
@SecuritySchemes(
    value = [
        SecurityScheme(
            name = "OAuth2",
            type = SecuritySchemeType.OAUTH2,
            flows = OAuthFlows(
                authorizationCode = OAuthFlow(
                    authorizationUrl = "https://reportstream.oktapreview.com/oauth2/default/v1/authorize",
                    tokenUrl = "https://reportstream.oktapreview.com/oauth2/default/v1/token",
                    scopes = [
                        OAuthScope(
                            name = "openid",
                            description = "OpenID Request scope"
                        )
                    ]
                )
            ),
            description = "OAUTH2 Authorization for Report Stream API Access."
        ),
        SecurityScheme(
            name = "ServerToServer",
            type = SecuritySchemeType.HTTP,
            scheme = "Bearer",
            bearerFormat = "JWT",
            description = "HTTP Bearer Token Authorization for Report Stream API Access."
        )
    ]
)
class OpenApiDummy