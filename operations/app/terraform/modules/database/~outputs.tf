output "postgres_server_id" {
  description = "The ID of the primary PostgreSQL server."
  value       = var.postgres_server_id
}

output "postgres_server_name" {
  description = "The name of the primary PostgreSQL server."
  value       = var.postgres_server_name
}

output "postgres_server_fqdn" {
  description = "The FQDN of the primary PostgreSQL server."
  value       = var.postgres_server_fqdn
}

output "postgres_replicas" {
  description = "A map of replica server names to their resource IDs."
  value       = var.postgres_replica_ids
}
