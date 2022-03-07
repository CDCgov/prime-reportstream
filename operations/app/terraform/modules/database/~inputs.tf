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
  description = "Database Server Location"
}

variable "rsa_key_2048" {
  type        = string
  description = "Name of the 2048 length RSA key in the Key Vault. Omitting will use Azure-managed key instead of a customer-key."
}

variable "aad_group_postgres_admin" {
  type        = string
  description = "Azure Active Directory Group ID for postgres_admin"
}

variable "is_metabase_env" {
  type        = bool
  description = "Should Metabase be deployed in this environment"
}

variable "use_cdc_managed_vnet" {
  type        = bool
  description = "If the environment should be deployed to the CDC managed VNET"
}

variable "postgres_user" {}
variable "postgres_pass" {}
variable "db_sku_name" {}
variable "db_version" {}
variable "db_storage_mb" {}
variable "db_auto_grow" {}
variable "db_prevent_destroy" {}

variable "db_threat_detection" {}
variable "endpoint_subnet" {}
variable "db_replica" {}
variable "application_key_vault_id" {}

variable "west_vnet_subnets" {
  type        = list(string)
  description = "West Vnet subnets"
}

variable "east_vnet_subnets" {
  type        = list(string)
  description = "East Vnet subnets"
}

variable "vnet_subnets" {
  type        = list(string)
  description = "Vnet subnets"
}
