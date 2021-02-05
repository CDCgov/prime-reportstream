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
