resource "azurerm_storage_container" "storage_container_hhsprotect" {
  name                 = "hhsprotect"
  storage_account_name = azurerm_storage_account.storage_partner.name
}

resource "azurerm_storage_container" "storage_container_dcipher" {
  name                 = "dcipher"
  storage_account_name = azurerm_storage_account.storage_partner.name
}
