locals {
  all_app_settings = {
    "AzureFunctionsJobHost__functionTimeout"    = "01:00:00"
    "AzureWebJobs.emailScheduleEngine.Disabled" = 0
    "AzureWebJobs.send.Disabled"                = 0

    "POSTGRES_USER"     = "${var.postgres_user}@${var.resource_prefix}-pgsql"
    "POSTGRES_PASSWORD" = var.postgres_pass

    "PRIME_ENVIRONMENT" = var.environment

    "OKTA_baseUrl"     = var.okta_base_url
    "OKTA_redirect"    = var.okta_redirect_url
    "OKTA_authkey"     = var.OKTA_authKey
    "OKTA_ClientId"    = var.OKTA_clientId
    "RS_OKTA_baseUrl"  = var.RS_okta_base_url
    "RS_OKTA_redirect" = var.RS_okta_redirect_url
    "RS_OKTA_authkey"  = var.RS_OKTA_authKey
    "RS_OKTA_ClientId" = var.RS_OKTA_clientId
    "ETOR_TI_baseurl"  = var.etor_ti_base_url
    "cdctiautomated"   = var.cdctiautomated_sa
    "JAVA_OPTS"        = var.JAVA_OPTS
    # Manage client secrets via a Key Vault
    "CREDENTIAL_STORAGE_METHOD" = "AZURE"
    "CREDENTIAL_KEY_VAULT_NAME" = var.client_config_key_vault_name

    # Manage app secrets via a Key Vault
    "SECRET_STORAGE_METHOD" = "AZURE"
    "SECRET_KEY_VAULT_NAME" = var.app_config_key_vault_name

    # Route outbound traffic through the VNET
    "WEBSITE_VNET_ROUTE_ALL" = 1

    # Route storage account access through the VNET
    "WEBSITE_CONTENTOVERVNET" = 1

    # Use the CDC DNS for everything; they have mappings for all our internal
    # resources, so if we add a new resource we'll have to contact them (see
    # prime-router/docs/dns.md)
    "WEBSITE_DNS_SERVER" = "${var.dns_ip}"

    "DOCKER_REGISTRY_SERVER_URL"      = var.container_registry_login_server
    "DOCKER_REGISTRY_SERVER_USERNAME" = var.container_registry_admin_username
    "DOCKER_REGISTRY_SERVER_PASSWORD" = var.container_registry_admin_password

    # With this variable set, clients can only see (and pull) signed images from the registry
    # First make signing work, then enable this
    # "DOCKER_CONTENT_TRUST" = 1

    "WEBSITES_ENABLE_APP_SERVICE_STORAGE" = false

    # Cron-like schedule for running the function app that reaches out to remote
    # sites and verifies they're up
    "REMOTE_CONNECTION_CHECK_SCHEDULE" = "15 */2 * * *"

    # App Insights
    "APPINSIGHTS_INSTRUMENTATIONKEY"                  = var.ai_instrumentation_key
    "APPINSIGHTS_PROFILERFEATURE_VERSION"             = "1.0.0"
    "APPINSIGHTS_SNAPSHOTFEATURE_VERSION"             = "1.0.0"
    "APPLICATIONINSIGHTS_CONFIGURATION_CONTENT"       = ""
    "APPLICATIONINSIGHTS_CONNECTION_STRING"           = var.ai_connection_string
    "APPLICATIONINSIGHTS_ENABLE_AGENT"                = "true"
    "DiagnosticServices_EXTENSION_VERSION"            = "~3"
    "InstrumentationEngine_EXTENSION_VERSION"         = "disabled"
    "SnapshotDebugger_EXTENSION_VERSION"              = "disabled"
    "XDT_MicrosoftApplicationInsights_BaseExtensions" = "disabled"
    "XDT_MicrosoftApplicationInsights_Mode"           = "recommended"
    "XDT_MicrosoftApplicationInsights_PreemptSdk"     = "disabled"

    "FEATURE_FLAG_SETTINGS_ENABLED" = true
  }

  functionapp_slot_prod_settings_names      = keys(azurerm_function_app.function_app.app_settings)
  functionapp_slot_candidate_settings_names = keys(azurerm_function_app_slot.candidate.app_settings)
  functionapp_slot_settings_names           = distinct(concat(local.functionapp_slot_prod_settings_names, local.functionapp_slot_candidate_settings_names))

  # Any settings provided implicitly by Azure that we don't want to swap
  sticky_slot_implicit_settings_names = tolist([
    "AzureWebJobsStorage",
    "OKTA_authKey",
    "OKTA_ClientId",
    "OKTA_scope",
    "RS_OKTA_authkey",
    "RS_OKTA_baseUrl",
    "RS_OKTA_ClientId",
    "RS_OKTA_redirect",
    "RS_OKTA_scope",
    "PartnerStorage",
    "POSTGRES_URL",
    "POSTGRES_REPLICA_URL"
  ])

  # Any setting not in the common list is therefore unique
  sticky_slot_unique_settings_names = tolist(
    setsubtract(local.functionapp_slot_settings_names, keys(local.all_app_settings))
  )

  sticky_slot_settings = join(",",
    distinct(sort(concat(local.sticky_slot_implicit_settings_names, local.sticky_slot_unique_settings_names)))
  )

  # Origin records
  cors_all = [
    "https://hhs-prime.okta.com"
  ]
  cors_prod = [
    "https://prime.cdc.gov",
    "https://reportstream.cdc.gov"
  ]
  cors_lower = [
    "https://${var.environment}.reportstream.cdc.gov",
    "https://${var.environment}.prime.cdc.gov",
    "https://swaggeruiapidocs.z13.web.core.windows.net",
    "https://reportstream.oktapreview.com"
  ]
  cors_trial_frontends = [
    "https://pdhstagingpublictrial01.z13.web.core.windows.net",
    "https://pdhstagingpublictrial02.z13.web.core.windows.net",
    "https://pdhstagingpublictrial03.z13.web.core.windows.net"
  ]
  concat_allowed_origins = concat(local.cors_all, var.environment == "prod" ? local.cors_prod : local.cors_lower)
  allowed_origins        = concat(local.concat_allowed_origins, var.environment == "staging" ? local.cors_trial_frontends : [])

  active_slot_settings = merge(local.all_app_settings, {
    "POSTGRES_URL"         = "jdbc:postgresql://${var.resource_prefix}-pgsql.postgres.database.azure.com:5432/prime_data_hub?sslmode=require"
    "POSTGRES_REPLICA_URL" = "jdbc:postgresql://${var.resource_prefix}-pgsql-replica.postgres.database.azure.com:5432/prime_data_hub?sslmode=require"
    # HHS Protect Storage Account
    "PartnerStorage" = var.sa_partner_connection_string
    "OKTA_scope"     = var.OKTA_scope
  })

  candidate_slot_settings = merge(local.all_app_settings, {
    "POSTGRES_URL"         = "jdbc:postgresql://${var.resource_prefix}-pgsql.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require"
    "POSTGRES_REPLICA_URL" = "jdbc:postgresql://${var.resource_prefix}-pgsql-replica.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require"
    # HHS Protect Storage Account
    "PartnerStorage" = var.primary_connection_string
    "OKTA_scope"     = var.OKTA_scope
  })
}
