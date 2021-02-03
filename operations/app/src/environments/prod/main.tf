locals {
  target_env = "prod"
}

terraform {
  required_version = "= 0.14.5" # This version must also be changed in other environments
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = "= 2.45.1" # This version must also be changed in other environments
    }
  }
  backend "azurerm" {
    resource_group_name = "prime-data-hub-prod"
    storage_account_name = "pdhprodstorageaccount"
    container_name = "terraformstate"
    key = "terraform.tfstate"
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  subscription_id = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}

module "prime_data_hub" {
  source = "../../modules/prime_data_hub"
  environment = local.target_env
  resource_group = "prime-data-hub-${local.target_env}"
  resource_prefix = "pdhprod"
}
