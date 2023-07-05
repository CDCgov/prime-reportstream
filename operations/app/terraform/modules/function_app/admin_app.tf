locals {
  # Tie to existing project variables
  interface = {
    function_app_name          = "${var.resource_prefix}-admin-functionapp"
    resource_group_name        = var.resource_group
    location                   = var.location
    app_service_plan_id        = var.app_service_plan
    storage_account_name       = "${var.resource_prefix}storageaccount"
    storage_account_access_key = var.primary_access_key

    # Path to functions parent directory
    # How to create functions:
    # VS Code Azure Functions extension > Create new project
    functions_path = "../../../functions/admin"
  }
  # Set all "Application Settings"
  # Add/remove to adjust dynamically
  app_settings = {
    WEBSITE_DNS_SERVER                 = "${var.dns_ip}"
    FUNCTIONS_WORKER_RUNTIME           = "python"
    WEBSITE_VNET_ROUTE_ALL             = 1
    WEBSITE_CONTENTOVERVNET            = 1
    SCM_DO_BUILD_DURING_DEPLOYMENT     = true
    WEBSITE_HTTPLOGGING_RETENTION_DAYS = 3
    POSTGRES_HOST                      = "${var.resource_prefix}-pgsql.postgres.database.azure.com"
    POSTGRES_USER                      = "@Microsoft.KeyVault(SecretUri=https://${var.app_config_key_vault_name}.vault.azure.net/secrets/functionapp-postgres-user)"
    POSTGRES_PASSWORD                  = "@Microsoft.KeyVault(SecretUri=https://${var.app_config_key_vault_name}.vault.azure.net/secrets/functionapp-postgres-pass)"
  }
  # Set app configuration
  config = {
    use_32_bit_worker_process = false
    vnet_route_all_enabled    = true
    # Deployments may fail if not always on
    always_on                = true
    environment              = var.environment
    linux_fx_version         = "PYTHON|3.9"
    FUNCTIONS_WORKER_RUNTIME = "python"
  }
  # Set network configuration
  network = {
    subnet_id = var.use_cdc_managed_vnet ? var.subnets.public_subnets[0] : var.subnets.public_subnets[2]
  }
  # Set ip restrictions within site_config
  ip_restrictions = [
    # Vnet access
    { action = "Allow", name = "AllowVNetTraffic", priority = 100, virtual_network_subnet_id = var.subnets.public_subnets[2], service_tag = null, ip_address = null },
    { action = "Allow", name = "AllowVNetEastTraffic", priority = 100, virtual_network_subnet_id = var.subnets.public_subnets[0], service_tag = null, ip_address = null },
    { action = "Allow", name = "AllowFrontDoorTraffic", priority = 110, virtual_network_subnet_id = null, service_tag = "AzureFrontDoor.Backend", ip_address = null },
    # Administrator access
    { action = "Allow", name = "admin01", priority = 200, virtual_network_subnet_id = null, service_tag = null, ip_address = "24.163.118.70/32" }
  ]
}

########################################
#
#         DO NOT EDIT BELOW!
#
########################################

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
    dynamic "ip_restriction" {
      for_each = local.ip_restrictions
      content {
        action                    = ip_restriction.value.action
        name                      = ip_restriction.value.name
        priority                  = ip_restriction.value.priority
        virtual_network_subnet_id = ip_restriction.value.virtual_network_subnet_id
        service_tag               = ip_restriction.value.service_tag
        ip_address                = ip_restriction.value.ip_address
      }
    }
  }
  app_settings = local.app_settings
  identity {
    type = "SystemAssigned"
  }
  lifecycle {
    ignore_changes = [
      tags,
      site_config[0].ip_restriction
    ]
  }
  depends_on = [
    var.app_service_plan
  ]
  tags = {
    environment = local.config.environment
    managed-by  = "terraform"
  }
}

resource "time_sleep" "wait_admin_function_app" {
  create_duration = "2m"

  depends_on = [azurerm_function_app.admin]
  triggers = {
    function_app = azurerm_function_app.admin.identity.0.principal_id
  }
}

resource "azurerm_app_service_virtual_network_swift_connection" "admin_function_app_vnet_integration" {
  app_service_id = azurerm_function_app.admin.id
  subnet_id      = local.network.subnet_id
}

locals {
  admin_publish_command = <<EOF
      az functionapp deployment source config-zip --resource-group ${local.interface.resource_group_name} --name ${azurerm_function_app.admin.name} --src ${data.archive_file.admin_function_app.output_path} --build-remote false --timeout 600
    EOF
}

data "archive_file" "admin_function_app" {
  type        = "zip"
  source_dir  = local.interface.functions_path
  output_path = "function-app-admin.zip"

  excludes = [
    ".venv",
    ".vscode",
    "local.settings.json",
    "getting_started.md",
    "README.md",
    ".gitignore"
  ]
}

resource "null_resource" "admin_function_app_publish" {
  provisioner "local-exec" {
    command = var.is_temp_env == true ? "echo 'admin app disabled'" : local.admin_publish_command
  }
  depends_on = [
    local.admin_publish_command,
    azurerm_function_app.admin,
    time_sleep.wait_admin_function_app
  ]
  triggers = {
    input_json           = filemd5(data.archive_file.admin_function_app.output_path)
    publish_code_command = local.admin_publish_command
  }
}
