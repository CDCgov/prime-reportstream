locals {
    location = "eastus"
}

resource "azurerm_resource_group" "resource_group" {
  name = var.resource_group
  location = local.location
}

module "storage" {
    source = "../storage"
    environment = var.environment
    resource_group = var.resource_group
    name = "${var.resource_prefix}storageaccount"
    location = local.location
}
