variable "resource_id" {
  type        = string
  description = "ID of the resource for which an endpoint will be created"
}

variable "name" {
  type        = string
  description = "The name of the resource for which an endpoint will be created"
}

variable "type" {
  type        = string
  description = "Type of private endpoint to create. Options include: key_vault"
}

variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "location" {
  type        = string
  description = "Network Location"
}

variable "endpoint_subnet_ids" {
  type        = list(string)
  description = "Private Endpoint Subnet ID(s)"
}

variable "endpoint_subnet_id_for_dns" {
  type        = string
  description = "The endpoint the DNS record should point to"
}