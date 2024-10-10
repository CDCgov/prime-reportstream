resource "azurerm_storage_account" "default" {
  name                            = "sa${var.common.env}${var.common.uid}data"
  location                        = var.common.location
  resource_group_name             = var.common.resource_group.name
  account_tier                    = "Standard"
  account_replication_type        = "LRS"
  public_network_access_enabled   = true
  is_hns_enabled                  = true
  allow_nested_items_to_be_public = false

  identity {
    type = "SystemAssigned"
  }
}

resource "azurerm_storage_container" "default" {
  name                  = "container-${var.common.env}"
  storage_account_name  = azurerm_storage_account.default.name
  container_access_type = "private"
}

resource "azurerm_role_assignment" "la_azure_db" {
  scope                = azurerm_storage_account.default.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = var.logic_app.principal_id
}
