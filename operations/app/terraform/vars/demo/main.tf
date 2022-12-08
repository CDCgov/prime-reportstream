##########
## 01-Initialize
##########

module "init" {
  source                      = "../../modules/init"
  environment                 = local.init.environment
  resource_group              = local.init.resource_group_name
  resource_prefix             = local.init.resource_prefix
  location                    = local.init.location
  aad_object_keyvault_admin   = local.ad.aad_object_keyvault_admin
  terraform_caller_ip_address = local.network.terraform_caller_ip_address
  use_cdc_managed_vnet        = local.network.use_cdc_managed_vnet
  terraform_object_id         = local.ad.terraform_object_id
  application_kv_name         = local.key_vault.application_kv_name
  app_config_kv_name          = local.key_vault.app_config_kv_name
  client_config_kv_name       = local.key_vault.client_config_kv_name
  dns_vnet                    = local.network.dns_vnet
  network                     = local.network.config
  random_id                   = local.init.random_id
}

##########
## 01-network
##########

module "network" {
  source          = "../../modules/network"
  environment     = local.init.environment
  resource_group  = local.init.resource_group_name
  resource_prefix = local.init.resource_prefix
  location        = local.init.location
  azure_vns       = local.network.config
}

module "nat_gateway" {
  source          = "../../modules/nat_gateway"
  environment     = local.init.environment
  resource_group  = local.init.resource_group_name
  resource_prefix = local.init.resource_prefix
  location        = local.init.location
  subnets         = module.network.subnets
}

##########
## 02-config
##########

module "app_service_plan" {
  source          = "../../modules/app_service_plan"
  environment     = local.init.environment
  resource_group  = local.init.resource_group_name
  resource_prefix = local.init.resource_prefix
  location        = local.init.location
  app_tier        = local.app.app_tier
  app_size        = local.app.app_size
}

module "key_vault" {
  source                      = "../../modules/key_vault"
  environment                 = local.init.environment
  resource_group              = local.init.resource_group_name
  resource_prefix             = local.init.resource_prefix
  location                    = local.init.location
  aad_object_keyvault_admin   = local.ad.aad_object_keyvault_admin
  terraform_caller_ip_address = local.network.terraform_caller_ip_address
  use_cdc_managed_vnet        = local.network.use_cdc_managed_vnet
  subnets                     = module.network.subnets
  cyberark_ip_ingress         = ""
  terraform_object_id         = local.ad.terraform_object_id
  application_kv_name         = local.key_vault.application_kv_name
  app_config_kv_name          = local.key_vault.app_config_kv_name
  client_config_kv_name       = local.key_vault.client_config_kv_name
  dns_vnet                    = local.network.dns_vnet
  dns_zones                   = module.network.dns_zones
  admin_function_app          = module.function_app.admin_function_app
  is_temp_env                 = local.is_temp_env
}

module "container_registry" {
  source               = "../../modules/container_registry"
  environment          = local.init.environment
  resource_group       = local.init.resource_group_name
  resource_prefix      = local.init.resource_prefix
  location             = local.init.location
  enable_content_trust = false
  subnets              = module.network.subnets
}


##########
## 03-Persistent
##########

module "database" {
  source                   = "../../modules/database"
  environment              = local.init.environment
  resource_group           = local.init.resource_group_name
  resource_prefix          = local.init.resource_prefix
  location                 = local.init.location
  rsa_key_2048             = data.azurerm_key_vault_key.pdh-2048-key.id
  aad_group_postgres_admin = local.ad.aad_group_postgres_admin
  is_metabase_env          = local.init.is_metabase_env
  use_cdc_managed_vnet     = local.network.use_cdc_managed_vnet
  postgres_user            = data.azurerm_key_vault_secret.postgres_user.value
  postgres_pass            = data.azurerm_key_vault_secret.postgres_pass.value
  postgres_readonly_user   = data.azurerm_key_vault_secret.postgres_readonly_user.value
  postgres_readonly_pass   = data.azurerm_key_vault_secret.postgres_readonly_pass.value
  db_sku_name              = local.database.db_sku_name
  db_version               = local.database.db_version
  db_storage_mb            = local.database.db_storage_mb
  db_auto_grow             = local.database.db_auto_grow
  db_prevent_destroy       = local.database.db_prevent_destroy
  db_threat_detection      = local.database.db_threat_detection
  subnets                  = module.network.subnets
  db_replica               = local.database.db_replica
  application_key_vault_id = module.key_vault.application_key_vault_id
  dns_vnet                 = local.network.dns_vnet
  dns_zones                = module.network.dns_zones
}

module "storage" {
  source                        = "../../modules/storage"
  environment                   = local.init.environment
  resource_group                = local.init.resource_group_name
  resource_prefix               = local.init.resource_prefix
  location                      = local.init.location
  rsa_key_4096                  = local.security.rsa_key_4096
  terraform_caller_ip_address   = local.network.terraform_caller_ip_address
  use_cdc_managed_vnet          = local.network.use_cdc_managed_vnet
  subnets                       = module.network.subnets
  application_key_vault_id      = module.key_vault.application_key_vault_id
  dns_vnet                      = local.network.dns_vnet
  dns_zones                     = module.network.dns_zones
  delete_pii_storage_after_days = local.security.delete_pii_storage_after_days
  is_temp_env                   = local.is_temp_env
}


##########
## 04-App
##########

module "function_app" {
  source                            = "../../modules/function_app"
  environment                       = local.init.environment
  resource_group                    = local.init.resource_group_name
  resource_prefix                   = local.init.resource_prefix
  location                          = local.init.location
  ai_instrumentation_key            = module.application_insights.instrumentation_key
  ai_connection_string              = module.application_insights.connection_string
  okta_base_url                     = local.init.okta_base_url
  okta_redirect_url                 = local.init.okta_redirect_url
  terraform_caller_ip_address       = local.network.terraform_caller_ip_address
  use_cdc_managed_vnet              = local.network.use_cdc_managed_vnet
  primary_access_key                = module.storage.sa_primary_access_key
  container_registry_login_server   = module.container_registry.container_registry_login_server
  primary_connection_string         = module.storage.sa_primary_connection_string
  app_service_plan                  = module.app_service_plan.service_plan_id
  pagerduty_url                     = data.azurerm_key_vault_secret.pagerduty_url.value
  postgres_user                     = data.azurerm_key_vault_secret.postgres_user.value
  postgres_pass                     = data.azurerm_key_vault_secret.postgres_pass.value
  container_registry_admin_username = module.container_registry.container_registry_admin_username
  container_registry_admin_password = module.container_registry.container_registry_admin_password
  subnets                           = module.network.subnets
  application_key_vault_id          = module.key_vault.application_key_vault_id
  app_config_key_vault_name         = module.key_vault.app_config_key_vault_name
  sa_partner_connection_string      = module.storage.sa_partner_connection_string
  client_config_key_vault_id        = module.key_vault.client_config_key_vault_id
  app_config_key_vault_id           = module.key_vault.app_config_key_vault_id
  dns_ip                            = local.network.dns_ip
  is_temp_env                       = local.is_temp_env
  function_runtime_version          = local.app.function_runtime_version
  storage_account                   = module.storage.storage_account_id
}

module "front_door" {
  source                      = "../../modules/front_door"
  environment                 = local.init.environment
  resource_group              = local.init.resource_group_name
  resource_prefix             = local.init.resource_prefix
  location                    = local.init.location
  https_cert_names            = local.security.https_cert_names
  is_metabase_env             = local.init.is_metabase_env
  public_primary_web_endpoint = module.storage.sa_public_primary_web_endpoint
  application_key_vault_id    = module.key_vault.application_key_vault_id
}

/* module "sftp" {
  source                      = "../../modules/sftp"
  environment                 = local.init.environment
  resource_group              = local.init.resource_group_name
  resource_prefix             = local.init.resource_prefix
  location                    = local.init.location
  key_vault_id                = module.key_vault.application_key_vault_id
  terraform_caller_ip_address = local.network.terraform_caller_ip_address
  nat_gateway_id              = module.nat_gateway.nat_gateway_id
} */

module "sftp_container" {
  count = local.init.environment != "prod" ? 1 : 0

  source                = "../../modules/sftp_container"
  environment           = local.init.environment
  resource_group        = local.init.resource_group_name
  resource_prefix       = local.init.resource_prefix
  location              = local.init.location
  use_cdc_managed_vnet  = local.network.use_cdc_managed_vnet
  sa_primary_access_key = module.storage.sa_primary_access_key
  dns_zones             = module.network.dns_zones
  storage_account       = module.storage.storage_account
}

module "metabase" {
  count = local.init.is_metabase_env ? 1 : 0

  source                 = "../../modules/metabase"
  environment            = local.init.environment
  resource_group         = local.init.resource_group_name
  resource_prefix        = local.init.resource_prefix
  location               = local.init.location
  ai_instrumentation_key = module.application_insights.metabase_instrumentation_key
  ai_connection_string   = module.application_insights.metabase_connection_string
  use_cdc_managed_vnet   = local.network.use_cdc_managed_vnet
  service_plan_id        = module.app_service_plan.service_plan_id
  postgres_server_name   = module.database.postgres_server_name
  postgres_user          = data.azurerm_key_vault_secret.postgres_user.value
  postgres_pass          = data.azurerm_key_vault_secret.postgres_pass.value
  subnets                = module.network.subnets
}


##########
## 05-Monitor
##########

module "log_analytics_workspace" {
  source                     = "../../modules/log_analytics_workspace"
  environment                = local.init.environment
  resource_group             = local.init.resource_group_name
  resource_prefix            = local.init.resource_prefix
  location                   = local.init.location
  service_plan_id            = module.app_service_plan.service_plan_id
  container_registry_id      = module.container_registry.container_registry_id
  postgres_server_id         = module.database.postgres_server_id
  application_key_vault_id   = module.key_vault.application_key_vault_id
  app_config_key_vault_id    = module.key_vault.app_config_key_vault_id
  client_config_key_vault_id = module.key_vault.client_config_key_vault_id
  function_app_id            = module.function_app.function_app_id
  //front_door_id                 = module.front_door.front_door_id
  nat_gateway_id                = module.nat_gateway.nat_gateway_id
  primary_vnet_id               = module.network.primary_vnet_id
  replica_vnet_id               = module.network.replica_vnet_id
  storage_account_id            = module.storage.storage_account_id
  storage_public_id             = module.storage.storage_public_id
  storage_partner_id            = module.storage.storage_partner_id
  action_group_businesshours_id = module.application_insights.action_group_businesshours_id
  //data_factory_id               = module.data_factory.data_factory_id
  //sftp_instance_01_id           = module.sftp.sftp_instance_ids[0]
}

module "application_insights" {
  source                      = "../../modules/application_insights"
  environment                 = local.init.environment
  resource_group              = local.init.resource_group_name
  resource_prefix             = local.init.resource_prefix
  location                    = local.init.location
  is_metabase_env             = local.init.is_metabase_env
  pagerduty_url               = data.azurerm_key_vault_secret.pagerduty_url.value
  pagerduty_businesshours_url = data.azurerm_key_vault_secret.pagerduty_businesshours_url.value
  postgres_server_id          = module.database.postgres_server_id
  service_plan_id             = module.app_service_plan.service_plan_id
  workspace_id                = module.log_analytics_workspace.law_id
}

##########
## 06-Integration
##########

/* module "data_factory" {
  source                       = "../../modules/data_factory"
  environment                  = local.init.environment
  resource_group               = local.init.resource_group_name
  resource_prefix              = local.init.resource_prefix
  location                     = local.init.location
  key_vault_id                 = module.key_vault.application_key_vault_id
  terraform_caller_ip_address  = local.network.terraform_caller_ip_address
  sa_primary_connection_string = module.storage.sa_primary_connection_string
  storage_account_id           = module.storage.storage_account_id
  sftp_storage                 = module.sftp.sftp_storage
  sftp_shares                  = module.sftp.sftp_shares
} */
