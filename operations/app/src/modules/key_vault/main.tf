terraform {
  required_version = ">= 0.14"
}

data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "application" {
  name = "${var.resource_prefix}-keyvault"
  location = var.location
  resource_group_name = var.resource_group
  sku_name = "premium"
  tenant_id = data.azurerm_client_config.current.tenant_id
}

output "application_key_vault_id" {
  value = azurerm_key_vault.application.id
}