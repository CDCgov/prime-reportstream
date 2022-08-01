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

resource "azurerm_monitor_diagnostic_setting" "diagnostics" {
  for_each                   = data.azurerm_monitor_diagnostic_categories.diagnostics
  name                       = "${var.resource_prefix}-${each.key}-diag"
  target_resource_id         = each.value.id
  log_analytics_workspace_id = azurerm_log_analytics_workspace.law.id

  dynamic "log" {
    for_each = each.value.logs
    content {
      category = log.value

      retention_policy {
        enabled = true
        days    = 60
      }
    }
  }

  dynamic "metric" {
    for_each = each.value.metrics
    content {
      category = metric.value

      retention_policy {
        enabled = true
        days    = 60
      }
    }
  }

  lifecycle {
    ignore_changes = [
      # Case does not apply correctly
      log_analytics_workspace_id,
      log_analytics_destination_type
    ]
  }
}
