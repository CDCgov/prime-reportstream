# Network

data "azurerm_subnet" "container" {
  name                 = "container"
  virtual_network_name = "${var.vnet1}"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "container_subnet" {
  name                 = "container"
  virtual_network_name = "${var.vnet2}"
  resource_group_name  = var.resource_group
}
