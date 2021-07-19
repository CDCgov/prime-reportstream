# Manage a separate storage account for the candidate slot to run smoke tests against

resource "azurerm_storage_account" "storage_account_candidate" {
  resource_group_name = var.resource_group
  name = "${var.resource_prefix}candidate"
  location = var.location
  account_tier = "Standard"
  account_replication_type = "GRS"
  min_tls_version = "TLS1_2"
  allow_blob_public_access = false
  enable_https_traffic_only = true

  network_rules {
    default_action = "Deny"
    ip_rules = []
    virtual_network_subnet_ids = [
      data.azurerm_subnet.public.id,
      data.azurerm_subnet.container.id,
      data.azurerm_subnet.endpoint.id
    ]
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
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
}

module "storageaccount_candidate_file_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_account_candidate.id
  name = azurerm_storage_account.storage_account_candidate.name
  type = "storage_account_file"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
}

module "storageaccount_candidate_queue_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_account_candidate.id
  name = azurerm_storage_account.storage_account_candidate.name
  type = "storage_account_queue"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
}

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
    }
  }
}

# Grant the storage account Key Vault access, to access encryption keys
resource "azurerm_key_vault_access_policy" "storage_policy_candidate" {
  key_vault_id = data.azurerm_key_vault.application.id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azurerm_storage_account.storage_account_candidate.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_storage_account_customer_managed_key" "storage_key_candidate" {
  count = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name = var.rsa_key_4096
  key_vault_id = data.azurerm_key_vault.application.id
  key_version = null // Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_account_candidate.id

  depends_on = [azurerm_key_vault_access_policy.storage_policy_candidate]
}


// Partner

resource "azurerm_storage_account" "storage_partner_candidate" {
  resource_group_name = var.resource_group
  name = "${var.resource_prefix}candpartner"
  location = var.location
  account_tier = "Standard"
  account_kind = "StorageV2"
  is_hns_enabled = true # This enable Data Lake v2 for HHS Protect
  account_replication_type = "GRS"
  min_tls_version = "TLS1_2"
  allow_blob_public_access = false
  enable_https_traffic_only = true

  network_rules {
    default_action = "Deny"
    ip_rules = split(",", data.azurerm_key_vault_secret.hhsprotect_ip_ingress.value)
    virtual_network_subnet_ids = [
      data.azurerm_subnet.public.id,
      data.azurerm_subnet.endpoint.id
    ]
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

# Grant the storage account Key Vault access, to access encryption keys
resource "azurerm_key_vault_access_policy" "storage_candidate_partner_policy" {
  key_vault_id = data.azurerm_key_vault.application.id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azurerm_storage_account.storage_partner_candidate.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_storage_account_customer_managed_key" "storage_candidate_partner_key" {
  count = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name = var.rsa_key_4096
  key_vault_id = data.azurerm_key_vault.application.id
  key_version = null // Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_partner_candidate.id

  depends_on = [azurerm_key_vault_access_policy.storage_candidate_partner_policy]
}

module "storageaccountcandidatepartner_blob_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_partner_candidate.id
  name = azurerm_storage_account.storage_partner_candidate.name
  type = "storage_account_blob"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
}

resource "azurerm_storage_container" "storage_candidate_container_hhsprotect" {
  name = "hhsprotect"
  storage_account_name = azurerm_storage_account.storage_partner_candidate.name
}

resource "azurerm_storage_management_policy" "storage_candidate_partner_retention_policy" {
  storage_account_id = azurerm_storage_account.storage_partner_candidate.id

  rule {
    name = "30dayretention"
    enabled = true

    filters {
      prefix_match = ["hhsprotect/"]
      blob_types = ["blockBlob", "appendBlob"]
    }

    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = 30
      }
      snapshot {
        delete_after_days_since_creation_greater_than = 30
      }
    }
  }
}