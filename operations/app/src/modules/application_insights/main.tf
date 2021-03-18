terraform {
    required_version = ">= 0.14"
}

locals{
  devops = {
    "cglodosky" = "que3@cdc.gov"
    "rhawes" = "qom6@cdc.gov"
    "rheft" = "qwp3@cdc.gov"
  }

  developers = {
    "jduff" = "qop5@cdc.gov"
    "mreeves" = "qva8@cdc.gov"
    "myoung" = "qtv1@cdc.gov"
  }

  ping_url = (var.environment == "prod" ? "https://prime.cdc.gov" : "https://${var.environment}.prime.cdc.gov")
}

resource "azurerm_application_insights" "app_insights" {
  name = "${var.resource_prefix}-appinsights"
  location = var.location
  resource_group_name = var.resource_group
  application_type = "web"

  tags = {
    environment = var.environment
  }
}

resource "azurerm_monitor_action_group" "action_group" {
  count = (var.environment != "dev" ? 1 : 0)
  name = "${var.resource_prefix}-actiongroup"
  resource_group_name = var.resource_group
  short_name = "ReportStream"

  sms_receiver {
    name = "cglodosky_-SMSAction-"
    country_code = "1"
    phone_number = "7155704677"
  }

  dynamic "email_receiver" {
    for_each = local.devops
    content {
      name = "${email_receiver.key}_-EmailAction-"
      email_address = email_receiver.value
      use_common_alert_schema = true
    }
  }

  dynamic "email_receiver" {
    for_each = (var.environment == "prod" ? local.developers : {})
    content {
      name = "${email_receiver.key}_-EmailAction-"
      email_address = email_receiver.value
      use_common_alert_schema = true
    }
  }
}

# Severity 0 - Critical 
# Severity 1 - Error
# Severity 2 - Warning
# Severity 3 - Informational
# Severity 4 - Verbose

resource "azurerm_monitor_metric_alert" "availability_alert" {
  count = (var.environment != "dev" ? 1 : 0)
  name = "Degraded Availability"
  resource_group_name = var.resource_group
  scopes = [azurerm_application_insights.app_insights.id]
  window_size = "PT1H"
  frequency = "PT30M"
  severity = 0

  criteria {
    metric_namespace = "microsoft.insights/components"
    metric_name = "availabilityResults/availabilityPercentage"
    aggregation = "Average"
    operator = "LessThan"
    threshold = 100
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }
}

resource "azurerm_monitor_metric_alert" "exception_alert" {
  count = (var.environment != "dev" ? 1 : 0)
  name = "Exception(s) Raised"
  resource_group_name = var.resource_group
  scopes = [azurerm_application_insights.app_insights.id]
  window_size = "PT1H"
  frequency = "PT30M"
  severity = 1

  criteria {
    metric_namespace = "microsoft.insights/components"
    metric_name = "exceptions/count"
    aggregation = "Count"
    operator = "GreaterThan"
    threshold = 0
  }

  action {
    action_group_id = azurerm_monitor_action_group.action_group[0].id
  }
}

resource "azurerm_application_insights_web_test" "ping_test" {
  count = (var.environment != "dev" ? 1 : 0)
  name = "${var.resource_prefix}-pingfunctions"
  location = var.location
  resource_group_name = var.resource_group
  application_insights_id = azurerm_application_insights.app_insights.id
  kind = "ping"
  frequency = 300
  timeout = 60
  enabled = true
  retry_enabled = true
  geo_locations = ["us-va-ash-azr"]

  configuration = <<XML
    <WebTest Name="" Id="" Enabled="True" CssProjectStructure="" CssIteration="" Timeout="60" WorkItemIds="" xmlns="http://microsoft.com/schemas/VisualStudio/TeamTest/2010" Description="" CredentialUserName="" CredentialPassword="" PreAuthenticate="True" Proxy="default" StopOnError="False" RecordedResultFile="" ResultsLocale="">
      <Items>
        <Request Method="GET"  Version="1.1" Url="${local.ping_url}" ThinkTime="0" Timeout="60" ParseDependentRequests="False" FollowRedirects="True" RecordResult="True" Cache="False" ResponseTimeGoal="0" Encoding="utf-8" ExpectedHttpStatusCode="200" ExpectedResponseUrl="" ReportingName="" IgnoreHttpStatusCode="False"/>
      </Items>
    </WebTest>
    XML

  tags = {
    "environment" = var.environment
    # This prevents terraform from seeing a tag change for each plan/apply
    "hidden-link:/subscriptions/7d1e3999-6577-4cd5-b296-f518e5c8e677/resourceGroups/${var.resource_group}/providers/microsoft.insights/components/${var.resource_prefix}-appinsights" = "Resource"
  }
}

output "instrumentation_key" {
  value = azurerm_application_insights.app_insights.instrumentation_key
}

output "app_id" {
  value = azurerm_application_insights.app_insights.app_id
}
