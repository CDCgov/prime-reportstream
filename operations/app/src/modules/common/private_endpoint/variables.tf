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

variable "endpoint_subnet_id" {
  type        = string
  description = "Private Endpoint Subnet ID"
}

variable "create_dns_record" {
  type        = bool
  description = "If the private endpoint should be associated via DNS. For resources with multiple private endpoints, DNS can only be registered against a single VNET. If this is enabled against multiple VNETs, Azure will silently and unpredictably overwrite DNS records."
}