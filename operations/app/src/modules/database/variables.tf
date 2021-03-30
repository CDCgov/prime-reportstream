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
    description = "Database Server Name"
}

variable "location" {
    type = string
    description = "Database Server Location"
}

variable "endpoint_subnet_id" {
    type = string
    description = "Private Endpoint Subnet ID"
}

variable "endpoint2_subnet_id" {
    type = string
    description = "Private Endpoint2 Subnet ID"
}

variable "app_config_key_vault_id" {
    type = string
    description = "Key Vault used for database user/pass"
}

variable "eventhub_namespace_name" {
    type = string
    description = "Event hub to stream logs to"
}

variable "eventhub_manage_auth_rule_id" {
    type = string
    description = "Event Hub Manage Authorization Rule ID"
}
