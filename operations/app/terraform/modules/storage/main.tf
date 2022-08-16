data "azurerm_client_config" "current" {}

resource "azurerm_storage_account" "storage_account" {
  resource_group_name       = var.resource_group
  name                      = "${var.resource_prefix}storageaccount"
  location                  = var.location
  account_tier              = "Standard"
  account_replication_type  = "GRS"
  min_tls_version           = "TLS1_2"
  allow_blob_public_access  = false
  enable_https_traffic_only = true

  network_rules {
    default_action = "Deny"
    bypass         = ["AzureServices"]

    # ip_rules = sensitive(concat(
    #   split(",", data.azurerm_key_vault_secret.cyberark_ip_ingress.value),
    #   [split("/", var.terraform_caller_ip_address)[0]], # Storage accounts only allow CIDR-notation for /[0-30]
    # ))

    virtual_network_subnet_ids = var.subnets.primary_subnets
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
      network_rules[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

module "storageaccount_blob_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_storage_account.storage_account.id
  name           = azurerm_storage_account.storage_account.name
  type           = "storage_account_blob"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["blob"].name
}

module "storageaccountpartner_blob_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_storage_account.storage_partner.id
  name           = azurerm_storage_account.storage_partner.name
  type           = "storage_account_blob"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["blob"].name
}

module "storageaccount_file_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_storage_account.storage_account.id
  name           = azurerm_storage_account.storage_account.name
  type           = "storage_account_file"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["file"].name
}

module "storageaccount_queue_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_storage_account.storage_account.id
  name           = azurerm_storage_account.storage_account.name
  type           = "storage_account_queue"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["queue"].name
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
    name    = "piiretention"
    enabled = true

    filters {
      prefix_match = ["reports/"]
      blob_types   = ["blockBlob", "appendBlob"]
    }

    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = var.delete_pii_storage_after_days
      }
      snapshot {
        delete_after_days_since_creation_greater_than = var.delete_pii_storage_after_days
      }
      # Terraform does not appear to support deletion of versions
      # This needs to be manually checked in the policy and set to 60 days
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
resource "azurerm_key_vault_access_policy" "storage_policy" {
  key_vault_id = var.application_key_vault_id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_storage_account.storage_account.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_storage_account_customer_managed_key" "storage_key" {
  count              = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name           = var.rsa_key_4096
  key_vault_id       = var.application_key_vault_id
  key_version        = null # Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_account.id

  depends_on = [azurerm_key_vault_access_policy.storage_policy]
}


# # Static website

resource "azurerm_storage_account" "storage_public" {
  resource_group_name       = var.resource_group
  name                      = "${var.resource_prefix}public"
  location                  = var.location
  account_tier              = "Standard"
  account_kind              = "StorageV2"
  account_replication_type  = "GRS"
  min_tls_version           = "TLS1_2"
  allow_blob_public_access  = false
  enable_https_traffic_only = true

  static_website {
    index_document     = "index.html"
    error_404_document = "404.html"
  }

  network_rules {
    default_action = "Allow"
  }

  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    prevent_destroy = false
  }

  tags = {
    environment = var.environment
  }
}


# # Partner

resource "azurerm_storage_account" "storage_partner" {
  resource_group_name       = var.resource_group
  name                      = "${var.resource_prefix}partner"
  location                  = var.location
  account_tier              = "Standard"
  account_kind              = "StorageV2"
  is_hns_enabled            = true # Enable Data Lake v2 for HHS Protect
  account_replication_type  = "GRS"
  min_tls_version           = "TLS1_2"
  allow_blob_public_access  = false
  enable_https_traffic_only = true

  network_rules {
    default_action = "Deny"
    bypass         = ["None"]

    ip_rules = sensitive(concat(
      split(",", data.azurerm_key_vault_secret.hhsprotect_ip_ingress.value),
      split(",", data.azurerm_key_vault_secret.cyberark_ip_ingress.value),
      var.terraform_caller_ip_address, # Storage accounts only allow CIDR-notation for /[0-30]
    ))

    # ip_rules = [var.terraform_caller_ip_address]

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
      network_rules[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}



# Grant the storage account Key Vault access, to access encryption keys
resource "azurerm_key_vault_access_policy" "storage_partner_policy" {
  key_vault_id = var.application_key_vault_id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = azurerm_storage_account.storage_partner.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_storage_account_customer_managed_key" "storage_partner_key" {
  count              = var.rsa_key_4096 != null && var.rsa_key_4096 != "" ? 1 : 0
  key_name           = var.rsa_key_4096
  key_vault_id       = var.application_key_vault_id
  key_version        = null # Null allows automatic key rotation
  storage_account_id = azurerm_storage_account.storage_partner.id

  depends_on = [azurerm_key_vault_access_policy.storage_partner_policy]
}

resource "azurerm_storage_management_policy" "storage_partner_retention_policy" {
  storage_account_id = azurerm_storage_account.storage_partner.id

  rule {
    name    = "30dayretention"
    enabled = true

    filters {
      prefix_match = ["hhsprotect/"]
      blob_types   = ["blockBlob", "appendBlob"]
    }

    actions {
      base_blob {
        delete_after_days_since_modification_greater_than = 30
      }
      snapshot {
        delete_after_days_since_creation_greater_than = 30
      }
      # Terraform does not appear to support deletion of versions
      # This needs to be manually checked in the policy and set to 60 days
    }
  }

  lifecycle {
    ignore_changes = [
      # -1 value is applied, but not accepted in tf
      rule[0].actions[0].base_blob[0].tier_to_cool_after_days_since_last_access_time_greater_than
    ]
  }
}
