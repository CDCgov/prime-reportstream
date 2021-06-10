terraform {
    required_version = ">= 0.14"
}

data "azurerm_client_config" "current" {}

data "azurerm_key_vault_secret" "postgres_user" {
  key_vault_id = var.app_config_key_vault_id
  name = "functionapp-postgres-user"
}

data "azurerm_key_vault_secret" "postgres_pass" {
  key_vault_id = var.app_config_key_vault_id
  name = "functionapp-postgres-pass"
}

resource "azurerm_postgresql_server" "postgres_server" {
  name = var.name
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
      storage_mb # Supports auto-grow
    ]
  }

  tags = {
    "environment" = var.environment
  }
}

resource "azurerm_postgresql_server" "postgres_server_replica" {
  name = "${var.name}-replica"
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
      storage_mb # Supports auto-grow
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
  endpoint_subnet_id = var.endpoint_subnet_id
}

module "postgres_private2_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_postgresql_server.postgres_server_replica.id
  name = azurerm_postgresql_server.postgres_server_replica.name
  type = "postgres_server"
  resource_group = var.resource_group
  location = azurerm_postgresql_server.postgres_server_replica.location
  endpoint_subnet_id = var.endpoint2_subnet_id
}

# Grant the database Key Vault access, to access encryption keys
resource "azurerm_key_vault_access_policy" "postgres_policy" {
  # This is a hack. The postgres module has a bug where it does not export the values until after being updated.
  # By using a count, we workout the bug by running two deploy. The first deploy created the system-assigned identity.
  # The second deploy adds the Key Value access policy.
  count = azurerm_postgresql_server.postgres_server.identity.0.principal_id != null ? 1 : 0

  key_vault_id = var.key_vault_id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azurerm_postgresql_server.postgres_server.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

data "azurerm_key_vault_key" "postgres_server_encryption_key" {
  count = var.rsa_key_2048 != null && var.rsa_key_2048 != "" && azurerm_postgresql_server.postgres_server.identity.0.principal_id != null ? 1 : 0
  key_vault_id = var.key_vault_id
  name = var.rsa_key_2048

  depends_on = [azurerm_key_vault_access_policy.postgres_policy[0]]
}

resource "azurerm_postgresql_server_key" "postgres_server_key" {
  count = length(data.azurerm_key_vault_key.postgres_server_encryption_key)
  server_id = azurerm_postgresql_server.postgres_server.id
  key_vault_key_id = data.azurerm_key_vault_key.postgres_server_encryption_key[0].id
}

# Grant the database Key Vault access, to access encryption keys
resource "azurerm_key_vault_access_policy" "postgres_replica_policy" {
  # This is a hack. The postgres module has a bug where it does not export the values until after being updated.
  # By using a count, we workout the bug by running two deploy. The first deploy created the system-assigned identity.
  # The second deploy adds the Key Value access policy.
  count = azurerm_postgresql_server.postgres_server_replica.identity.0.principal_id != null ? 1 : 0

  key_vault_id = var.key_vault_id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = azurerm_postgresql_server.postgres_server_replica.identity.0.principal_id

  key_permissions = ["get", "unwrapkey", "wrapkey"]
}

resource "azurerm_postgresql_active_directory_administrator" "postgres_aad_admin" {
  server_name = azurerm_postgresql_server.postgres_server.name
  resource_group_name = var.resource_group
  login = "reportstream_pgsql_admin"
  tenant_id = data.azurerm_client_config.current.tenant_id
  # pgsql_admin AAD group
  object_id = "c4031f1f-229c-4a8a-b3b9-23bae9dbf197"
}

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

output "server_name" {
  value = azurerm_postgresql_server.postgres_server.name
}

output "postgres_user" {
  value = data.azurerm_key_vault_secret.postgres_user.value
  sensitive = true
}

output "postgres_pass" {
  value = data.azurerm_key_vault_secret.postgres_pass.value
  sensitive = true
}
