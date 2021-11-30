variable "environment" {
  type        = string
  description = "Target Environment"
}

variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "resource_prefix" {
  type        = string
  description = "Resource Prefix"
}

variable "location" {
  type        = string
  description = "Network Location"
}

variable "vnet_ids" {
  description = "Vnet IDs"
}

variable "vnet_names" {
  description = "Vnet Names"
}

variable "vnet_address_space" {
  description = "The address space of the newly created vNet"
}

variable "vnets" {
  description = "List of vnet objects"
}