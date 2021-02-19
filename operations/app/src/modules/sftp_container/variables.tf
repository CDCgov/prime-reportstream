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
    description = "Function App Location"
}

variable "container_subnet_id" {
    type = string
    description = "Container Subnet ID"
}

variable "storage_account_name" {
    type = string
    description = "Storage Account Name"
}

variable "storage_account_key" {
    type = string
    description = "Storage Account Key"
    sensitive = true
}
