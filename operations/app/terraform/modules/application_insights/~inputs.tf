variable "environment" {
  type        = string
  description = "Target Environment"
}

variable "resource_group" {
  type        = string
  description = "Resource Group Name"
}

variable "location" {
  type        = string
  description = "Function App Location"
}

variable "resource_prefix" {
  type        = string
  description = "Resource Prefix"
}

variable "is_metabase_env" {
  type        = bool
  description = "Should Metabase be deployed in this environment"
}

variable "pagerduty_url" {}
variable "pagerduty_businesshours_url" {}
variable "postgres_server_id" {}
variable "service_plan_id" {}

variable "workspace_id" {
  type        = string
  description = "Log Analytics Workspace resource id"
}
