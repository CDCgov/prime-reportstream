
module "app_service_plan" {
  source          = "../../modules/app_service_plan"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
}

module "key_vault" {
  source                      = "../../modules/key_vault"
  environment                 = var.environment
  resource_group              = var.resource_group
  resource_prefix             = var.resource_prefix
  location                    = var.location
  aad_object_keyvault_admin   = var.aad_object_keyvault_admin
  terraform_caller_ip_address = var.terraform_caller_ip_address
  use_cdc_managed_vnet        = var.use_cdc_managed_vnet
}

module "container_registry" {
  source               = "../../modules/container_registry"
  environment          = var.environment
  resource_group       = var.resource_group
  resource_prefix      = var.resource_prefix
  location             = var.location
  enable_content_trust = true
}
