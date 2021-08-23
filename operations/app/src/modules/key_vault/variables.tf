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

variable "aad_object_keyvault_admin" {
  type        = string
  description = "Azure Active Directory ID for a user or group who will be given write access to Key Vaults"
}