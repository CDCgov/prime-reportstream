locals {
  ping_url         = (var.environment == "prod" ? "https://prime.cdc.gov" : "https://${var.environment}.prime.cdc.gov")
  alerting_enabled = (var.environment == "prod" || var.environment == "staging" ? 1 : 0)
}

resource "azurerm_application_insights" "app_insights" {
  name                = "${var.resource_prefix}-appinsights"
  location            = var.location
  resource_group_name = var.resource_group
  application_type    = "web"
  workspace_id        = var.workspace_id

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
    "environment" = var.environment
  }
}