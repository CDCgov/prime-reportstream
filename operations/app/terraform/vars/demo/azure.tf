terraform {
  required_version = ">= 1.0.5, <= 1.2.3" # This version must also be changed in other environments

  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      # This version must also be changed in other environments
      # 3.0 has breaking changes
      version = ">= 2.8.0, < 3.0.0"
    }
  }

  # NOTE: injected at init time by calling terraform init with the "--backend-config=<path to .tfbackend file>"
  # See configurations directory
  backend "azurerm" {
    container_name = "terraformstatefile"
    key            = "demo.terraform.tfstate"
  }
}

provider "azurerm" {
  features {
    template_deployment {
      delete_nested_items_during_deletion = false
    }
    key_vault {
      recover_soft_deleted_key_vaults = false
      // Required to purge secret. Will cause error since need sub purge KV permissions.
      // Solution available in azurerm 3.0 https://github.com/hashicorp/terraform-provider-azurerm/issues/10273
      purge_soft_delete_on_destroy = false
    }
    log_analytics_workspace {
      permanently_delete_on_destroy = true
    }
  }
  skip_provider_registration = true
  subscription_id            = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}
