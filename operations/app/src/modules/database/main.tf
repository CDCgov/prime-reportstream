terraform {
    required_version = ">= 0.14"
}

resource "azurerm_postgresql_server" "postgres_server" {
  name = var.name
  location = var.location
  resource_group_name = var.resource_group

  administrator_login = var.postgres_user
  administrator_login_password = var.postgres_password

  sku_name = "GP_Gen5_4"
  version = "11"
  storage_mb = 5120

  geo_redundant_backup_enabled = true
  auto_grow_enabled = (var.environment == "prod" ? true : false)

  ssl_enforcement_enabled = true
  ssl_minimal_tls_version_enforced = "TLS1_2"
}

resource "azurerm_postgresql_virtual_network_rule" "allow_public_subnet" {
  name = "AllowPublicSubnet"
  resource_group_name = var.resource_group
  server_name = azurerm_postgresql_server.postgres_server.name
  subnet_id = var.public_subnet_id
}
