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
    description = "Storage Account Location"
}

variable "rsa_key_4096" {
    type = string
    description = "Name of the 2048 length RSA key in the Key Vault. Omitting will use Azure-managed key instead of a customer-key."
}
