terraform {
    required_version = ">= 0.14"
}

resource "azurerm_container_registry" "container_registry" {
  name = var.name
  resource_group_name = var.resource_group
  location = var.location
  sku = "Premium"
  admin_enabled = false
  network_rule_set {
    virtual_network {
      action = "Allow"
      subnet_id = var.public_subnet_id
    }
  }

  tags = {
    environment = var.environment
  }
}
