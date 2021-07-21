// Network

data "azurerm_subnet" "container" {
  name = "container"
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name = var.resource_group
}


// Storage Account

data "azurerm_storage_account" "storage_account" {
  name = "${var.resource_prefix}storageaccount"
  resource_group_name = var.resource_group
}