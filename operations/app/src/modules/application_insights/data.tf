data "azurerm_key_vault" "application" {
  name = "${var.resource_prefix}-keyvault"
  resource_group_name = var.resource_group
}

data "azurerm_key_vault_secret" "pagerduty_url" {
  key_vault_id = data.azurerm_key_vault.application.id
  name = "pagerduty-integration-url"
}