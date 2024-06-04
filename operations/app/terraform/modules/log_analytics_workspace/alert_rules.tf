# Example: Alerting Action with result count trigger
resource "azurerm_monitor_scheduled_query_rules_alert" "functionapp_fatal" {
  name                = format("%s-alertrule-functionapp-fatal", var.resource_prefix)
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group  = [var.action_group_slack_id]
    email_subject = "Found FATAL-ALERT in Production FunctionApp logs"
  }
  data_source_id = local.log_analytics_workspace_id
  description    = "Found FATAL-ALERT in FunctionApp logs"
  enabled        = true
  query          = <<-EOT
            // 2022-03-31: Exclude co-phd.elr-test -- this is a known error per Rick Hood
            FunctionAppLogs
            | where Message contains 'FATAL-ALERT' and not(Message contains 'co-phd.elr-test')
            and not(Message contains 'IGNORE--')
        EOT
  throttling     = 120
  severity       = 2
  frequency      = 15
  time_window    = 30

  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "metabase_webapp_alertrule" {
  name                = format("%s-metabase-webapp-alertrule", var.resource_prefix)
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group = [var.action_group_metabase_id]
  }
  data_source_id = local.log_analytics_workspace_id
  description    = "Critical Alert found in Metabase WebApp logs: Service unavailable"
  enabled        = false
  query          = <<-EOT
            AzureDiagnostics
            | where requestUri_s contains "metabase/api/health" and httpStatusCode_d != 200
            | where errorInfo_s == "OriginConnectionRefused"
        EOT
  tags           = {}
  severity       = 0
  frequency      = 5
  time_window    = 5

  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "functionapp_401errors" {
  name                = format("%s-alertrule-fa-401errors", var.resource_prefix)
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group = [var.action_group_slack_id]
  }
  data_source_id = local.log_analytics_workspace_id
  description    = "Found 10 and more 401s errors in FunctionApp logs"
  enabled        = true
  query          = <<-EOT
      AzureDiagnostics
      | where httpStatusCode_d == 401
      | where requestUri_s contains 'organizations'
        and not(requestUri_s contains 'simple_report')
        and not(requestUri_s contains 'ignore')
        and not(requestUri_s contains 'al-phd') 
  EOT
  throttling     = 120
  severity       = 2
  frequency      = 15
  time_window    = 30

  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 10
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "functionapp_504errors" {
  name                = format("%s-alertrule-fa-504errors", var.resource_prefix)
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group = var.resource_prefix == "pdhstaging" ? [var.action_group_id] : [var.action_group_slack_id]
  }
  data_source_id = local.log_analytics_workspace_id
  description    = "Found 1 or more 504s errors in FunctionApp logs"
  enabled        = true
  query          = <<-EOT
      AzureDiagnostics
      | where httpStatusCode_d == 504
  EOT
  frequency      = 5
  time_window    = 5

  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }
}

resource "azurerm_monitor_scheduled_query_rules_alert" "poison-queue-msg" {
  name                = format("%s-alertrule-poison-queue-msg", var.resource_prefix)
  location            = var.location
  resource_group_name = var.resource_group

  action {
    action_group  = [var.action_group_slack_id]
    email_subject = "Found a new message in a poison queue"
  }
  data_source_id = local.log_analytics_workspace_id
  description    = "Found a new message in a poison queue"
  enabled        = true
  query          = <<-EOT
            StorageQueueLogs
             | where OperationName contains "PutMessage"
             | where AccountName contains "storageaccount"
             | where StatusText contains "Success"
             | where ObjectKey contains "-poison/"
        EOT
  throttling     = 120
  severity       = 2
  frequency      = 15
  time_window    = 30

  trigger {
    operator  = "GreaterThanOrEqual"
    threshold = 1
  }
}


locals {
  log_analytics_workspace_id = azurerm_log_analytics_workspace.law.id
  # log_analytics_workspace_id = replace(replace(azurerm_log_analytics_workspace.law.id, "Microsoft.OperationalInsights", "microsoft.operationalinsights"), "resourceGroups", "resourcegroups")
}