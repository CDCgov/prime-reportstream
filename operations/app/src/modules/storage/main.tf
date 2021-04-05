terraform {
    required_version = ">= 0.14"
}

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

output "storage_account_name" {
  value = azurerm_storage_account.storage_account.name
}

output "storage_account_key" {
  value = azurerm_storage_account.storage_account.primary_access_key
}
