locals {
  location = "eastus"
}

module "storage" {
  source = "../storage"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  name = "${var.resource_prefix}storageaccount"
  location = local.location
  public_subnet_id = module.network.public_subnet_id
  container_subnet_id = module.network.container_subnet_id
  endpoint_subnet_id = module.network.endpoint_subnet_id
  key_vault_id = module.key_vault.application_key_vault_id
  rsa_key_4096 = var.rsa_key_4096
}

module "network" {
  source = "../network"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  location = local.location
}

module "container_registry" {
  source = "../container_registry"
  environment = var.environment
  resource_group = var.resource_group
  name = "${var.resource_prefix}containerregistry"
  location = local.location
  public_subnet_id = module.network.public_subnet_id
  endpoint_subnet_id = module.network.endpoint_subnet_id
}

module "app_service_plan" {
  source = "../app_service_plan"
  environment = var.environment
  resource_group = var.resource_group
  location = local.location
  resource_prefix = var.resource_prefix
  key_vault_id = module.key_vault.application_key_vault_id
}

module "function_app" {
  source = "../function_app"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  location = local.location
  app_service_plan_id = module.app_service_plan.app_service_plan_id
  storage_account_name = module.storage.storage_account_name
  storage_account_key = module.storage.storage_account_key
  public_subnet_id = module.network.public_subnet_id
  endpoint_subnet_id = module.network.endpoint_subnet_id
  postgres_user = "${module.database.postgres_user}@${module.database.server_name}"
  postgres_password = module.database.postgres_pass
  postgres_url = "jdbc:postgresql://${module.database.server_name}.postgres.database.azure.com:5432/prime_data_hub?sslmode=require"
  okta_redirect_url = var.okta_redirect_url
  login_server = module.container_registry.login_server
  admin_user = module.container_registry.admin_username
  admin_password = module.container_registry.admin_password
  ai_instrumentation_key = module.application_insights.instrumentation_key
  app_config_key_vault_id = module.key_vault.app_config_key_vault_id
  client_config_key_vault_id = module.key_vault.client_config_key_vault_id
  storage_partner_connection_string = module.storage.storage_partner_connection_string
}

module "database" {
  source = "../database"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  name = "${var.resource_prefix}-pgsql"
  location = local.location
  endpoint_subnet_id = module.network.endpoint_subnet_id
  endpoint2_subnet_id = module.network.endpoint2_subnet_id
  app_config_key_vault_id = module.key_vault.app_config_key_vault_id
  key_vault_id = module.key_vault.application_key_vault_id
  rsa_key_2048 = var.rsa_key_2048
  is_metabase_env = var.is_metabase_env
}

module "key_vault" {
  source = "../key_vault"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  location = local.location
  endpoint_subnet_id = module.network.endpoint_subnet_id
}

module "front_door" {
  source = "../front_door"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  key_vault_id = module.key_vault.application_key_vault_id
  https_cert_names = var.https_cert_names
  storage_web_endpoint = module.storage.storage_web_endpoint
  is_metabase_env = var.is_metabase_env
}

module "sftp_container" {
  count = (var.environment == "prod" ? 0 : 1)
  source = "../sftp_container"
  environment = var.environment
  resource_group = var.resource_group
  name = "${var.resource_prefix}-sftpserver"
  location = local.location
  container_subnet_id = module.network.container_subnet_id
  storage_account_name = module.storage.storage_account_name
  storage_account_key = module.storage.storage_account_key
}

module "metabase" {
  count = var.is_metabase_env ? 1 : 0
  source = "../metabase"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  name = "${var.resource_prefix}-metabase"
  location = local.location
  app_service_plan_id = module.app_service_plan.app_service_plan_id
  public_subnet_id = module.network.public_subnet_id
  postgres_url = "postgresql://${module.database.server_name}.postgres.database.azure.com:5432/metabase?user=${module.database.postgres_user}@${module.database.server_name}&password=${module.database.postgres_pass}&sslmode=require&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
  ai_instrumentation_key = module.application_insights.instrumentation_key
}

module "nat_gateway" {
  source = "../nat_gateway"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  location = local.location
  public_subnet_id = module.network.public_subnet_id
}

module "application_insights" {
  source = "../application_insights"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  location = local.location
  key_vault_id = module.key_vault.application_key_vault_id
}
