resource "azurerm_log_analytics_workspace" "law" {
  name                = "${var.resource_prefix}-law"
  location            = var.location
  resource_group_name = var.resource_group
  sku                 = "PerGB2018"
  retention_in_days   = 30

  tags = {
    environment = var.environment
  }
}

locals {
  default = {
    "function_app" = {
      id   = data.azurerm_function_app.function_app.id
      name = "function_app"
      diag_logs = data.azurerm_monitor_diagnostic_categories.function_app.logs
    },
    "service_plan" = {
      id   = data.azurerm_app_service_plan.service_plan.id
      name = "service_plan"
      diag_logs = data.azurerm_monitor_diagnostic_categories.service_plan.logs
    },
    "container_registry" = {
      id   = data.azurerm_container_registry.container_registry.id
      name = "container_registry"
      diag_logs = data.azurerm_monitor_diagnostic_categories.container_registry.logs
    },
    "postgres_server" = {
      id   = data.azurerm_postgresql_server.postgres_server.id
      name = "postgres_server"
      diag_logs = data.azurerm_monitor_diagnostic_categories.postgres_server.logs
    },
    "front_door" = {
      id   = data.azurerm_resources.front_door.resources[0].id
      name = "front_door"
      diag_logs = data.azurerm_monitor_diagnostic_categories.front_door.logs
    },
    "application_key_vault" = {
      id   = data.azurerm_key_vault.application_key_vault.id
      name = "application_key_vault"
      diag_logs = data.azurerm_monitor_diagnostic_categories.application_key_vault.logs
    },
    "app_config_key_vault" = {
      id   = data.azurerm_key_vault.app_config_key_vault.id
      name = "app_config_key_vault"
      diag_logs = data.azurerm_monitor_diagnostic_categories.app_config_key_vault.logs
    },
    "client_config_key_vault" = {
      id   = data.azurerm_key_vault.client_config_key_vault.id
      name = "client_config_key_vault"
      diag_logs = data.azurerm_monitor_diagnostic_categories.client_config_key_vault.logs
    },
    "west_vnet" = {
      id   = data.azurerm_virtual_network.west_vnet.id
      name = "west_vnet"
      diag_logs = data.azurerm_monitor_diagnostic_categories.west_vnet.logs
    },
    "east_vnet" = {
      id   = data.azurerm_virtual_network.east_vnet.id
      name = "east_vnet"
      diag_logs = data.azurerm_monitor_diagnostic_categories.east_vnet.logs
    },
    "storage_account" = {
      id   = data.azurerm_storage_account.storage_account.id
      name = "storage_account"
      diag_logs = data.azurerm_monitor_diagnostic_categories.storage_account.logs
    },
    "storage_public" = {
      id   = data.azurerm_storage_account.storage_public.id
      name = "storage_public"
      diag_logs = data.azurerm_monitor_diagnostic_categories.storage_public.logs
    },
    "storage_partner" = {
      id   = data.azurerm_storage_account.storage_partner.id
      name = "storage_partner"
      diag_logs = data.azurerm_monitor_diagnostic_categories.storage_partner.logs
    }
  }
}

resource "azurerm_monitor_diagnostic_setting" "diagnostics" {
  for_each                   = local.default
  name                       = "${var.resource_prefix}-${each.value.name}-diag"
  target_resource_id         = each.value.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.law.id

  dynamic "log" {
    for_each = each.value.diag_logs
    content {
      category = log.value

        retention_policy {
        enabled = true
        days    = 60
        }
    }
  }

  metric {
    category = "AllMetrics"

    retention_policy {
      enabled = true
      days    = 60
    }
  }
}
