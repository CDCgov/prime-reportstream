variable "environment" {
  type        = string
  description = "Target Environment"
}

variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "resource_prefix" {
  type        = string
  description = "Resource Prefix"
}

variable "location" {
  type        = string
  description = "Function App Location"
}

variable "service_plan_id" {
  type        = string
  description = "Application Service Plan resource id"
}

variable "container_registry_id" {
  type        = string
  description = "Container Registry resource id"
}

variable "postgres_server_id" {
  type        = string
  description = "Postgres Server resource id"
}

variable "application_key_vault_id" {
  type        = string
  description = "App Key Vault resource id"
}

variable "app_config_key_vault_id" {
  type        = string
  description = "App Config Key Vault resource id"
}

variable "client_config_key_vault_id" {
  type        = string
  description = "Client Config Key Vault resource id"
}
