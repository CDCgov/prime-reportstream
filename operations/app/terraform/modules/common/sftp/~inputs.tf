variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "location" {
  type        = string
  description = "Network Location"
}

variable "resource_prefix" {
  type = string
}

variable "environment" {
  type = string
}

variable "cpu" {
  type = number
}

variable "memory" {
  type = number
}

variable "users" {
  description = "Usernames"
}

variable "sshaccess" {
  description = "Usernames in SFTP access format"
}

variable "instance" {
  type        = string
  description = "Instance name"
}

variable "instance_users" {
  description = "Instances appended with usernames"
}

variable "sftp_folder" {
  type        = string
  description = "SFTP folder name"
}

variable "key_vault_id" {
  type        = string
  description = "Key Vault resource id"
}

variable "storage_account" {
  description = "Storage account to host file shares"
}

variable "admin_share" {
  description = "Admin file share"
}

variable "scripts_share" {
  description = "Startup scripts file share"
}

variable "nat_gateway_id" {
  type        = string
  description = "NAT gateway resource id"
}

variable "network_profile_id" {
  type        = string
  description = "Network profile resource id"
}

variable "subnet_id" {}
