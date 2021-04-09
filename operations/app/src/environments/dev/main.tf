terraform {
  required_version = "= 0.14.5" # This version must also be changed in other environments
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = "= 2.45.1" # This version must also be changed in other environments
    }
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  subscription_id = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}

module "prime_data_hub" {
  source = "../../modules/prime_data_hub"
  environment = "dev"
  resource_group = "prime-dev-${var.dev_name}"
  resource_prefix = var.dev_name
  okta_redirect_url = "https://prime-data-hub-${var.dev_name}.azurefd.net/download"
  https_cert_names = []
  rsa_key_2048 = null
  rsa_key_4096 = null
}
