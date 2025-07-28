variable "environment" {
  type        = string
  description = "The target environment (e.g., 'test', 'staging', 'prod')."
}

variable "resource_group" {
  type        = string
  description = "The name of the resource group for the application, not the database."
}

variable "database_resource_group_name" {
  type        = string
  description = "The name of the resource group where the external PostgreSQL servers are located."
}

variable "primary_server_name" {
  type        = string
  description = "The name of the primary PostgreSQL server to look up."
}

variable "replica_server_names" {
  type        = list(string)
  description = "A list of names for the read replica PostgreSQL servers to look up."
  default     = []
}

variable "database_names" {
  type        = list(string)
  description = "A list of database names to look up on the primary server."
  default     = []
}

variable "is_metabase_env" {
  type        = bool
  description = "Flag to indicate if the Metabase database should be looked up. If true, 'metabase' must be in `var.database_names`."
  default     = false
}
