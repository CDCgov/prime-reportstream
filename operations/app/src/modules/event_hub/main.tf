terraform {
    required_version = ">= 0.14"
}

resource "azurerm_eventhub_namespace" "eventhub_namespace" {
  name = "${var.resource_prefix}-eventhub"
  location = var.location
  resource_group_name = var.resource_group
  sku = "Standard"
  capacity = 1
  auto_inflate_enabled = true
  zone_redundant = true

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_eventhub_namespace_authorization_rule" "diagnostic_auth_rule" {
  name = "DiagnosticSharedAccessKey"
  namespace_name = azurerm_eventhub_namespace.eventhub_namespace.name
  resource_group_name = var.resource_group

  listen = false
  send = true
  manage = false
}

output "eventhub_namespace_name" {
  value = azurerm_eventhub_namespace.eventhub_namespace.name
}

output "diagnostic_auth_rule_id" {
  value = azurerm_eventhub_namespace_authorization_rule.diagnostic_auth_rule.id
}
