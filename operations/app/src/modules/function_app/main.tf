terraform {
    required_version = ">= 0.14"
}

resource "azurerm_app_service_plan" "service_plan" {
  name = "${var.resource_prefix}-serviceplan"
  location = var.location
  resource_group_name = var.resource_group
  kind = (var.environment == "prod" ? "elastic" : "Linux")
  reserved = true
  maximum_elastic_worker_count = 10

  sku {
    tier = (var.environment == "prod" ? "ElasticPremium" : "PremiumV2")
    size = (var.environment == "prod" ? "EP1" : "P2v2")
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_function_app" "function_app" {
  name = "${var.resource_prefix}-functionapp"
  location = var.location
  resource_group_name = var.resource_group
  app_service_plan_id = azurerm_app_service_plan.service_plan.id
  storage_account_name = var.storage_account_name
  storage_account_access_key = var.storage_account_key
  https_only = true
  os_type = "linux"
  version = 3

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
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_app_service_virtual_network_swift_connection" "function_app_vnet_integration" {
  app_service_id = azurerm_function_app.function_app.id
  subnet_id = var.public_subnet_id
}
