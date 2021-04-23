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

data "azurerm_virtual_network" "virtual_network" {
  name = "${var.resource_prefix}-vnet-peer-HUB"
  resource_group_name = var.resource_group
  // CIDR: 172.17.4.0/38
}

resource "azurerm_subnet" "public" {
  name = "public"
  resource_group_name = var.resource_group
  virtual_network_name = data.azurerm_virtual_network.virtual_network.name
  address_prefixes = ["172.17.4.0/29"]
  service_endpoints = ["Microsoft.ContainerRegistry", 
                       "Microsoft.Storage",
                       "Microsoft.Sql",
                       "Microsoft.Web",
                       "Microsoft.KeyVault",
                       "Microsoft.EventHub"]
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
  virtual_network_name = data.azurerm_virtual_network.virtual_network.name
  address_prefixes = ["172.17.4.8/29"]
  service_endpoints = ["Microsoft.Storage", "Microsoft.KeyVault", "Microsoft.EventHub"]
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


## Outputs

output "public_subnet_id" {
  value = azurerm_subnet.public.id
}

output "container_subnet_id" {
  value = azurerm_subnet.container.id
}
