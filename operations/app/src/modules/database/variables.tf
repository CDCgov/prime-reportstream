variable "environment" {
    type = string
    description = "Target Environment"
}

variable "resource_group" {
    type = string
    description = "Resource Group Name"
}

variable "resource_prefix" {
    type = string
    description = "Resource Prefix"
}

variable "name" {
    type = string
    description = "Database Server Name"
}

variable "location" {
    type = string
    description = "Database Server Location"
}

variable "endpoint_subnet_id" {
    type = string
    description = "Private Endpoint Subnet ID"
}

variable "endpoint2_subnet_id" {
    type = string
    description = "Private Endpoint2 Subnet ID"
}

variable "app_config_key_vault_id" {
    type = string
    description = "Key Vault used for database user/pass"
}

variable "key_vault_id" {
    type = string
    description = "Key Vault used for data encryption"
}

variable "rsa_key_2048" {
    type = string
    description = "Name of the 2048 length RSA key in the Key Vault. Omitting will use Azure-managed key instead of a customer-key."
}

variable "is_metabase_env" {
    type = bool
    description = "Should Metabase be deployed in this environment"
}