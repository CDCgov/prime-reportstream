data "azurerm_key_vault" "application" {
  name                = "${var.resource_prefix}-keyvault"
  resource_group_name = var.resource_group
}

data "azurerm_key_vault_secret" "pagerduty_url" {
  key_vault_id = data.azurerm_key_vault.application.id
  name         = "pagerduty-integration-url"
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