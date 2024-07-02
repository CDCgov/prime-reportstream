terraform {
    required_version = ">= 1.8.5, <2" # This version must also be changed in other environments

    required_providers {
        azurerm = {
            source  = "hashicorp/azurerm"
            version = ">=3, < 4"
        }
    }

    # NOTE: injected at init time by calling terraform init with the "--backend-config=<path to .tfbackend file>"
    # See configurations directory
    backend "azurerm" {
        resource_group_name  = "ophdst-prim-tst-moderate-rest-rg"
        storage_account_name = "pdhtstterraform"
        container_name       = "terraformstate"
        key                  = "tst.terraform.tfstate"
    }
}

provider "azurerm" {
    features {
        template_deployment {
            delete_nested_items_during_deletion = false
        }
    }
    skip_provider_registration = true
    subscription_id            = "320d8d57-c87c-4434-827f-59ee7d86687a"
}
