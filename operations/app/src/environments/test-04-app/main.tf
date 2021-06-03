locals {
  environment = "test"
  resource_group = "prime-data-hub-test"
  resource_prefix = "pdhtest"
  location = "eastus"

  is_metabase_env = true

  https_cert_names = ["test-prime-cdc-gov", "test-reportstream-cdc-gov"]
  okta_redirect_url = "https://test.prime.cdc.gov/download"
}

terraform {
  required_version = "= 0.14.5" # This version must also be changed in other environments

  required_providers {
    azurerm = {
      source = "hashicorp/azurerm"
      version = ">= 2.61.0" # This version must also be changed in other environments
    }
  }

  backend "azurerm" {
    resource_group_name = "prime-data-hub-test"
    storage_account_name = "pdhtestterraform"
    container_name = "terraformstate"
    key = "app.tfstate"
  }
}

provider "azurerm" {
  features {}
  skip_provider_registration = true
  subscription_id = "7d1e3999-6577-4cd5-b296-f518e5c8e677"
}

module "application_insights" {
  source = "../../modules/application_insights"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
}

module "function_app" {
  source = "../../modules/function_app"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
  ai_instrumentation_key = module.application_insights.instrumentation_key
  okta_redirect_url = local.okta_redirect_url
}

module "front_door" {
  source = "../../modules/front_door"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
  https_cert_names = local.https_cert_names
  is_metabase_env = local.is_metabase_env
}

module "sftp_container" {
  source = "../../modules/sftp_container"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
}

module "metabase" {
  source = "../../modules/metabase"
  environment = local.environment
  resource_group = local.resource_group
  resource_prefix = local.resource_prefix
  location = local.location
  ai_instrumentation_key = module.application_insights.instrumentation_key
}