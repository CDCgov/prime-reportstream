resource "azurerm_key_vault" "default" {
  name                       = "kv-${var.common.uid}-${var.common.env}-${var.key}"
  location                   = var.common.location
  resource_group_name        = var.common.resource_group.name
  tenant_id                  = var.common.tenant_id
  sku_name                   = "standard"
  soft_delete_retention_days = 7
  purge_protection_enabled   = false
  enable_rbac_authorization  = true
}

resource "azurerm_key_vault_secret" "default" {
  for_each = var.secrets

  name         = each.key
  value        = each.value.value
  key_vault_id = azurerm_key_vault.default.id

  lifecycle {
    ignore_changes = [value]
  }
}

