terraform {
  required_version = "= 1.0.5" # This version must also be changed in other environments

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
  subscription_id            = "e72f6092-24af-40fe-888c-7379b9c6748a"
}