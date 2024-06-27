# Postgres Server
data "azurerm_client_config" "current" {}

resource "azurerm_postgresql_flexible_server" "postgres_server" {
  #checkov:skip=CKV_AZURE_68: "Ensure that PostgreSQL server disables public network access"
  #checkov:skip=CKV2_AZURE_42: "Ensure Azure PostgreSQL server is configured with private endpoint"
  name                         = "${var.resource_prefix}-pgsql"
  location                     = var.location
  resource_group_name          = var.resource_group
  delegated_subnet_id          = var.subnets.postgres_subnets[0]
  private_dns_zone_id          = var.dns_zones["postgres"].id
  zone                         = "1"
  geo_redundant_backup_enabled = true

  administrator_login    = var.postgres_user
  administrator_password = var.postgres_pass

  sku_name   = var.db_sku_name
  version    = var.db_version
  storage_mb = var.db_storage_mb

  auto_grow_enabled = var.db_auto_grow

  public_network_access_enabled = false # (var.environment != "prod" && var.environment != "staging") ? true : false



  lifecycle {
    prevent_destroy = false
    # validated 5/21/2024
    ignore_changes = [
      storage_mb,            # Auto-grow will change the size
      administrator_login,   # This can't change without a redeploy
      administrator_password # This can't change without a redeploy
    ]
  }

  tags = {
    environment = var.environment
  }
}


# Replica Server
resource "azurerm_postgresql_flexible_server" "postgres_server_replica" {
  #checkov:skip=CKV2_AZURE_42: "Ensure Azure PostgreSQL server is configured with private endpoint"
  count                  = var.db_replica ? 1 : 0
  name                   = "${azurerm_postgresql_flexible_server.postgres_server.name}-replica"
  location               = "eastus"
  resource_group_name    = var.resource_group
  delegated_subnet_id    = var.subnets.postgres_subnets[0]
  private_dns_zone_id    = var.dns_zones["postgres"].id
  administrator_login    = var.postgres_user
  administrator_password = var.postgres_pass

  create_mode      = "Replica"
  source_server_id = azurerm_postgresql_flexible_server.postgres_server.id

  sku_name   = var.db_sku_name
  version    = var.db_version
  storage_mb = var.db_storage_mb

  auto_grow_enabled = var.db_auto_grow

  public_network_access_enabled = false

  lifecycle {
    prevent_destroy = false
    # validated 5/21/2024
    ignore_changes = [
      storage_mb,            # Auto-grow will change the size
      administrator_login,   # This can't change without a redeploy
      administrator_password # This can't change without a redeploy
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



# User Administration
resource "azurerm_postgresql_flexible_server_active_directory_administrator" "postgres_aad_admin" {
  server_name         = azurerm_postgresql_flexible_server.postgres_server.name
  resource_group_name = var.resource_group
  principal_name      = "reportstream_pgsql_admin"
  principal_type      = "Group"
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

resource "azurerm_postgresql_flexible_server_database" "prime_data_hub_db" {
  name = "prime_data_hub"

  server_id = azurerm_postgresql_flexible_server.postgres_server.id
  charset   = "UTF8"
  collation = "English_United States.1252"

  lifecycle {
    prevent_destroy = false
  }
}

resource "azurerm_postgresql_flexible_server_database" "prime_data_hub_candidate_db" {
  name = "prime_data_hub_candidate"

  server_id = azurerm_postgresql_flexible_server.postgres_server.id
  charset   = "UTF8"
  collation = "English_United States.1252"

  lifecycle {
    prevent_destroy = false
  }
}

resource "azurerm_postgresql_flexible_server_database" "metabase_db" {
  count = var.is_metabase_env ? 1 : 0
  name  = "metabase"

  server_id = azurerm_postgresql_flexible_server.postgres_server.id
  charset   = "UTF8"
  collation = "English_United States.1252"

  lifecycle {
    prevent_destroy = false
  }
}
