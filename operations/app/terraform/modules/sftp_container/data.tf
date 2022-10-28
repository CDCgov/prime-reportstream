# Network

data "azurerm_subnet" "container" {
  name                 = "container"
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "container_subnet" {
  name                 = "container"
  virtual_network_name = "${var.resource_prefix}-East-vnet"
  resource_group_name  = var.resource_group
}
