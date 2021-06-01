locals {
  dev_name = "rheft"
}

terraform {
  required_version = "= 0.14.5" # This version must also be changed in other environments

  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = ">= 2.61.0" # This version must also be changed in other environments
    }
  }

  backend "azurerm" {
    resource_group_name = "prime-dev-rheft"
    storage_account_name = "rheftterraform"
    container_name = "terraformstate"
    key = "network.tfstate"
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  subscription_id = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}

module "network" {
  source = "../../modules/network"
  environment = "dev"
  resource_group = "prime-dev-rheft"
  resource_prefix = "rheft"
  location = "eastus"
}