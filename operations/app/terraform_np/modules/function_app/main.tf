resource "azurerm_function_app" "function_app" {
  name                       = "${var.resource_prefix}-functionapp"
  location                   = var.location
  resource_group_name        = var.resource_group
  app_service_plan_id        = var.app_service_plan
  storage_account_name       = "${var.resource_prefix}storageaccount"
  storage_account_access_key = var.primary_access_key
  https_only                 = true
  os_type                    = "linux"
  version                    = var.function_runtime_version
  enable_builtin_logging     = true

  site_config {
    ip_restriction {
      action                    = "Allow"
      name                      = "AllowVNetTraffic"
      priority                  = 100
      virtual_network_subnet_id = var.subnets.public_subnets[2]
    }

    ip_restriction {
      action                    = "Allow"
      name                      = "AllowVNetEastTraffic"
      priority                  = 100
      virtual_network_subnet_id = var.subnets.public_subnets[0]
    }

    ip_restriction {
      action      = "Allow"
      name        = "AllowFrontDoorTraffic"
      priority    = 110
      service_tag = "AzureFrontDoor.Backend"
    }

    scm_use_main_ip_restriction = true

    http2_enabled             = true
    always_on                 = true
    use_32_bit_worker_process = false
    linux_fx_version          = "DOCKER|${var.container_registry_login_server}/${var.resource_prefix}:latest"
    ftps_state                = "Disabled"

    cors {
      allowed_origins = local.allowed_origins
    }
  }

  app_settings = local.active_slot_settings

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = var.environment
  }

  lifecycle {
    ignore_changes = [
      # validated 5/29/2024
      # Allows Docker versioning via GitHub Actions
      site_config[0].linux_fx_version,
      tags
    ]
  }
}


resource "azurerm_key_vault_access_policy" "functionapp_app_config_access_policy" {
  key_vault_id = var.app_config_key_vault_id
  tenant_id    = azurerm_function_app.function_app.identity[0].tenant_id
  object_id    = azurerm_function_app.function_app.identity[0].principal_id

  secret_permissions = [
    "Get",
  ]
}

resource "azurerm_key_vault_access_policy" "functionapp_client_config_access_policy" {
  key_vault_id = var.client_config_key_vault_id
  tenant_id    = azurerm_function_app.function_app.identity.0.tenant_id
  object_id    = azurerm_function_app.function_app.identity.0.principal_id

  secret_permissions = [
    "Get",
  ]

}

resource "azurerm_app_service_virtual_network_swift_connection" "function_app_vnet_integration" {
  app_service_id = azurerm_function_app.function_app.id
  subnet_id      = var.use_cdc_managed_vnet ? var.subnets.public_subnets[0] : var.subnets.public_subnets[2]
}

# Enable sticky slot settings
# Done via a template due to a missing Terraform feature:
# https://github.com/terraform-providers/terraform-provider-azurerm/issues/1440
# ! Before apply, delete existing "functionapp_sticky_settings" deployment from RG
resource "azurerm_resource_group_template_deployment" "functionapp_sticky_settings" {
  name                = "functionapp_sticky_settings"
  resource_group_name = var.resource_group
  deployment_mode     = "Incremental"

  template_content = <<TEMPLATE
{
  "$schema": "https://schema.management.azure.com/schemas/2015-01-01/deploymentTemplate.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
      "stickyAppSettingNames": {
        "type": "String"
      },
      "webAppName": {
        "type": "String"
      }
  },
  "variables": {
    "appSettingNames": "[split(parameters('stickyAppSettingNames'),',')]"
  },
  "resources": [
      {
        "type": "Microsoft.Web/sites/config",
        "name": "[concat(parameters('webAppName'), '/slotconfignames')]",
        "apiVersion": "2015-08-01",
        "properties": {
          "appSettingNames": "[variables('appSettingNames')]"
        }
      }
  ]
}
TEMPLATE

  parameters_content = jsonencode({
    webAppName = {
      value = azurerm_function_app.function_app.name
    }
    stickyAppSettingNames = {
      value = local.sticky_slot_settings
    }
  })

  lifecycle {
    ignore_changes = [

    ]
  }
  depends_on = [
    azurerm_function_app.function_app,
    azurerm_function_app_slot.candidate,
  ]
}
