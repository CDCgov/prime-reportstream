resource "azurerm_mssql_elasticpool" "default" {
  for_each = try(var.epool) == true ? toset(["default"]) : []

  name                = "epool-${var.common.uid}-${var.common.env}-${var.key}"
  resource_group_name = var.common.resource_group.name
  location            = var.common.location
  server_name         = azurerm_mssql_server.default.name
  license_type        = "LicenseIncluded"
  enclave_type        = "VBS"
  max_size_gb         = 4.8828125

  sku {
    name     = "BasicPool"
    tier     = "Basic"
    capacity = 50
  }

  per_database_settings {
    min_capacity = 0
    max_capacity = 5
  }
}