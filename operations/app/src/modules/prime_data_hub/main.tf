locals {
    location = "eastus"
}

module "storage" {
    source = "../storage"
    environment = var.environment
    resource_group = var.resource_group
    name = "${var.resource_prefix}storageaccount"
    location = local.location
    subnet_ids = [module.network.public_subnet_id,
                  module.network.container_subnet_id,
                  module.network.private_subnet_id]
    sftp_share_name = module.sftp_container.name
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
}

module "function_app" {
    source = "../function_app"
    environment = var.environment
    resource_group = var.resource_group
    resource_prefix = var.resource_prefix
    location = local.location
    storage_account_name = module.storage.storage_account_name
    storage_account_key = module.storage.storage_account_key
    public_subnet_id = module.network.public_subnet_id
    postgres_user = "${var.postgres_user}@${module.database.server_name}"
    postgres_password = var.postgres_password
    postgres_url = "jdbc:postgresql://${module.database.server_name}.postgres.database.azure.com:5432/prime_data_hub?sslmode=require"
    redox_secret = var.redox_secret
    okta_client_id = var.okta_client_id
    az_phd_user = var.az_phd_user
    az_phd_password = var.az_phd_password
    login_server = module.container_registry.login_server
    admin_user = module.container_registry.admin_username
    admin_password = module.container_registry.admin_password
    ai_instrumentation_key = module.application_insights.instrumentation_key
}

module "database" {
    source = "../database"
    environment = var.environment
    resource_group = var.resource_group
    name = "${var.resource_prefix}-pgsql"
    location = local.location
    postgres_user = var.postgres_user
    postgres_password = var.postgres_password
    public_subnet_id = module.network.public_subnet_id
    private_subnet_id = module.network.private_subnet_id
}

module "key_vault" {
    source = "../key_vault"
    environment = var.environment
    resource_group = var.resource_group
    resource_prefix = var.resource_prefix
    location = local.location
}

module "front_door" {
    source = "../front_door"
    environment = var.environment
    resource_group = var.resource_group
    resource_prefix = var.resource_prefix
    key_vault_id = module.key_vault.application_key_vault_id
    access_eventhub = module.event_hub.frontdoor_access_eventhub
    waf_eventhub = module.event_hub.frontdoor_waf_eventhub
    auth_rule = module.event_hub.frontdoor_auth_rule
}

module "sftp_container" {
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
    source = "../metabase"
    environment = var.environment
    resource_group = var.resource_group
    name = "${var.resource_prefix}-metabase"
    location = local.location
    app_service_plan_id = module.function_app.app_service_plan_id
    public_subnet_id = module.network.public_subnet_id
    postgres_url = "postgresql://${module.database.server_name}.postgres.database.azure.com:5432/metabase?user=${var.postgres_user}@${module.database.server_name}&password=${var.postgres_password}&sslmode=require&ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory"
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
    name = "${var.resource_prefix}-appinsights"
    location = local.location
}

module "event_hub" {
    source = "../event_hub"
    environment = var.environment
    resource_group = var.resource_group
    resource_prefix = var.resource_prefix
    location = local.location
}
