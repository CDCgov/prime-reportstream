resource "azurerm_service_plan" "service_plan" {
  name                   = "${var.resource_prefix}-serviceplan"
  location               = var.location
  resource_group_name    = var.resource_group
  sku_name               = var.app_size
  os_type                = "Linux"
  zone_balancing_enabled = true
  worker_count           = 6
  tags = {
    environment = var.environment
  }
  lifecycle {
    ignore_changes = [
      zone_balancing_enabled, worker_count
    ]
  }
}
