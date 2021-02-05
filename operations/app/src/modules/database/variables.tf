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

variable "postgres_user" {
    type = string
    description = "Database Server Username"
    sensitive = true
}

variable "postgres_password" {
    type = string
    description = "Database Server Password"
    sensitive = true
}

variable "public_subnet_id" {
    type = string
    description = "Public Subnet ID"
}
