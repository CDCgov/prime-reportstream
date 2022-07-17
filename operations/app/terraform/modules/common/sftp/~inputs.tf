variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "location" {
  type        = string
  description = "Network Location"
}

variable "resource_prefix" {}
variable "environment" {}

variable "cpu" {
  type = number
}

variable "memory" {
  type = number
}

variable "users_file" {
  description = "User file path"
}

variable "sftp_folder" {
  description = "SFTP folder name"
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault resource id"
}

variable "storage_account" {
  description = "Storage account to host file shares"
}
