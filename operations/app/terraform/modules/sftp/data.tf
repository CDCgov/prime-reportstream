data "azurerm_subnet" "container_subnet" {
  name                 = "container"
  virtual_network_name = "${var.vnet_name}"
  resource_group_name  = var.resource_group
}
