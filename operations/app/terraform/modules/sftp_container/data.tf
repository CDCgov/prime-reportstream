# Network
data "azurerm_subnet" "container_subnet" {
  name                 = var.subnet_name
  virtual_network_name = var.vnet
  resource_group_name  = var.resource_group
}
