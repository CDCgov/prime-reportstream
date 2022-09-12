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

variable "sftp_user_name" {
  description = "Username for the SFTP site"
  default     = "foo"
}

variable "sftp_password" {
  description = "Password for the SFTP site"
  default     = "bar"
}

variable "sftp_folder" {
  description = "SFTP folder name"
  default     = "default"
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

variable "nat_gateway_id" {
  type        = string
  description = "NAT gateway resource id"
}

variable "sshnames" {
  description = "SSH Names"
}

variable "sshinstances" {
  description = "SSH Instances"
}

variable "sftp_dir" {
  description = "SFTP Script Directory"
}
