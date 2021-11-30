# Severity 0 - Critical
# Severity 1 - Error
# Severity 2 - Warning
# Severity 3 - Informational
# Severity 4 - Verbose

resource "azurerm_monitor_scheduled_query_rules_alert" "http_response_time" {
  count = local.alerting_enabled

  name                = "${var.environment}-api-http-response"
  description         = "${var.environment} network response >= 10000ms(10s)"
  location            = var.location
  resource_group_name = var.resource_group
  severity            = 2
  frequency           = 5
  time_window         = 5

  data_source_id = azurerm_application_insights.app_insights.id

  query = <<-QUERY
requests
| where duration >= 10000 and timestamp >= ago(5m)
  QUERY

  trigger {
    operator  = "GreaterThan"
    threshold = 14
  }

  action {
    action_group = [azurerm_monitor_action_group.action_group[0].id]
  }
}

resource "azurerm_monitor_smart_detector_alert_rule" "failure_anomalies" {
  count = local.alerting_enabled

  name                = "${var.environment}-failure-anomalies"
  description         = "${var.environment} Failure Anomalies notifies you of an unusual rise in the rate of failed HTTP requests or dependency calls."
  resource_group_name = var.resource_group
  severity            = "Sev1"
  scope_resource_ids  = [azurerm_application_insights.app_insights.id]
  frequency           = "PT1M"
  detector_type       = "FailureAnomaliesDetector"

  action_group {
    ids = [azurerm_monitor_action_group.action_group[0].id]
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "http_2xx_failed_requests" {
  count = local.alerting_enabled

  name                = "${var.environment}-api-2xx-failed-requests"
  description         = "${var.environment} HTTP Server 2xx Errors (where successful request == false) >= 10"
  location            = var.location
  resource_group_name = var.resource_group
  severity            = 1
  frequency           = 5
  time_window         = 5

  data_source_id = azurerm_application_insights.app_insights.id

  query = <<-QUERY
requests
| where toint(resultCode) between (200 .. 299) and success == false and timestamp >= ago(5m)
  QUERY

  trigger {
    operator  = "GreaterThan"
    threshold = 14
  }

  action {
    action_group = [azurerm_monitor_action_group.action_group[0].id]
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "http_4xx_errors" {
  count = local.alerting_enabled

  name                = "${var.environment}-api-4xx-errors"
  description         = "${var.environment} HTTP Server 4xx Errors (excluding 401s) >= 10"
  location            = var.location
  resource_group_name = var.resource_group
  severity            = 1
  frequency           = 5
  time_window         = 5

  data_source_id = azurerm_application_insights.app_insights.id

  query = <<-QUERY
requests
| where toint(resultCode) >= 400 and toint(resultCode) < 500 and toint(resultCode) != 401 and timestamp >= ago(5m)
  QUERY

  trigger {
    operator  = "GreaterThan"
    threshold = 99
  }

  action {
    action_group = [azurerm_monitor_action_group.action_group[0].id]
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "http_5xx_errors" {
  count = local.alerting_enabled

  name                = "${var.environment}-api-5xx-errors"
  description         = "${var.environment} HTTP Server 5xx Errors >= 10"
  location            = var.location
  resource_group_name = var.resource_group
  severity            = 0
  frequency           = 5
  time_window         = 5

  data_source_id = azurerm_application_insights.app_insights.id

  query = <<-QUERY
requests
| where toint(resultCode) >= 500 and timestamp >= ago(5m)
  QUERY

  trigger {
    operator  = "GreaterThan"
    threshold = 9
  }

  action {
    action_group = [azurerm_monitor_action_group.action_group[0].id]
  }
}