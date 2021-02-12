terraform {
  required_version = ">= 0.14"
}

resource "azurerm_eventhub" "log_stream" {
  name = "${var.resource_prefix}-${var.resource_type}-${var.log_type}-log"
  namespace_name = var.eventhub_namespace_name
  resource_group_name = var.resource_group
  partition_count = 1
  message_retention = 1
}

resource "azurerm_eventhub_authorization_rule" "log_stream_resource_auth_rule" {
  name = "resource-${var.resource_type}-${var.log_type}-policy"
  namespace_name = var.eventhub_namespace_name
  eventhub_name = azurerm_eventhub.log_stream.name
  resource_group_name = var.resource_group

  // Diagnostic settings require all permissions
  listen = true
  send = true
  manage = true
}

resource "azurerm_eventhub_authorization_rule" "log_stream_splunk_auth_rule" {
  name = "splunk-${var.resource_type}-${var.log_type}-policy"
  namespace_name = var.eventhub_namespace_name
  eventhub_name = azurerm_eventhub.log_stream.name
  resource_group_name = var.resource_group

  // Splunk only needs listen capabilities
  listen = true
  send = false
  manage = false

  lifecycle {
    // Access keys are communicate to Splunk, so we never want to destroy this
    prevent_destroy = true
  }
}

output "event_hub_name" {
  value = azurerm_eventhub.log_stream.name
}

output "event_hub_resource_auth_rule_id" {
  value = azurerm_eventhub_authorization_rule.log_stream_resource_auth_rule.id
}

output "event_hub_splunk_auth_rule_id" {
  value = azurerm_eventhub_authorization_rule.log_stream_resource_auth_rule.id
}