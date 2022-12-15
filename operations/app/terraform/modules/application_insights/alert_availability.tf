# Severity 0 - Critical
# Severity 1 - Error
# Severity 2 - Warning
# Severity 3 - Informational
# Severity 4 - Verbose

resource "azurerm_monitor_metric_alert" "availability_alert" {
  count               = local.alerting_enabled
  name                = "${var.resource_prefix}-availability_alert"
  description         = "Degraded Availability"
  resource_group_name = var.resource_group
  scopes              = [azurerm_application_insights.app_insights.id]
  window_size         = "PT1H"
  frequency           = "PT15M"
  severity            = 0

  criteria {
    metric_namespace = "microsoft.insights/components"
    metric_name      = "availabilityResults/availabilityPercentage"
    aggregation      = "Average"
    operator         = "LessThan"
    threshold        = 95
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_monitor_metric_alert" "availability_alert_warning" {
  count               = local.alerting_enabled
  name                = "${var.resource_prefix}-availability-alert-warning"
  description         = "Degraded Availability"
  resource_group_name = var.resource_group
  scopes              = [azurerm_application_insights.app_insights.id]
  window_size         = "PT1H"
  frequency           = "PT15M"
  severity            = 2

  criteria {
    metric_namespace = "microsoft.insights/components"
    metric_name      = "availabilityResults/availabilityPercentage"
    aggregation      = "Average"
    operator         = "LessThan"
    threshold        = 100
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_application_insights_web_test" "ping_test" {
  count                   = local.alerting_enabled
  name                    = "${var.resource_prefix}-pingfunctions"
  location                = var.location
  resource_group_name     = var.resource_group
  application_insights_id = azurerm_application_insights.app_insights.id
  kind                    = "ping"
  frequency               = 300
  timeout                 = 60
  enabled                 = true
  retry_enabled           = true
  geo_locations           = ["us-va-ash-azr"]

  configuration = <<XML
<WebTest Name="" Id="" Enabled="True" CssProjectStructure="" CssIteration="" Timeout="60" WorkItemIds="" xmlns="http://microsoft.com/schemas/VisualStudio/TeamTest/2010" Description="" CredentialUserName="" CredentialPassword="" PreAuthenticate="True" Proxy="default" StopOnError="False" RecordedResultFile="" ResultsLocale="">
      <Items>
        <Request Method="GET" Version="1.1" Url="${local.ping_url}" ThinkTime="0" Timeout="60" ParseDependentRequests="False" FollowRedirects="True" RecordResult="True" Cache="False" ResponseTimeGoal="0" Encoding="utf-8" ExpectedHttpStatusCode="200" ExpectedResponseUrl="" ReportingName="" IgnoreHttpStatusCode="False"/>
      </Items>
    </WebTest>
    XML

  tags = {
    environment = var.environment
    # This prevents terraform from seeing a tag change for each plan/apply
    "hidden-link:/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/${var.resource_group}/providers/Microsoft.Insights/components/${var.resource_prefix}-appinsights" = "Resource"
  }
}

resource "azurerm_application_insights_web_test" "metabase_test" {
  name                    = "${var.resource_prefix}-metabase-health-check"
  location                = var.location
  resource_group_name     = var.resource_group
  application_insights_id = azurerm_application_insights.app_insights.id
  kind                    = "ping"
  frequency               = 300
  timeout                 = 60
  enabled                 = true
  retry_enabled           = true
  geo_locations           = ["us-va-ash-azr", "us-ca-sjc-azr", "us-fl-mia-edge"]
  configuration           = <<XML
<WebTest Name="" Id="" Enabled="True" CssProjectStructure="" CssIteration="" Timeout="60" WorkItemIds="" xmlns="http://microsoft.com/schemas/VisualStudio/TeamTest/2010" Description="" CredentialUserName="" CredentialPassword="" PreAuthenticate="True" Proxy="default" StopOnError="False" RecordedResultFile="" ResultsLocale="">
      <Items>
        <Request Method="GET" Version="1.1" Url="https://${var.environment}.prime.cdc.gov/metabase/api/health" ThinkTime="0" Timeout="60" ParseDependentRequests="False" FollowRedirects="True" RecordResult="True" Cache="False" ResponseTimeGoal="0" Encoding="utf-8" ExpectedHttpStatusCode="200" ExpectedResponseUrl="" ReportingName="" IgnoreHttpStatusCode="False"/>
      </Items>
    </WebTest>
    XML

  tags = {
    environment = var.environment
    # This prevents terraform from seeing a tag change for each plan/apply
    "hidden-link:/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/${var.resource_group}/providers/Microsoft.Insights/components/${var.resource_prefix}-appinsights" = "Resource"
  }
}