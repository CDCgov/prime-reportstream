# # Key Vault

data "azurerm_key_vault_secret" "hhsprotect_ip_ingress" {
  name         = "hhsprotect-ip-ingress"
  key_vault_id = var.application_key_vault_id
}

data "azurerm_key_vault_secret" "cyberark_ip_ingress" {
  name         = "cyberark-ip-ingress"
  key_vault_id = var.application_key_vault_id
}
