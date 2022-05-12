locals {
  # Tie to existing project variables
  interface = {
    function_app_name          = "${var.resource_prefix}-admin-functionapp"
    resource_group_name        = var.resource_group
    location                   = var.location
    app_service_plan_id        = var.app_service_plan
    storage_account_name       = "${var.resource_prefix}storageaccount"
    storage_account_access_key = var.primary_access_key
  }
  # Set all "Application Settings"
  # Add/remove to adjust dynamically
  app_settings = {
    WEBSITE_DNS_SERVER       = "${var.dns_ip}"
    FUNCTIONS_WORKER_RUNTIME = "python"
    WEBSITE_VNET_ROUTE_ALL   = 1
    WEBSITE_CONTENTOVERVNET  = 1
  }
  # Set app configuration
  config = {
    use_32_bit_worker_process = false
    vnet_route_all_enabled    = true
    always_on                 = false
    environment               = var.environment
    linux_fx_version          = "python|3.9"
    FUNCTIONS_WORKER_RUNTIME  = "python"
  }
}


resource "azurerm_function_app" "admin" {
  name                       = local.interface.function_app_name
  location                   = local.interface.location
  resource_group_name        = local.interface.resource_group_name
  app_service_plan_id        = local.interface.app_service_plan_id
  storage_account_name       = local.interface.storage_account_name
  storage_account_access_key = local.interface.storage_account_access_key
  https_only                 = true
  os_type                    = "linux"
  version                    = "~4"
  enable_builtin_logging     = false
  site_config {
    ftps_state                = "Disabled"
    linux_fx_version          = local.config.linux_fx_version
    use_32_bit_worker_process = local.config.use_32_bit_worker_process
    vnet_route_all_enabled    = local.config.vnet_route_all_enabled
    always_on                 = local.config.always_on
  }
  app_settings = local.app_settings
  identity {
    type = "SystemAssigned"
  }
  lifecycle {
    ignore_changes = [
      tags
    ]
  }
  tags = {
    environment = local.config.environment
    managed-by  = "terraform"
  }
}
