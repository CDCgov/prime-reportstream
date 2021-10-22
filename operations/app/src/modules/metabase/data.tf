// Network

data "azurerm_subnet" "public" {
  name                 = "public"
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "public_subnet" {
  name                 = "public"
  virtual_network_name = "${var.resource_prefix}-East-vnet"
  resource_group_name  = var.resource_group
}


// Key Vault

data "azurerm_key_vault" "app_config" {
  name                = "${var.resource_prefix}-appconfig"
  resource_group_name = var.resource_group
}


// App Service Plan

data "azurerm_app_service_plan" "service_plan" {
  name                = "${var.resource_prefix}-serviceplan"
  resource_group_name = var.resource_group
}


// Database

data "azurerm_postgresql_server" "postgres_server" {
  name                = "${var.resource_prefix}-pgsql"
  resource_group_name = var.resource_group
}

data "azurerm_key_vault_secret" "postgres_user" {
  key_vault_id = data.azurerm_key_vault.app_config.id
  name         = "functionapp-postgres-user"
}

data "azurerm_key_vault_secret" "postgres_pass" {
  key_vault_id = data.azurerm_key_vault.app_config.id
  name         = "functionapp-postgres-pass"
}