locals {
  all_app_settings = {
    "POSTGRES_USER"     = "${var.postgres_user}@${var.resource_prefix}-pgsql"
    "POSTGRES_PASSWORD" = var.postgres_pass

    "PRIME_ENVIRONMENT" = var.environment

    "OKTA_baseUrl"  = var.okta_base_url
    "OKTA_redirect" = var.okta_redirect_url

    # Manage client secrets via a Key Vault
    "CREDENTIAL_STORAGE_METHOD" = "AZURE"
    "CREDENTIAL_KEY_VAULT_NAME" = "${var.resource_prefix}-clientconfig"

    # Manage app secrets via a Key Vault
    "SECRET_STORAGE_METHOD" = "AZURE"
    "SECRET_KEY_VAULT_NAME" = "${var.resource_prefix}-appconfig"

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
    "ApplicationInsightsAgent_EXTENSION_VERSION"      = "~3"
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

  functionapp_slot_settings_names = distinct(concat(local.functionapp_slot_prod_settings_names, local.functionapp_slot_candidate_settings_names))

  # Any settings provided implicitly by Azure that we don't want to swap
  sticky_slot_implicit_settings_names = tolist([
    "AzureWebJobsStorage"
  ])

  # Any settings that we want to exclude from sticky settings
  sticky_slot_exclude_settings_names = tolist([
    "AzureWebJobs.emailScheduleEngine.Disabled",
    "AzureWebJobs.send.Disabled"
  ])

  # Any setting not in the common list is therefore unique
  sticky_slot_unique_settings_names = tolist(
    setsubtract(
      setsubtract(local.functionapp_slot_settings_names, keys(local.all_app_settings)),
      local.sticky_slot_exclude_settings_names
    )
  )

  # Origin records
  cors_all = [
    "https://hhs-prime.okta.com"
  ]
  cors_prod = [
    "https://prime.cdc.gov",
    "https://reportstream.cdc.gov",
  ]
  cors_lower = [
    "https://${var.environment}.reportstream.cdc.gov",
    "https://${var.environment}.prime.cdc.gov",
    "https://swaggeruiapidocs.z13.web.core.windows.net"
  ]
  cors_trial_frontends = [
    "https://pdhstagingpublictrial01.z13.web.core.windows.net",
    "https://pdhstagingpublictrial02.z13.web.core.windows.net",
    "https://pdhstagingpublictrial03.z13.web.core.windows.net"
  ]
  concat_allowed_origins = concat(local.cors_all, var.environment == "prod" ? local.cors_prod : local.cors_lower)
  allowed_origins        = concat(local.concat_allowed_origins, var.environment == "staging" ? local.cors_trial_frontends : [])
}

resource "azurerm_function_app" "function_app" {
  name                       = "${var.resource_prefix}-functionapp"
  location                   = var.location
  resource_group_name        = var.resource_group
  app_service_plan_id        = var.app_service_plan
  storage_account_name       = "${var.resource_prefix}storageaccount"
  storage_account_access_key = var.primary_access_key
  https_only                 = true
  os_type                    = "linux"
  version                    = var.function_runtime_version
  enable_builtin_logging     = false

  site_config {
    ip_restriction {
      action                    = "Allow"
      name                      = "AllowVNetTraffic"
      priority                  = 100
      virtual_network_subnet_id = var.subnets.public_subnets[2]
    }

    ip_restriction {
      action                    = "Allow"
      name                      = "AllowVNetEastTraffic"
      priority                  = 100
      virtual_network_subnet_id = var.subnets.public_subnets[0]
    }

    ip_restriction {
      action      = "Allow"
      name        = "AllowFrontDoorTraffic"
      priority    = 110
      service_tag = "AzureFrontDoor.Backend"
    }

    scm_use_main_ip_restriction = true

    http2_enabled             = true
    always_on                 = true
    use_32_bit_worker_process = false
    linux_fx_version          = "DOCKER|${var.container_registry_login_server}/${var.resource_prefix}:latest"
    ftps_state                = "Disabled"

    cors {
      allowed_origins = local.allowed_origins
    }
  }

  app_settings = merge(local.all_app_settings, {
    "POSTGRES_URL"         = "jdbc:postgresql://${var.resource_prefix}-pgsql.postgres.database.azure.com:5432/prime_data_hub?sslmode=require"
    "POSTGRES_REPLICA_URL" = "jdbc:postgresql://${var.resource_prefix}-pgsql-replica.postgres.database.azure.com:5432/prime_data_hub?sslmode=require"
    # HHS Protect Storage Account
    "PartnerStorage" = var.sa_partner_connection_string
  })

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = var.environment
  }

  lifecycle {
    ignore_changes = [
      # Allows Docker versioning via GitHub Actions
      site_config[0].linux_fx_version,
      storage_account_access_key,
      tags,
      # Temp ignore app_settings during terraform overhaul
      app_settings["APPINSIGHTS_INSTRUMENTATIONKEY"],
      app_settings["APPLICATIONINSIGHTS_CONNECTION_STRING"],
      app_settings["DOCKER_REGISTRY_SERVER_PASSWORD"],
      app_settings["POSTGRES_PASSWORD"],
      app_settings["POSTGRES_USER"],
      app_settings["PartnerStorage"],
      app_settings["AzureWebJobs.send.Disabled"],
      app_settings["AzureWebJobs.emailScheduleEngine.Disabled"]
    ]
  }
}


resource "azurerm_key_vault_access_policy" "functionapp_app_config_access_policy" {
  key_vault_id = var.app_config_key_vault_id
  tenant_id    = azurerm_function_app.function_app.identity[0].tenant_id
  object_id    = azurerm_function_app.function_app.identity[0].principal_id

  secret_permissions = [
    "Get",
  ]
}

resource "azurerm_key_vault_access_policy" "functionapp_client_config_access_policy" {
  key_vault_id = var.client_config_key_vault_id
  tenant_id    = azurerm_function_app.function_app.identity.0.tenant_id
  object_id    = azurerm_function_app.function_app.identity.0.principal_id

  secret_permissions = [
    "Get",
  ]

}

resource "azurerm_app_service_virtual_network_swift_connection" "function_app_vnet_integration" {
  app_service_id = azurerm_function_app.function_app.id
  subnet_id      = var.use_cdc_managed_vnet ? var.subnets.public_subnets[0] : var.subnets.public_subnets[2]
}

# Enable sticky slot settings
# Done via a template due to a missing Terraform feature:
# https://github.com/terraform-providers/terraform-provider-azurerm/issues/1440
# ! Before apply, delete existing "functionapp_sticky_settings" deployment from RG
resource "azurerm_resource_group_template_deployment" "functionapp_sticky_settings" {
  name                = "functionapp_sticky_settings"
  resource_group_name = var.resource_group
  deployment_mode     = "Incremental"

  template_content = <<TEMPLATE
{
  "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
      "stickyAppSettingNames": {
        "type": "String"
      },
      "webAppName": {
        "type": "String"
      }
  },
  "variables": {
    "appSettingNames": "[split(parameters('stickyAppSettingNames'),',')]"
  },
  "resources": [
      {
        "type": "Microsoft.Web/sites/config",
        "name": "[concat(parameters('webAppName'), '/slotconfignames')]",
        "apiVersion": "2015-08-01",
        "properties": {
          "appSettingNames": "[variables('appSettingNames')]"
        }
      }
  ]
}
TEMPLATE

  parameters_content = jsonencode({
    webAppName = {
      value = azurerm_function_app.function_app.name
    }
    stickyAppSettingNames = {
      value = join(",", concat(local.sticky_slot_implicit_settings_names, local.sticky_slot_unique_settings_names))
    }
  })

  depends_on = [
    azurerm_function_app.function_app,
    azurerm_function_app_slot.candidate,
  ]
}
