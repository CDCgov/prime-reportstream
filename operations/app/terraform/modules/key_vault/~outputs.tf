output "application_key_vault_id" {
  value = data.azurerm_key_vault.application.id
}

output "app_config_key_vault_id" {
  value = data.azurerm_key_vault.app_config.id
}

output "app_config_key_vault_name" {
  value = data.azurerm_key_vault.app_config.name
}

output "client_config_key_vault_id" {
  value = azurerm_key_vault.client_config.id
}
