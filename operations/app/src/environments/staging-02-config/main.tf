locals {
  environment = "staging"
  resource_group = "prime-data-hub-staging"
  resource_prefix = "pdhstaging"
  location = "eastus"
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
    resource_group_name = "prime-data-hub-staging"
    storage_account_name = "pdhstagingterraform"
    container_name = "terraformstate"
    key = "config.tfstate"
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  subscription_id = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}

module "app_service_plan" {
  source = "../../modules/app_service_plan"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
}

module "key_vault" {
  source = "../../modules/key_vault"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
}

module "container_registry" {
  source = "../../modules/container_registry"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
}