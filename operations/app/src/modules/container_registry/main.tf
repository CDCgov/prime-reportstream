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
    default_action = "Deny"

    virtual_network {
      action = "Allow"
      subnet_id = var.public_subnet_id
    }

    # GitHub Actions
    ip_rule {
      action = "Allow"
      ip_range = "13.64.0.0/16"
    }

    # GitHub Actions
    ip_rule {
      action = "Allow"
      ip_range = "13.65.0.0/16"
    }
  }

  tags = {
    environment = var.environment
  }
}

// DISABLED AS FUNCTION APP CAN NOT CONNECT - RKH

//module "container_registry_private_endpoint" {
//  source = "../common/private_endpoint"
//  resource_id = azurerm_container_registry.container_registry.id
//  name = azurerm_container_registry.container_registry.name
//  type = "container_registry"
//  resource_group = var.resource_group
//  location = var.location
//  endpoint_subnet_id = var.endpoint_subnet_id
//}

output "login_server" {
  value = azurerm_container_registry.container_registry.login_server
}

output "admin_username" {
  value = azurerm_container_registry.container_registry.admin_username
}

output "admin_password" {
  value = azurerm_container_registry.container_registry.admin_password
}
