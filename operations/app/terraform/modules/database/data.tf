# --- Data Sources for Existing Postgres Servers ---

# Look up the primary PostgreSQL server.
data "azurerm_postgresql_server" "primary" {
  name                = var.primary_server_name
  resource_group_name = var.database_resource_group_name
}

# Look up the replica PostgreSQL servers.
data "azurerm_postgresql_server" "replicas" {
  for_each = toset(var.replica_server_names)

  name                = each.key
  resource_group_name = var.database_resource_group_name
}
