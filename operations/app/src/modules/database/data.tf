data "azurerm_client_config" "current" {}


// Network

data "azurerm_subnet" "endpoint" {
  name = "endpoint"
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name = var.resource_group
}

data "azurerm_subnet" "endpoint_replica" {
  name = "endpoint"
  virtual_network_name = "${var.resource_prefix}-vnet-peer"
  resource_group_name = var.resource_group
}


// Key Vault

data "azurerm_key_vault" "application" {
  name = "${var.resource_prefix}-keyvault"
  resource_group_name = var.resource_group
}

data "azurerm_key_vault" "app_config" {
  name = "${var.resource_prefix}-appconfig"
  resource_group_name = var.resource_group
}


// Postgres User

data "azurerm_key_vault_secret" "postgres_user" {
  key_vault_id = data.azurerm_key_vault.app_config.id
  name = "functionapp-postgres-user"
}

data "azurerm_key_vault_secret" "postgres_pass" {
  key_vault_id = data.azurerm_key_vault.app_config.id
  name = "functionapp-postgres-pass"
}


// Encryption

data "azurerm_key_vault_key" "postgres_server_encryption_key" {
  count = var.rsa_key_2048 != null && var.rsa_key_2048 != "" ? 1 : 0
  key_vault_id = data.azurerm_key_vault.application.id
  name = var.rsa_key_2048

  depends_on = [azurerm_key_vault_access_policy.postgres_policy]
}