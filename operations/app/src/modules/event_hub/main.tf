terraform {
    required_version = ">= 0.14"
}

resource "azurerm_eventhub_namespace" "eventhub_namespace" {
  name = "${var.resource_prefix}-eventhub"
  location = var.location
  resource_group_name = var.resource_group
  sku = "Standard"
  capacity = 1
  auto_inflate_enabled = var.environment == "prod" ? true : false
  maximum_throughput_units = var.environment == "prod" ? 10 : 0
  zone_redundant = true

  network_rulesets {
    default_action = "Deny"
    trusted_service_access_enabled = true

    ip_rule {
      ip_mask = "165.225.48.94/32"
    }

    ip_rule {
      ip_mask = "165.225.48.87/32"
    }

    virtual_network_rule {
      subnet_id = var.public_subnet_id
    }

    virtual_network_rule {
      subnet_id = var.container_subnet_id
    }
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_eventhub_namespace_authorization_rule" "eventhub_manage_auth_rule" {
  name = "RootManageSharedAccessKey"
  namespace_name = azurerm_eventhub_namespace.eventhub_namespace.name
  resource_group_name = var.resource_group

  listen = true
  send = true
  manage = true
}

output "eventhub_namespace_name" {
  value = azurerm_eventhub_namespace.eventhub_namespace.name
}

output "manage_auth_rule_id" {
  value = azurerm_eventhub_namespace_authorization_rule.eventhub_manage_auth_rule.id
}
