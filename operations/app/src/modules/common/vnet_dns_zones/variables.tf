variable "resource_prefix" {
  type        = string
  description = "Resource Prefix"
}

variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "dns_zone_names" {
  type        = list(string)
  description = "List of DNS zone names, ex. ['privatelink.vaultcore.azure.net']"
}

variable "vnet" {
  type        = object({ id = string, name = string })
  description = "VNET to associate the DNS zones with"
}