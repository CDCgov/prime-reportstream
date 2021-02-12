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

  tags = {
    environment = var.environment
  }
}

resource "azurerm_eventhub" "frontdoor_access" {
  name = "${var.resource_prefix}-front_door-access-log"
  namespace_name = azurerm_eventhub_namespace.eventhub_namespace.name
  resource_group_name = var.resource_group
  partition_count = 1
  message_retention = 1
}

resource "azurerm_eventhub" "frontdoor_waf" {
  name = "${var.resource_prefix}-front_door-waf-log"
  namespace_name = azurerm_eventhub_namespace.eventhub_namespace.name
  resource_group_name = var.resource_group
  partition_count = 1
  message_retention = 1
}

resource "azurerm_eventhub_namespace_authorization_rule" "frontdoor_auth_rule" {
  name = "RootManageSharedAccessKey"
  namespace_name = azurerm_eventhub_namespace.eventhub_namespace.name
  resource_group_name = var.resource_group

  listen = true
  send = true
  manage = true
}

output "frontdoor_access_eventhub" {
  value = azurerm_eventhub.frontdoor_access.name
}

output "frontdoor_waf_eventhub" {
  value = azurerm_eventhub.frontdoor_waf.name
}

output "frontdoor_auth_rule" {
  value = azurerm_eventhub_namespace_authorization_rule.frontdoor_auth_rule.id
}
