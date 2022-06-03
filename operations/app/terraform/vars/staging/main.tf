##########
## 01-network
##########

module "network" {
  source          = "../../modules/network"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
  azure_vns       = var.network
}

module "nat_gateway" {
  source          = "../../modules/nat_gateway"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
  subnets         = module.network.subnets
}


##########
## 02-config
##########

module "app_service_plan" {
  source          = "../../modules/app_service_plan"
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
  app_tier        = var.app_tier
  app_size        = var.app_size
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
  subnets                     = module.network.subnets
  cyberark_ip_ingress         = ""
  terraform_object_id         = var.terraform_object_id
  application_kv_name         = var.application_kv_name
  app_config_kv_name          = var.app_config_kv_name
  client_config_kv_name       = var.client_config_kv_name
  dns_vnet                    = var.dns_vnet
  dns_zones                   = module.network.dns_zones
  admin_function_app          = module.function_app.admin_function_app
}

module "container_registry" {
  source               = "../../modules/container_registry"
  environment          = var.environment
  resource_group       = var.resource_group
  resource_prefix      = var.resource_prefix
  location             = var.location
  enable_content_trust = false
  subnets              = module.network.subnets
}


##########
## 03-Persistent
##########

module "database" {
  source                   = "../../modules/database"
  environment              = var.environment
  resource_group           = var.resource_group
  resource_prefix          = var.resource_prefix
  location                 = var.location
  rsa_key_2048             = data.azurerm_key_vault_key.pdh-2048-key.id
  aad_group_postgres_admin = var.aad_group_postgres_admin
  is_metabase_env          = var.is_metabase_env
  use_cdc_managed_vnet     = var.use_cdc_managed_vnet
  postgres_user            = data.azurerm_key_vault_secret.postgres_user.value
  postgres_pass            = data.azurerm_key_vault_secret.postgres_pass.value
  db_sku_name              = var.db_sku_name
  db_version               = var.db_version
  db_storage_mb            = var.db_storage_mb
  db_auto_grow             = var.db_auto_grow
  db_prevent_destroy       = var.db_prevent_destroy
  db_threat_detection      = var.db_threat_detection
  subnets                  = module.network.subnets
  db_replica               = var.db_replica
  application_key_vault_id = module.key_vault.application_key_vault_id
  dns_vnet                 = var.dns_vnet
  dns_zones                = module.network.dns_zones
}

module "storage" {
  source                        = "../../modules/storage"
  environment                   = var.environment
  resource_group                = var.resource_group
  resource_prefix               = var.resource_prefix
  location                      = var.location
  rsa_key_4096                  = var.rsa_key_4096
  terraform_caller_ip_address   = var.terraform_caller_ip_address
  use_cdc_managed_vnet          = var.use_cdc_managed_vnet
  subnets                       = module.network.subnets
  application_key_vault_id      = module.key_vault.application_key_vault_id
  dns_vnet                      = var.dns_vnet
  dns_zones                     = module.network.dns_zones
  delete_pii_storage_after_days = var.delete_pii_storage_after_days
}


##########
## 04-App
##########

module "function_app" {
  source                            = "../../modules/function_app"
  environment                       = var.environment
  resource_group                    = var.resource_group
  resource_prefix                   = var.resource_prefix
  location                          = var.location
  ai_instrumentation_key            = module.application_insights.instrumentation_key
  ai_connection_string              = module.application_insights.connection_string
  okta_base_url                     = var.okta_base_url
  okta_redirect_url                 = var.okta_redirect_url
  terraform_caller_ip_address       = var.terraform_caller_ip_address
  use_cdc_managed_vnet              = var.use_cdc_managed_vnet
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
  dns_ip                            = var.dns_ip
}

module "front_door" {
  source                      = "../../modules/front_door"
  environment                 = var.environment
  resource_group              = var.resource_group
  resource_prefix             = var.resource_prefix
  location                    = var.location
  https_cert_names            = var.https_cert_names
  is_metabase_env             = var.is_metabase_env
  public_primary_web_endpoint = module.storage.sa_public_primary_web_endpoint
  application_key_vault_id    = module.key_vault.application_key_vault_id
}

module "sftp_container" {
  count = var.environment != "prod" ? 1 : 0

  source                = "../../modules/sftp_container"
  environment           = var.environment
  resource_group        = var.resource_group
  resource_prefix       = var.resource_prefix
  location              = var.location
  use_cdc_managed_vnet  = var.use_cdc_managed_vnet
  sa_primary_access_key = module.storage.sa_primary_access_key
}

module "metabase" {
  count = var.is_metabase_env ? 1 : 0

  source                 = "../../modules/metabase"
  environment            = var.environment
  resource_group         = var.resource_group
  resource_prefix        = var.resource_prefix
  location               = var.location
  ai_instrumentation_key = module.application_insights.metabase_instrumentation_key
  ai_connection_string   = module.application_insights.metabase_connection_string
  use_cdc_managed_vnet   = var.use_cdc_managed_vnet
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
  source                        = "../../modules/log_analytics_workspace"
  environment                   = var.environment
  resource_group                = var.resource_group
  resource_prefix               = var.resource_prefix
  location                      = var.location
  service_plan_id               = module.app_service_plan.service_plan_id
  container_registry_id         = module.container_registry.container_registry_id
  postgres_server_id            = module.database.postgres_server_id
  application_key_vault_id      = module.key_vault.application_key_vault_id
  app_config_key_vault_id       = module.key_vault.app_config_key_vault_id
  client_config_key_vault_id    = module.key_vault.client_config_key_vault_id
  function_app_id               = module.function_app.function_app_id
  front_door_id                 = module.front_door.front_door_id
  nat_gateway_id                = module.nat_gateway.nat_gateway_id
  primary_vnet_id               = module.network.primary_vnet_id
  replica_vnet_id               = module.network.replica_vnet_id
  storage_account_id            = module.storage.storage_account_id
  storage_public_id             = module.storage.storage_public_id
  storage_partner_id            = module.storage.storage_partner_id
  action_group_businesshours_id = module.application_insights.action_group_businesshours_id
}

module "application_insights" {
  source                      = "../../modules/application_insights"
  environment                 = var.environment
  resource_group              = var.resource_group
  resource_prefix             = var.resource_prefix
  location                    = var.location
  is_metabase_env             = var.is_metabase_env
  pagerduty_url               = data.azurerm_key_vault_secret.pagerduty_url.value
  pagerduty_businesshours_url = data.azurerm_key_vault_secret.pagerduty_businesshours_url.value
  postgres_server_id          = module.database.postgres_server_id
  service_plan_id             = module.app_service_plan.service_plan_id
  workspace_id                = module.log_analytics_workspace.law_id
}
