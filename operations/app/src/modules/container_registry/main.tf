terraform {
    required_version = ">= 0.14"
}

resource "azurerm_container_registry" "container_registry" {
  name = var.name
  resource_group_name = var.resource_group
  location = var.location
  sku = "Premium"
  admin_enabled = true

  network_rule_set {
    default_action = "Allow"

    virtual_network {
      action = "Allow"
      subnet_id = var.public_subnet_id
    }
  }

  tags = {
    environment = var.environment
  }
}

output "login_server" {
  value = azurerm_container_registry.container_registry.login_server
}

output "admin_username" {
  value = azurerm_container_registry.container_registry.admin_username
}

output "admin_password" {
  value = azurerm_container_registry.container_registry.admin_password
}
