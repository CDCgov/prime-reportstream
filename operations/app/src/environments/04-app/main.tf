terraform {
  required_version = "= 0.14.5"
  # This version must also be changed in other environments

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

module "application_insights" {
  source          = "../../modules/application_insights"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
}

module "function_app" {
  source                 = "../../modules/function_app"
  environment            = var.environment
  resource_group         = var.resource_group
  resource_prefix        = var.resource_prefix
  location               = var.location
  ai_instrumentation_key = module.application_insights.instrumentation_key
  okta_redirect_url      = var.okta_redirect_url
}

module "front_door" {
  source           = "../../modules/front_door"
  environment      = var.environment
  resource_group   = var.resource_group
  resource_prefix  = var.resource_prefix
  location         = var.location
  https_cert_names = var.https_cert_names
  is_metabase_env  = var.is_metabase_env
}

module "sftp_container" {
  count = var.environment != "prod" ? 1 : 0

  source          = "../../modules/sftp_container"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
}

module "metabase" {
  count = var.is_metabase_env ? 1 : 0

  source                 = "../../modules/metabase"
  environment            = var.environment
  resource_group         = var.resource_group
  resource_prefix        = var.resource_prefix
  location               = var.location
  ai_instrumentation_key = module.application_insights.instrumentation_key
}
