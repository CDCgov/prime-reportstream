data "azurerm_virtual_network" "west_vnet" {
  name                = "${var.resource_prefix}-West-vnet"
  resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "east_vnet" {
  name                = "${var.resource_prefix}-East-vnet"
  resource_group_name = var.resource_group
}
