locals {
  environment = "test"
  resource_group = "prime-data-hub-test"
  resource_prefix = "pdhtest"
  location = "eastus"
  rsa_key_2048 = "pdhtest-2048-key"
  rsa_key_4096 = "pdhtest-key"
  aad_group_postgres_admin = "c4031f1f-229c-4a8a-b3b9-23bae9dbf197"
  is_metabase_env = true
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
    resource_group_name = "prime-data-hub-test"
    storage_account_name = "pdhtestterraform"
    container_name = "terraformstate"
    key = "persistent.tfstate"
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  subscription_id = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}

module "database" {
  source = "../../modules/database"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
  rsa_key_2048 = local.rsa_key_2048
  aad_group_postgres_admin = local.aad_group_postgres_admin
  is_metabase_env = local.is_metabase_env
}

module "storage" {
  source = "../../modules/storage"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
  rsa_key_4096 = local.rsa_key_4096
}