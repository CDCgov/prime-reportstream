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
  description = "Azure Data Center"
}

variable "is_metabase_env" {
  type        = bool
  description = "If Metabase will be deployed in the environment"
  default     = true
}

variable "https_cert_names" {
  type = list(string)
  description = "List of SSL certs to associate with the Front Door"
}

variable "okta_redirect_url" {
  type = string
  description = "URL to redirect to after Okta login"
}