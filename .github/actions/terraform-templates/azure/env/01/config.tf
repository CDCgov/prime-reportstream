terraform {
  required_version = ">= 1.7.5"
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.99.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.48.0"
    }
    mssql = {
      source  = "betr-io/mssql"
      version = "~> 0.3"
    }
    azapi = {
      source  = "Azure/azapi"
      version = "1.12.1"
    }
  }
}

provider "azurerm" {
  # This is only required when the User, Service Principal, or Identity running Terraform lacks the permissions to register Azure Resource Providers.
  skip_provider_registration = false
  features {
    template_deployment {
      delete_nested_items_during_deletion = true
    }
    key_vault {
      recover_soft_deleted_key_vaults = true
      purge_soft_delete_on_destroy    = true
    }
  }
}
