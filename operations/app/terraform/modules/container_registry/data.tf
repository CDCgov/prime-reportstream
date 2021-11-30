data "azurerm_subnet" "public" {
  name                 = "public"
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "public_subnet" {
  name                 = "public"
  virtual_network_name = "${var.resource_prefix}-East-vnet"
  resource_group_name  = var.resource_group
}