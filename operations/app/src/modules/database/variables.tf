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

variable "location" {
    type = string
    description = "Database Server Location"
}

variable "rsa_key_2048" {
    type = string
    description = "Name of the 2048 length RSA key in the Key Vault. Omitting will use Azure-managed key instead of a customer-key."
}

variable "aad_group_postgres_admin" {
    type = string
    description = "Azure Active Directory Group ID for postgres_admin"
}

variable "is_metabase_env" {
    type = bool
    description = "Should Metabase be deployed in this environment"
}