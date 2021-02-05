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
    description = "Function App Location"
}

variable "storage_account_name" {
    type = string
    description = "Storage Account Name"
}

variable "storage_account_key" {
    type = string
    description = "Storage Account Key"
}

variable "public_subnet_id" {
    type = string
    description = "Public Subnet ID"
}
