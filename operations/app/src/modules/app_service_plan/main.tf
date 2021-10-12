resource "azurerm_app_service_plan" "service_plan" {
  name                = "${var.resource_prefix}-serviceplan"
  location            = var.location
  resource_group_name = var.resource_group
  kind                = "Linux"
  reserved            = true

  sku {
    tier = "PremiumV3"
    size = "P2v3"
  }

  tags = {
    environment = var.environment
  }
}
