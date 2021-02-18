terraform {
    required_version = ">= 0.14"
}

resource "azurerm_storage_account" "storage_account" {
  resource_group_name = var.resource_group
  name = var.name
  location = var.location
  account_tier = "Standard"
  account_replication_type = "LRS"

  network_rules {
    default_action = "Deny"
    virtual_network_subnet_ids = var.subnet_ids
  }

  lifecycle {
    prevent_destroy = true
  }
  
  tags = {
    environment = var.environment
  }
}

resource "azurerm_storage_account_customer_managed_key" "storage_key" {
  count = var.environment == "test" ? 1 : 0 // Only use Vormetric keys in test
  key_name = "${var.resource_prefix}-key"
  key_vault_id = var.key_vault_id
  key_version = null // Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_account.id
}

output "storage_account_name" {
  value = azurerm_storage_account.storage_account.name
}

output "storage_account_key" {
  value = azurerm_storage_account.storage_account.primary_access_key
}
