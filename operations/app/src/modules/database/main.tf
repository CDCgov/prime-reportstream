terraform {
    required_version = ">= 0.14"
}

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

  ssl_enforcement_enabled = true
  ssl_minimal_tls_version_enforced = "TLS1_2"

  threat_detection_policy {
    enabled = true
    email_account_admins = true
  }

  lifecycle {
    prevent_destroy = true
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

  ssl_enforcement_enabled = true
  ssl_minimal_tls_version_enforced = "TLS1_2"

  threat_detection_policy {
    enabled = true
    email_account_admins = true
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    "environment" = var.environment
  }
}

resource "azurerm_postgresql_active_directory_administrator" "postgres_aad_admin" {
  server_name = azurerm_postgresql_server.postgres_server.name
  resource_group_name = var.resource_group
  login = "reportstream_pgsql_admin"
  tenant_id = data.azurerm_client_config.current.tenant_id
  # pgsql_admin AAD group
  object_id = "c4031f1f-229c-4a8a-b3b9-23bae9dbf197"
}

resource "azurerm_postgresql_virtual_network_rule" "allow_public_subnet" {
  name = "AllowPublicSubnet"
  resource_group_name = var.resource_group
  server_name = azurerm_postgresql_server.postgres_server.name
  subnet_id = var.public_subnet_id
}

resource "azurerm_postgresql_virtual_network_rule" "allow_private_subnet" {
  name = "AllowPrivateSubnet"
  resource_group_name = var.resource_group
  server_name = azurerm_postgresql_server.postgres_server.name
  subnet_id = var.private_subnet_id
}

resource "azurerm_postgresql_virtual_network_rule" "allow_private2_subnet" {
  name = "AllowPrivateSubnet"
  resource_group_name = var.resource_group
  server_name = azurerm_postgresql_server.postgres_server_replica.name
  subnet_id = var.private2_subnet_id
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
  count = (var.environment == "test" || var.environment == "prod" ? 1 : 0)
  name = "metabase"
  resource_group_name = var.resource_group
  server_name = azurerm_postgresql_server.postgres_server.name
  charset = "UTF8"
  collation = "English_United States.1252"

  lifecycle {
    prevent_destroy = true
  }
}

module "postgresql_db_log_event_hub_log" {
  source = "../event_hub_log"
  resource_type = "postgresql"
  log_type = "db"
  eventhub_namespace_name = var.eventhub_namespace_name
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
}

resource "azurerm_monitor_diagnostic_setting" "postgresql_db_log" {
  name = "${var.resource_prefix}-postgresql-db-log"
  target_resource_id = azurerm_postgresql_server.postgres_server.id
  eventhub_name = module.postgresql_db_log_event_hub_log.event_hub_name
  eventhub_authorization_rule_id = var.eventhub_manage_auth_rule_id

  log {
    category = "PostgreSQLLogs"
    enabled  = true

    retention_policy {
      days = 0
      enabled = false
    }
  }

  log {
    category = "QueryStoreRuntimeStatistics"
    enabled  = false

    retention_policy {
      days = 0
      enabled = false
    }
  }

  log {
    category = "QueryStoreWaitStatistics"
    enabled  = false

    retention_policy {
      days = 0
      enabled = false
    }
  }

  metric {
    category = "AllMetrics"
    enabled = false

    retention_policy {
      days = 0
      enabled = false
    }
  }
}

data "azurerm_client_config" "current" {}

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
