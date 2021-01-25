terraform {
    required_version = ">= 0.14"
}

resource "azurerm_network_security_group" "nsg_public" {
  name = "${var.resource_prefix}-nsg.public"
  location = var.location
  resource_group_name = var.resource_group
}

resource "azurerm_network_security_group" "nsg_private" {
  name = "${var.resource_prefix}-nsg.private"
  location = var.location
  resource_group_name = var.resource_group
}

resource "azurerm_virtual_network" "virtual_network" {
  name = "${var.resource_prefix}-vnet"
  location = var.location
  resource_group_name = var.resource_group
  address_space = ["10.0.0.0/16"]
  dns_servers = ["10.0.0.4", "10.0.0.5"]

  subnet {
    name = "public"
    address_prefix = "10.0.1.0/24"
    security_group = azurerm_network_security_group.nsg_public.id
  }

  subnet {
    name = "container"
    address_prefix = "10.0.2.0/24"
    security_group = azurerm_network_security_group.nsg_private.id
  }

  subnet {
    name = "private"
    address_prefix = "10.0.3.0/24"
    security_group = azurerm_network_security_group.nsg_private.id
  }

  tags = {
    environment = var.environment
  }
}
