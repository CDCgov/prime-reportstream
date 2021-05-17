// Main storage account - candidate slot

resource "azurerm_storage_account" "storage_account_candidate" {
  resource_group_name = var.resource_group
  name = "${var.name}candidate"
  location = var.location
  account_tier = "Standard"
  account_replication_type = "GRS"

  network_rules {
    default_action = "Deny"
    ip_rules = []
    virtual_network_subnet_ids = [var.public_subnet_id, var.container_subnet_id, var.endpoint_subnet_id]
  }

  # Required for customer-managed encryption
  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    environment = var.environment
  }
}

module "storageaccount_candidate_blob_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_account_candidate.id
  name = azurerm_storage_account.storage_account_candidate.name
  type = "storage_account_blob"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = var.endpoint_subnet_id
}

module "storageaccount_candidate_file_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_account_candidate.id
  name = azurerm_storage_account.storage_account_candidate.name
  type = "storage_account_file"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = var.endpoint_subnet_id
}

module "storageaccount_candidate_queue_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_account_candidate.id
  name = azurerm_storage_account.storage_account_candidate.name
  type = "storage_account_queue"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = var.endpoint_subnet_id
}

# Point-in-time restore, soft delete, versioning, and change feed were
# enabled in the portal as terraform does not currently support this.
# At some point, this should be moved into an azurerm_template_deployment
# resource.
# These settings can be configured under the "Data protection" blade
# for Blob service

resource "azurerm_storage_management_policy" "retention_policy_candidate" {
  storage_account_id = azurerm_storage_account.storage_account_candidate.id

  rule {
    name = "30dayretention"
    enabled = true

    filters {
      prefix_match = ["reports/"]
      blob_types = ["blockBlob", "appendBlob"]
    }

    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = 30
      }
      snapshot {
        delete_after_days_since_creation_greater_than = 30
      }
      # Terraform does not appear to support deletion of versions
      # This needs to be manually checked in the policy and set to 30 days
    }
  }
}

# Grant the storage account Key Vault access, to access encryption keys
resource "azurerm_key_vault_access_policy" "storage_policy_candidate" {
  count = azurerm_storage_account.storage_account_candidate.identity.0.principal_id != null ? 1 : 0

  key_vault_id = var.key_vault_id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azurerm_storage_account.storage_account_candidate.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_storage_account_customer_managed_key" "storage_key_candidate" {
  count = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name = var.rsa_key_4096
  key_vault_id = var.key_vault_id
  key_version = null // Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_account_candidate.id

  depends_on = [azurerm_key_vault_access_policy.storage_policy]
}

output "storage_account_candidate_name" {
  value = azurerm_storage_account.storage_account_candidate.name
}

output "storage_account_candidate_key" {
  value = azurerm_storage_account.storage_account_candidate.primary_access_key
}