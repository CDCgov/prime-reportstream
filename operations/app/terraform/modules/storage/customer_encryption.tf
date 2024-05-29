locals {
  customer_managed_encryption_accounts = {
    storage = {
      id = azurerm_storage_account.storage_account.id
    }
    storage_partner = {
      id = azurerm_storage_account.storage_partner.id
    }
    candidate = {
      id = azurerm_storage_account.storage_account_candidate.id
    }
    candidate_partner = {
      id = azurerm_storage_account.storage_partner_candidate.id
    }
  }
}

resource "azurerm_storage_account_customer_managed_key" "customer_key" {
  for_each = var.rsa_key_4096 != null ? local.customer_managed_encryption_accounts : {}

  key_name           = var.rsa_key_4096
  key_vault_id       = var.application_key_vault_id
  key_version        = null # Null allows automatic key rotation
  storage_account_id = each.value.id

  depends_on = [
    azurerm_key_vault_access_policy.storage_policy,
    azurerm_key_vault_access_policy.storage_partner_policy,
    azurerm_key_vault_access_policy.storage_policy_candidate,
    azurerm_key_vault_access_policy.storage_candidate_partner_policy
  ]
}
