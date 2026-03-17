variable "environment" {
  type        = string
  description = "The target environment (e.g., 'test', 'staging', 'prod')."
}

variable "resource_group" {
  type        = string
  description = "The name of the resource group for the application, not the database."
}

variable "postgres_server_id" {
  type        = string
  description = "The full resource ID of the primary PostgreSQL server"
}

variable "postgres_server_name" {
  type        = string
  description = "The name of the primary PostgreSQL server"
}

variable "postgres_server_fqdn" {
  type        = string
  description = "The FQDN of the primary PostgreSQL server"
}

variable "postgres_replica_ids" {
  type        = map(string)
  description = "Map of replica server names to their resource IDs"
  default     = {}
}

variable "is_metabase_env" {
  type        = bool
  description = "Flag to indicate if the Metabase environment is enabled"
  default     = false
}
