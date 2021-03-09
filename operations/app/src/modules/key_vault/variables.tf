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
  description = "Network Location"
}

variable "public_subnet_id" {
  type = string
  description = "Public Subnet ID"
}

variable "private_subnet_id" {
  type = string
  description = "Private Subnet ID"
}

variable "gateway_subnet_id" {
  type = string
  description = "VPN Gateway Subnet ID"
}