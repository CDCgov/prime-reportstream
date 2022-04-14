data "azurerm_virtual_network" "west_vnet" {
  name                = "${var.resource_prefix}-West-vnet"
  resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "east_vnet" {
  name                = "${var.resource_prefix}-East-vnet"
  resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "vnet" {
  name                = "${var.resource_prefix}-vnet"
  resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "vnet_peer" {
  name                = "${var.resource_prefix}-vnet-peer"
  resource_group_name = var.resource_group
}

data "azurerm_subnet" "west_vnet" {
  for_each = toset(data.azurerm_virtual_network.west_vnet.subnets)

  name                 = each.value
  virtual_network_name = "${var.resource_prefix}-West-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "east_vnet" {
  for_each = toset(data.azurerm_virtual_network.east_vnet.subnets)

  name                 = each.value
  virtual_network_name = "${var.resource_prefix}-East-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "vnet" {
  for_each = toset(data.azurerm_virtual_network.vnet.subnets)

  name                 = each.value
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "peer_vnet" {
  for_each = toset(data.azurerm_virtual_network.vnet_peer.subnets)

  name                 = each.value
  virtual_network_name = "${var.resource_prefix}-vnet-peer"
  resource_group_name  = var.resource_group
}