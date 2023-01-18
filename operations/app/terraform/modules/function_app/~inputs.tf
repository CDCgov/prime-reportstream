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
  type        = list(string)
  description = "The IP address of the Terraform script caller. This IP will have already been whitelisted; it's inclusion is to prevent its removal during terraform apply calls."
  sensitive   = true
}

variable "use_cdc_managed_vnet" {
  type        = bool
  description = "If the environment should be deployed to the CDC managed VNET"
}

variable "pagerduty_url" {}
variable "app_service_plan" {}
variable "storage_account" {}
variable "primary_access_key" {
  sensitive = true
}
variable "container_registry_login_server" {}
variable "container_registry_admin_username" {}
variable "container_registry_admin_password" {
  sensitive = true
}
variable "primary_connection_string" {
  sensitive = true
}
variable "postgres_user" {}
variable "postgres_pass" {
  sensitive = true
}
variable "application_key_vault_id" {}
variable "sa_partner_connection_string" {
  sensitive = true
}
variable "client_config_key_vault_id" {}
variable "app_config_key_vault_id" {}
variable "app_config_key_vault_name" {}
variable "dns_ip" {}
variable "okta_base_url" {}

variable "subnets" {
  description = "A set of all available subnet combinations"
}

variable "is_temp_env" {
  default     = false
  description = "Is a temporary environment. true or false"
}
variable "function_runtime_version" {
  type        = string
  description = "function app runtime version"
}
