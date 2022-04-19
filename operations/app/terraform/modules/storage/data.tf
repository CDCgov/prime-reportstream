# // Key Vault

data "azurerm_key_vault" "application" {
  name                = "${var.resource_prefix}-keyvault"
  resource_group_name = var.resource_group
}

data "azurerm_key_vault_secret" "hhsprotect_ip_ingress" {
  name         = "hhsprotect-ip-ingress"
  key_vault_id = data.azurerm_key_vault.application.id
}

data "azurerm_key_vault_secret" "cyberark_ip_ingress" {
  name         = "cyberark-ip-ingress"
  key_vault_id = data.azurerm_key_vault.application.id
}