variable "environment" {
    type = string
    description = "Target Environment"
}

variable "resource_group" {
    type = string
    description = "Resource Group Name"
}

variable "location" {
    type = string
    description = "Function App Location"
}

variable "resource_prefix" {
    type = string
    description = "Resource Prefix"
}

variable "key_vault_id" {
    type = string
    description = "Application Key Vault ID"
}
