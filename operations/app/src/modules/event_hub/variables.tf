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

variable "public_subnet_id" {
  type = string
  description = "Public Subnet ID"
}

variable "private_subnet_id" {
  type = string
  description = "Private Subnet ID"
}

variable "container_subnet_id" {
  type = string
  description = "Container Subnet ID"
}