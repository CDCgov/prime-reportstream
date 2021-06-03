locals {
  location = "eastus"
}

module "front_door" {
  source = "../front_door"
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  key_vault_id = module.key_vault.application_key_vault_id
  eventhub_namespace_name = module.event_hub.eventhub_namespace_name
  eventhub_manage_auth_rule_id = module.event_hub.manage_auth_rule_id
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