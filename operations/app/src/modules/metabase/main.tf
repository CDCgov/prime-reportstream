resource "azurerm_app_service" "metabase" {
  name = "${var.resource_prefix}-metabase"
  location = var.location
  resource_group_name = var.resource_group
  app_service_plan_id = data.azurerm_app_service_plan.service_plan.id
  https_only = true

  site_config {
    ip_restriction {
      action = "Allow"
      name = "AllowVNetTraffic"
      priority = 100
      virtual_network_subnet_id = data.azurerm_subnet.public.id
    }
    ip_restriction {
      action = "Allow"
      name = "AllowFrontDoorTraffic"
      priority = 110
      service_tag = "AzureFrontDoor.Backend"
    }
    scm_use_main_ip_restriction = true

    always_on = true
    linux_fx_version = "DOCKER|metabase/metabase"
  }

  app_settings = {
    "MB_DB_CONNECTION_URI" = "postgresql://${data.azurerm_postgresql_server.postgres_server.name}.postgres.database.azure.com:5432/metabase?user=${data.azurerm_key_vault_secret.postgres_user.value}@${data.azurerm_postgresql_server.postgres_server.name}&password=${data.azurerm_key_vault_secret.postgres_pass.value}&sslmode=require&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
    "WEBSITE_VNET_ROUTE_ALL" = 1
    "WEBSITES_ENABLE_APP_SERVICE_STORAGE" = false
    "APPINSIGHTS_INSTRUMENTATIONKEY" = var.ai_instrumentation_key
  }
}

resource "azurerm_app_service_virtual_network_swift_connection" "metabase_vnet_integration" {
  app_service_id = azurerm_app_service.metabase.id
  subnet_id = data.azurerm_subnet.public.id
}
