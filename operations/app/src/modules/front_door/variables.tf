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

variable "eventhub_diagnostic_auth_rule_id" {
  type = string
  description = "Event Hub Authorization Rule ID"
}
