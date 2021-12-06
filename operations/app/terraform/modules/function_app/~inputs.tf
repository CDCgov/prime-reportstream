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

variable "ai_instrumentation_key" {
  type        = string
  description = "Application Insights Instrumentation Key"
  sensitive   = true
}

variable "ai_connection_string" {
  type        = string
  description = "Application Insights Connection String"
  sensitive   = true
}

variable "okta_redirect_url" {
  type        = string
  description = "Okta Redirect URL"
}

variable "terraform_caller_ip_address" {
  type        = string
  description = "The IP address of the Terraform script caller. This IP will have already been whitelisted; it's inclusion is to prevent its removal during terraform apply calls."
  sensitive   = true
}

variable "use_cdc_managed_vnet" {
  type        = bool
  description = "If the environment should be deployed to the CDC managed VNET"
}

variable "public_subnet" {}
variable "pagerduty_url" {}
variable "app_service_plan" {}
variable "primary_access_key" {}
variable "container_registry_login_server" {}
variable "container_registry_admin_username" {}
variable "container_registry_admin_password" {}
variable "primary_connection_string" {}
variable "postgres_user" {}
variable "postgres_pass" {}
variable "application_key_vault_id" {}