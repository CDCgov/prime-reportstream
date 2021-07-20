// Postgres Server

resource "azurerm_postgresql_server" "postgres_server" {
  name = "${var.resource_prefix}-pgsql"
  location = var.location
  resource_group_name = var.resource_group
  administrator_login = data.azurerm_key_vault_secret.postgres_user.value
  administrator_login_password = data.azurerm_key_vault_secret.postgres_pass.value

  sku_name = "GP_Gen5_4"
  version = "11"
  storage_mb = 5120

  auto_grow_enabled = true

  public_network_access_enabled = false
  ssl_enforcement_enabled = true
  ssl_minimal_tls_version_enforced = "TLS1_2"

  threat_detection_policy {
    enabled = true
    email_account_admins = true
  }

  # Required for customer-managed encryption
  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    prevent_destroy = true
    ignore_changes = [
      storage_mb, # Auto-grow will change the size
      administrator_login_password # This can't change without a redeploy
    ]
  }

  tags = {
    "environment" = var.environment
  }
}

module "postgres_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_postgresql_server.postgres_server.id
  name = azurerm_postgresql_server.postgres_server.name
  type = "postgres_server"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
}


// Replicate Server

resource "azurerm_postgresql_server" "postgres_server_replica" {
  name = "${azurerm_postgresql_server.postgres_server.name}-replica"
  location = "westus"
  resource_group_name = var.resource_group
  administrator_login = data.azurerm_key_vault_secret.postgres_user.value
  administrator_login_password = data.azurerm_key_vault_secret.postgres_pass.value

  create_mode = "Replica"
  creation_source_server_id = azurerm_postgresql_server.postgres_server.id

  sku_name = "GP_Gen5_4"
  version = "11"
  storage_mb = 5120

  auto_grow_enabled = true

  public_network_access_enabled = false
  ssl_enforcement_enabled = true
  ssl_minimal_tls_version_enforced = "TLS1_2"

  threat_detection_policy {
    enabled = true
    email_account_admins = true
  }

  # Required for customer-managed encryption
  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    prevent_destroy = true
    ignore_changes = [
      storage_mb, # Auto-grow will change the size
      administrator_login_password # This can't change without a redeploy
    ]
  }

  tags = {
    "environment" = var.environment
  }
}

module "postgres_private_endpoint_replica" {
  source = "../common/private_endpoint"
  resource_id = azurerm_postgresql_server.postgres_server_replica.id
  name = azurerm_postgresql_server.postgres_server_replica.name
  type = "postgres_server"
  resource_group = var.resource_group
  location = azurerm_postgresql_server.postgres_server_replica.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint_replica.id
}


// User Administration

resource "azurerm_postgresql_active_directory_administrator" "postgres_aad_admin" {
  server_name = azurerm_postgresql_server.postgres_server.name
  resource_group_name = var.resource_group
  login = "reportstream_pgsql_admin"
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = var.aad_group_postgres_admin
}


// Encryption

resource "azurerm_key_vault_access_policy" "postgres_policy" {
  key_vault_id = data.azurerm_key_vault.application.id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azurerm_postgresql_server.postgres_server.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_postgresql_server_key" "postgres_server_key" {
  count = length(data.azurerm_key_vault_key.postgres_server_encryption_key)
  server_id = azurerm_postgresql_server.postgres_server.id
  key_vault_key_id = data.azurerm_key_vault_key.postgres_server_encryption_key[0].id
}

// Databases

resource "azurerm_postgresql_database" "prime_data_hub_db" {
  name = "prime_data_hub"
  resource_group_name = var.resource_group
  server_name = azurerm_postgresql_server.postgres_server.name
  charset = "UTF8"
  collation = "English_United States.1252"

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_postgresql_database" "prime_data_hub_candidate_db" {
  name = "prime_data_hub_candidate"
  resource_group_name = var.resource_group
  server_name = azurerm_postgresql_server.postgres_server.name
  charset = "UTF8"
  collation = "English_United States.1252"

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_postgresql_database" "metabase_db" {
  count = var.is_metabase_env ? 1 : 0
  name = "metabase"
  resource_group_name = var.resource_group
  server_name = azurerm_postgresql_server.postgres_server.name
  charset = "UTF8"
  collation = "English_United States.1252"

  lifecycle {
    prevent_destroy = true
  }
}