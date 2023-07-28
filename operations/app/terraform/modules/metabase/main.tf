resource "azurerm_app_service" "metabase" {
  name                = "${var.resource_prefix}-metabase"
  location            = var.location
  resource_group_name = var.resource_group
  app_service_plan_id = var.service_plan_id
  https_only          = true

  identity {
    type = "SystemAssigned"
  }

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

    ftps_state = "Disabled"

    scm_use_main_ip_restriction = true

    always_on        = true
    linux_fx_version = "DOCKER|metabase/metabase"
  }

  app_settings = {
    "MB_DB_CONNECTION_URI" = "postgresql://${var.postgres_server_name}.postgres.database.azure.com:5432/metabase?user=${var.postgres_user}@${var.postgres_server_name}&password=${var.postgres_pass}&sslmode=require&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
    "MB_PASSWORD_LENGTH"="10"

    # Route outbound traffic through the VNET
    "WEBSITE_VNET_ROUTE_ALL" = 1

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
  }

  lifecycle {
    ignore_changes = [
      # Temp ignore app_settings during terraform overhaul
      app_settings["APPINSIGHTS_INSTRUMENTATIONKEY"],
      app_settings["APPLICATIONINSIGHTS_CONNECTION_STRING"],
      app_settings["MB_DB_CONNECTION_URI"],
    ]
  }
}

resource "azurerm_app_service_virtual_network_swift_connection" "metabase_vnet_integration" {
  app_service_id = azurerm_app_service.metabase.id
  subnet_id      = var.use_cdc_managed_vnet ? var.subnets.public_subnets[0] : var.subnets.public_subnets[2]
}
