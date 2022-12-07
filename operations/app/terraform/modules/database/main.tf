# Postgres Server
data "azurerm_client_config" "current" {}

resource "azurerm_postgresql_server" "postgres_server" {
  name                         = "${var.resource_prefix}-pgsql"
  location                     = var.location
  resource_group_name          = var.resource_group
  administrator_login          = var.postgres_user
  administrator_login_password = var.postgres_pass

  sku_name   = var.db_sku_name
  version    = var.db_version
  storage_mb = var.db_storage_mb

  auto_grow_enabled = var.db_auto_grow

  public_network_access_enabled    = false
  ssl_enforcement_enabled          = true
  ssl_minimal_tls_version_enforced = "TLS1_2"

  threat_detection_policy {
    enabled              = var.db_threat_detection
    email_account_admins = true
  }

  # Required for customer-managed encryption
  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      storage_mb, # Auto-grow will change the size
      administrator_login,
      administrator_login_password # This can't change without a redeploy
    ]
  }

  tags = {
    environment = var.environment
  }
}

module "postgres_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_postgresql_server.postgres_server.id
  name           = azurerm_postgresql_server.postgres_server.name
  type           = "postgres_server"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["postgres"].name
}


# Replicate Server

resource "azurerm_postgresql_server" "postgres_server_replica" {
  count                        = var.db_replica ? 1 : 0
  name                         = "${azurerm_postgresql_server.postgres_server.name}-replica"
  location                     = "westus"
  resource_group_name          = var.resource_group
  administrator_login          = var.postgres_user
  administrator_login_password = var.postgres_pass

  create_mode               = "Replica"
  creation_source_server_id = azurerm_postgresql_server.postgres_server.id

  sku_name   = var.db_sku_name
  version    = var.db_version
  storage_mb = var.db_storage_mb

  auto_grow_enabled = var.db_auto_grow

  public_network_access_enabled    = false
  ssl_enforcement_enabled          = true
  ssl_minimal_tls_version_enforced = "TLS1_2"

  threat_detection_policy {
    enabled              = var.db_threat_detection
    email_account_admins = true
  }

  # Required for customer-managed encryption
  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      storage_mb,                  # Auto-grow will change the size
      administrator_login,         # Temp ignore during terraform overhaul
      administrator_login_password # This can't change without a redeploy
    ]
  }

  tags = {
    environment = var.environment
  }

  timeouts {
    create = "3h"
    delete = "2h"
  }
}

module "postgres_private_endpoint_replica" {
  for_each = var.db_replica ? var.subnets.replica_endpoint_subnets : []

  source         = "../common/private_endpoint"
  resource_id    = azurerm_postgresql_server.postgres_server_replica[0].id
  name           = azurerm_postgresql_server.postgres_server_replica[0].name
  type           = "postgres_server"
  resource_group = var.resource_group
  location       = azurerm_postgresql_server.postgres_server_replica[0].location

  endpoint_subnet_ids = each.value
  dns_vnet            = var.dns_vnet == "East-vnet" ? "West-vnet" : var.dns_vnet
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["postgres"].name
}


# User Administration

resource "azurerm_postgresql_active_directory_administrator" "postgres_aad_admin" {
  server_name         = azurerm_postgresql_server.postgres_server.name
  resource_group_name = var.resource_group
  login               = "reportstream_pgsql_admin"
  tenant_id           = data.azurerm_client_config.current.tenant_id
  object_id           = var.aad_group_postgres_admin
}


# Encryption

#resource "azurerm_key_vault_access_policy" "postgres_policy" {
#  key_vault_id = var.application_key_vault_id
#  tenant_id    = data.azurerm_client_config.current.tenant_id
#  object_id    = azurerm_postgresql_server.postgres_server.identity.0.principal_id
#
#  key_permissions = ["get", "unwrapkey", "wrapkey"]
#}

#resource "azurerm_key_vault_key" "postgres_server_encryption_key" {
#  name         = "tfex-key-2"
#  key_vault_id = var.application_key_vault_id
#  key_type     = "RSA"
#  key_size     = 2048
#  key_opts     = ["decrypt", "encrypt", "sign", "unwrapKey", "verify", "wrapKey"]
#
#  depends_on = [
#    azurerm_key_vault_access_policy.postgres_policy
#  ]
#}

#resource "azurerm_postgresql_server_key" "postgres_server_key" {
#  server_id        = azurerm_postgresql_server.postgres_server.id
#  key_vault_key_id = var.rsa_key_2048
#}

# Databases

resource "azurerm_postgresql_database" "prime_data_hub_db" {
  name                = "prime_data_hub"
  resource_group_name = var.resource_group
  server_name         = azurerm_postgresql_server.postgres_server.name
  charset             = "UTF8"
  collation           = "English_United States.1252"

  lifecycle {
    prevent_destroy = false
  }
}

resource "azurerm_postgresql_database" "prime_data_hub_candidate_db" {
  name                = "prime_data_hub_candidate"
  resource_group_name = var.resource_group
  server_name         = azurerm_postgresql_server.postgres_server.name
  charset             = "UTF8"
  collation           = "English_United States.1252"

  lifecycle {
    prevent_destroy = false
  }
}

resource "azurerm_postgresql_database" "metabase_db" {
  count               = var.is_metabase_env ? 1 : 0
  name                = "metabase"
  resource_group_name = var.resource_group
  server_name         = azurerm_postgresql_server.postgres_server.name
  charset             = "UTF8"
  collation           = "English_United States.1252"

  lifecycle {
    prevent_destroy = false
  }
}
