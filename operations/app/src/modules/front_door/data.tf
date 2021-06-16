// Key Vault

data "azurerm_key_vault" "application" {
  name = "${var.resource_prefix}-keyvault"
  resource_group_name = var.resource_group
}

data "azurerm_key_vault_secret" "https_cert" {
  count = length(var.https_cert_names)
  key_vault_id = data.azurerm_key_vault.application.id
  name = var.https_cert_names[count.index]
}


// Storage

data "azurerm_storage_account" "storage_public" {
  name = "${var.resource_prefix}public"
  resource_group_name = var.resource_group
}