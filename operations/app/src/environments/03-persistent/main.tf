terraform {
  required_version = "= 0.14.5" # This version must also be changed in other environments

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 2.61.0" # This version must also be changed in other environments
    }
  }

  # NOTE: injected at init time by calling terraform init with the "--backend-config=<path to .tfbackend file>"
  # See configurations directory
  backend "azurerm" {
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  subscription_id            = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}

module "database" {
  source                   = "../../modules/database"
  environment              = var.environment
  resource_group           = var.resource_group
  resource_prefix          = var.resource_prefix
  location                 = var.location
  rsa_key_2048             = var.rsa_key_2048
  aad_group_postgres_admin = var.aad_group_postgres_admin
  is_metabase_env          = var.is_metabase_env
}

module "storage" {
  source          = "../../modules/storage"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
  rsa_key_4096    = var.rsa_key_4096
}
