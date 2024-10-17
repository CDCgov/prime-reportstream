output "id" {
  value = azurerm_mssql_server.default.identity[0].principal_id
}

output "fqdn" {
  value = azurerm_mssql_server.default.fully_qualified_domain_name
}

output "db_name" {
  value = azurerm_mssql_database.default.name
}
