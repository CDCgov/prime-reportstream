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

variable "container_registry_login_server" {
  type = string
}

variable "container_registry_admin_username" {
  type = string
}

variable "container_registry_admin_password" {
  type      = string
  sensitive = true
}

variable "chatops_slack_bot_token" {
  type      = string
  sensitive = true
}

variable "chatops_slack_app_token" {
  type      = string
  sensitive = true
}

variable "chatops_github_token" {
  type      = string
  sensitive = true
}

variable "chatops_github_repo" {
  type = string
}

variable "chatops_github_target_branches" {
  type = string
}

variable "storage_account" {
}
