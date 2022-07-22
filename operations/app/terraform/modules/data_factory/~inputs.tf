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
  description = "Function App Location"
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault resource id"
}

variable "terraform_caller_ip_address" {
  type        = list(string)
  description = "The IP address of the Terraform script caller. This IP will have already been whitelisted; it's inclusion is to prevent its removal during terraform apply calls."
  sensitive   = true
}

variable "sa_primary_connection_string" {
  sensitive = true
}

variable "storage_account_id" {}
variable "sftp_storage" {}
variable "sftp_shares" {}
