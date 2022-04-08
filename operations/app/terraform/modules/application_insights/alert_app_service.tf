# Severity 0 - Critical
# Severity 1 - Error
# Severity 2 - Warning
# Severity 3 - Informational
# Severity 4 - Verbose

locals {
  app_service_cpu_threshold = 70
  app_service_mem_threshold = 85
}


resource "azurerm_monitor_metric_alert" "app_service_cpu_util" {
  count = local.alerting_enabled

  name                = "${var.environment}-api-cpu"
  description         = "${var.environment} CPU utilization is greater than ${local.app_service_cpu_threshold}%"
  resource_group_name = var.resource_group
  scopes              = [var.service_plan_id]
  frequency           = "PT1M"
  window_size         = "PT5M"
  severity            = 1

  criteria {
    aggregation      = "Average"
    metric_name      = "CpuPercentage"
    metric_namespace = "Microsoft.Web/serverfarms"
    operator         = "GreaterThanOrEqual"
    threshold        = local.app_service_cpu_threshold
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }
}

resource "azurerm_monitor_metric_alert" "app_service_mem_util" {
  count = local.alerting_enabled

  name                = "${var.environment}-api-mem"
  description         = "${var.environment} memory utilization is greater than ${local.app_service_mem_threshold}%"
  resource_group_name = var.resource_group
  scopes              = [var.service_plan_id]
  frequency           = "PT1M"
  window_size         = "PT5M"
  severity            = 1

  criteria {
    aggregation      = "Average"
    metric_name      = "MemoryPercentage"
    metric_namespace = "Microsoft.Web/serverfarms"
    operator         = "GreaterThanOrEqual"
    threshold        = local.app_service_mem_threshold
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }
}