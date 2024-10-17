# Create an Azure SQL Database
resource "azurerm_mssql_database" "default" {
  for_each = toset(var.databases)

  name                 = each.value
  server_id            = azurerm_mssql_server.default.id
  collation            = "SQL_Latin1_General_CP1_CI_AS"
  license_type         = "LicenseIncluded"
  read_scale           = false
  zone_redundant       = false
  enclave_type         = "VBS"
  storage_account_type = "Local"
  #auto_pause_delay_in_minutes = 10

  sku_name        = try(var.epool) == true ? "ElasticPool" : "S0"
  elastic_pool_id = try(var.epool) == true ? azurerm_mssql_elasticpool.default["default"].id : null

  # prevent the possibility of accidental data loss
  lifecycle {
    #prevent_destroy = true
  }
}
