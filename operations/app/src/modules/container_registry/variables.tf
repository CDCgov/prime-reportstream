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
    description = "Container Registry Name"
}

variable "location" {
    type = string
    description = "Container Registry Location"
}

variable "public_subnet_id" {
    type = string
    description = "Public Subnet ID"
}

variable "endpoint_subnet_id" {
    type = string
    description = "Private Endpoint Subnet ID"
}
