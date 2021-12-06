output "sa_primary_access_key" {
  value = azurerm_storage_account.storage_account.primary_access_key
}

output "sa_primary_connection_string" {
  value = azurerm_storage_account.storage_account.primary_connection_string
}