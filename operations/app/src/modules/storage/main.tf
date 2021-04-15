terraform {
    required_version = ">= 0.14"
}

data "azurerm_client_config" "current" {}

resource "azurerm_storage_account" "storage_account" {
  resource_group_name = var.resource_group
  name = var.name
  location = var.location
  account_tier = "Standard"
  account_replication_type = "GRS"

  network_rules {
    default_action = "Deny"
    ip_rules = []
    virtual_network_subnet_ids = [var.public_subnet_id, var.container_subnet_id]
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

module "storageaccount_blob_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_account.id
  name = azurerm_storage_account.storage_account.name
  type = "storage_account_blob"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = var.endpoint_subnet_id
}

module "storageaccount_file_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_account.id
  name = azurerm_storage_account.storage_account.name
  type = "storage_account_file"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = var.endpoint_subnet_id
}

module "storageaccount_queue_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_account.id
  name = azurerm_storage_account.storage_account.name
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

resource "azurerm_storage_management_policy" "retention_policy" {
  storage_account_id = azurerm_storage_account.storage_account.id

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
resource "azurerm_key_vault_access_policy" "storage_policy" {
  count = azurerm_storage_account.storage_account.identity.0.principal_id != null ? 1 : 0

  key_vault_id = var.key_vault_id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azurerm_storage_account.storage_account.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_storage_account_customer_managed_key" "storage_key" {
  count = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name = var.rsa_key_4096
  key_vault_id = var.key_vault_id
  key_version = null // Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_account.id

  depends_on = [azurerm_key_vault_access_policy.storage_policy]
}

module "storageaccount_access_log_event_hub_log" {
  source = "../event_hub_log"
  resource_type = "storage_account"
  log_type = "access"
  eventhub_namespace_name = var.eventhub_namespace_name
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
}

resource "azurerm_monitor_diagnostic_setting" "storageaccount_access_log" {
  name = "${var.resource_prefix}-storage_account-access-log"
  # Workaround for target resource id here: https://github.com/terraform-providers/terraform-provider-azurerm/issues/8275#issuecomment-755222989
  target_resource_id = "${azurerm_storage_account.storage_account.id}/blobServices/default/"
  eventhub_name = module.storageaccount_access_log_event_hub_log.event_hub_name
  eventhub_authorization_rule_id = var.eventhub_manage_auth_rule_id

  log {
    category = "StorageRead"
    enabled  = true

    retention_policy {
      days = 0
      enabled = false
    }
  }

  log {
    category = "StorageWrite"
    enabled  = true

    retention_policy {
      days = 0
      enabled = false
    }
  }

  log {
    category = "StorageDelete"
    enabled  = true

    retention_policy {
      days = 0
      enabled = false
    }
  }

  metric {
    category = "Transaction"
    enabled = false

    retention_policy {
      days = 0
      enabled = false
    }
  }

  metric {
    category = "Capacity"
    enabled  = false

    retention_policy {
      days = 0
      enabled = false
    }
  }
}


// Static website

resource "azurerm_storage_account" "storage_public" {
  resource_group_name = var.resource_group
  name = "${var.resource_prefix}public"
  location = var.location
  account_tier = "Standard"
  account_kind = "StorageV2"
  account_replication_type = "GRS"
  min_tls_version = "TLS1_2"
  allow_blob_public_access = false

  static_website {
    index_document = "index.html"
    error_404_document = "404.html"
  }

  network_rules {
    default_action = "Allow"
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    environment = var.environment
  }
}


// Partner

resource "azurerm_storage_account" "storage_partner" {
  resource_group_name = var.resource_group
  name = "${var.resource_prefix}partner"
  location = var.location
  account_tier = "Standard"
  account_kind = "StorageV2"
  account_replication_type = "GRS"
  min_tls_version = "TLS1_2"
  allow_blob_public_access = false

  network_rules {
    default_action = "Deny"
    ip_rules = []
    virtual_network_subnet_ids = [var.public_subnet_id]
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
resource "azurerm_key_vault_access_policy" "storage_partner_policy" {
  key_vault_id = var.key_vault_id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azurerm_storage_account.storage_partner.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_storage_account_customer_managed_key" "storage_partner_key" {
  count = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name = var.rsa_key_4096
  key_vault_id = var.key_vault_id
  key_version = null // Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_partner.id

  depends_on = [azurerm_key_vault_access_policy.storage_partner_policy]
}

module "storageaccountpartner_blob_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_storage_account.storage_partner.id
  name = azurerm_storage_account.storage_partner.name
  type = "storage_account_blob"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = var.endpoint_subnet_id
}

resource "azurerm_storage_container" "storage_container_hhsprotect" {
  name = "hhsprotect"
  storage_account_name = azurerm_storage_account.storage_partner.name
}

resource "azurerm_storage_management_policy" "storage_partner_retention_policy" {
  storage_account_id = azurerm_storage_account.storage_partner.id

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
      # Terraform does not appear to support deletion of versions
      # This needs to be manually checked in the policy and set to 30 days
    }
  }
}



output "storage_account_name" {
  value = azurerm_storage_account.storage_account.name
}

output "storage_account_key" {
  value = azurerm_storage_account.storage_account.primary_access_key
}

output "storage_account_public_id" {
  value = azurerm_storage_account.storage_public.id
}

output "storage_web_endpoint" {
  value = azurerm_storage_account.storage_public.primary_web_endpoint
}

output "storage_partner_blob_connection_string" {
  value = azurerm_storage_account.storage_partner.primary_blob_connection_string
  sensitive = true
}