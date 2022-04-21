# Severity 0 - Critical
# Severity 1 - Error
# Severity 2 - Warning
# Severity 3 - Informational
# Severity 4 - Verbose

locals {
  database_cpu_threshold = 70
  database_mem_threshold = 85
}

resource "azurerm_monitor_metric_alert" "db_cpu_util" {
  count = local.alerting_enabled

  name                = "${var.environment}-db-cpu"
  description         = "${var.environment} database CPU utilization is greater than ${local.database_cpu_threshold}%"
  resource_group_name = var.resource_group
  scopes              = [var.postgres_server_id]
  frequency           = "PT1M"
  window_size         = "PT5M"
  severity            = 1

  criteria {
    aggregation      = "Average"
    metric_name      = "cpu_percent"
    metric_namespace = "Microsoft.DBforPostgreSQL/servers"
    operator         = "GreaterThanOrEqual"
    threshold        = local.database_cpu_threshold
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }
}

resource "azurerm_monitor_metric_alert" "db_mem_util" {
  count = local.alerting_enabled

  name                = "${var.environment}-db-mem"
  description         = "${var.environment} database memory utilization is greater than ${local.database_mem_threshold}%"
  resource_group_name = var.resource_group
  scopes              = [var.postgres_server_id]
  frequency           = "PT1M"
  window_size         = "PT5M"
  severity            = 1

  criteria {
    aggregation      = "Average"
    metric_name      = "memory_percent"
    metric_namespace = "Microsoft.DBforPostgreSQL/servers"
    operator         = "GreaterThanOrEqual"
    threshold        = local.database_mem_threshold
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "db_query_duration" {
  count = local.alerting_enabled

  name                = "${var.environment}-db-query-duration"
  description         = "${var.environment} DB query durations >= 10s"
  location            = var.location
  resource_group_name = var.resource_group
  severity            = 2
  frequency           = 5
  time_window         = 5

  data_source_id = local.app_insights_id

  query = <<-QUERY
dependencies
| where timestamp >= ago(5m) and name has "SQL:" and duration >= 10000
  QUERY

  trigger {
    operator  = "GreaterThan"
    threshold = 0
  }

  action {
    action_group = [local.action_group_id]
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "db_query_duration_over_time_window" {
  count = local.alerting_enabled

  name                = "${var.environment}-db-query-duration-over-time-window"
  description         = "10+ DB queries with durations over 1.25s in the past 5 minutes"
  location            = var.location
  resource_group_name = var.resource_group
  severity            = 1
  frequency           = 5
  time_window         = 5

  data_source_id = local.app_insights_id

  query = <<-QUERY
dependencies
| where timestamp >= ago(5m) and name has "SQL:" and duration > 1250
  QUERY

  trigger {
    operator  = "GreaterThan"
    threshold = 10
  }

  action {
    action_group = [local.action_group_id]
  }
}