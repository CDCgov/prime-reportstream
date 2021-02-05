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

variable "postgres_user" {
    type = string
    description = "Database Server Username"
}

variable "postgres_password" {
    type = string
    description = "Database Server Password"
}
