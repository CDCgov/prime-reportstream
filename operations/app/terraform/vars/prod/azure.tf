terraform {
  required_version = ">= 1.7.4, <2" # This version must also be changed in other environments

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 3, < 5"
    }
  }

  # NOTE: injected at init time by calling terraform init with the "--backend-config=<path to .tfbackend file>"
  # See configurations directory
  backend "azurerm" {
    resource_group_name  = "prime-data-hub-prod"
    storage_account_name = "pdhprodterraform"
    container_name       = "terraformstatefile"
    key                  = "prod.terraform.tfstate"
  }
}

provider "azurerm" {
  features {
    template_deployment {
      delete_nested_items_during_deletion = false
    }
  }
  skip_provider_registration = true
  subscription_id            = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}
