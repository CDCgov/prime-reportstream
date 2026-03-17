resource "azurerm_linux_web_app" "metabase" {
  #checkov:skip=CKV_AZURE_222: "Ensure that Azure Web App public network access is disabled"
  name                          = "${var.resource_prefix}-metabase"
  location                      = var.location
  resource_group_name           = var.resource_group
  service_plan_id               = var.service_plan_id
  https_only                    = true
  public_network_access_enabled = true

  identity {
    type = "SystemAssigned"
  }

  site_config {

    application_stack {
      docker_image_name   = "metabase/metabase:v0.50.7"
      docker_registry_url = "https://index.docker.io/v1"
    }

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

    ftps_state                    = "Disabled"
    scm_use_main_ip_restriction   = true
    always_on                     = true
    vnet_route_all_enabled        = false
    ip_restriction_default_action = "Deny"
    scm_minimum_tls_version       = "1.0"
    use_32_bit_worker             = false

  }

  logs {
    detailed_error_messages = false
    failed_request_tracing  = false

    http_logs {
      file_system {
        retention_in_days = 30
        retention_in_mb   = 35
      }
    }
  }

  app_settings = {
    "MB_DB_CONNECTION_URI" = "jdbc:postgresql://${var.postgres_server_name}.postgres.database.azure.com:5432/metabase?user=${var.postgres_user}%40${var.postgres_server_name}&password=${var.postgres_pass}&sslmode=require&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
    "MB_PASSWORD_LENGTH"   = "10"

    # Use the VNET DNS server (so we receive private endpoint URLs)
    "WEBSITE_DNS_SERVER" = "168.63.129.16"

    "WEBSITES_ENABLE_APP_SERVICE_STORAGE" = false

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
    "SMTP_PASSWORD"                                   = var.sendgrid_password
    "SMTP_PORT"                                       = "587"
    "SMTP_SERVER"                                     = "smtp.sendgrid.net"
    "SMTP_USERNAME"                                   = "apikey"
    "WEBSITE_SMTP_ENABLESSL"                          = "true"
    "WEBSITE_SMTP_PASSWORD"                           = var.sendgrid_password
    "WEBSITE_SMTP_SERVER"                             = "smtp.sendgrid.net"
    "WEBSITE_SMTP_USERNAME"                           = "apikey"
  }

  sticky_settings {
    app_setting_names = [
      "APPINSIGHTS_INSTRUMENTATIONKEY",
      "APPLICATIONINSIGHTS_CONNECTION_STRING ",
      "APPINSIGHTS_PROFILERFEATURE_VERSION",
      "APPINSIGHTS_SNAPSHOTFEATURE_VERSION",
      "ApplicationInsightsAgent_EXTENSION_VERSION",
      "XDT_MicrosoftApplicationInsights_BaseExtensions",
      "DiagnosticServices_EXTENSION_VERSION",
      "InstrumentationEngine_EXTENSION_VERSION",
      "SnapshotDebugger_EXTENSION_VERSION",
      "XDT_MicrosoftApplicationInsights_Mode",
      "XDT_MicrosoftApplicationInsights_PreemptSdk",
      "APPLICATIONINSIGHTS_CONFIGURATION_CONTENT",
      "XDT_MicrosoftApplicationInsightsJava",
      "XDT_MicrosoftApplicationInsights_NodeJS",
    ]
  }

  lifecycle {
    ignore_changes = [
      # validated 5/30/2024
      # The AzureRM Terraform provider provides regional virtual network integration
      # via the standalone resource app_service_virtual_network_swift_connection and
      # in-line within this resource using the virtual_network_subnet_id property.
      # You cannot use both methods simultaneously.
      # If the virtual network is set via the resource app_service_virtual_network_swift_connection
      # then ignore_changes should be used in the web app configuration.
      virtual_network_subnet_id,
      # Ignore auto-generated hidden-links
      tags
    ]
  }
}

resource "azurerm_app_service_virtual_network_swift_connection" "metabase_vnet_integration" {
  app_service_id = azurerm_linux_web_app.metabase.id
  subnet_id      = var.use_cdc_managed_vnet ? var.subnets.public_subnets[0] : var.subnets.public_subnets[2]
}
