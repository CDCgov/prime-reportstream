terraform {
  required_version = "= 1.0.5" # This version must also be changed in other environments

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 2.61.0"
      # This version must also be changed in other environments
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

module "app_service_plan" {
  source          = "../../modules/app_service_plan"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
}

module "key_vault" {
  source                      = "../../modules/key_vault"
  environment                 = var.environment
  resource_group              = var.resource_group
  resource_prefix             = var.resource_prefix
  location                    = var.location
  aad_object_keyvault_admin   = var.aad_object_keyvault_admin
  terraform_caller_ip_address = var.terraform_caller_ip_address
}

module "container_registry" {
  source               = "../../modules/container_registry"
  environment          = var.environment
  resource_group       = var.resource_group
  resource_prefix      = var.resource_prefix
  location             = var.location
  enable_content_trust = true
}
