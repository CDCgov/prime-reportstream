variable "target_env" {
  description = "Enter the target deployment environment (eg. cglodosky, test, prod):"
  type = string
}

locals {
  env_prefix = (var.target_env == "prod" ? "pdhprod" : (var.target_env == "test" ? "pdhtest" : var.target_env))
  resource_group = (var.target_env == "prod" ? "prime-data-hub-prod" : (var.target_env == "test" ? "prime-data-hub-test" : "prime-dev-${var.target_env}"))
  location = "eastus"
}

terraform {
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = ">= 2.26"
    }
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
}

resource "azurerm_resource_group" "resourcegroup" {
  name     = local.resource_group
  location = local.location
}

resource "azurerm_storage_account" "storage_account" {
  name                     = "${local.env_prefix}storageaccount"
  resource_group_name      = local.resource_group
  location                 = local.location
  account_tier             = "Standard"
  account_replication_type = "LRS"

  tags = {
    environment = var.target_env
  }
}
