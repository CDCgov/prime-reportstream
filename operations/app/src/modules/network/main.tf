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

  tags = {
    environment = var.environment
  }
}

resource "azurerm_subnet" "public" {
  name = "public"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.1.0/24"]
  service_endpoints = ["Microsoft.ContainerRegistry", 
                       "Microsoft.Storage",
                       "Microsoft.Sql",
                       "Microsoft.Web",
                       "Microsoft.KeyVault"]
  delegation {
    name = "server_farms"
    service_delegation {
      name = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_subnet_network_security_group_association" "public_public" {
  subnet_id = azurerm_subnet.public.id
  network_security_group_id = azurerm_network_security_group.nsg_public.id
}

resource "azurerm_subnet" "container" {
  name = "container"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.2.0/24"]
  service_endpoints = ["Microsoft.Storage", "Microsoft.KeyVault"]
  delegation {
      name = "container_groups"
      service_delegation {
        name = "Microsoft.ContainerInstance/containerGroups"
        actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
      }
  }
}

resource "azurerm_subnet_network_security_group_association" "container_public" {
  subnet_id = azurerm_subnet.container.id
  network_security_group_id = azurerm_network_security_group.nsg_public.id
}

resource "azurerm_subnet" "private" {
  name = "private"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  address_prefixes = ["10.0.3.0/24"]
  service_endpoints = ["Microsoft.Storage", "Microsoft.Sql", "Microsoft.KeyVault"]

  delegation {
    name = "server_farms"
    service_delegation {
      name = "Microsoft.Web/serverFarms"
      actions = ["Microsoft.Network/virtualNetworks/subnets/action"]
    }
  }
}

resource "azurerm_subnet_network_security_group_association" "private_private" {
  subnet_id = azurerm_subnet.private.id
  network_security_group_id = azurerm_network_security_group.nsg_private.id
}

resource "azurerm_virtual_network" "virtual_network_2" {
  name = "${var.resource_prefix}-vnet2"
  location = "westus"
  resource_group_name = var.resource_group
  address_space = ["10.1.0.0/16"]

  tags = {
    environment = var.environment
  }
}

resource "azurerm_subnet" "private2" {
  name = "private"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network_2.name
  address_prefixes = ["10.1.23.0/24"]
  service_endpoints = ["Microsoft.Sql"]
}

resource "azurerm_virtual_network_peering" "vnet_vnet2" {
  name = "${var.resource_prefix}-peer-vnet-vnet2"
  resource_group_name = var.resource_group
  virtual_network_name = azurerm_virtual_network.virtual_network.name
  remote_virtual_network_id = azurerm_virtual_network.virtual_network_2.id
}

output "public_subnet_id" {
  value = azurerm_subnet.public.id
}

output "container_subnet_id" {
  value = azurerm_subnet.container.id
}

output "private_subnet_id" {
  value = azurerm_subnet.private.id
}

output "private2_subnet_id" {
  value = azurerm_subnet.private2.id
}
