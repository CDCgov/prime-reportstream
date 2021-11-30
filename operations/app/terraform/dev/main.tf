## Set up our Azure Virtual Network.
## Need to determine a way to run or not if vnets are pre-configured
module "vnet" {
  source = "../modules/vnet"
  resource_group = var.resource_group
  environment = var.environment
  resource_prefix = var.resource_prefix
}

##########
## 01-network
##########

module "network" {
  source          = "../modules/network"
  vnet_address_space = module.vnet.vnet_address_spaces
  vnet_ids        = module.vnet.ids
  vnets = module.vnet.vnets
  vnet_names      = module.vnet.names
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
}

module "nat_gateway" {
  source           = "../modules/nat_gateway"
  environment      = var.environment
  resource_group   = var.resource_group
  resource_prefix  = var.resource_prefix
  location         = var.location
  public_subnet_id = module.network.public_subnet_ids[1]
}


##########
## 02-config
##########

module "app_service_plan" {
  source          = "../modules/app_service_plan"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
  app_tier        = var.app_tier
  app_size        = var.app_size
}

module "key_vault" {
  source                      = "../modules/key_vault"
  environment                 = var.environment
  resource_group              = var.resource_group
  resource_prefix             = var.resource_prefix
  location                    = var.location
  aad_object_keyvault_admin   = var.aad_object_keyvault_admin
  terraform_caller_ip_address = var.terraform_caller_ip_address
  use_cdc_managed_vnet        = var.use_cdc_managed_vnet
  public_subnet = module.network.public_subnet_ids
  container_subnet = module.network.container_subnet_ids
  endpoint_subnet = module.network.endpoint_subnet_ids
  cyberark_ip_ingress = ""
}

# module "container_registry" {
#   source               = "../modules/container_registry"
#   environment          = var.environment
#   resource_group       = var.resource_group
#   resource_prefix      = var.resource_prefix
#   location             = var.location
#   enable_content_trust = true
# }