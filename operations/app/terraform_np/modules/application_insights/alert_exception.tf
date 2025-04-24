# Severity 0 - Critical
# Severity 1 - Error
# Severity 2 - Warning
# Severity 3 - Informational
# Severity 4 - Verbose

resource "azurerm_monitor_scheduled_query_rules_alert" "exception_alert_critical" {
  count               = local.alerting_enabled
  name                = "Over 100 Exceptions Raised in the Last Hour"
  description         = "Over 100 Exceptions Raised in the Last Hour"
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group = [azurerm_monitor_action_group.action_group[0].id]
  }
  data_source_id = azurerm_application_insights.app_insights.id
  enabled        = true
  query          = <<-EOT
    let requests = requests
    | distinct id;
    let pageViews = pageViews
    | distinct id;
    let trace = union requests, pageViews;
    exceptions
    | join kind=inner trace on $left.operation_ParentId == $right.id
  EOT
  throttling     = 120
  severity       = 0
  frequency      = 15
  time_window    = 60

  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 100
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "exception_alert_error" {
  count               = local.alerting_enabled
  name                = "Over 10 Exceptions Raised in the Last Hour"
  description         = "Over 10 Exceptions Raised in the Last Hour"
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group = [azurerm_monitor_action_group.action_group[0].id]
  }
  data_source_id = azurerm_application_insights.app_insights.id
  enabled        = true
  query          = <<-EOT
    let requests = requests
    | distinct id;
    let pageViews = pageViews
    | distinct id;
    let trace = union requests, pageViews;
    exceptions
    | join kind=inner trace on $left.operation_ParentId == $right.id
  EOT
  throttling     = 120
  severity       = 2
  frequency      = 15
  time_window    = 60

  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 10
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "exception_alert_warn" {
  count               = local.alerting_enabled
  name                = "One or More Exceptions Raised in the Last Hour"
  description         = "One or More Exceptions Raised in the Last Hour"
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group = [azurerm_monitor_action_group.action_group[0].id]
  }
  data_source_id = azurerm_application_insights.app_insights.id
  enabled        = true
  query          = <<-EOT
    let requests = requests
    | distinct id;
    let pageViews = pageViews
    | distinct id;
    let trace = union requests, pageViews;
    exceptions
    | join kind=inner trace on $left.operation_ParentId == $right.id
  EOT
  throttling     = 120
  severity       = 3
  frequency      = 15
  time_window    = 60

  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }
}