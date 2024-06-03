resource "azurerm_service_plan" "service_plan" {
  #checkov:skip=CKV_AZURE_225: "Ensure the App Service Plan is zone redundant"
  name                   = "${var.resource_prefix}-serviceplan"
  location               = var.location
  resource_group_name    = var.resource_group
  sku_name               = var.app_size
  os_type                = "Linux"
  zone_balancing_enabled = false
  worker_count           = 6
  tags = {
    environment = var.environment
  }
}
