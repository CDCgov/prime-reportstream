# Example: Alerting Action with result count trigger
resource "azurerm_monitor_scheduled_query_rules_alert" "functionapp_fatal" {
  name                = format("%s-alertrule-functionapp-fatal", var.resource_prefix)
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group           = []
    email_subject          = "Email Header"
    custom_webhook_payload = "{}"
  }
  data_source_id = azurerm_log_analytics_workspace.law.id
  description    = "Found FATAL-ALERT in FunctionApp logs"
  enabled        = true
  query          = <<-QUERY
  // 2022-03-31: Exclude co-phd.elr-test -- this is a known error per Rick Hood
  FunctionAppLogs
  | where Message contains 'FATAL-ALERT' and not(Message contains 'co-phd.elr-test')
  QUERY
  //query_type = "Number"
  throttling  = 60
  severity    = 2
  frequency   = 5
  time_window = 5
  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }
}
