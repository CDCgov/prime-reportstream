locals {
    location = "eastus"
}

module "storage" {
    source = "../storage"
    environment = var.environment
    resource_group = var.resource_group
    name = "${var.resource_prefix}storageaccount"
    location = local.location
}

module "network" {
    source = "../network"
    environment = var.environment
    resource_group = var.resource_group
    name = "${var.resource_prefix}-vnet"
    location = local.location
}
