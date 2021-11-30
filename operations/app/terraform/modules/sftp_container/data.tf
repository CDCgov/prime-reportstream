// Network

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

data "azurerm_private_dns_zone" "prime_local" {
  name                = "prime.local"
  resource_group_name = var.resource_group
}


// Storage Account

data "azurerm_storage_account" "storage_account" {
  name                = "${var.resource_prefix}storageaccount"
  resource_group_name = var.resource_group
}