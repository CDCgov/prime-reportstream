resource "azurerm_service_plan" "service_plan" {
  name                = "${var.resource_prefix}-serviceplan"
  location            = var.location
  resource_group_name = var.resource_group
  sku_name            = var.app_size
  os_type             = "Linux"
  tags = {
    environment = var.environment
  }
}
