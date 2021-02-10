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

variable "access_eventhub" {
  type = string
  description = "Access Log Event Hub Namespace"
}

variable "waf_eventhub" {
  type = string
  description = "WAF Log Event Hub Namespace"
}

variable "auth_rule" {
  type = string
  description = "Authorization Rule for Event Hub"
}
