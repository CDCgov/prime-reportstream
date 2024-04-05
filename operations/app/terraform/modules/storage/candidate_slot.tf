# Manage a separate storage account for the candidate slot to run smoke tests against

resource "azurerm_storage_account" "storage_account_candidate" {
  resource_group_name             = var.resource_group
  name                            = "${var.resource_prefix}candidate"
  location                        = var.location
  account_tier                    = "Standard"
  account_replication_type        = "GRS"
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  enable_https_traffic_only       = true

  network_rules {
    default_action = var.is_temp_env == true ? "Allow" : "Deny"
    bypass         = ["None"]

    ip_rules = var.terraform_caller_ip_address

    virtual_network_subnet_ids = var.subnets.vnet_public_container_endpoint_subnets
  }

  # Required for customer-managed encryption
  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      # Temp ignore ip_rules during tf development
      customer_managed_key,
      network_rules[0].ip_rules,
      network_rules[0].private_link_access
    ]
  }

  tags = {
    environment = var.environment
  }
}

module "storageaccount_candidate_blob_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_storage_account.storage_account_candidate.id
  name           = azurerm_storage_account.storage_account_candidate.name
  type           = "storage_account_blob"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["blob"].name
}

module "storageaccountcandidatepartner_blob_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_storage_account.storage_partner_candidate.id
  name           = azurerm_storage_account.storage_partner_candidate.name
  type           = "storage_account_blob"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["blob"].name
}

module "storageaccount_candidate_file_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_storage_account.storage_account_candidate.id
  name           = azurerm_storage_account.storage_account_candidate.name
  type           = "storage_account_file"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["file"].name
}

module "storageaccount_candidate_queue_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_storage_account.storage_account_candidate.id
  name           = azurerm_storage_account.storage_account_candidate.name
  type           = "storage_account_queue"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["queue"].name
}

resource "azurerm_storage_management_policy" "retention_policy_candidate" {
  storage_account_id = azurerm_storage_account.storage_account_candidate.id

  rule {
    name    = "piiretention"
    enabled = true

    filters {
      prefix_match = ["reports/"]
      blob_types   = ["blockBlob", "appendBlob"]
    }

    actions {
      dynamic "base_blob" {
        for_each = var.is_temp_env == false ? ["enabled"] : []
        content {
          delete_after_days_since_modification_greater_than = var.delete_pii_storage_after_days
        }
      }
      snapshot {
        delete_after_days_since_creation_greater_than = var.delete_pii_storage_after_days
      }
    }
  }

  lifecycle {
    ignore_changes = [
      # -1 value is applied, but not accepted in tf
      rule[0].actions[0].base_blob[0].tier_to_cool_after_days_since_last_access_time_greater_than
    ]
  }
}

# Grant the storage account Key Vault access, to access encryption keys
resource "azurerm_key_vault_access_policy" "storage_policy_candidate" {
  key_vault_id = var.application_key_vault_id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_storage_account.storage_account_candidate.identity.0.principal_id

  key_permissions = [
    "Get",
    "UnwrapKey",
    "WrapKey"
  ]
}

resource "azurerm_storage_account_customer_managed_key" "storage_key_candidate" {
  count              = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name           = var.rsa_key_4096
  key_vault_id       = var.application_key_vault_id
  key_version        = null # Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_account_candidate.id

  depends_on = [azurerm_key_vault_access_policy.storage_policy_candidate]
}


# # Partner

resource "azurerm_storage_account" "storage_partner_candidate" {
  resource_group_name             = var.resource_group
  name                            = "${var.resource_prefix}candpartner"
  location                        = var.location
  account_tier                    = "Standard"
  account_kind                    = "StorageV2"
  is_hns_enabled                  = true # This enable Data Lake v2 for HHS Protect
  account_replication_type        = "GRS"
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  enable_https_traffic_only       = true

  network_rules {
    default_action = var.is_temp_env == true ? "Allow" : "Deny"
    bypass         = ["None"]

    # ip_rules = sensitive(concat(
    #   split(",", data.azurerm_key_vault_secret.hhsprotect_ip_ingress.value),
    #   split(",", data.azurerm_key_vault_secret.cyberark_ip_ingress.value),
    #   [split("/", var.terraform_caller_ip_address)[0]], # Storage accounts only allow CIDR-notation for /[0-30]
    # ))

    ip_rules = var.terraform_caller_ip_address

    virtual_network_subnet_ids = var.subnets.primary_public_endpoint_subnets
  }

  # Required for customer-managed encryption
  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      # Temp ignore ip_rules during tf development
      secondary_blob_connection_string,
      customer_managed_key,
      network_rules[0].ip_rules,
      network_rules[0].private_link_access
    ]
  }

  tags = {
    environment = var.environment
  }
}

# Grant the storage account Key Vault access, to access encryption keys
resource "azurerm_key_vault_access_policy" "storage_candidate_partner_policy" {
  key_vault_id = var.application_key_vault_id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_storage_account.storage_partner_candidate.identity.0.principal_id

  key_permissions = [
    "Get",
    "UnwrapKey",
    "WrapKey"
  ]
}

resource "azurerm_storage_account_customer_managed_key" "storage_candidate_partner_key" {
  count              = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name           = var.rsa_key_4096
  key_vault_id       = var.application_key_vault_id
  key_version        = null # Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_partner_candidate.id

  depends_on = [azurerm_key_vault_access_policy.storage_candidate_partner_policy]
}

resource "azurerm_storage_container" "storage_candidate_container_hhsprotect" {
  name                 = "hhsprotect"
  storage_account_name = azurerm_storage_account.storage_partner_candidate.name
}

resource "azurerm_storage_management_policy" "storage_candidate_partner_retention_policy" {
  storage_account_id = azurerm_storage_account.storage_partner_candidate.id

  rule {
    name    = "30dayretention"
    enabled = true

    filters {
      prefix_match = ["hhsprotect/"]
      blob_types   = ["blockBlob", "appendBlob"]
    }

    actions {
      dynamic "base_blob" {
        for_each = var.is_temp_env == false ? ["enabled"] : []
        content {
          delete_after_days_since_modification_greater_than = 30
        }
      }
      snapshot {
        delete_after_days_since_creation_greater_than = 30
      }
    }
  }

  lifecycle {
    ignore_changes = [
      # -1 value is applied, but not accepted in tf
      rule[0].actions[0].base_blob[0].tier_to_cool_after_days_since_last_access_time_greater_than
    ]
  }
}
