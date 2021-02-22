variable "environment" {
    type = string
    description = "Target Environment"
}

variable "resource_group" {
    type = string
    description = "Resource Group Name"
}

variable "name" {
    type = string
    description = "Database Server Name"
}

variable "location" {
    type = string
    description = "Database Server Location"
}

variable "public_subnet_id" {
    type = string
    description = "Public Subnet ID"
}

variable "private_subnet_id" {
    type = string
    description = "Private Subnet ID"
}

variable "app_config_key_vault_id" {
    type = string
    description = "Key Vault used for database user/pass"
}
