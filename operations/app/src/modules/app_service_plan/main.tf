terraform {
    required_version = ">= 0.14"
}

resource "azurerm_app_service_plan" "service_plan" {
  name = var.name
  location = var.location
  resource_group_name = var.resource_group
  kind = "Linux"

  sku {
    tier = "PremiumV2"
    size = "P2v2"
  }

  tags = {
    environment = var.environment
  }
}

output "app_service_plan_id" {
  value = azurerm_app_service_plan.service_plan.id
}
