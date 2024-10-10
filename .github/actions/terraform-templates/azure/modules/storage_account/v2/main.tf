resource "azurerm_storage_account" "default" {
  name                            = "sa${var.common.env}${var.common.uid}${var.key}"
  location                        = var.common.location
  resource_group_name             = var.common.resource_group.name
  account_tier                    = var.account_tier
  account_replication_type        = "LRS"
  public_network_access_enabled   = true
  is_hns_enabled                  = false
  allow_nested_items_to_be_public = false
  account_kind                    = var.account_kind

  identity {
    type = "SystemAssigned"
  }
}
