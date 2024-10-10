data "http" "icanhazip" {
  url = "http://icanhazip.com"
}

locals {
  public_ip = chomp(data.http.icanhazip.response_body)
}

resource "azuread_group" "sqladmins" {
  display_name     = "sqladmins-${var.common.env}"
  owners           = [var.common.owner.object_id]
  security_enabled = true

  members = [
    var.common.owner.object_id,
    /* more users */
  ]
}

# Create an Azure SQL Server
resource "azurerm_mssql_server" "default" {
  name                = "sqlserver-${var.common.uid}-${var.common.env}"
  resource_group_name = var.common.resource_group.name
  location            = var.common.location
  version             = "12.0"
  minimum_tls_version = "1.2"

  azuread_administrator {
    login_username              = azuread_group.sqladmins.display_name
    object_id                   = azuread_group.sqladmins.object_id
    azuread_authentication_only = true
  }

  identity {
    type = "SystemAssigned"
  }
}

# Create an Azure SQL Database
resource "azurerm_mssql_database" "default" {
  name                 = "db-${var.common.env}"
  server_id            = azurerm_mssql_server.default.id
  collation            = "SQL_Latin1_General_CP1_CI_AS"
  license_type         = "LicenseIncluded"
  max_size_gb          = 10
  read_scale           = false
  sku_name             = "S0"
  zone_redundant       = false
  enclave_type         = "VBS"
  storage_account_type = "Local"
  #auto_pause_delay_in_minutes = 10

  # prevent the possibility of accidental data loss
  lifecycle {
    #prevent_destroy = true
  }
}

resource "mssql_user" "la" {
  server {
    host = azurerm_mssql_server.default.fully_qualified_domain_name
    azuread_default_chain_auth {}
  }

  database = azurerm_mssql_database.default.name
  username = var.logic_app_name
  roles    = ["db_datareader", "db_datawriter"]

  depends_on = [azurerm_mssql_firewall_rule.default]
}

resource "azurerm_mssql_firewall_rule" "default" {
  name             = "terraform-firewall-rule"
  server_id        = azurerm_mssql_server.default.id
  start_ip_address = local.public_ip
  end_ip_address   = local.public_ip
}

resource "azurerm_mssql_firewall_rule" "azure_access" {
  name             = "allow-access-from-azure"
  server_id        = azurerm_mssql_server.default.id
  start_ip_address = "0.0.0.0"
  end_ip_address   = "0.0.0.0"
}
