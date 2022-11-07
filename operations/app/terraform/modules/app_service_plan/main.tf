resource "azurerm_service_plan" "service_plan" {
  name                = "${var.resource_prefix}-serviceplan"
  location            = var.location
  resource_group_name = var.resource_group
  #kind                = "Linux"
  #reserved            = true
  sku_name            = "P3v2"
  os_type             = "Linux"
  tags = {
    environment = var.environment
  }
}
