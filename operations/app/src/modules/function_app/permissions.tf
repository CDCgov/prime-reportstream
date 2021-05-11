resource "azurerm_key_vault_access_policy" "key_vault_appconfig_access" {
  for_each = local.slots

  key_vault_id = var.app_config_key_vault_id
  tenant_id = each.value.identity.0.tenant_id
  object_id = each.value.identity.0.principal_id

  secret_permissions = ["Get"]
}

resource "azurerm_key_vault_access_policy" "key_vault_clientconfig_access" {
  for_each = local.slots

  key_vault_id = var.client_config_key_vault_id
  tenant_id = each.value.identity.0.tenant_id
  object_id = each.value.identity.0.principal_id

  secret_permissions = ["Get"]
}