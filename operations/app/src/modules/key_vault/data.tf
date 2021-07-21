data "azurerm_client_config" "current" {}

data "azurerm_subnet" "endpoint" {
  name = "endpoint"
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name = var.resource_group
}