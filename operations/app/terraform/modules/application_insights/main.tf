locals {
  ping_url                = (var.environment == "prod" ? "https://prime.cdc.gov" : "https://${var.environment}.prime.cdc.gov")
  alerting_enabled        = (var.environment == "prod" || var.environment == "staging" ? 1 : 0)
  prod_exclusive_alerting = (var.environment == "prod" ? 1 : 0)
}

resource "azurerm_application_insights" "app_insights" {
  name                = "${var.resource_prefix}-appinsights"
  location            = var.location
  resource_group_name = var.resource_group
  application_type    = "web"
  workspace_id        = var.workspace_id

  # Sonarcloud flag
  # needs to be true so the front-end can send events to App Insights
  # https://github.com/CDCgov/prime-reportstream/issues/3097
  internet_ingestion_enabled = true
  # needs to be true to allow engineer debugging through Azure UI
  internet_query_enabled = true

  tags = {
    environment = var.environment
  }
}

resource "azurerm_monitor_action_group" "action_group" {
  count               = local.alerting_enabled
  name                = "${var.resource_prefix}-actiongroup"
  resource_group_name = var.resource_group
  short_name          = "ReportStream"

  webhook_receiver {
    name                    = "PagerDuty"
    service_uri             = var.pagerduty_url
    use_common_alert_schema = true
  }
  tags = {
    environment = var.environment
  }
}

resource "azurerm_monitor_action_group" "action_group_metabase" {
  name                = "${var.resource_prefix}-actiongroup-metabase"
  resource_group_name = var.resource_group
  short_name          = "mb-check"

  webhook_receiver {
    name                    = "PagerDuty-mbhealthcheck"
    service_uri             = var.pagerduty_url
    use_common_alert_schema = true
  }
  tags = {
    environment = var.environment
  }
}

resource "azurerm_monitor_action_group" "action_group_slack" {
  name                = "${var.resource_prefix}-actiongroup-slack"
  resource_group_name = var.resource_group
  short_name          = "Slack-AG"

  email_receiver {
    name          = "slack-notification"
    email_address = var.slack_email_address
  }

  tags = {
    environment = var.environment
  }
}
resource "azurerm_monitor_action_group" "action_group_dummy" {
  name                = "${var.resource_prefix}-actiongroup-dummy"
  resource_group_name = var.resource_group
  short_name          = "Dummy AG"

  webhook_receiver {
    name                    = "Dummy URL"
    service_uri             = "https://foo.local"
    use_common_alert_schema = false
  }

  tags = {
    environment = var.environment
  }
}

# Prevent TF changes where Microsoft.Insights is forced lowercase
locals {
  action_group_id = try(replace(azurerm_monitor_action_group.action_group[0].id, "Microsoft.Insights", "microsoft.insights"), "")
  action_group_slack_id = (local.alerting_enabled == 1 ?
    try(replace(azurerm_monitor_action_group.action_group_slack.id, "Microsoft.Insights", "microsoft.insights"), "") :
  azurerm_monitor_action_group.action_group_slack.id)
  app_insights_id = replace(azurerm_application_insights.app_insights.id, "Microsoft.Insights", "microsoft.insights")
  action_group_metabase_id = (local.alerting_enabled == 1 ?
    try(replace(azurerm_monitor_action_group.action_group_metabase.id, "Microsoft.Insights", "microsoft.insights"), "") :
  azurerm_monitor_action_group.action_group_metabase.id)
}
