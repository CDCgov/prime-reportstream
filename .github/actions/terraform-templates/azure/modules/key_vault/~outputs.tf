output "meta" {
  value = azurerm_key_vault.default
}

output "secrets" {
  value = azurerm_key_vault_secret.default
}
