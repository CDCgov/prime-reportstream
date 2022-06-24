# Severity 0 - Critical
# Severity 1 - Error
# Severity 2 - Warning
# Severity 3 - Informational
# Severity 4 - Verbose

resource "azurerm_monitor_metric_alert" "exception_alert_critical" {
  count               = local.alerting_enabled
  name                = "Over 100 Exceptions Raised in the Last Hour"
  description         = "Over 100 Exceptions Raised in the Last Hour"
  resource_group_name = var.resource_group
  scopes              = [azurerm_application_insights.app_insights.id]
  window_size         = "PT1H"
  frequency           = "PT1M"
  severity            = 0

  criteria {
    metric_namespace = "microsoft.insights/components"
    metric_name      = "exceptions/count"
    aggregation      = "Count"
    operator         = "GreaterThan"
    threshold        = 99
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_monitor_metric_alert" "exception_alert_error" {
  count               = local.alerting_enabled
  name                = "Over 10 Exceptions Raised in the Last Hour"
  description         = "Over 10 Exceptions Raised in the Last Hour"
  resource_group_name = var.resource_group
  scopes              = [azurerm_application_insights.app_insights.id]
  window_size         = "PT1H"
  frequency           = "PT1M"
  severity            = 1

  criteria {
    metric_namespace = "microsoft.insights/components"
    metric_name      = "exceptions/count"
    aggregation      = "Count"
    operator         = "GreaterThan"
    threshold        = 9
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_monitor_metric_alert" "exception_alert_warn" {
  count               = local.prod_exclusive_alerting
  name                = "One or More Exceptions Raised in the Last Hour"
  description         = "One or More Exceptions Raised in the Last Hour"
  resource_group_name = var.resource_group
  scopes              = [azurerm_application_insights.app_insights.id]
  window_size         = "PT1H"
  frequency           = "PT1M"
  severity            = 2

  criteria {
    metric_namespace = "microsoft.insights/components"
    metric_name      = "exceptions/count"
    aggregation      = "Count"
    operator         = "GreaterThan"
    threshold        = 0
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }

  tags = {
    environment = var.environment
  }
}