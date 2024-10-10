data "http" "icanhazip" {
  url = "http://icanhazip.com"
}

locals {
  public_ip = chomp(data.http.icanhazip.response_body)
}

# Create an Azure SQL Server
resource "azurerm_mssql_server" "default" {
  name                = "sqlserver-${var.common.uid}-${var.common.env}-${var.key}"
  resource_group_name = var.common.resource_group.name
  location            = var.common.location
  version             = "12.0"
  minimum_tls_version = "1.2"

  administrator_login          = var.admin_username
  administrator_login_password = var.admin_password

  azuread_administrator {
    login_username              = var.sqladmins.display_name
    object_id                   = var.sqladmins.object_id
    azuread_authentication_only = false
  }

  identity {
    type = "SystemAssigned"
  }
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
