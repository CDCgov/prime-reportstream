terraform {
    required_version = ">= 0.14"
}

resource "azurerm_application_insights" "app_insights" {
  name = var.name
  location = var.location
  resource_group_name = var.resource_group
  application_type = "web"

  tags = {
    environment = var.environment
  }
}

output "instrumentation_key" {
  value = azurerm_application_insights.app_insights.instrumentation_key
}

output "app_id" {
  value = azurerm_application_insights.app_insights.app_id
}
