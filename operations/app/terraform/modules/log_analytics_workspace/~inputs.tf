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

variable "function_app_id" {
  type        = string
  description = "Function App resource id"
}

variable "front_door_id" {
  type        = string
  description = "Front Door resource id"
  default     = ""
}

variable "nat_gateway_id" {
  type        = string
  description = "Nat Gateway resource id"
}

variable "primary_vnet_id" {
  type        = string
  description = "Primary vnet resource id"
}

variable "replica_vnet_id" {
  type        = string
  description = "Replica vnet resource id"
}

variable "storage_account_id" {
  type        = string
  description = "Storage Account resource id"
}

variable "storage_public_id" {
  type        = string
  description = "Storage Public resource id"
}

variable "storage_partner_id" {
  type        = string
  description = "Storage Partner resource id"
}

variable "action_group_slack_id" {
  type        = string
  description = "Slack action group resource id"
}

variable "action_group_metabase_id" {
  type        = string
  description = "Metabase healthcheck action group resource id"
}

variable "data_factory_id" {
  type        = string
  description = "Data factory resource id"
  default     = ""
}

variable "sftp_instance_01_id" {
  type        = string
  description = "SFTP instance 01 resource id"
  default     = ""
}

variable "law_retention_period" {
  type        = string
  description = "How long to keep logs for in LAW"
  default     = "30"
}

variable "action_group_id" {
  type        = string
  description = "Action group resource id"
}
