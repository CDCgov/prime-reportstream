

module "database" {
  source                   = "../../modules/database"
  environment              = var.environment
  resource_group           = var.resource_group
  resource_prefix          = var.resource_prefix
  location                 = var.location
  rsa_key_2048             = var.rsa_key_2048
  aad_group_postgres_admin = var.aad_group_postgres_admin
  is_metabase_env          = var.is_metabase_env
  use_cdc_managed_vnet     = var.use_cdc_managed_vnet
}

module "storage" {
  source                      = "../../modules/storage"
  environment                 = var.environment
  resource_group              = var.resource_group
  resource_prefix             = var.resource_prefix
  location                    = var.location
  rsa_key_4096                = var.rsa_key_4096
  terraform_caller_ip_address = var.terraform_caller_ip_address
  use_cdc_managed_vnet        = var.use_cdc_managed_vnet
}
