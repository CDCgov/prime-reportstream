output "postgres_server_id" {
  description = "The ID of the primary PostgreSQL server."
  value       = data.azurerm_postgresql_server.primary.id
}
output "postgres_server_name" {
  description = "The name of the primary PostgreSQL server."
  value       = data.azurerm_postgresql_server.primary.name
}

output "postgres_server_fqdn" {
  description = "The FQDN of the primary PostgreSQL server."
  value       = data.azurerm_postgresql_server.primary.fqdn
}

output "postgres_replicas" {
  description = "A map of the replica PostgreSQL servers, keyed by name."
  value       = data.azurerm_postgresql_server.replicas
}
