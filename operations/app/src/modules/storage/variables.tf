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
    description = "Storage Account Name"
}

variable "location" {
    type = string
    description = "Storage Account Location"
}

variable "public_subnet_id" {
    type = string
    description = "Public Subnet ID"
}

variable "container_subnet_id" {
    type = string
    description = "Container Subnet ID"
}

variable "endpoint_subnet_id" {
    type = string
    description = "Private Endpoint Subnet ID"
}

variable "key_vault_id" {
    type = string
    description = "Key Vault used to encrypt blob storage"
}

variable "rsa_key_4096" {
    type = string
    description = "Name of the 2048 length RSA key in the Key Vault. Omitting will use Azure-managed key instead of a customer-key."
}
