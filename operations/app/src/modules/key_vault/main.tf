terraform {
  required_version = ">= 0.14"
}

locals {
  object_ids = ["34232fe8-00ad-4bd0-9afb-eb9b3cc93ffe",
                "cd341fbc-26a3-405c-a350-c4237a27aa93",
                "637fb7df-c200-4e0d-ba86-608576acb786",
                "aabc25d7-dd99-42b9-8f3a-fd593b1f229a"]
}

data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "application" {
  name = "${var.resource_prefix}-keyvault"
  location = var.location
  resource_group_name = var.resource_group
  sku_name = "premium"
  tenant_id = data.azurerm_client_config.current.tenant_id
}

resource "azurerm_key_vault_access_policy" "access_policy" {
  count = length(local.object_ids)
  key_vault_id = azurerm_key_vault.application.id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = local.object_ids[count.index]

  key_permissions = [
    "Get",
    "List",
    "Update",
    "Create",
    "Import",
    "Delete",
    "Recover",
    "Backup",
    "Restore"
  ]

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore"
  ]

  certificate_permissions = [ 
    "Get",
    "List",
    "Update",
    "Create",
    "Import",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
    "ManageContacts",
    "ManageIssuers",
    "GetIssuers",
    "ListIssuers",
    "SetIssuers",
    "DeleteIssuers" 
  ]
}

output "application_key_vault_id" {
  value = azurerm_key_vault.application.id
}
