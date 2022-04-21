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
  description = "App Service Location"
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
variable "use_cdc_managed_vnet" {
  type        = bool
  description = "If the environment should be deployed to the CDC managed VNET"
}
variable "service_plan_id" {
  type        = string
  description = "Application Service Plan resource id"
}
variable "postgres_server_name" {
  type        = string
  description = "Postgres Server name"
}

variable "postgres_user" {}
variable "postgres_pass" {
  sensitive = true
}

variable "subnets" {
  description = "A set of all available subnet combinations"
}
