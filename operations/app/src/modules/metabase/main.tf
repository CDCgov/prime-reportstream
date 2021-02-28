terraform {
    required_version = ">= 0.14"
}

resource "azurerm_app_service_plan" "service_plan" {
  count = (var.environment == "prod" ? 1 : 0)
  name = "${var.resource_prefix}-metabaseserviceplan"
  location = var.location
  resource_group_name = var.resource_group
  kind = "Linux"
  reserved = true
  maximum_elastic_worker_count = 1

  sku {
    tier = "PremiumV2"
    size = "P2v2"
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_app_service" "metabase" {
  name = var.name
  location = var.location
  resource_group_name = var.resource_group
  app_service_plan_id = (var.environment == "prod" ? azurerm_app_service_plan.service_plan[0].id : var.app_service_plan_id)
  https_only = true

  site_config {
    ip_restriction {
      action = "Allow"
      name = "AllowVNetTraffic"
      priority = 100
      virtual_network_subnet_id = var.public_subnet_id
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
    "MB_DB_CONNECTION_URI" = var.postgres_url
    "WEBSITE_VNET_ROUTE_ALL" = 1
    "WEBSITES_ENABLE_APP_SERVICE_STORAGE" = false
    "APPINSIGHTS_INSTRUMENTATIONKEY" = var.ai_instrumentation_key
  }
}

resource "azurerm_app_service_virtual_network_swift_connection" "metabase_vnet_integration" {
  app_service_id = azurerm_app_service.metabase.id
  subnet_id = var.public_subnet_id
}
