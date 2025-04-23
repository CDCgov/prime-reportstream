data "azurerm_client_config" "current" {}

data "azurerm_key_vault" "app_config" {
  name                = var.app_config_kv_name
  resource_group_name = var.resource_group
}

data "azurerm_key_vault" "application" {
  name                = var.application_kv_name
  resource_group_name = var.resource_group
}
