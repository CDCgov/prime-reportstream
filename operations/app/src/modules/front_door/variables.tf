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

variable "key_vault_id" {
  type = string
  description = "Key Vault used for HTTPS certificate"
}

variable "eventhub_namespace_name" {
  type = string
  description = "Event hub to stream logs to"
}

variable "eventhub_manage_auth_rule_id" {
  type = string
  description = "Event Hub Manage Authorization Rule ID"
}

variable "https_cert_names" {
  type = list
  description = "The HTTPS cert to associate with the front door. Omitting will not associate a domain to the front door."
}

variable "storage_web_endpoint" {
  type = string
  description = "Where the static site is located"
}
