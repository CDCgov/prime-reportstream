variable "dev_name" {
  type = string
  description = "Name of developer (eg. cglodosky, rheft, jduff, etc.)"
}

terraform {
  required_version = ">= 0.14"
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = ">= 2.26"
    }
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
}

module "prime_data_hub" {
  source = "../../modules/prime_data_hub"
  environment = "dev"
  resource_group = "prime-dev-${var.dev_name}"
  resource_prefix = var.dev_name
}
