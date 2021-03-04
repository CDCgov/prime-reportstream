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

variable "name" {
    type = string
    description = "Storage Account Name"
}

variable "location" {
    type = string
    description = "Storage Account Location"
}

variable "subnet_ids" {
    type = list(string)
    description = "List of VNet Subnet IDs"
}

variable "eventhub_namespace_name" {
    type = string
    description = "Event hub to stream logs to"
}

variable "eventhub_manage_auth_rule_id" {
    type = string
    description = "Event Hub Manage Authorization Rule ID"
}
variable "key_vault_id" {
    type = string
    description = "Key Vault used to encrypt blob storage"
}
