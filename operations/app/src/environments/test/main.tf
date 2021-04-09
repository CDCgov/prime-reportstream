locals {
  target_env = "test"
}

terraform {
  required_version = "= 0.14.5" # This version must also be changed in other environments
  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = "= 2.45.1" # This version must also be changed in other environments
    }
  }
  backend "azurerm" {
    resource_group_name = "prime-data-hub-test"
    storage_account_name = "pdhteststorageaccount"
    container_name = "terraformstate"
    key = "terraform.tfstate"
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  subscription_id = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}

module "prime_data_hub" {
  source = "../../modules/prime_data_hub"
  environment = local.target_env
  resource_group = "prime-data-hub-${local.target_env}"
  resource_prefix = "pdhtest"
  okta_redirect_url = "https://test.prime.cdc.gov/download"
  https_cert_names = ["test-prime-cdc-gov", "test-reportstream-cdc-gov"]
  rsa_key_2048 = "pdhtest-2048-key"
  rsa_key_4096 = "pdhtest-key"
}
